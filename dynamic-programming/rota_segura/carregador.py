"""Leitura de bairro.json e cenario.json com validação na entrada.

Todo dado inválido falha aqui, com EntradaInvalidaError e mensagem clara,
e nunca no meio do algoritmo.
"""

import json
import math
from pathlib import Path
from typing import Any

from rota_segura.modelos import (
    Abrigo,
    Bairro,
    Cenario,
    EntradaInvalidaError,
    Trecho,
    chave_trecho,
)

# Os custos são comparados em milímetros inteiros (ver grafo.py), então 1 mm é
# a menor distância representável.
DISTANCIA_MINIMA_M = 0.001
FATOR_RISCO_MAXIMO = 1.0


def _ler_json(caminho: Path) -> Any:
    try:
        with caminho.open(encoding="utf-8") as arquivo:
            return json.load(arquivo)
    except OSError as erro:
        raise EntradaInvalidaError(f"não foi possível abrir '{caminho}': {erro.strerror}") from erro
    except json.JSONDecodeError as erro:
        raise EntradaInvalidaError(f"'{caminho}' não é um JSON válido: {erro}") from erro


def _objeto(valor: Any, onde: str) -> dict[str, Any]:
    if not isinstance(valor, dict):
        raise EntradaInvalidaError(f"{onde}: esperado um objeto JSON, recebido {valor!r}")
    return valor


def _lista(valor: Any, onde: str) -> list[Any]:
    if not isinstance(valor, list):
        raise EntradaInvalidaError(f"{onde}: esperada uma lista, recebido {valor!r}")
    return valor


def _campo(dados: dict[str, Any], chave: str, onde: str) -> Any:
    if chave not in dados:
        raise EntradaInvalidaError(f"{onde}: campo obrigatório '{chave}' ausente")
    return dados[chave]


def _inteiro(valor: Any, onde: str) -> int:
    if isinstance(valor, bool) or not isinstance(valor, int):
        raise EntradaInvalidaError(f"{onde}: esperado número inteiro, recebido {valor!r}")
    return valor


def _numero(valor: Any, onde: str) -> float:
    if isinstance(valor, bool) or not isinstance(valor, (int, float)) or not math.isfinite(valor):
        raise EntradaInvalidaError(f"{onde}: esperado número finito, recebido {valor!r}")
    return float(valor)


def _esquina(valor: Any, esquinas: frozenset[int], onde: str) -> int:
    esquina = _inteiro(valor, onde)
    if esquina not in esquinas:
        raise EntradaInvalidaError(f"{onde}: esquina {esquina} não existe no bairro")
    return esquina


def carregar_bairro(caminho: str | Path) -> Bairro:
    """Lê e valida o mapa: esquinas e trechos (distância em metros e fator de risco)."""
    return bairro_de_dados(_ler_json(Path(caminho)))


def bairro_de_dados(bruto: Any) -> Bairro:
    """Valida o mapa já lido (dicionário com 'esquinas' e 'trechos'). Usado pelo arquivo e pela API."""
    dados = _objeto(bruto, "bairro")
    esquinas = frozenset(
        _inteiro(e, "bairro.esquinas") for e in _lista(_campo(dados, "esquinas", "bairro"), "bairro.esquinas")
    )
    trechos: list[Trecho] = []
    for i, bruto in enumerate(_lista(_campo(dados, "trechos", "bairro"), "bairro.trechos")):
        onde = f"bairro.trechos[{i}]"
        item = _objeto(bruto, onde)
        origem = _esquina(_campo(item, "origem", onde), esquinas, f"{onde}.origem")
        destino = _esquina(_campo(item, "destino", onde), esquinas, f"{onde}.destino")
        distancia = _numero(_campo(item, "distancia_m", onde), f"{onde}.distancia_m")
        if distancia < DISTANCIA_MINIMA_M:
            raise EntradaInvalidaError(
                f"{onde}.distancia_m: a distância deve ser maior que zero (mínimo 1 mm), recebido {distancia}"
            )
        fator = _numero(item.get("fator_risco", 0.0), f"{onde}.fator_risco")
        if fator < 0:
            raise EntradaInvalidaError(f"{onde}.fator_risco: o fator de risco não pode ser negativo, recebido {fator}")
        if fator > FATOR_RISCO_MAXIMO:
            raise EntradaInvalidaError(f"{onde}.fator_risco: o fator de risco deve ser no máximo 1, recebido {fator}")
        trechos.append(Trecho(origem, destino, distancia, fator))
    return Bairro(esquinas, tuple(trechos))


def carregar_cenario(caminho: str | Path, bairro: Bairro) -> Cenario:
    """Lê e valida o cenário (trechos alagados e abrigos) contra o bairro já carregado."""
    return cenario_de_dados(_ler_json(Path(caminho)), bairro)


def cenario_de_dados(bruto: Any, bairro: Bairro) -> Cenario:
    """Valida o cenário já lido contra o bairro. Usado pelo arquivo e pela API."""
    dados = _objeto(bruto, "cenario")
    trechos_existentes = {chave_trecho(t.origem, t.destino) for t in bairro.trechos}

    alagados: set[tuple[int, int]] = set()
    for i, bruto in enumerate(_lista(_campo(dados, "trechos_alagados", "cenario"), "cenario.trechos_alagados")):
        onde = f"cenario.trechos_alagados[{i}]"
        par = _lista(bruto, onde)
        if len(par) != 2:
            raise EntradaInvalidaError(f"{onde}: esperado par [origem, destino], recebido {par!r}")
        chave = chave_trecho(_esquina(par[0], bairro.esquinas, onde), _esquina(par[1], bairro.esquinas, onde))
        if chave not in trechos_existentes:
            raise EntradaInvalidaError(f"{onde}: não existe trecho entre as esquinas {chave[0]} e {chave[1]}")
        alagados.add(chave)

    abrigos: list[Abrigo] = []
    for i, bruto in enumerate(_lista(_campo(dados, "abrigos", "cenario"), "cenario.abrigos")):
        onde = f"cenario.abrigos[{i}]"
        item = _objeto(bruto, onde)
        esquina = _esquina(_campo(item, "esquina", onde), bairro.esquinas, f"{onde}.esquina")
        capacidade = _inteiro(_campo(item, "capacidade", onde), f"{onde}.capacidade")
        if capacidade < 0:
            raise EntradaInvalidaError(f"{onde}.capacidade: a capacidade não pode ser negativa, recebido {capacidade}")
        ocupacao = _inteiro(_campo(item, "ocupacao", onde), f"{onde}.ocupacao")
        if ocupacao < 0:
            raise EntradaInvalidaError(f"{onde}.ocupacao: a ocupação não pode ser negativa, recebido {ocupacao}")
        abrigos.append(Abrigo(esquina, capacidade, ocupacao))

    return Cenario(frozenset(alagados), tuple(abrigos))
