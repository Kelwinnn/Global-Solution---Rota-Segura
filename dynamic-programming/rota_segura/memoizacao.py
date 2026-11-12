"""Memoização do resultado por cenário.

Um mesmo alerta gera muitas consultas iguais. A árvore do Dijkstra multi-origem
já responde a todas as esquinas, então ela é calculada uma vez por cenário e
reaproveitada. A chave é (trechos alagados, esquinas de abrigos com vaga):
mudou o cenário, mudou a chave. Mudar só a ocupação, sem lotar nem liberar
nenhum abrigo, não muda a rota e reaproveita o cálculo.
"""

from collections import OrderedDict
from threading import Lock

from rota_segura.dijkstra import dijkstra_multi_origem, rota_ate_abrigo
from rota_segura.grafo import ArvoreRotas, abrigos_com_vaga, montar_grafo
from rota_segura.modelos import Bairro, Cenario, EntradaInvalidaError, ResultadoRota

ChaveCenario = tuple[frozenset[tuple[int, int]], frozenset[tuple[int, int]]]  # (alagados, (esquina, id) com vaga)
CAPACIDADE_PADRAO = 128


class CalculadoraRotas:
    """Calcula rotas em um bairro fixo, guardando uma árvore de rotas por cenário (LRU limitado).

    No serviço o mapa é fixo, então a chave é só o cenário. O limite evita crescer sem fim
    quando cada chamada traz áreas alagadas diferentes.
    """

    def __init__(self, bairro: Bairro, capacidade: int = CAPACIDADE_PADRAO) -> None:
        if capacidade < 1:
            raise ValueError(f"capacidade do cache deve ser >= 1, recebido {capacidade}")
        self._bairro = bairro
        self._capacidade = capacidade
        self._memo: OrderedDict[ChaveCenario, ArvoreRotas] = OrderedDict()
        self._trava = Lock()  # o FastAPI atende rotas síncronas em várias threads
        self.acertos = 0
        self.calculos = 0

    def arvore(self, cenario: Cenario) -> ArvoreRotas:
        """Primeira consulta do cenário: O((V + E) log V). Repetidas: O(A) para montar a chave,
        com A = número de abrigos e trechos alagados. Memória: O(V) por cenário guardado.
        """
        abrigos = abrigos_com_vaga(cenario)
        chave: ChaveCenario = (cenario.trechos_alagados, frozenset(abrigos.items()))
        with self._trava:
            if chave in self._memo:
                self._memo.move_to_end(chave)
                self.acertos += 1
                return self._memo[chave]
            grafo = montar_grafo(self._bairro, cenario.trechos_alagados)
            arvore = dijkstra_multi_origem(grafo, abrigos)
            self.calculos += 1
            self._memo[chave] = arvore
            if len(self._memo) > self._capacidade:
                self._memo.popitem(last=False)
            return arvore

    def rota(self, cenario: Cenario, origem: int) -> ResultadoRota:
        """Rota da origem até o abrigo com vaga mais próximo, ou SEM_ROTA_SEGURA."""
        if origem not in self._bairro.esquinas:
            raise EntradaInvalidaError(f"origem: esquina {origem} não existe no bairro")
        return rota_ate_abrigo(self.arvore(cenario), origem)
