from rota_segura.dijkstra import dijkstra_multi_origem, rota_ate_abrigo
from rota_segura.grafo import abrigos_com_vaga, montar_grafo
from rota_segura.modelos import Abrigo, Bairro, Cenario, ResultadoRota, StatusRota, Trecho


def _bairro(*trechos: Trecho) -> Bairro:
    esquinas = frozenset(e for t in trechos for e in (t.origem, t.destino))
    return Bairro(esquinas, trechos)


def _rota(bairro: Bairro, cenario: Cenario, origem: int) -> ResultadoRota:
    grafo = montar_grafo(bairro, cenario.trechos_alagados)
    return rota_ate_abrigo(dijkstra_multi_origem(grafo, abrigos_com_vaga(cenario)), origem)


def _cenario(*abrigos: Abrigo, alagados: frozenset[tuple[int, int]] = frozenset()) -> Cenario:
    return Cenario(alagados, abrigos)


def test_caminho_vai_da_origem_ate_o_abrigo() -> None:
    bairro = _bairro(Trecho(1, 2, 100), Trecho(2, 3, 50), Trecho(3, 4, 80))
    resultado = _rota(bairro, _cenario(Abrigo(4, 10, 0)), origem=1)
    assert resultado == ResultadoRota(
        StatusRota.ROTA_ENCONTRADA, (1, 2, 3, 4), 230.0, "Siga a rota até o abrigo na esquina 4"
    )


def test_distancia_informada_e_em_metros_mesmo_com_risco() -> None:
    resultado = _rota(_bairro(Trecho(1, 2, 100, fator_risco=0.5)), _cenario(Abrigo(2, 10, 0)), origem=1)
    assert resultado.distancia_m == 100.0


def test_risco_alto_faz_preferir_caminho_mais_longo() -> None:
    bairro = _bairro(Trecho(1, 3, 100, fator_risco=2.0), Trecho(1, 2, 100), Trecho(2, 3, 100))
    assert _rota(bairro, _cenario(Abrigo(3, 10, 0)), origem=1).caminho == (1, 2, 3)


def test_rn05_rota_nunca_usa_trecho_alagado() -> None:
    bairro = _bairro(Trecho(1, 3, 100), Trecho(1, 2, 200), Trecho(2, 3, 200))
    resultado = _rota(bairro, _cenario(Abrigo(3, 10, 0), alagados=frozenset({(1, 3)})), origem=1)
    assert resultado.caminho == (1, 2, 3)
    assert resultado.distancia_m == 400.0


def test_rn05_trecho_alagado_bloqueia_os_dois_sentidos() -> None:
    bairro = _bairro(Trecho(3, 1, 100), Trecho(1, 2, 200), Trecho(2, 3, 200))
    resultado = _rota(bairro, _cenario(Abrigo(3, 10, 0), alagados=frozenset({(1, 3)})), origem=1)
    assert resultado.caminho == (1, 2, 3)


def test_rn06_abrigo_lotado_e_ignorado_mesmo_estando_mais_perto() -> None:
    bairro = _bairro(Trecho(1, 2, 10), Trecho(1, 3, 500))
    resultado = _rota(bairro, _cenario(Abrigo(2, 30, 30), Abrigo(3, 30, 5)), origem=1)
    assert resultado.caminho == (1, 3)


def test_rn06_rota_pode_atravessar_esquina_de_abrigo_lotado() -> None:
    bairro = _bairro(Trecho(1, 2, 10), Trecho(2, 3, 10))
    resultado = _rota(bairro, _cenario(Abrigo(2, 30, 30), Abrigo(3, 30, 5)), origem=1)
    assert resultado.caminho == (1, 2, 3)


def test_quem_ja_esta_no_abrigo_tem_distancia_zero() -> None:
    resultado = _rota(_bairro(Trecho(1, 2, 100)), _cenario(Abrigo(2, 10, 0)), origem=2)
    assert resultado.status is StatusRota.ROTA_ENCONTRADA
    assert resultado.caminho == (2,)
    assert resultado.distancia_m == 0.0


def test_cada_esquina_vai_para_o_abrigo_com_vaga_mais_proximo() -> None:
    bairro = _bairro(Trecho(1, 2, 100), Trecho(2, 3, 100), Trecho(3, 4, 100))
    cenario = _cenario(Abrigo(1, 10, 0), Abrigo(4, 10, 0))
    assert _rota(bairro, cenario, origem=2).caminho == (2, 1)
    assert _rota(bairro, cenario, origem=3).caminho == (3, 4)


def test_desempate_por_menor_distancia_com_custo_igual() -> None:
    bairro = _bairro(Trecho(1, 3, 100, fator_risco=0.5), Trecho(1, 2, 150))
    resultado = _rota(bairro, _cenario(Abrigo(2, 10, 0), Abrigo(3, 10, 0)), origem=1)
    assert resultado.caminho == (1, 3)


def test_desempate_por_menor_id_do_abrigo() -> None:
    bairro = _bairro(Trecho(1, 3, 100), Trecho(1, 2, 100))
    resultado = _rota(bairro, _cenario(Abrigo(3, 10, 0), Abrigo(2, 10, 0)), origem=1)
    assert resultado.caminho == (1, 2)


def test_desempate_por_menor_id_da_proxima_esquina() -> None:
    # 1→3→4 e 1→2→4 têm 100 m. A esquina 3 sai antes da fila (30 m do abrigo),
    # mas a próxima esquina escolhida tem de ser a de menor ID, a 2.
    bairro = _bairro(Trecho(1, 3, 70), Trecho(3, 4, 30), Trecho(1, 2, 50), Trecho(2, 4, 50))
    resultado = _rota(bairro, _cenario(Abrigo(4, 10, 0)), origem=1)
    assert resultado.caminho == (1, 2, 4)


def test_desempate_usa_o_id_do_abrigo_e_nao_a_esquina() -> None:
    bairro = _bairro(Trecho(1, 2, 100), Trecho(1, 3, 100))
    cenario = _cenario(Abrigo(2, 10, 0, id_abrigo=9), Abrigo(3, 10, 0, id_abrigo=4))
    arvore = dijkstra_multi_origem(montar_grafo(bairro, cenario.trechos_alagados), abrigos_com_vaga(cenario))
    assert arvore.rotulos[1][2] == 4  # empata em custo e distância; vence o abrigo de menor id (esquina 3)
    assert rota_ate_abrigo(arvore, 1).caminho == (1, 3)
