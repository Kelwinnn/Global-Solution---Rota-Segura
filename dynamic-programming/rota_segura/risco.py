"""Probabilidade de alagamento (/risk): variáveis diárias de chuva + modelo logístico treinado em data-science/.

A lógica das variáveis é a da seção 1 de data-science/02_modelos.ipynb:
 - soma por hora no fuso America/Sao_Paulo; hora sem leitura fica vazia (falha da estação, não chuva zero);
 - chuva_24h, chuva_max_1h, chuva_max_3h (soma móvel de 3 h, mínimo 3 horas) do dia da última leitura;
 - chuva_dia_anterior; chuva_72h (dia + 2 anteriores, mínimo 3 dias); chuva_7d_anteriores (7 dias antes, mínimo 5);
 - mes_sen e mes_cos.
Variável que não pôde ser calculada vira 0 mm e marca lowData (isso SUBESTIMA a chuva).
O modelo é um arquivo joblib (pickle): só carregue arquivos de confiança.
"""

import logging
import math
from collections.abc import Sequence
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from zoneinfo import ZoneInfo

import joblib
import numpy as np
import pandas as pd
import sklearn

from rota_segura.modelos import EntradaInvalidaError

registro = logging.getLogger("rota_segura.risco")

FUSO = ZoneInfo("America/Sao_Paulo")
DIAS_MINIMOS = 8  # dia atual + 7 anteriores
VARIAVEIS_CONHECIDAS = (
    "chuva_24h",
    "chuva_max_1h",
    "chuva_max_3h",
    "chuva_dia_anterior",
    "chuva_72h",
    "chuva_7d_anteriores",
    "mes_sen",
    "mes_cos",
)


@dataclass(frozen=True, slots=True)
class Leitura:
    instante: datetime  # com fuso
    chuva_mm: float
    nivel_cm: float


@dataclass(frozen=True, slots=True)
class ModeloRisco:
    pipeline: Any  # sklearn.pipeline.Pipeline treinado em DataFrame
    variaveis: tuple[str, ...]
    limite: float
    versao: str


@dataclass(frozen=True, slots=True)
class ResultadoRisco:
    probabilidade: float
    alta: bool
    poucos_dados: bool


@dataclass(frozen=True, slots=True)
class VariaveisDoDia:
    valores: dict[str, float]
    dias_com_leitura: int  # dos 8 dias que terminam no dia da última leitura
    completo: bool  # False se alguma variável precisou virar 0 mm


def validar_leituras(leituras: Sequence[Leitura]) -> None:
    """Lista não vazia, com fuso, em ordem crescente, chuva e nível sem valor negativo."""
    if not leituras:
        raise EntradaInvalidaError("readings: a lista de leituras está vazia")
    anterior: datetime | None = None
    for i, leitura in enumerate(leituras):
        onde = f"readings[{i}]"
        if leitura.instante.tzinfo is None or leitura.instante.utcoffset() is None:
            raise EntradaInvalidaError(f"{onde}.timestamp: falta o fuso horário (ex.: -03:00)")
        if not math.isfinite(leitura.chuva_mm) or leitura.chuva_mm < 0:
            raise EntradaInvalidaError(f"{onde}.rainMm: deve ser um número maior ou igual a zero")
        if not math.isfinite(leitura.nivel_cm) or leitura.nivel_cm < 0:
            raise EntradaInvalidaError(f"{onde}.waterLevelCm: deve ser um número maior ou igual a zero")
        if anterior is not None and leitura.instante < anterior:
            raise EntradaInvalidaError(f"{onde}.timestamp: leituras fora de ordem crescente")
        anterior = leitura.instante


def variaveis_do_dia(leituras: Sequence[Leitura]) -> VariaveisDoDia:
    """Variáveis do dia da última leitura. Tempo O(n log n) nas leituras, memória O(n)."""
    validar_leituras(leituras)
    indice = pd.DatetimeIndex([pd.Timestamp(l.instante.astimezone(FUSO)) for l in leituras])
    chuva = pd.Series([l.chuva_mm for l in leituras], index=indice)
    chuva = chuva[~chuva.index.duplicated(keep="first")].sort_index()

    por_hora = chuva.resample("1h").sum(min_count=1)
    max_3h = por_hora.rolling(3, min_periods=3).sum()
    dia = pd.DataFrame(
        {
            "chuva_24h": por_hora.resample("1D").sum(min_count=1),
            "chuva_max_1h": por_hora.resample("1D").max(),
            "chuva_max_3h": max_3h.resample("1D").max(),
        }
    )
    dia.index = pd.DatetimeIndex(dia.index).tz_localize(None).normalize()
    dia["chuva_dia_anterior"] = dia["chuva_24h"].shift(1)
    dia["chuva_72h"] = dia["chuva_24h"].rolling(3, min_periods=3).sum()
    dia["chuva_7d_anteriores"] = dia["chuva_24h"].shift(1).rolling(7, min_periods=5).sum()

    referencia = chuva.index.max().tz_localize(None).normalize()
    linha = dia.loc[referencia]
    valores: dict[str, float] = {}
    completo = True
    for nome in ("chuva_24h", "chuva_max_1h", "chuva_max_3h", "chuva_dia_anterior", "chuva_72h", "chuva_7d_anteriores"):
        valor = float(linha[nome])
        if math.isnan(valor):
            valor, completo = 0.0, False
        valores[nome] = valor
    mes = referencia.month
    valores["mes_sen"] = float(np.sin(2 * np.pi * mes / 12))
    valores["mes_cos"] = float(np.cos(2 * np.pi * mes / 12))

    dias_leitura = {d for d in pd.DatetimeIndex(chuva.index).tz_localize(None).normalize() if referencia - pd.Timedelta(days=DIAS_MINIMOS - 1) <= d <= referencia}
    return VariaveisDoDia(valores, len(dias_leitura), completo)


def prever(modelo: ModeloRisco, leituras: Sequence[Leitura]) -> ResultadoRisco:
    """Probabilidade de alagar no dia da última leitura. lowData: menos de 8 dias com leitura
    ou alguma variável sem dados (vira 0 mm, o que subestima a chuva).
    """
    variaveis = variaveis_do_dia(leituras)
    linha = pd.DataFrame([[variaveis.valores[nome] for nome in modelo.variaveis]], columns=list(modelo.variaveis))
    probabilidade = float(modelo.pipeline.predict_proba(linha)[0, 1])
    poucos = variaveis.dias_com_leitura < DIAS_MINIMOS or not variaveis.completo
    return ResultadoRisco(probabilidade, probabilidade >= modelo.limite, poucos)


def carregar_modelo(caminho: Path) -> ModeloRisco:
    """Lê o joblib (chaves pipeline, variaveis, limite; opcionais versao e sklearn).

    Versão do modelo: chave 'versao' ou, na falta dela, a data de modificação do arquivo.
    Se 'sklearn' diferir da versão instalada, só avisa no log.
    """
    try:
        conteudo = joblib.load(caminho)
    except Exception as erro:  # arquivo ausente, corrompido ou de outra versão
        raise EntradaInvalidaError(f"não foi possível carregar o modelo '{caminho}': {erro}") from erro
    if not isinstance(conteudo, dict):
        raise EntradaInvalidaError(f"'{caminho}': esperado um dicionário com pipeline, variaveis e limite")
    for chave in ("pipeline", "variaveis", "limite"):
        if chave not in conteudo:
            raise EntradaInvalidaError(f"'{caminho}': chave '{chave}' ausente")
    variaveis = tuple(str(v) for v in conteudo["variaveis"])
    desconhecidas = [v for v in variaveis if v not in VARIAVEIS_CONHECIDAS]
    if desconhecidas:
        raise EntradaInvalidaError(f"'{caminho}': variáveis desconhecidas {desconhecidas}")
    if not hasattr(conteudo["pipeline"], "predict_proba"):
        raise EntradaInvalidaError(f"'{caminho}': 'pipeline' não tem predict_proba")
    treinado_com = conteudo.get("sklearn")
    if treinado_com is not None and treinado_com != sklearn.__version__:
        registro.warning(
            "modelo treinado com scikit-learn %s, mas o serviço usa %s; confira as probabilidades",
            treinado_com,
            sklearn.__version__,
        )
    versao = conteudo.get("versao")
    if versao is None:
        versao = datetime.fromtimestamp(caminho.stat().st_mtime, tz=timezone.utc).astimezone(FUSO).date().isoformat()
    return ModeloRisco(conteudo["pipeline"], variaveis, float(conteudo["limite"]), str(versao))
