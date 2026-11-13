import math
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

import joblib
import numpy as np
import pandas as pd
import pytest
import sklearn
from fastapi.testclient import TestClient
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from rota_segura.api import criar_app
from rota_segura.modelos import EntradaInvalidaError
from rota_segura.risco import (
    VARIAVEIS_CONHECIDAS,
    Leitura,
    ModeloRisco,
    carregar_modelo,
    variaveis_do_dia,
)

SP = timezone(timedelta(hours=-3))


def modelo_pequeno(limite: float = 0.5) -> ModeloRisco:
    """Modelo de brinquedo: quanto mais chuva em 24 h, maior a chance. Não depende do modelo real."""
    sorteio = np.random.default_rng(42)
    chuva = sorteio.uniform(0, 80, 200)
    dados = pd.DataFrame({nome: 0.0 for nome in VARIAVEIS_CONHECIDAS}, index=range(200))
    dados["chuva_24h"] = chuva
    dados["chuva_max_1h"] = chuva / 4
    dados["chuva_max_3h"] = chuva / 2
    dados["mes_sen"] = sorteio.uniform(-1, 1, 200)
    alvo = (chuva > 40).astype(int)
    pipeline = Pipeline([("escala", StandardScaler()), ("modelo", LogisticRegression(max_iter=1000))])
    pipeline.fit(dados[list(VARIAVEIS_CONHECIDAS)], alvo)
    return ModeloRisco(pipeline, VARIAVEIS_CONHECIDAS, limite, "teste")


def leituras_horarias(dias: list[int], mm: float = 0.5) -> list[Leitura]:
    """Uma leitura por hora, nos dias de novembro/2026 informados (horário de Brasília)."""
    return [Leitura(datetime(2026, 11, d, h, 0, tzinfo=SP), mm, 1.0) for d in dias for h in range(24)]


def corpo(leituras: list[Leitura]) -> dict[str, Any]:
    return {
        "areaId": 3,
        "lat": -23.49,
        "lng": -46.41,
        "readings": [
            {"timestamp": l.instante.isoformat(), "rainMm": l.chuva_mm, "waterLevelCm": l.nivel_cm} for l in leituras
        ],
    }


def test_risk_sem_modelo_responde_503() -> None:
    cliente = TestClient(criar_app(None, None))
    resposta = cliente.post("/risk", json=corpo(leituras_horarias([19])))
    assert resposta.status_code == 503
    assert resposta.json()["error"] == "MODEL_UNAVAILABLE"
    assert cliente.get("/health").json()["modelLoaded"] is False


def test_risk_com_menos_de_8_dias_marca_low_data() -> None:
    cliente = TestClient(criar_app(None, modelo_pequeno()))
    poucos = cliente.post("/risk", json=corpo(leituras_horarias([17, 18, 19]))).json()
    assert poucos["lowData"] is True
    assert 0.0 <= poucos["floodProbability"] <= 1.0 and poucos["modelVersion"] == "teste"

    oito_dias = cliente.post("/risk", json=corpo(leituras_horarias(list(range(12, 20))))).json()
    assert oito_dias["lowData"] is False and oito_dias["areaId"] == 3

    # Um dia no meio sem nenhuma leitura: só 7 dias com leitura, então lowData.
    buraco = cliente.post("/risk", json=corpo(leituras_horarias([12, 13, 14, 16, 17, 18, 19]))).json()
    assert buraco["lowData"] is True


def test_risk_alta_probabilidade_compara_com_o_limite() -> None:
    chuvoso = leituras_horarias(list(range(12, 20)), mm=5.0)  # 120 mm por dia
    seco = leituras_horarias(list(range(12, 20)), mm=0.0)
    alto = TestClient(criar_app(None, modelo_pequeno(limite=0.5))).post("/risk", json=corpo(chuvoso)).json()
    baixo = TestClient(criar_app(None, modelo_pequeno(limite=0.5))).post("/risk", json=corpo(seco)).json()
    assert alto["highProbability"] is True and alto["floodProbability"] >= 0.5
    assert baixo["highProbability"] is False and baixo["floodProbability"] < 0.5


def test_variaveis_do_risk_batem_com_o_calculo_manual() -> None:
    # Dias 12 a 18: 24 leituras de 0,5 mm por dia = 12 mm/dia.
    # Dia 19: 21 horas de 0,5 mm (10,5), hora 10 com duas leituras (6,0 + 1,0 = 7,0), hora 11 = 4,0, hora 12 = 3,0.
    anteriores = leituras_horarias(list(range(12, 19)))
    dia = [l for l in leituras_horarias([19]) if l.instante.hour not in (10, 11, 12)]
    dia += [
        Leitura(datetime(2026, 11, 19, 10, 0, tzinfo=SP), 6.0, 1.0),
        Leitura(datetime(2026, 11, 19, 10, 30, tzinfo=SP), 1.0, 1.0),
        Leitura(datetime(2026, 11, 19, 11, 0, tzinfo=SP), 4.0, 1.0),
        Leitura(datetime(2026, 11, 19, 12, 0, tzinfo=SP), 3.0, 1.0),
    ]
    dia.sort(key=lambda l: l.instante)

    resultado = variaveis_do_dia(anteriores + dia)
    v = resultado.valores
    assert v["chuva_24h"] == pytest.approx(10.5 + 7.0 + 4.0 + 3.0)  # 24,5
    assert v["chuva_max_1h"] == pytest.approx(7.0)
    assert v["chuva_max_3h"] == pytest.approx(7.0 + 4.0 + 3.0)  # horas 10, 11 e 12 = 14,0
    assert v["chuva_dia_anterior"] == pytest.approx(12.0)
    assert v["chuva_72h"] == pytest.approx(24.5 + 12.0 + 12.0)  # 48,5
    assert v["chuva_7d_anteriores"] == pytest.approx(7 * 12.0)  # 84,0
    assert v["mes_sen"] == pytest.approx(math.sin(2 * math.pi * 11 / 12))  # -0,5 (novembro)
    assert v["mes_cos"] == pytest.approx(math.cos(2 * math.pi * 11 / 12))  # 0,866
    assert resultado.dias_com_leitura == 8 and resultado.completo is True


def test_leitura_em_utc_cai_no_dia_de_sao_paulo() -> None:
    # 01:00 UTC do dia 19 é 22:00 do dia 18 em Brasília.
    resultado = variaveis_do_dia([Leitura(datetime(2026, 11, 19, 1, 0, tzinfo=timezone.utc), 5.0, 1.0)])
    assert resultado.valores["chuva_24h"] == pytest.approx(5.0)
    assert resultado.valores["chuva_dia_anterior"] == 0.0  # sem dado vira 0 mm
    assert resultado.completo is False and resultado.dias_com_leitura == 1


@pytest.mark.parametrize(
    "mudanca",
    [
        {"readings": []},
        {"readings": [{"timestamp": "2026-11-19T10:00:00", "rainMm": 1.0, "waterLevelCm": 1.0}]},  # sem fuso
        {"readings": [{"timestamp": "ontem", "rainMm": 1.0, "waterLevelCm": 1.0}]},
        {"readings": [{"timestamp": "2026-11-19T10:00:00-03:00", "rainMm": -1.0, "waterLevelCm": 1.0}]},
        {
            "readings": [
                {"timestamp": "2026-11-19T11:00:00-03:00", "rainMm": 1.0, "waterLevelCm": 1.0},
                {"timestamp": "2026-11-19T10:00:00-03:00", "rainMm": 1.0, "waterLevelCm": 1.0},
            ]
        },
    ],
)
def test_risk_entrada_invalida_responde_400(mudanca: dict[str, Any]) -> None:
    cliente = TestClient(criar_app(None, modelo_pequeno()))
    resposta = cliente.post("/risk", json=corpo(leituras_horarias([19])) | mudanca)
    assert resposta.status_code == 400 and resposta.json()["error"] == "INVALID_INPUT"


def test_carregar_modelo_le_versao_e_avisa_sklearn_diferente(tmp_path: Path, caplog: pytest.LogCaptureFixture) -> None:
    modelo = modelo_pequeno()
    arquivo = tmp_path / "modelo.joblib"
    joblib.dump(
        {"pipeline": modelo.pipeline, "variaveis": list(VARIAVEIS_CONHECIDAS), "limite": 0.4, "versao": "2026-11-17", "sklearn": "0.0.1"},
        arquivo,
    )
    with caplog.at_level("WARNING", logger="rota_segura.risco"):
        carregado = carregar_modelo(arquivo)
    assert (carregado.versao, carregado.limite) == ("2026-11-17", 0.4)
    assert "0.0.1" in caplog.text and sklearn.__version__ in caplog.text


def test_carregar_modelo_sem_versao_usa_data_do_arquivo(tmp_path: Path, caplog: pytest.LogCaptureFixture) -> None:
    arquivo = tmp_path / "modelo.joblib"
    joblib.dump({"pipeline": modelo_pequeno().pipeline, "variaveis": list(VARIAVEIS_CONHECIDAS), "limite": 0.4}, arquivo)
    with caplog.at_level("WARNING", logger="rota_segura.risco"):
        carregado = carregar_modelo(arquivo)
    assert len(carregado.versao) == 10 and carregado.versao[4] == "-"  # AAAA-MM-DD
    assert caplog.text == ""


def test_carregar_modelo_invalido(tmp_path: Path) -> None:
    with pytest.raises(EntradaInvalidaError, match="não foi possível carregar"):
        carregar_modelo(tmp_path / "nao_existe.joblib")
    arquivo = tmp_path / "ruim.joblib"
    joblib.dump({"pipeline": modelo_pequeno().pipeline, "variaveis": ["chuva_24h"]}, arquivo)
    with pytest.raises(EntradaInvalidaError, match="limite"):
        carregar_modelo(arquivo)
