"""Rota segura em coordenadas: liga o contrato (lat/lng) ao Dijkstra multi-origem sobre o mapa fixo.

Passos de cada chamada:
 1. origem e abrigos viram a esquina mais próxima (haversine); origem a mais de 300 m = entrada inválida;
 2. abrigo sem vaga é ignorado (RN06);
 3. áreas alagadas tiram trechos do grafo (RN05);
 4. a árvore do Dijkstra multi-origem vem da memoização (chave = alagados + abrigos com vaga);
 5. distanceMeters = grafo + perna origem->esquina + perna esquina->abrigo; durationMin = ceil(distância / 70).
"""

import math
from collections.abc import Sequence
from dataclasses import dataclass

from rota_segura.dijkstra import rota_ate_abrigo
from rota_segura.mapa import (
    LIMITE_ORIGEM_M,
    AreaAlagada,
    Mapa,
    Ponto,
    esquina_mais_proxima,
    haversine_m,
    trechos_alagados,
)
from rota_segura.memoizacao import CalculadoraRotas
from rota_segura.modelos import Abrigo, Cenario, EntradaInvalidaError, StatusRota

METROS_POR_MINUTO = 70


@dataclass(frozen=True, slots=True)
class AbrigoGeo:
    id: int
    nome: str
    ponto: Ponto
    vagas: int


@dataclass(frozen=True, slots=True)
class RotaGeo:
    status: StatusRota
    abrigo: AbrigoGeo | None
    distancia_m: int | None
    duracao_min: int | None
    caminho: tuple[Ponto, ...]


SEM_ROTA = RotaGeo(StatusRota.SEM_ROTA_SEGURA, None, None, None, ())


def duracao_min(distancia_m: int) -> int:
    """A pé a 70 m/min, arredondado para cima."""
    return math.ceil(distancia_m / METROS_POR_MINUTO)


class Roteador:
    def __init__(self, mapa: Mapa, capacidade_cache: int = 128) -> None:
        self.mapa = mapa
        self.calculadora = CalculadoraRotas(mapa.bairro, capacidade_cache)

    def rota(self, origem: Ponto, abrigos: Sequence[AbrigoGeo], areas: Sequence[AreaAlagada]) -> RotaGeo:
        for area in areas:
            if area.raio_m <= 0:
                raise EntradaInvalidaError(f"floodedAreas: raio da área {area.area_id} deve ser maior que zero")
        ids = [a.id for a in abrigos]
        if len(set(ids)) != len(ids):
            raise EntradaInvalidaError("shelters: ids de abrigo repetidos")
        for a in abrigos:
            if a.vagas < 0:
                raise EntradaInvalidaError(f"shelters: abrigo {a.id} com vagas negativas ({a.vagas})")

        esquina_origem, distancia_origem = esquina_mais_proxima(self.mapa, origem)
        if distancia_origem > LIMITE_ORIGEM_M:
            raise EntradaInvalidaError(
                f"origin: a {distancia_origem:.0f} m da esquina mais próxima "
                f"(máximo {LIMITE_ORIGEM_M:.0f} m); fora do bairro-piloto"
            )

        com_vaga = {a.id: a for a in abrigos if a.vagas > 0}  # RN06
        cenario = Cenario(
            trechos_alagados(self.mapa, areas),
            tuple(
                Abrigo(esquina_mais_proxima(self.mapa, a.ponto)[0], a.vagas, 0, a.id)
                for a in com_vaga.values()
            ),
        )
        arvore = self.calculadora.arvore(cenario)
        resultado = rota_ate_abrigo(arvore, esquina_origem)
        if resultado.status is StatusRota.SEM_ROTA_SEGURA:
            return SEM_ROTA

        escolhido = com_vaga[arvore.rotulos[esquina_origem][2]]
        coordenadas = self.mapa.coordenadas
        esquina_abrigo = resultado.caminho[-1]
        total = (
            resultado.distancia_m
            + distancia_origem
            + haversine_m(coordenadas[esquina_abrigo], escolhido.ponto)
        )
        distancia = round(total)
        pontos = [origem, *(coordenadas[e] for e in resultado.caminho), escolhido.ponto]
        caminho = tuple(p for i, p in enumerate(pontos) if i == 0 or p != pontos[i - 1])
        return RotaGeo(StatusRota.ROTA_ENCONTRADA, escolhido, distancia, duracao_min(distancia), caminho)

