from pathlib import Path

import pandas as pd
import pytest

from rota_dados.erros import EntradaInvalidaError
from rota_dados.ped import (
    dias_com_dado,
    encontrar_lacunas,
    gerar_relatorio,
    histograma_intervalos,
)

CABECALHO = (
    "cidade,codestacao,datahora,id_sensor,latitude,longitude,nome,offset,qualificacao,uf,valor\n"
)


def _csv(tmp_path: Path, linhas: list[tuple[str, int]]) -> Path:
    corpo = "".join(
        f"SÃO PAULO,999A,{dh},{sensor},-23.0,-46.0,Teste,,0,SP,0.0\n" for dh, sensor in linhas
    )
    arquivo = tmp_path / "ped.csv"
    arquivo.write_text(CABECALHO + corpo, encoding="utf-8")
    return arquivo


def _dia(dia: str, horas: int) -> list[tuple[str, int]]:
    return [(f"{dia} {h:02d}:00:00", 10) for h in range(horas)]


def test_histograma_usa_instantes_unicos() -> None:
    serie = pd.Series(pd.to_datetime(["2020-01-01 00:00", "2020-01-01 00:00", "2020-01-01 00:10"]))
    assert histograma_intervalos(serie) == {10.0: 1}


def test_lacuna_e_dias_consecutivos_sem_dado() -> None:
    datas = [pd.Timestamp("2020-01-01"), pd.Timestamp("2020-01-02"), pd.Timestamp("2020-01-06")]
    lacunas = encontrar_lacunas(datas)
    assert len(lacunas) == 1
    assert (lacunas[0].inicio.isoformat(), lacunas[0].fim.isoformat(), lacunas[0].dias) == (
        "2020-01-03",
        "2020-01-05",
        3,
    )


def test_dia_com_dado_exige_20_horas_distintas() -> None:
    horas_19 = pd.to_datetime([f"2020-01-01 {h:02d}:30" for h in range(19)])
    horas_20 = pd.to_datetime([f"2020-01-02 {h:02d}:30" for h in range(20)])
    # várias leituras na mesma hora contam como uma hora só
    repetidas = pd.to_datetime([f"2020-01-03 00:{m:02d}" for m in range(0, 60, 10)])
    dias = dias_com_dado(pd.Series(horas_19.append(horas_20).append(repetidas)))
    assert list(dias["n_horas"]) == [19, 20, 1]
    assert list(dias["dia_com_dado"]) == [False, True, False]


def test_relatorio_dia_parcial_e_so_primeiro_e_ultimo(tmp_path: Path) -> None:
    linhas = _dia("2020-01-01", 24) + _dia("2020-01-04", 24) + _dia("2020-01-05", 3)
    linhas.append(("2020-01-05 00:30:00", 240))  # outro sensor é ignorado
    r = gerar_relatorio(_csv(tmp_path, linhas))
    assert (r.estacao, r.municipio) == ("999A", "SÃO PAULO")
    assert r.dias_no_periodo == 5
    assert r.dias_com_dado == 2
    assert r.dias_parcial == 2
    assert r.dias_sem_dado == 3
    assert [x.dias for x in r.lacunas] == [2]


def test_arquivo_inexistente_e_entrada_invalida(tmp_path: Path) -> None:
    with pytest.raises(EntradaInvalidaError):
        gerar_relatorio(tmp_path / "nao_existe.csv")


def test_coluna_ausente_e_entrada_invalida(tmp_path: Path) -> None:
    arquivo = tmp_path / "x.csv"
    arquivo.write_text("a,b\n1,2\n", encoding="utf-8")
    with pytest.raises(EntradaInvalidaError):
        gerar_relatorio(arquivo)


def test_sem_sensor_10_e_entrada_invalida(tmp_path: Path) -> None:
    with pytest.raises(EntradaInvalidaError):
        gerar_relatorio(_csv(tmp_path, [("2020-01-01 00:00:00", 240)]))
