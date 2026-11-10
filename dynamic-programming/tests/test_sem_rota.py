"""Sem rota segura é uma resposta normal, com orientação, nunca uma exceção (RN05, RN06)."""

import json
from pathlib import Path

import pytest

from rota_segura.__main__ import main
from rota_segura.bellman_ford import bellman_ford_multi_origem
from rota_segura.dijkstra import rota_ate_abrigo
from rota_segura.grafo import abrigos_com_vaga, montar_grafo
from rota_segura.memoizacao import CalculadoraRotas
from rota_segura.modelos import Abrigo, Bairro, Cenario, ResultadoRota, StatusRota, Trecho

BAIRRO = Bairro(frozenset({1, 2, 3, 4}), (Trecho(1, 2, 100), Trecho(2, 3, 100), Trecho(3, 4, 100)))
SEM_ROTA = ResultadoRota(StatusRota.SEM_ROTA_SEGURA, (), 0.0, "Fique em local alto")
DADOS = Path(__file__).parent / "dados"


def test_rn05_alagamento_que_isola_a_origem_da_sem_rota_segura() -> None:
    cenario = Cenario(frozenset({(2, 3)}), (Abrigo(4, 10, 0),))
    assert CalculadoraRotas(BAIRRO).rota(cenario, 1) == SEM_ROTA


def test_rn06_todos_os_abrigos_lotados_da_sem_rota_segura() -> None:
    cenario = Cenario(frozenset(), (Abrigo(4, 10, 10), Abrigo(2, 5, 5)))
    assert CalculadoraRotas(BAIRRO).rota(cenario, 1) == SEM_ROTA


def test_rn06_abrigo_com_capacidade_zero_nao_tem_vaga() -> None:
    cenario = Cenario(frozenset(), (Abrigo(4, 0, 0),))
    assert CalculadoraRotas(BAIRRO).rota(cenario, 1) == SEM_ROTA


def test_cenario_sem_nenhum_abrigo_da_sem_rota_segura() -> None:
    assert CalculadoraRotas(BAIRRO).rota(Cenario(frozenset(), ()), 3) == SEM_ROTA


def test_estar_no_abrigo_lotado_tambem_da_sem_rota_segura() -> None:
    cenario = Cenario(frozenset(), (Abrigo(4, 10, 10),))
    assert CalculadoraRotas(BAIRRO).rota(cenario, 4) == SEM_ROTA


def test_bellman_ford_tambem_leva_a_sem_rota_segura() -> None:
    cenario = Cenario(frozenset({(2, 3)}), (Abrigo(4, 10, 0),))
    arvore = bellman_ford_multi_origem(montar_grafo(BAIRRO, cenario.trechos_alagados), abrigos_com_vaga(cenario))
    assert rota_ate_abrigo(arvore, 1) == SEM_ROTA


def test_comando_sem_rota_responde_json_e_sai_sem_erro(capsys: pytest.CaptureFixture[str]) -> None:
    # No exemplo, o trecho 2-3 está alagado e o único abrigo fica na esquina 4.
    argumentos = ["--bairro", str(DADOS / "bairro_exemplo.json"), "--cenario", str(DADOS / "cenario_exemplo.json")]
    assert main([*argumentos, "--origem", "1"]) == 0
    assert json.loads(capsys.readouterr().out) == {
        "status": "sem_rota_segura",
        "caminho": [],
        "distancia_m": 0.0,
        "orientacao": "Fique em local alto",
    }
