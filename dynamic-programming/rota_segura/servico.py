"""Cache das árvores de rotas para o serviço HTTP.

No serviço o mapa chega em cada requisição, então o cache não pode assumir um
bairro fixo (como faz CalculadoraRotas). A chave é uma impressão digital
(SHA-256, estável entre processos) do mapa, dos trechos alagados e dos abrigos
com vaga. Mudou qualquer um dos três, mudou a chave e a árvore é recalculada.
Mudar só a ocupação, sem lotar nem liberar abrigo, não muda a chave (RN06).
"""

import hashlib
import json
from collections import OrderedDict
from threading import Lock

from rota_segura.dijkstra import dijkstra_multi_origem
from rota_segura.grafo import ArvoreRotas, abrigos_com_vaga, custo_trecho_mm, montar_grafo
from rota_segura.modelos import Bairro, Cenario, chave_trecho

CAPACIDADE_PADRAO = 128


def impressao_digital(bairro: Bairro, cenario: Cenario) -> str:
    """SHA-256 do que define o resultado: grafo (em mm, como os algoritmos o veem), alagados e abrigos com vaga.

    Independe da ordem das listas e do sentido do trecho. Tempo O((V + E) log E), memória O(V + E).
    """
    trechos = sorted(
        (*chave_trecho(t.origem, t.destino), round(t.distancia_m * 1000), custo_trecho_mm(t)) for t in bairro.trechos
    )
    conteudo = {
        "esquinas": sorted(bairro.esquinas),
        "trechos": trechos,
        "alagados": sorted(cenario.trechos_alagados),
        "abrigos": sorted(abrigos_com_vaga(cenario)),
    }
    return hashlib.sha256(json.dumps(conteudo, separators=(",", ":")).encode()).hexdigest()


class CacheArvores:
    """Memoização LRU da árvore do Dijkstra multi-origem, com limite de entradas.

    Sem limite, um serviço que recebe mapas diferentes a cada chamada cresceria sem parar.
    Acerto: O(V + E log E) para a impressão digital. Falha: soma o Dijkstra, O((V + E) log V).
    Memória: O(V) por entrada guardada.
    """

    def __init__(self, capacidade: int = CAPACIDADE_PADRAO) -> None:
        if capacidade < 1:
            raise ValueError(f"capacidade do cache deve ser >= 1, recebido {capacidade}")
        self._capacidade = capacidade
        self._entradas: OrderedDict[str, ArvoreRotas] = OrderedDict()
        self._trava = Lock()  # o FastAPI atende rotas síncronas em várias threads
        self.acertos = 0
        self.calculos = 0

    def arvore(self, bairro: Bairro, cenario: Cenario) -> ArvoreRotas:
        chave = impressao_digital(bairro, cenario)
        with self._trava:
            if chave in self._entradas:
                self._entradas.move_to_end(chave)
                self.acertos += 1
                return self._entradas[chave]
            grafo = montar_grafo(bairro, cenario.trechos_alagados)
            arvore = dijkstra_multi_origem(grafo, abrigos_com_vaga(cenario))
            self.calculos += 1
            self._entradas[chave] = arvore
            if len(self._entradas) > self._capacidade:
                self._entradas.popitem(last=False)
            return arvore
