from rota_segura.grafo import montar_grafo
from rota_segura.medicao import gerar_mapa_sintetico, medir, tabela_markdown


def test_mapa_sintetico_tem_o_tamanho_pedido_e_e_conexo() -> None:
    bairro, _ = gerar_mapa_sintetico(30)
    assert bairro.esquinas == frozenset(range(30))

    grafo = montar_grafo(bairro, frozenset())
    visitadas, pilha = {0}, [0]
    while pilha:
        for vizinho, _, _ in grafo[pilha.pop()]:
            if vizinho not in visitadas:
                visitadas.add(vizinho)
                pilha.append(vizinho)
    assert visitadas == bairro.esquinas


def test_mapa_sintetico_e_sempre_o_mesmo_para_a_mesma_semente() -> None:
    assert gerar_mapa_sintetico(50) == gerar_mapa_sintetico(50)


def test_medicao_pequena_mede_os_dois_algoritmos() -> None:
    medicao = medir(50, repeticoes=1)
    assert medicao.esquinas == 50
    assert medicao.bellman_ford_ms is not None
    assert "| 50 |" in tabela_markdown([medicao])
