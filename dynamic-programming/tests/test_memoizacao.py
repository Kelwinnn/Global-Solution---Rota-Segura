import pytest

from rota_segura.memoizacao import CalculadoraRotas
from rota_segura.modelos import Abrigo, Bairro, Cenario, EntradaInvalidaError, StatusRota, Trecho

BAIRRO = Bairro(frozenset({1, 2, 3}), (Trecho(1, 2, 100), Trecho(2, 3, 100), Trecho(1, 3, 300)))


def test_mesmo_cenario_reaproveita_o_calculo() -> None:
    calculadora = CalculadoraRotas(BAIRRO)
    cenario = Cenario(frozenset(), (Abrigo(3, 10, 0),))
    assert calculadora.arvore(cenario) is calculadora.arvore(cenario)


def test_cenario_igual_em_outro_objeto_reaproveita_o_calculo() -> None:
    calculadora = CalculadoraRotas(BAIRRO)
    primeira = calculadora.arvore(Cenario(frozenset({(1, 2)}), (Abrigo(3, 10, 0),)))
    segunda = calculadora.arvore(Cenario(frozenset({(1, 2)}), (Abrigo(3, 10, 0),)))
    assert primeira is segunda


def test_mudar_ocupacao_sem_lotar_reaproveita_o_calculo() -> None:
    calculadora = CalculadoraRotas(BAIRRO)
    antes = calculadora.arvore(Cenario(frozenset(), (Abrigo(3, 10, 2),)))
    depois = calculadora.arvore(Cenario(frozenset(), (Abrigo(3, 10, 7),)))
    assert antes is depois


def test_novo_trecho_alagado_recalcula_a_rota() -> None:
    calculadora = CalculadoraRotas(BAIRRO)
    abrigos = (Abrigo(3, 10, 0),)
    assert calculadora.rota(Cenario(frozenset(), abrigos), 1).caminho == (1, 2, 3)
    assert calculadora.rota(Cenario(frozenset({(2, 3)}), abrigos), 1).caminho == (1, 3)


def test_abrigo_que_lota_recalcula_a_rota() -> None:
    calculadora = CalculadoraRotas(BAIRRO)
    assert calculadora.rota(Cenario(frozenset(), (Abrigo(3, 10, 9),)), 1).status is StatusRota.ROTA_ENCONTRADA
    assert calculadora.rota(Cenario(frozenset(), (Abrigo(3, 10, 10),)), 1).status is StatusRota.SEM_ROTA_SEGURA


def test_origem_inexistente_e_rejeitada() -> None:
    calculadora = CalculadoraRotas(BAIRRO)
    with pytest.raises(EntradaInvalidaError, match="esquina 99 não existe"):
        calculadora.rota(Cenario(frozenset(), (Abrigo(3, 10, 0),)), 99)
