"""Transforma bairro + cenário no grafo que os algoritmos percorrem.

- Esquina = vértice; trecho = aresta de mão dupla (rota a pé).
- Custo do trecho = distância × (1 + fator_risco).
- Trecho alagado sai do grafo (RN05); abrigo lotado não é origem (RN06).

Custos e distâncias ficam em milímetros inteiros. Assim, 100 m com risco 0,1
custa exatamente 110 000, e não 110,00000000000001 como em float: empates
reais continuam empates e o desempate é sempre o mesmo.
"""

from dataclasses import dataclass

from rota_segura.modelos import Bairro, Cenario, Trecho, chave_trecho

# (vizinho, custo_mm, distancia_mm)
Aresta = tuple[int, int, int]
Grafo = dict[int, list[Aresta]]

# (custo_mm, distancia_mm, esquina do abrigo). A comparação de tuplas do Python
# já aplica o desempate: menor custo, depois menor distância, depois menor ID.
Rotulo = tuple[int, int, int]


@dataclass(frozen=True, slots=True)
class ArvoreRotas:
    """Resultado de uma execução multi-origem: vale para todas as esquinas do cenário.

    rotulos: melhor rótulo de cada esquina que alcança um abrigo com vaga.
    proximo: próxima esquina no caminho rumo ao abrigo (abrigos não têm entrada).
    """

    rotulos: dict[int, Rotulo]
    proximo: dict[int, int]


def custo_trecho_mm(trecho: Trecho) -> int:
    return round(trecho.distancia_m * (1 + trecho.fator_risco) * 1000)


def montar_grafo(bairro: Bairro, trechos_alagados: frozenset[tuple[int, int]]) -> Grafo:
    """Dicionário de adjacência sem os trechos alagados (RN05).

    Tempo O(V + E), memória O(V + E).
    """
    grafo: Grafo = {esquina: [] for esquina in bairro.esquinas}
    for trecho in bairro.trechos:
        if chave_trecho(trecho.origem, trecho.destino) in trechos_alagados:
            continue
        custo = custo_trecho_mm(trecho)
        distancia = round(trecho.distancia_m * 1000)
        grafo[trecho.origem].append((trecho.destino, custo, distancia))
        grafo[trecho.destino].append((trecho.origem, custo, distancia))
    return grafo


def abrigos_com_vaga(cenario: Cenario) -> frozenset[int]:
    """Esquinas com pelo menos um abrigo com vaga; os lotados são ignorados (RN06)."""
    return frozenset(abrigo.esquina for abrigo in cenario.abrigos if abrigo.tem_vaga)
