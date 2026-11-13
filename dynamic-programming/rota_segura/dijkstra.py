"""Dijkstra multi-origem: algoritmo principal da rota segura.

Em vez de rodar um Dijkstra por pessoa, a fila começa com todos os abrigos com
vaga ao mesmo tempo (rótulo zero). Como os trechos são de mão dupla, a distância
do abrigo até a esquina é a mesma da esquina até o abrigo, e uma única execução
dá, para cada esquina, o abrigo com vaga mais próximo e o caminho até ele.
"""

import heapq

from rota_segura.grafo import Abrigos, ArvoreRotas, Grafo, Rotulo, ids_dos_abrigos
from rota_segura.modelos import ResultadoRota, StatusRota

ORIENTACAO_SEM_ROTA = "Fique em local alto"


def dijkstra_multi_origem(grafo: Grafo, abrigos: Abrigos) -> ArvoreRotas:
    """Melhor rótulo (custo, distância, id do abrigo) e próxima esquina para cada esquina alcançável.

    Empate de rótulo entre dois vizinhos: fica o de menor ID como próxima esquina.
    Tempo O((V + E) log V) com heapq; memória O(V + E).
    """
    rotulos: dict[int, Rotulo] = {}
    proximo: dict[int, int] = {}
    fila: list[tuple[Rotulo, int]] = []
    for esquina, id_abrigo in ids_dos_abrigos(abrigos).items():
        rotulos[esquina] = (0, 0, id_abrigo)
        heapq.heappush(fila, (rotulos[esquina], esquina))

    finalizadas: set[int] = set()
    while fila:
        rotulo_u, u = heapq.heappop(fila)
        if u in finalizadas:
            continue  # entrada velha na fila: u já saiu com um rótulo melhor
        finalizadas.add(u)
        custo_u, distancia_u, abrigo_u = rotulo_u
        for v, custo, distancia in grafo[u]:
            novo = (custo_u + custo, distancia_u + distancia, abrigo_u)
            atual = rotulos.get(v)
            if atual is None or novo < atual:
                rotulos[v] = novo
                proximo[v] = u
                heapq.heappush(fila, (novo, v))
            elif novo == atual and u < proximo[v]:
                proximo[v] = u
    return ArvoreRotas(rotulos, proximo)


def rota_ate_abrigo(arvore: ArvoreRotas, origem: int) -> ResultadoRota:
    """Reconstrói o caminho seguindo a próxima esquina até chegar ao abrigo.

    Sem rótulo para a origem, não há rota segura: devolve SEM_ROTA_SEGURA, não lança exceção.
    Tempo O(tamanho do caminho), memória O(tamanho do caminho).
    """
    if origem not in arvore.rotulos:
        return ResultadoRota(StatusRota.SEM_ROTA_SEGURA, (), 0.0, ORIENTACAO_SEM_ROTA)
    caminho = [origem]
    while caminho[-1] in arvore.proximo:
        caminho.append(arvore.proximo[caminho[-1]])
    _, distancia_mm, abrigo = arvore.rotulos[origem]
    return ResultadoRota(
        StatusRota.ROTA_ENCONTRADA,
        tuple(caminho),
        distancia_mm / 1000,
        f"Siga a rota até o abrigo na esquina {abrigo}",
    )
