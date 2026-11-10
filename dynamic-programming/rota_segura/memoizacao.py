"""Memoização do resultado por cenário.

Um mesmo alerta gera muitas consultas iguais. A árvore do Dijkstra multi-origem
já responde a todas as esquinas, então ela é calculada uma vez por cenário e
reaproveitada. A chave é (trechos alagados, esquinas de abrigos com vaga):
mudou o cenário, mudou a chave. Mudar só a ocupação, sem lotar nem liberar
nenhum abrigo, não muda a rota e reaproveita o cálculo.
"""

from rota_segura.dijkstra import dijkstra_multi_origem, rota_ate_abrigo
from rota_segura.grafo import ArvoreRotas, abrigos_com_vaga, montar_grafo
from rota_segura.modelos import Bairro, Cenario, EntradaInvalidaError, ResultadoRota

ChaveCenario = tuple[frozenset[tuple[int, int]], frozenset[int]]


class CalculadoraRotas:
    """Calcula rotas em um bairro fixo, guardando uma árvore de rotas por cenário."""

    def __init__(self, bairro: Bairro) -> None:
        self._bairro = bairro
        self._memo: dict[ChaveCenario, ArvoreRotas] = {}

    def arvore(self, cenario: Cenario) -> ArvoreRotas:
        """Primeira consulta do cenário: O((V + E) log V). Repetidas: O(A) para montar a chave,
        com A = número de abrigos e trechos alagados. Memória: O(V) por cenário guardado.
        """
        chave: ChaveCenario = (cenario.trechos_alagados, abrigos_com_vaga(cenario))
        if chave not in self._memo:
            grafo = montar_grafo(self._bairro, cenario.trechos_alagados)
            self._memo[chave] = dijkstra_multi_origem(grafo, chave[1])
        return self._memo[chave]

    def rota(self, cenario: Cenario, origem: int) -> ResultadoRota:
        """Rota da origem até o abrigo com vaga mais próximo, ou SEM_ROTA_SEGURA."""
        if origem not in self._bairro.esquinas:
            raise EntradaInvalidaError(f"origem: esquina {origem} não existe no bairro")
        return rota_ate_abrigo(self.arvore(cenario), origem)
