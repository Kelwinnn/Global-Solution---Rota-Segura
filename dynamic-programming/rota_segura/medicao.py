"""Medição de tempo em mapas sintéticos: python -m rota_segura.medicao

O mapa é uma grade de ruas (cerca de 2 trechos por esquina, como um bairro
real), com 5% dos trechos alagados e 1 abrigo a cada 50 esquinas, um terço deles
lotado. A semente é fixa, então os mapas são sempre os mesmos.
"""

import json
import math
import random
import sys
import time
import tracemalloc
from dataclasses import dataclass
from pathlib import Path

from rota_segura.bellman_ford import bellman_ford_multi_origem
from rota_segura.grafo import abrigos_com_vaga, montar_grafo
from rota_segura.mapa import AreaAlagada, Ponto, carregar_mapa, esquina_mais_proxima, trechos_alagados
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
    return medir_mapa(bairro, cenario, origens, repeticoes)


def medir_mapa(
    bairro: Bairro,
    cenario: Cenario,
    origens: list[int],
    repeticoes: int = 5,
    limite_bellman_ford: int = LIMITE_BELLMAN_FORD,
) -> Medicao:
    """Mede Dijkstra (melhor de `repeticoes`), consulta memoizada e, até `limite_bellman_ford` esquinas, Bellman-Ford."""
    n_esquinas = len(bairro.esquinas)
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
    if n_esquinas <= limite_bellman_ford:
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


def memoria_bellman_ford_mb(bairro: Bairro, cenario: Cenario) -> float:
    """Pico de memória (tracemalloc) de uma execução do Bellman-Ford: a tabela dp[k][v] tem V × V posições."""
    grafo = montar_grafo(bairro, cenario.trechos_alagados)
    tracemalloc.start()
    try:
        bellman_ford_multi_origem(grafo, abrigos_com_vaga(cenario))
        return tracemalloc.get_traced_memory()[1] / 1e6
    finally:
        tracemalloc.stop()


def medir_grafo_real(
    caminho_mapa: Path, caminho_abrigos: Path, raio_alagado_m: float = 150.0
) -> tuple[Medicao, float, int]:
    """Mede no grafo do Jardim Helena, com os abrigos sugeridos e uma área alagada de 150 m no centro do mapa.

    Devolve (medição, memória do Bellman-Ford em MB, abrigos usados). O Bellman-Ford roda no grafo inteiro.
    """
    mapa = carregar_mapa(caminho_mapa)
    abrigos_json = json.loads(caminho_abrigos.read_text(encoding="utf-8"))["abrigos"]
    abrigos = tuple(
        Abrigo(esquina_mais_proxima(mapa, Ponto(a["lat"], a["lng"]))[0], a["capacidade"], 0, a["id"]) for a in abrigos_json
    )
    lats = [p.lat for p in mapa.coordenadas.values()]
    lngs = [p.lng for p in mapa.coordenadas.values()]
    centro = Ponto((min(lats) + max(lats)) / 2, (min(lngs) + max(lngs)) / 2)
    alagados = trechos_alagados(mapa, [AreaAlagada(1, centro, raio_alagado_m)])
    cenario = Cenario(alagados, abrigos)
    origens = random.Random(SEMENTE).choices(sorted(mapa.bairro.esquinas), k=1_000)
    medicao = medir_mapa(mapa.bairro, cenario, origens, limite_bellman_ford=len(mapa.bairro.esquinas))
    return medicao, memoria_bellman_ford_mb(mapa.bairro, cenario), len(abrigos)


if __name__ == "__main__":
    if "--grafo-real" in sys.argv:
        resultado, memoria_mb, n_abrigos = medir_grafo_real(
            Path("dados/grafo_jardim_helena.json"), Path("docs/abrigos_sugeridos.json")
        )
        print(tabela_markdown([resultado]))
        print()
        print(f"abrigos: {n_abrigos} | Bellman-Ford, pico de memória: {memoria_mb:.0f} MB")
    else:
        print(tabela_markdown([medir(n) for n in TAMANHOS]))
