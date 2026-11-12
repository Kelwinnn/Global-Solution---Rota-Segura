"""Serviço FastAPI do contrato docs/contrato.md: GET /saude e POST /rotas.

Subir: python -m uvicorn rota_segura.api:app   (de dentro de dynamic-programming/)
O JSON da API é camelCase; os identificadores internos continuam em português/snake_case.
O mapa chega em cada requisição e o serviço não guarda mapa (só o cache de cálculo).
"""

import logging
import re
from collections.abc import Sequence
from typing import Any

import uvicorn
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel

from rota_segura.carregador import bairro_de_dados, cenario_de_dados
from rota_segura.dijkstra import rota_ate_abrigo
from rota_segura.modelos import EntradaInvalidaError, ResultadoRota, StatusRota
from rota_segura.servico import CacheArvores

registro = logging.getLogger("rota_segura.api")


class _Entrada(BaseModel):
    """Só aceita os nomes camelCase do contrato e não converte tipos (ex.: "100" não vira 100)."""

    model_config = ConfigDict(alias_generator=to_camel, strict=True)


class TrechoEntrada(_Entrada):
    origem: int
    destino: int
    distancia_m: float
    fator_risco: float = 0.0


class MapaEntrada(_Entrada):
    esquinas: list[int]
    trechos: list[TrechoEntrada]


class AbrigoEntrada(_Entrada):
    esquina: int
    capacidade: int
    ocupacao: int


class CenarioEntrada(_Entrada):
    trechos_alagados: list[list[int]]
    abrigos: list[AbrigoEntrada]


class RequisicaoRotas(_Entrada):
    mapa: MapaEntrada
    cenario: CenarioEntrada
    origens: list[int]


class _Saida(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class RotaSaida(_Saida):
    origem: int
    status: StatusRota
    caminho: list[int]
    distancia_m: float
    orientacao: str


class RespostaRotas(_Saida):
    rotas: list[RotaSaida]


class RespostaSaude(BaseModel):
    status: str


_SNAKE = re.compile(r"_([a-z])")


def _campo_na_api(mensagem: str) -> str:
    """Troca os nomes internos da mensagem do carregador pelos da API.

    'bairro.trechos[0].distancia_m: ...' vira 'mapa.trechos[0].distanciaM: ...'.
    Só mexe no trecho antes do primeiro ': ', que é onde o carregador cita o campo.
    """
    campo, separador, resto = mensagem.partition(": ")
    if not separador:
        return mensagem
    if campo == "bairro" or campo.startswith("bairro."):
        campo = "mapa" + campo.removeprefix("bairro")
    return _SNAKE.sub(lambda m: m.group(1).upper(), campo) + separador + resto.replace("no bairro", "no mapa")


def _mensagem_validacao(erros: Sequence[Any]) -> str:
    """Primeiro erro estrutural do pydantic, apontando o campo (já em camelCase, pois o loc usa o alias)."""
    erro = erros[0]
    if erro["type"] == "json_invalid":
        return "corpo: a requisição não é um JSON válido"
    campo = ""
    for parte in erro["loc"][1:]:  # [0] é 'body'
        campo += f"[{parte}]" if isinstance(parte, int) else f".{parte}" if campo else str(parte)
    detalhe = "campo obrigatório ausente" if erro["type"] == "missing" else f"valor inválido ({erro['msg']})"
    return f"{campo or 'corpo'}: {detalhe}"


def _erro(status: int, erro: str, mensagem: str) -> JSONResponse:
    return JSONResponse(status_code=status, content={"erro": erro, "mensagem": mensagem})


def _rota_saida(origem: int, resultado: ResultadoRota) -> RotaSaida:
    return RotaSaida(
        origem=origem,
        status=resultado.status,
        caminho=list(resultado.caminho),
        distancia_m=resultado.distancia_m,
        orientacao=resultado.orientacao,
    )


def criar_app(cache: CacheArvores | None = None) -> FastAPI:
    """Cria o serviço. Os testes passam um cache próprio para observar acertos e cálculos."""
    cache_rotas = cache if cache is not None else CacheArvores()
    app = FastAPI(title="Rota Segura", version="1.0.0")

    @app.exception_handler(RequestValidationError)
    async def _validacao(_: Request, erro: RequestValidationError) -> JSONResponse:
        return _erro(400, "entrada_invalida", _mensagem_validacao(erro.errors()))

    @app.exception_handler(EntradaInvalidaError)
    async def _entrada_invalida(_: Request, erro: EntradaInvalidaError) -> JSONResponse:
        return _erro(400, "entrada_invalida", _campo_na_api(str(erro)))

    @app.exception_handler(Exception)
    async def _inesperado(_: Request, erro: Exception) -> JSONResponse:
        registro.exception("falha inesperada", exc_info=erro)
        return _erro(500, "erro_interno", "Falha inesperada ao calcular a rota.")

    @app.get("/saude")
    def saude() -> RespostaSaude:
        return RespostaSaude(status="ok")

    @app.post("/rotas")
    def rotas(requisicao: RequisicaoRotas) -> RespostaRotas:
        """Uma busca multi-origem (ou o cache) atende todas as origens: O(1) de busca + O(L) por rota."""
        bairro = bairro_de_dados(requisicao.mapa.model_dump())
        cenario = cenario_de_dados(requisicao.cenario.model_dump(), bairro)
        for i, origem in enumerate(requisicao.origens):
            if origem not in bairro.esquinas:
                raise EntradaInvalidaError(f"origens[{i}]: esquina {origem} não existe no mapa")
        arvore = cache_rotas.arvore(bairro, cenario)
        return RespostaRotas(rotas=[_rota_saida(o, rota_ate_abrigo(arvore, o)) for o in requisicao.origens])

    return app


app = criar_app()

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000)
