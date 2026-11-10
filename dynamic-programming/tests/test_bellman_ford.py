from rota_segura.bellman_ford import bellman_ford_multi_origem, tabela_bellman_ford
from rota_segura.grafo import montar_grafo
from rota_segura.modelos import Bairro, Trecho

SEM_ALAGAMENTO: frozenset[tuple[int, int]] = frozenset()


def _bairro(*trechos: Trecho) -> Bairro:
    esquinas = frozenset(e for t in trechos for e in (t.origem, t.destino))
    return Bairro(esquinas, trechos)


def test_linha_k_da_tabela_usa_no_maximo_k_trechos() -> None:
    grafo = montar_grafo(_bairro(Trecho(1, 2, 100), Trecho(2, 3, 50)), SEM_ALAGAMENTO)
    dp = tabela_bellman_ford(grafo, frozenset({3}))

    assert len(dp) == 3  # k = 0, 1, 2 (V - 1 = 2)
    assert dp[0] == {1: None, 2: None, 3: (0, 0, 3)}
    assert dp[1] == {1: None, 2: (50_000, 50_000, 3), 3: (0, 0, 3)}
    assert dp[2][1] == (150_000, 150_000, 3)


def test_custo_do_trecho_e_distancia_vezes_um_mais_risco() -> None:
    grafo = montar_grafo(_bairro(Trecho(1, 2, 100, fator_risco=0.5)), SEM_ALAGAMENTO)
    arvore = bellman_ford_multi_origem(grafo, frozenset({2}))
    assert arvore.rotulos[1] == (150_000, 100_000, 2)


def test_risco_alto_faz_preferir_caminho_mais_longo() -> None:
    bairro = _bairro(Trecho(1, 3, 100, fator_risco=2.0), Trecho(1, 2, 100), Trecho(2, 3, 100))
    arvore = bellman_ford_multi_origem(montar_grafo(bairro, SEM_ALAGAMENTO), frozenset({3}))
    assert arvore.rotulos[1] == (200_000, 200_000, 3)
    assert arvore.proximo[1] == 2


def test_rn05_trecho_alagado_nao_e_usado() -> None:
    bairro = _bairro(Trecho(1, 3, 100), Trecho(1, 2, 200), Trecho(2, 3, 200))
    arvore = bellman_ford_multi_origem(montar_grafo(bairro, frozenset({(1, 3)})), frozenset({3}))
    assert arvore.rotulos[1] == (400_000, 400_000, 3)
    assert arvore.proximo[1] == 2


def test_rn06_abrigo_lotado_e_ignorado_mesmo_mais_perto() -> None:
    # Só o abrigo 3 entra como origem: o 2 está lotado.
    bairro = _bairro(Trecho(1, 2, 10), Trecho(1, 3, 500))
    arvore = bellman_ford_multi_origem(montar_grafo(bairro, SEM_ALAGAMENTO), frozenset({3}))
    assert arvore.rotulos[1] == (500_000, 500_000, 3)
    assert arvore.rotulos[2] == (510_000, 510_000, 3)


def test_quem_esta_no_abrigo_tem_custo_zero() -> None:
    arvore = bellman_ford_multi_origem(montar_grafo(_bairro(Trecho(1, 2, 100)), SEM_ALAGAMENTO), frozenset({2}))
    assert arvore.rotulos[2] == (0, 0, 2)
    assert 2 not in arvore.proximo


def test_esquina_isolada_fica_sem_rotulo() -> None:
    bairro = _bairro(Trecho(1, 2, 100), Trecho(2, 3, 100))
    arvore = bellman_ford_multi_origem(montar_grafo(bairro, frozenset({(2, 3)})), frozenset({3}))
    assert 1 not in arvore.rotulos
    assert 2 not in arvore.rotulos


def test_desempate_por_menor_distancia_com_custo_igual() -> None:
    # Custo 150 pelos dois lados: 100 m com risco 0,5 até o abrigo 3, ou 150 m sem risco até o 2.
    bairro = _bairro(Trecho(1, 3, 100, fator_risco=0.5), Trecho(1, 2, 150))
    arvore = bellman_ford_multi_origem(montar_grafo(bairro, SEM_ALAGAMENTO), frozenset({2, 3}))
    assert arvore.rotulos[1] == (150_000, 100_000, 3)


def test_desempate_por_menor_id_do_abrigo_com_custo_e_distancia_iguais() -> None:
    bairro = _bairro(Trecho(1, 3, 100), Trecho(1, 2, 100))
    arvore = bellman_ford_multi_origem(montar_grafo(bairro, SEM_ALAGAMENTO), frozenset({2, 3}))
    assert arvore.rotulos[1] == (100_000, 100_000, 2)


def test_desempate_por_menor_id_da_proxima_esquina_no_mesmo_abrigo() -> None:
    bairro = _bairro(Trecho(1, 3, 70), Trecho(3, 4, 30), Trecho(1, 2, 50), Trecho(2, 4, 50))
    arvore = bellman_ford_multi_origem(montar_grafo(bairro, SEM_ALAGAMENTO), frozenset({4}))
    assert arvore.proximo[1] == 2


def test_risco_decimal_nao_quebra_empate_por_arredondamento() -> None:
    # Em float, 100 × 1,1 = 110,00000000000001 e o abrigo 3 perderia o empate.
    bairro = _bairro(Trecho(1, 2, 110), Trecho(1, 3, 100, fator_risco=0.1))
    arvore = bellman_ford_multi_origem(montar_grafo(bairro, SEM_ALAGAMENTO), frozenset({2, 3}))
    assert arvore.rotulos[1] == (110_000, 100_000, 3)
