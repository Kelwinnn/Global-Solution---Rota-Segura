"""Agrega o cache do CGE em dados/cge_diario.csv (uma linha por data).

Colunas: data, n_pontos_extremo_leste, alagou_extremo_leste, n_pontos_zona_leste,
alagou_zona_leste, pagina_ok. Página faltante => contagens vazias (NaN) e pagina_ok=False,
nunca zero. Também escreve dados/cge/pontos.csv (um ponto por linha) e dados/cge/faltantes.csv.

Uso: python -m rota_dados.cge_agregacao --inicio 2016-01-01 --fim 2025-12-31
"""

from __future__ import annotations

import argparse
import sys
from collections import Counter
from collections.abc import Sequence
from dataclasses import asdict, dataclass
from datetime import date, datetime
from pathlib import Path

import pandas as pd

from rota_dados.cge_coletor import PASTA_RAW_PADRAO, caminho_cache, intervalo_datas
from rota_dados.cge_parser import (
    Ponto,
    e_extremo_leste,
    e_zona_leste,
    nome_canonico,
    parsear_pagina,
)
from rota_dados.erros import EntradaInvalidaError

MOTIVO_SEM_CACHE = "sem_cache"
LIMITE_FALTANTES = 0.02


@dataclass(frozen=True)
class Agregado:
    diario: pd.DataFrame
    pontos: pd.DataFrame
    faltantes: pd.DataFrame
    repetidos_meia_noite: list[tuple[Ponto, Ponto]]


def ler_html(caminho: Path) -> str:
    dados = caminho.read_bytes()
    try:
        return dados.decode("utf-8")
    except UnicodeDecodeError:
        return dados.decode("latin-1")  # a página do CGE é ISO-8859-1


def _chave(p: Ponto) -> tuple[str, str, str, str, str, str]:
    return (
        nome_canonico(p.subprefeitura),
        p.logradouro,
        p.sentido,
        p.referencia,
        p.hora_inicio,
        p.hora_fim,
    )


def achar_repetidos_meia_noite(
    pontos_por_dia: dict[date, list[Ponto]],
) -> list[tuple[Ponto, Ponto]]:
    """Pontos que cruzam a meia-noite (hora_fim < hora_inicio) e reaparecem no dia seguinte."""
    repetidos: list[tuple[Ponto, Ponto]] = []
    for dia, pontos in sorted(pontos_por_dia.items()):
        seguinte = pontos_por_dia.get(date.fromordinal(dia.toordinal() + 1))
        if not seguinte:
            continue
        chaves = {_chave(p): p for p in seguinte}
        for p in pontos:
            if p.hora_fim < p.hora_inicio and _chave(p) in chaves:
                repetidos.append((p, chaves[_chave(p)]))
    return repetidos


def agregar(inicio: date, fim: date, pasta_raw: Path = PASTA_RAW_PADRAO) -> Agregado:
    linhas: list[dict[str, object]] = []
    faltantes: list[dict[str, object]] = []
    todos: list[Ponto] = []
    por_dia: dict[date, list[Ponto]] = {}
    for dia in intervalo_datas(inicio, fim):
        arquivo = caminho_cache(pasta_raw, dia)
        if not arquivo.exists():
            motivo: str | None = MOTIVO_SEM_CACHE
            pontos: list[Ponto] = []
        else:
            resultado = parsear_pagina(ler_html(arquivo), dia)
            motivo, pontos = resultado.motivo, resultado.pontos
        if motivo is not None:
            faltantes.append({"data": dia.isoformat(), "motivo": motivo})
            linhas.append(
                {
                    "data": dia.isoformat(),
                    "n_pontos_extremo_leste": pd.NA,
                    "alagou_extremo_leste": pd.NA,
                    "n_pontos_zona_leste": pd.NA,
                    "alagou_zona_leste": pd.NA,
                    "pagina_ok": False,
                }
            )
            continue
        n_extremo = sum(1 for p in pontos if e_extremo_leste(p.subprefeitura))
        n_zona = sum(1 for p in pontos if e_zona_leste(p.subprefeitura))
        linhas.append(
            {
                "data": dia.isoformat(),
                "n_pontos_extremo_leste": n_extremo,
                "alagou_extremo_leste": int(n_extremo > 0),
                "n_pontos_zona_leste": n_zona,
                "alagou_zona_leste": int(n_zona > 0),
                "pagina_ok": True,
            }
        )
        todos.extend(pontos)
        por_dia[dia] = pontos
    diario = pd.DataFrame(linhas).astype(
        {
            "n_pontos_extremo_leste": "Int64",
            "alagou_extremo_leste": "Int64",
            "n_pontos_zona_leste": "Int64",
            "alagou_zona_leste": "Int64",
        }
    )
    colunas_pontos = [
        "data", "zona", "subprefeitura", "hora_inicio", "hora_fim", "logradouro",
        "sentido", "referencia", "atividade", "transito",
    ]  # fmt: skip
    pontos_df = pd.DataFrame([asdict(p) for p in todos], columns=colunas_pontos)
    faltantes_df = pd.DataFrame(faltantes, columns=["data", "motivo"])
    return Agregado(diario, pontos_df, faltantes_df, achar_repetidos_meia_noite(por_dia))


def subprefeituras_nao_classificadas(pontos: pd.DataFrame) -> Counter[str]:
    """Nomes de subprefeitura vistos que não estão na Zona Leste (para conferência)."""
    nomes: Counter[str] = Counter()
    for nome in pontos["subprefeitura"]:
        if not e_zona_leste(str(nome)):
            nomes[nome_canonico(str(nome))] += 1
    return nomes


def relatorio_final(ag: Agregado) -> str:
    total = len(ag.diario)
    n_falt = len(ag.faltantes)
    linhas = [f"dias: {total} | páginas faltantes: {n_falt} ({n_falt / total:.2%})"]
    for motivo, n in Counter(ag.faltantes["motivo"]).most_common():
        linhas.append(f"  {motivo}: {n}")
    if total and n_falt / total > LIMITE_FALTANTES:
        linhas.append(f"ATENÇÃO: faltantes passaram de {LIMITE_FALTANTES:.0%} dos dias; avise antes de seguir.")
    linhas.append(
        f"pontos lidos: {len(ag.pontos)} | dias com alagamento no Extremo Leste: "
        f"{int(ag.diario['alagou_extremo_leste'].sum())} | na Zona Leste: "
        f"{int(ag.diario['alagou_zona_leste'].sum())}"
    )
    linhas.append(f"pontos que cruzam a meia-noite e reaparecem no dia seguinte: {len(ag.repetidos_meia_noite)}")
    if ag.repetidos_meia_noite:
        a, b = ag.repetidos_meia_noite[0]
        linhas.append(
            f"  exemplo: {a.data} e {b.data}, {a.subprefeitura}, {a.logradouro}, "
            f"{a.hora_inicio} a {a.hora_fim} (nos dois dias; contado nos dois)"
        )
    return "\n".join(linhas)


def gravar(ag: Agregado, saida: Path, pasta_cge: Path) -> None:
    saida.parent.mkdir(parents=True, exist_ok=True)
    pasta_cge.mkdir(parents=True, exist_ok=True)
    ag.diario.to_csv(saida, index=False)
    ag.pontos.to_csv(pasta_cge / "pontos.csv", index=False)
    ag.faltantes.to_csv(pasta_cge / "faltantes.csv", index=False)


def _data(texto: str) -> date:
    try:
        return datetime.strptime(texto, "%Y-%m-%d").date()
    except ValueError as erro:
        raise argparse.ArgumentTypeError(f"data inválida '{texto}' (use AAAA-MM-DD)") from erro


def main(argumentos: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Agrega o cache do CGE em CSV diário.")
    parser.add_argument("--inicio", type=_data, default=date(2016, 1, 1))
    parser.add_argument("--fim", type=_data, default=date(2025, 12, 31))
    parser.add_argument("--raw", type=Path, default=PASTA_RAW_PADRAO)
    parser.add_argument("--saida", type=Path, default=Path("dados/cge_diario.csv"))
    args = parser.parse_args(argumentos)
    try:
        ag = agregar(args.inicio, args.fim, args.raw)
    except EntradaInvalidaError as erro:
        print(f"erro: {erro}", file=sys.stderr)
        return 1
    gravar(ag, args.saida, args.raw.parent)
    print(relatorio_final(ag))
    desconhecidas = subprefeituras_nao_classificadas(ag.pontos)
    if desconhecidas:
        print("subprefeituras fora da Zona Leste (confira se algum nome é variação):")
        for nome, n in sorted(desconhecidas.items()):
            print(f"  {nome}: {n}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
