from dataclasses import dataclass
from enum import Enum


class EntradaInvalidaError(ValueError):
    """Dado de entrada invalido: esquina inexistente, distancia <=0, etc."""


class StatusRota(Enum):
    ROTA_ENCONTRADA = "rota_encontrada"
    SEM_ROTA_SEGURA = "sem_rota_segura"


@dataclass(frozen=True, slots=True)
class Trecho:
    origem: int
    destino: int
    distancia_m: float
    fator_risco: float = 0.0


@dataclass(frozen=True, slots=True)
class Abrigo:
    esquina: int
    capacidade: int
    ocupacao: int

    @property
    def tem_vaga(self) -> bool:
        return self.ocupacao < self.capacidade


@dataclass(frozen=True, slots=True)
class ResultadoRota:
    status: StatusRota
    caminho: tuple[int, ...]
    distancia_m: float
    orientacao: str


def chave_trecho(a: int, b: int) -> tuple[int, int]:
    """Identifica o trecho sem depender do sentido: (2, 3) e (3, 2) são a mesma rua."""
    return (a, b) if a <= b else (b, a)


@dataclass(frozen=True, slots=True)
class Bairro:
    esquinas: frozenset[int]
    trechos: tuple[Trecho, ...]


@dataclass(frozen=True, slots=True)
class Cenario:
    trechos_alagados: frozenset[tuple[int, int]]  # pares normalizados por chave_trecho
    abrigos: tuple[Abrigo, ...]
