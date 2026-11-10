"""Bellman-Ford em tabela (tabulação), multi-origem a partir dos abrigos com vaga.

É a programação dinâmica explícita do projeto e a referência dos testes:
O(V·E) de propósito, para comparar com o Dijkstra.

Recorrência, com "⊕" = somar custo e distância do trecho e manter o abrigo:

    dp[0][v] = (0, 0, v)  se v é abrigo com vaga, senão ∞
    dp[k][v] = min( dp[k-1][v],  min sobre trechos (v,u) de dp[k-1][u] ⊕ trecho(v,u) )

dp[k][v] é o melhor rótulo de v usando no máximo k trechos. Como todo custo é
positivo, a melhor rota não repete esquina e tem no máximo V-1 trechos, então
dp[V-1] é a resposta.
"""

from rota_segura.grafo import ArvoreRotas, Grafo, Rotulo

LinhaTabela = dict[int, Rotulo | None]  # None = ∞ (ainda sem rota)


def tabela_bellman_ford(grafo: Grafo, abrigos: frozenset[int]) -> list[LinhaTabela]:
    """Monta a tabela inteira dp[0..V-1][v].

    Tempo O(V·E): V-1 rodadas, cada uma relaxa todas as arestas.
    Memória O(V²): a tabela guarda as V linhas, com V colunas cada.
    """
    dp: list[LinhaTabela] = [{v: ((0, 0, v) if v in abrigos else None) for v in grafo}]
    for _ in range(1, len(grafo)):
        anterior = dp[-1]
        linha: LinhaTabela = {}
        for v, arestas in grafo.items():
            melhor = anterior[v]
            for u, custo, distancia in arestas:
                rotulo_u = anterior[u]
                if rotulo_u is None:
                    continue
                candidato = (rotulo_u[0] + custo, rotulo_u[1] + distancia, rotulo_u[2])
                if melhor is None or candidato < melhor:
                    melhor = candidato
            linha[v] = melhor
        dp.append(linha)
    return dp


def bellman_ford_multi_origem(grafo: Grafo, abrigos: frozenset[int]) -> ArvoreRotas:
    """Árvore de rotas a partir da última linha da tabela.

    A próxima esquina de v é o vizinho de menor ID que gera exatamente o rótulo de v.
    Tempo O(V·E), memória O(V²) (dominados pela tabela).
    """
    final = tabela_bellman_ford(grafo, abrigos)[-1]
    rotulos = {v: rotulo for v, rotulo in final.items() if rotulo is not None}
    proximo: dict[int, int] = {}
    for v, rotulo in rotulos.items():
        if v in abrigos:
            continue
        proximo[v] = min(
            u
            for u, custo, distancia in grafo[v]
            if u in rotulos and (rotulos[u][0] + custo, rotulos[u][1] + distancia, rotulos[u][2]) == rotulo
        )
    return ArvoreRotas(rotulos, proximo)
