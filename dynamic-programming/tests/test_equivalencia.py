"""Dijkstra e Bellman-Ford têm de dar o mesmo resultado em qualquer mapa válido.

O Bellman-Ford em tabela segue a recorrência ao pé da letra; se o Dijkstra
discordar dele em algum mapa, o Dijkstra está errado.
"""

from hypothesis import given, settings
from hypothesis import strategies as st

from rota_segura.bellman_ford import bellman_ford_multi_origem
from rota_segura.dijkstra import dijkstra_multi_origem, rota_ate_abrigo
from rota_segura.grafo import abrigos_com_vaga, montar_grafo
from rota_segura.modelos import Abrigo, Bairro, Cenario, Trecho, chave_trecho

# Metade dos mapas usa só valores pequenos e "redondos", que geram muitos
# empates: é onde os dois algoritmos mais poderiam divergir. A outra metade
# usa floats quaisquer.
REDONDOS = (st.integers(1, 3).map(float), st.sampled_from([0.0, 0.1, 0.5]))
QUAISQUER = (st.floats(0.001, 1000.0), st.floats(0.0, 3.0))


@st.composite
def mapas(draw: st.DrawFn) -> tuple[Bairro, Cenario]:
    distancias, fatores = REDONDOS if draw(st.booleans()) else QUAISQUER
    n = draw(st.integers(1, 9))
    esquinas = list(range(1, n + 1))
    pares = st.tuples(st.sampled_from(esquinas), st.sampled_from(esquinas))
    trechos = tuple(
        Trecho(a, b, draw(distancias), draw(fatores)) for a, b in draw(st.lists(pares, max_size=3 * n))
    )
    alagados = frozenset(
        chave_trecho(t.origem, t.destino) for t in trechos if draw(st.integers(0, 4)) == 0
    )
    abrigos = tuple(
        Abrigo(e, capacidade, draw(st.integers(0, capacidade)))
        for e in draw(st.lists(st.sampled_from(esquinas), max_size=3))
        for capacidade in [draw(st.integers(0, 3))]
    )
    return Bairro(frozenset(esquinas), trechos), Cenario(alagados, abrigos)


@settings(max_examples=500, deadline=None)
@given(mapas())
def test_dijkstra_e_bellman_ford_dao_o_mesmo_resultado(mapa: tuple[Bairro, Cenario]) -> None:
    bairro, cenario = mapa
    grafo = montar_grafo(bairro, cenario.trechos_alagados)
    abrigos = abrigos_com_vaga(cenario)

    por_dijkstra = dijkstra_multi_origem(grafo, abrigos)
    por_bellman_ford = bellman_ford_multi_origem(grafo, abrigos)

    assert por_dijkstra.rotulos == por_bellman_ford.rotulos
    assert por_dijkstra.proximo == por_bellman_ford.proximo
    for origem in bairro.esquinas:
        assert rota_ate_abrigo(por_dijkstra, origem) == rota_ate_abrigo(por_bellman_ford, origem)
