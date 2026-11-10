"""Medição de tempo em mapas sintéticos: python -m rota_segura.medicao

O mapa é uma grade de ruas (cerca de 2 trechos por esquina, como um bairro
real), com 5% dos trechos alagados e 1 abrigo a cada 50 esquinas, um terço deles
lotado. A semente é fixa, então os mapas são sempre os mesmos.
"""

import math
import random
import time
from dataclasses import dataclass

from rota_segura.bellman_ford import bellman_ford_multi_origem
from rota_segura.grafo import abrigos_com_vaga, montar_grafo
from rota_segura.memoizacao import CalculadoraRotas
from rota_segura.modelos import Abrigo, Bairro, Cenario, Trecho, chave_trecho

TAMANHOS = (100, 500, 1_000, 2_000, 5_000, 10_000)
# A tabela O(V²) do Bellman-Ford ocupou 37 MB com 1.000 esquinas e 148 MB com
# 2.000 (tracemalloc); por V², 10 mil esquinas pediriam cerca de 3,7 GB.
LIMITE_BELLMAN_FORD = 2_000
SEMENTE = 42


@dataclass(frozen=True, slots=True)
class Medicao:
    esquinas: int
    trechos: int
    dijkstra_ms: float  # primeira consulta do cenário: monta o grafo e roda o Dijkstra
    memo_us: float  # consulta repetida: árvore já memoizada, só reconstrói o caminho
    bellman_ford_ms: float | None  # None = não medido (acima do limite)


def gerar_mapa_sintetico(n_esquinas: int, semente: int = SEMENTE) -> tuple[Bairro, Cenario]:
    """Grade com n_esquinas preenchida linha a linha (sempre conexa antes dos alagamentos)."""
    sorteio = random.Random(semente)
    lado = math.ceil(math.sqrt(n_esquinas))
    trechos: list[Trecho] = []
    for e in range(n_esquinas):
        vizinhos = [e + 1] if (e + 1) % lado != 0 else []
        vizinhos.append(e + lado)
        for v in vizinhos:
            if v < n_esquinas:
                trechos.append(Trecho(e, v, sorteio.uniform(50, 200), sorteio.choice([0.0, 0.0, 0.25, 0.5, 1.0])))
    alagados = frozenset(chave_trecho(t.origem, t.destino) for t in sorteio.sample(trechos, len(trechos) // 20))
    abrigos = tuple(
        Abrigo(e, 100, 100 if i % 3 == 0 else 40)
        for i, e in enumerate(sorteio.sample(range(n_esquinas), max(2, n_esquinas // 50)))
    )
    return Bairro(frozenset(range(n_esquinas)), tuple(trechos)), Cenario(alagados, abrigos)


def medir(n_esquinas: int, repeticoes: int = 5) -> Medicao:
    bairro, cenario = gerar_mapa_sintetico(n_esquinas)
    origens = random.Random(SEMENTE).choices(range(n_esquinas), k=1_000)

    tempos_dijkstra = []
    for _ in range(repeticoes):
        calculadora = CalculadoraRotas(bairro)
        inicio = time.perf_counter()
        arvore_dijkstra = calculadora.arvore(cenario)
        tempos_dijkstra.append(time.perf_counter() - inicio)

    inicio = time.perf_counter()
    for origem in origens:
        calculadora.rota(cenario, origem)
    memo_us = (time.perf_counter() - inicio) / len(origens) * 1e6

    bellman_ford_ms = None
    if n_esquinas <= LIMITE_BELLMAN_FORD:
        inicio = time.perf_counter()
        grafo = montar_grafo(bairro, cenario.trechos_alagados)
        arvore_bf = bellman_ford_multi_origem(grafo, abrigos_com_vaga(cenario))
        bellman_ford_ms = (time.perf_counter() - inicio) * 1000
        if arvore_bf != arvore_dijkstra:
            raise RuntimeError(f"Dijkstra e Bellman-Ford divergiram no mapa de {n_esquinas} esquinas")

    return Medicao(n_esquinas, len(bairro.trechos), min(tempos_dijkstra) * 1000, memo_us, bellman_ford_ms)


def _br(valor: float, casas: int = 0) -> str:
    """Número no formato brasileiro: 1.234,56."""
    return f"{valor:,.{casas}f}".translate(str.maketrans(",.", ".,"))


def tabela_markdown(medicoes: list[Medicao]) -> str:
    linhas = [
        "| Esquinas (V) | Trechos (E) | Dijkstra, 1ª consulta (ms) | Consulta memoizada (µs) | Bellman-Ford (ms) |",
        "| ---: | ---: | ---: | ---: | ---: |",
    ]
    for m in medicoes:
        bf = _br(m.bellman_ford_ms) if m.bellman_ford_ms is not None else "não medido"
        linhas.append(f"| {_br(m.esquinas)} | {_br(m.trechos)} | {_br(m.dijkstra_ms, 2)} | {_br(m.memo_us, 1)} | {bf} |")
    return "\n".join(linhas)


if __name__ == "__main__":
    print(tabela_markdown([medir(n) for n in TAMANHOS]))
