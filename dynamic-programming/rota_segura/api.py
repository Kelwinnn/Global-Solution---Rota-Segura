"""Serviço FastAPI do contrato v2 (docs/contrato.md): GET /health, POST /route, POST /risk.

Subir (de dentro de dynamic-programming/):
    python -m uvicorn rota_segura.api:app --port 8000
(sem variáveis usa dados/grafo_jardim_helena.json e modelos/risco_logistica.joblib; GRAFO_PATH e MODELO_PATH trocam)
O JSON da API é camelCase; os identificadores internos continuam em português/snake_case.
O mapa e o modelo são carregados na subida. Se algum faltar, o serviço sobe mesmo assim e /health avisa.
"""

import logging
import os
from datetime import datetime
from pathlib import Path

import uvicorn
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel

from rota_segura.mapa import AreaAlagada, Mapa, Ponto, carregar_mapa, validar_ponto
from rota_segura.modelos import EntradaInvalidaError, StatusRota
from rota_segura.risco import Leitura, ModeloRisco, carregar_modelo, prever
from rota_segura.roteador import AbrigoGeo, Roteador, RotaGeo

VERSAO = "2.0.0"
RAIZ = Path(__file__).resolve().parent.parent  # dynamic-programming/ (ou /app na imagem)
GRAFO_PADRAO = RAIZ / "dados" / "grafo_jardim_helena.json"
MODELO_PADRAO = RAIZ / "modelos" / "risco_logistica.joblib"
registro = logging.getLogger("rota_segura.api")


class ServicoIndisponivelError(Exception):
    """Mapa ou modelo não carregado: 503."""

    def __init__(self, codigo: str, mensagem: str) -> None:
        super().__init__(mensagem)
        self.codigo = codigo


class _Entrada(BaseModel):
    """Só aceita os nomes camelCase do contrato e não converte tipos (ex.: "100" não vira 100)."""

    model_config = ConfigDict(alias_generator=to_camel, strict=True)


class PontoEntrada(_Entrada):
    lat: float
    lng: float


class AbrigoEntrada(_Entrada):
    id: int
    name: str
    lat: float
    lng: float
    available_spots: int


class AreaEntrada(_Entrada):
    area_id: int
    lat: float
    lng: float
    radius_m: float


class RequisicaoRota(_Entrada):
    origin: PontoEntrada
    shelters: list[AbrigoEntrada]
    flooded_areas: list[AreaEntrada]


class LeituraEntrada(_Entrada):
    timestamp: str
    rain_mm: float
    water_level_cm: float


class RequisicaoRisco(_Entrada):
    area_id: int
    lat: float
    lng: float
    readings: list[LeituraEntrada]


class _Saida(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class RespostaSaude(_Saida):
    status: str
    map_loaded: bool
    model_loaded: bool
    version: str


class PontoSaida(_Saida):
    lat: float
    lng: float


class AbrigoSaida(_Saida):
    id: int
    name: str
    lat: float
    lng: float


class RespostaRota(_Saida):
    status: str
    shelter: AbrigoSaida | None
    distance_meters: int | None
    duration_min: int | None
    path: list[PontoSaida]
    message: str


class RespostaRisco(_Saida):
    area_id: int
    flood_probability: float
    high_probability: bool
    low_data: bool
    model_version: str


MENSAGEM_SEM_ROTA = "Não há rota segura agora. Vá para um local alto e aguarde ajuda."


def _erro(status: int, erro: str, mensagem: str) -> JSONResponse:
    return JSONResponse(status_code=status, content={"error": erro, "message": mensagem})


def _resposta_rota(rota: RotaGeo) -> RespostaRota:
    if rota.status is StatusRota.SEM_ROTA_SEGURA or rota.abrigo is None:
        return RespostaRota(
            status="NO_SAFE_ROUTE", shelter=None, distance_meters=None, duration_min=None, path=[], message=MENSAGEM_SEM_ROTA
        )
    abrigo = rota.abrigo
    return RespostaRota(
        status="ROUTE_FOUND",
        shelter=AbrigoSaida(id=abrigo.id, name=abrigo.nome, lat=abrigo.ponto.lat, lng=abrigo.ponto.lng),
        distance_meters=rota.distancia_m,
        duration_min=rota.duracao_min,
        path=[PontoSaida(lat=p.lat, lng=p.lng) for p in rota.caminho],
        message=f"Siga a rota até {abrigo.nome}.",
    )


def _leitura(entrada: LeituraEntrada, i: int) -> Leitura:
    try:
        instante = datetime.fromisoformat(entrada.timestamp)
    except ValueError as erro:
        raise EntradaInvalidaError(f"readings[{i}].timestamp: '{entrada.timestamp}' não é ISO 8601") from erro
    return Leitura(instante, entrada.rain_mm, entrada.water_level_cm)


def criar_app(mapa: Mapa | None = None, modelo: ModeloRisco | None = None) -> FastAPI:
    """Cria o serviço. Os testes passam o mapa e o modelo; em produção vêm de GRAFO_PATH e MODELO_PATH."""
    roteador = Roteador(mapa) if mapa is not None else None
    app = FastAPI(title="Rota Segura (serviço Python)", version=VERSAO)
    app.state.roteador = roteador

    @app.exception_handler(EntradaInvalidaError)
    async def _entrada_invalida(_: Request, erro: EntradaInvalidaError) -> JSONResponse:
        return _erro(400, "INVALID_INPUT", str(erro))

    @app.exception_handler(ServicoIndisponivelError)
    async def _indisponivel(_: Request, erro: ServicoIndisponivelError) -> JSONResponse:
        return _erro(503, erro.codigo, str(erro))

    @app.exception_handler(Exception)
    async def _inesperado(_: Request, erro: Exception) -> JSONResponse:
        registro.exception("falha inesperada", exc_info=erro)
        return _erro(500, "INTERNAL_ERROR", "Falha inesperada no serviço.")

    @app.get("/health")
    def health() -> RespostaSaude:
        return RespostaSaude(status="ok", map_loaded=roteador is not None, model_loaded=modelo is not None, version=VERSAO)

    @app.post("/route")
    def route(requisicao: RequisicaoRota) -> RespostaRota:
        """Uma busca multi-origem (ou a memoização) atende a chamada: O(V + E) para alagados, O(V) para esquinas."""
        if roteador is None:
            raise ServicoIndisponivelError("MAP_UNAVAILABLE", "O mapa do bairro não está carregado.")
        origem = validar_ponto(requisicao.origin.lat, requisicao.origin.lng, "origin")
        abrigos = [
            AbrigoGeo(a.id, a.name, validar_ponto(a.lat, a.lng, f"shelters[{i}]"), a.available_spots)
            for i, a in enumerate(requisicao.shelters)
        ]
        areas = [
            AreaAlagada(a.area_id, validar_ponto(a.lat, a.lng, f"floodedAreas[{i}]"), a.radius_m)
            for i, a in enumerate(requisicao.flooded_areas)
        ]
        return _resposta_rota(roteador.rota(origem, abrigos, areas))

    @app.post("/risk")
    def risk(requisicao: RequisicaoRisco) -> RespostaRisco:
        if modelo is None:
            raise ServicoIndisponivelError("MODEL_UNAVAILABLE", "O modelo de risco não está carregado.")
        validar_ponto(requisicao.lat, requisicao.lng, "lat/lng")
        leituras = [_leitura(l, i) for i, l in enumerate(requisicao.readings)]
        resultado = prever(modelo, leituras)
        return RespostaRisco(
            area_id=requisicao.area_id,
            flood_probability=resultado.probabilidade,
            high_probability=resultado.alta,
            low_data=resultado.poucos_dados,
            model_version=modelo.versao,
        )

    return app


def app_da_configuracao() -> FastAPI:
    """Lê GRAFO_PATH e MODELO_PATH (padrão: dados/grafo_jardim_helena.json e modelos/risco_logistica.joblib).

    Falha ao carregar não derruba o serviço: /health mostra o que faltou.
    """
    mapa: Mapa | None = None
    modelo: ModeloRisco | None = None
    try:
        mapa = carregar_mapa(Path(os.environ.get("GRAFO_PATH", GRAFO_PADRAO)))
    except EntradaInvalidaError as erro:
        registro.error("mapa não carregado: %s", erro)
    try:
        modelo = carregar_modelo(Path(os.environ.get("MODELO_PATH", MODELO_PADRAO)))
    except EntradaInvalidaError as erro:
        registro.error("modelo não carregado: %s", erro)
    return criar_app(mapa, modelo)


app = app_da_configuracao()

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=int(os.environ.get("PORT", "8000")))
