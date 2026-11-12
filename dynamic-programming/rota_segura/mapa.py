"""Mapa fixo do serviço: esquinas com coordenadas, trechos e a geometria que liga o mundo real ao grafo.

Formato do arquivo (GRAFO_PATH), JSON:
    {"esquinas": [{"id": 1, "lat": -23.489, "lng": -46.412}, ...],
     "trechos":  [{"origem": 1, "destino": 2, "distanciaM": 105.0, "fatorRisco": 0.2}, ...]}
`distanciaM` é opcional (se ausente vale o haversine entre as pontas) e `fatorRisco` também (padrão 0, faixa 0 a 1).
"""

import json
import math
from collections.abc import Sequence
from dataclasses import dataclass
from pathlib import Path

from rota_segura.modelos import Bairro, EntradaInvalidaError, Trecho, chave_trecho

RAIO_TERRA_M = 6_371_008.8
LIMITE_ORIGEM_M = 300.0  # origem mais longe que isso da esquina mais próxima está fora do bairro-piloto


@dataclass(frozen=True, slots=True)
class Ponto:
    lat: float
    lng: float


@dataclass(frozen=True, slots=True)
class AreaAlagada:
    area_id: int
    centro: Ponto
    raio_m: float


@dataclass(frozen=True, slots=True)
class Mapa:
    coordenadas: dict[int, Ponto]
    bairro: Bairro


def validar_ponto(lat: float, lng: float, onde: str) -> Ponto:
    if not (-90.0 <= lat <= 90.0) or not (-180.0 <= lng <= 180.0):
        raise EntradaInvalidaError(f"{onde}: coordenada fora da faixa (lat {lat}, lng {lng})")
    return Ponto(lat, lng)


def haversine_m(a: Ponto, b: Ponto) -> float:
    """Distância em metros sobre a esfera. Tempo e memória O(1)."""
    lat_a, lat_b = math.radians(a.lat), math.radians(b.lat)
    d_lat = lat_b - lat_a
    d_lng = math.radians(b.lng - a.lng)
    h = math.sin(d_lat / 2) ** 2 + math.cos(lat_a) * math.cos(lat_b) * math.sin(d_lng / 2) ** 2
    return 2 * RAIO_TERRA_M * math.asin(min(1.0, math.sqrt(h)))


def esquina_mais_proxima(mapa: Mapa, ponto: Ponto) -> tuple[int, float]:
    """(id da esquina, distância em m). Empate: menor id. Tempo O(V), memória O(1)."""
    melhor = min(mapa.coordenadas, key=lambda e: (haversine_m(ponto, mapa.coordenadas[e]), e))
    return melhor, haversine_m(ponto, mapa.coordenadas[melhor])


def ponto_medio(a: Ponto, b: Ponto) -> Ponto:
    """Média das coordenadas: suficiente para trechos de poucas centenas de metros."""
    return Ponto((a.lat + b.lat) / 2, (a.lng + b.lng) / 2)


def trechos_alagados(mapa: Mapa, areas: Sequence[AreaAlagada]) -> frozenset[tuple[int, int]]:
    """RN05: o trecho sai do grafo se uma ponta ou o ponto médio estiver a até raio_m do centro de alguma área.

    Tempo O(E·A), memória O(E).
    """
    alagados: set[tuple[int, int]] = set()
    for trecho in mapa.bairro.trechos:
        a, b = mapa.coordenadas[trecho.origem], mapa.coordenadas[trecho.destino]
        pontos = (a, b, ponto_medio(a, b))
        if any(haversine_m(area.centro, p) <= area.raio_m for area in areas for p in pontos):
            alagados.add(chave_trecho(trecho.origem, trecho.destino))
    return frozenset(alagados)


def _numero(valor: object, onde: str) -> float:
    if isinstance(valor, bool) or not isinstance(valor, (int, float)) or not math.isfinite(valor):
        raise EntradaInvalidaError(f"{onde}: esperado um número, recebido {valor!r}")
    return float(valor)


def _inteiro(valor: object, onde: str) -> int:
    if isinstance(valor, bool) or not isinstance(valor, int):
        raise EntradaInvalidaError(f"{onde}: esperado um inteiro, recebido {valor!r}")
    return valor


def _lista(valor: object, onde: str) -> list[object]:
    if not isinstance(valor, list):
        raise EntradaInvalidaError(f"{onde}: esperado uma lista, recebido {valor!r}")
    return valor


def _objeto(valor: object, onde: str) -> dict[str, object]:
    if not isinstance(valor, dict):
        raise EntradaInvalidaError(f"{onde}: esperado um objeto JSON, recebido {valor!r}")
    return valor


def mapa_de_dados(dados: object) -> Mapa:
    """Valida o JSON do mapa. Todo dado inválido levanta EntradaInvalidaError com o campo."""
    raiz = _objeto(dados, "mapa")
    coordenadas: dict[int, Ponto] = {}
    for i, item in enumerate(_lista(raiz.get("esquinas"), "mapa.esquinas")):
        onde = f"mapa.esquinas[{i}]"
        dados_esquina = _objeto(item, onde)
        identificador = _inteiro(dados_esquina.get("id"), f"{onde}.id")
        if identificador in coordenadas:
            raise EntradaInvalidaError(f"{onde}.id: esquina {identificador} repetida")
        coordenadas[identificador] = validar_ponto(
            _numero(dados_esquina.get("lat"), f"{onde}.lat"), _numero(dados_esquina.get("lng"), f"{onde}.lng"), onde
        )
    if not coordenadas:
        raise EntradaInvalidaError("mapa.esquinas: o mapa precisa de ao menos uma esquina")

    trechos: list[Trecho] = []
    vistos: set[tuple[int, int]] = set()
    for i, item in enumerate(_lista(raiz.get("trechos"), "mapa.trechos")):
        onde = f"mapa.trechos[{i}]"
        dados_trecho = _objeto(item, onde)
        origem = _inteiro(dados_trecho.get("origem"), f"{onde}.origem")
        destino = _inteiro(dados_trecho.get("destino"), f"{onde}.destino")
        for nome, esquina in (("origem", origem), ("destino", destino)):
            if esquina not in coordenadas:
                raise EntradaInvalidaError(f"{onde}.{nome}: esquina {esquina} não existe no mapa")
        if origem == destino:
            raise EntradaInvalidaError(f"{onde}: origem e destino iguais ({origem})")
        chave = chave_trecho(origem, destino)
        if chave in vistos:
            raise EntradaInvalidaError(f"{onde}: trecho {origem}-{destino} repetido")
        vistos.add(chave)
        if dados_trecho.get("distanciaM") is None:
            distancia = haversine_m(coordenadas[origem], coordenadas[destino])
        else:
            distancia = _numero(dados_trecho["distanciaM"], f"{onde}.distanciaM")
        if distancia <= 0:
            raise EntradaInvalidaError(f"{onde}.distanciaM: deve ser maior que zero, recebido {distancia}")
        fator = 0.0 if dados_trecho.get("fatorRisco") is None else _numero(dados_trecho["fatorRisco"], f"{onde}.fatorRisco")
        if not 0.0 <= fator <= 1.0:
            raise EntradaInvalidaError(f"{onde}.fatorRisco: deve estar entre 0 e 1, recebido {fator}")
        trechos.append(Trecho(origem, destino, distancia, fator))
    return Mapa(coordenadas, Bairro(frozenset(coordenadas), tuple(trechos)))


def carregar_mapa(caminho: Path) -> Mapa:
    try:
        texto = caminho.read_text(encoding="utf-8")
    except OSError as erro:
        raise EntradaInvalidaError(f"não foi possível abrir '{caminho}': {erro.strerror}") from erro
    try:
        return mapa_de_dados(json.loads(texto))
    except json.JSONDecodeError as erro:
        raise EntradaInvalidaError(f"'{caminho}' não é um JSON válido: {erro}") from erro
