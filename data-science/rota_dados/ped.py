"""Relatório de cobertura do PED (Cemaden): uma estação por arquivo CSV.

Usa só o sensor 10. Uma leitura pode aparecer repetida; por isso tudo é
contado sobre instantes (timestamps) únicos.

Uso: python -m rota_dados.ped dados/ped/PED_Cemaden_*.csv
"""

from __future__ import annotations

import sys
from collections.abc import Sequence
from dataclasses import dataclass
from datetime import date
from pathlib import Path

import pandas as pd

from rota_dados.erros import EntradaInvalidaError

ID_SENSOR = 10
COLUNAS_OBRIGATORIAS = ("cidade", "codestacao", "datahora", "id_sensor", "nome", "valor")
HORAS_MINIMAS_DIA = 20  # dia_com_dado: pelo menos 20 horas distintas com leitura


@dataclass(frozen=True)
class Lacuna:
    """Sequência de dias consecutivos sem nenhuma leitura."""

    inicio: date
    fim: date
    dias: int


@dataclass(frozen=True)
class RelatorioPed:
    arquivo: str
    estacao: str
    municipio: str
    nome: str
    primeiro_instante: pd.Timestamp
    ultimo_instante: pd.Timestamp
    n_linhas_sensor: int
    n_instantes_unicos: int
    intervalo_modal_min: float
    dias_no_periodo: int
    dias_com_dado: int
    dias_parcial: int  # só o primeiro e o último dia da série
    dias_sem_dado: int
    histograma_intervalos: dict[float, int]
    lacunas: list[Lacuna]


def carregar_ped(caminho: Path) -> pd.DataFrame:
    """Lê o CSV do PED e valida colunas e datas."""
    if not caminho.is_file():
        raise EntradaInvalidaError(f"arquivo não encontrado: '{caminho}'")
    try:
        tabela = pd.read_csv(caminho)
    except (OSError, pd.errors.ParserError, pd.errors.EmptyDataError, UnicodeDecodeError) as erro:
        raise EntradaInvalidaError(f"não foi possível ler '{caminho}': {erro}") from erro
    faltando = [c for c in COLUNAS_OBRIGATORIAS if c not in tabela.columns]
    if faltando:
        raise EntradaInvalidaError(f"'{caminho}': colunas ausentes {faltando}")
    try:
        tabela["datahora"] = pd.to_datetime(tabela["datahora"], format="%Y-%m-%d %H:%M:%S")
    except (ValueError, TypeError) as erro:
        raise EntradaInvalidaError(
            f"'{caminho}': datahora fora do formato AAAA-MM-DD HH:MM:SS"
        ) from erro
    return tabela


def histograma_intervalos(instantes: pd.Series) -> dict[float, int]:
    """Contagem de intervalos (em minutos) entre instantes únicos consecutivos."""
    unicos = instantes.drop_duplicates().sort_values()
    minutos = unicos.diff().dropna().dt.total_seconds() / 60.0
    contagem = minutos.value_counts().sort_index()
    return {float(k): int(v) for k, v in contagem.items()}


def dias_com_dado(instantes: pd.Series) -> pd.DataFrame:
    """Uma linha por dia com leitura: data, n_horas (distintas) e dia_com_dado (>= 20 horas)."""
    unicos = instantes.drop_duplicates()
    dia = unicos.dt.normalize()
    horas = unicos.dt.floor("h")
    n_horas = horas.groupby(dia).nunique().sort_index()
    dias = pd.DataFrame({"data": n_horas.index, "n_horas": n_horas.to_numpy()})
    dias["dia_com_dado"] = dias["n_horas"] >= HORAS_MINIMAS_DIA
    return dias


def encontrar_lacunas(datas_com_dado: Sequence[pd.Timestamp]) -> list[Lacuna]:
    """Dias consecutivos sem dia_com_dado entre o primeiro e o último dia com dado."""
    lacunas: list[Lacuna] = []
    ordenadas = sorted(datas_com_dado)
    for anterior, atual in zip(ordenadas, ordenadas[1:]):
        salto = (atual - anterior).days
        if salto > 1:
            inicio = (anterior + pd.Timedelta(days=1)).date()
            fim = (atual - pd.Timedelta(days=1)).date()
            lacunas.append(Lacuna(inicio=inicio, fim=fim, dias=salto - 1))
    return lacunas


def gerar_relatorio(caminho: Path) -> RelatorioPed:
    tabela = carregar_ped(caminho)
    sensor = tabela[tabela["id_sensor"] == ID_SENSOR]
    if sensor.empty:
        raise EntradaInvalidaError(f"'{caminho}': nenhuma linha do sensor {ID_SENSOR}")
    primeiro = sensor.iloc[0]  # primeira linha do arquivo, na ordem original
    instantes = sensor["datahora"]
    histograma = histograma_intervalos(instantes)
    if not histograma:
        raise EntradaInvalidaError(f"'{caminho}': sensor {ID_SENSOR} tem um único instante")
    modal = max(histograma, key=lambda m: histograma[m])
    dias = dias_com_dado(instantes)
    todas = list(dias["data"])
    periodo = (todas[-1] - todas[0]).days + 1
    com_dado = list(dias.loc[dias["dia_com_dado"], "data"])
    n_com_dado = len(com_dado)
    unicos = instantes.drop_duplicates()
    return RelatorioPed(
        arquivo=caminho.name,
        estacao=str(primeiro["codestacao"]),
        municipio=str(primeiro["cidade"]),
        nome=str(primeiro["nome"]),
        primeiro_instante=unicos.min(),
        ultimo_instante=unicos.max(),
        n_linhas_sensor=len(sensor),
        n_instantes_unicos=len(unicos),
        intervalo_modal_min=modal,
        dias_no_periodo=periodo,
        dias_com_dado=n_com_dado,
        dias_parcial=2 if periodo > 1 else 1,
        dias_sem_dado=periodo - n_com_dado,
        histograma_intervalos=histograma,
        lacunas=encontrar_lacunas(com_dado),
    )


def formatar_relatorio(r: RelatorioPed, maximo_intervalos: int = 12) -> str:
    linhas = [
        f"== {r.arquivo} ==",
        f"estação: {r.estacao} | município: {r.municipio} | nome: {r.nome}",
        f"sensor {ID_SENSOR}: {r.n_linhas_sensor} linhas, {r.n_instantes_unicos} instantes únicos",
        f"período: {r.primeiro_instante} a {r.ultimo_instante} ({r.dias_no_periodo} dias)",
        f"intervalo mais comum: {r.intervalo_modal_min:g} min",
        f"dias com dado (>= {HORAS_MINIMAS_DIA} horas distintas): {r.dias_com_dado} | "
        f"dias sem dado: {r.dias_sem_dado} | dias parciais (1º e último da série): {r.dias_parcial}",
        "histograma de intervalos (min: contagem):",
    ]
    itens = sorted(r.histograma_intervalos.items(), key=lambda kv: -kv[1])
    for minutos, n in itens[:maximo_intervalos]:
        linhas.append(f"  {minutos:g}: {n}")
    if len(itens) > maximo_intervalos:
        resto = sum(n for _, n in itens[maximo_intervalos:])
        linhas.append(f"  outros ({len(itens) - maximo_intervalos} valores): {resto}")
    linhas.append(f"lacunas (dias seguidos sem dado): {len(r.lacunas)}")
    for lacuna in sorted(r.lacunas, key=lambda x: -x.dias)[:15]:
        linhas.append(f"  {lacuna.inicio} a {lacuna.fim}: {lacuna.dias} dias")
    return "\n".join(linhas)


def main(argumentos: Sequence[str] | None = None) -> int:
    caminhos = [Path(a) for a in (sys.argv[1:] if argumentos is None else argumentos)]
    if not caminhos:
        print("uso: python -m rota_dados.ped ARQUIVO.csv [ARQUIVO.csv ...]", file=sys.stderr)
        return 2
    try:
        for caminho in caminhos:
            print(formatar_relatorio(gerar_relatorio(caminho)))
            print()
    except EntradaInvalidaError as erro:
        print(f"erro: {erro}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
