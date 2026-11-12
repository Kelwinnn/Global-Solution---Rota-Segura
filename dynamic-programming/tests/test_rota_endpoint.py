from pathlib import Path
from typing import Any

import pytest
from fastapi.testclient import TestClient

from rota_segura.api import criar_app
from rota_segura.mapa import Mapa, Ponto, carregar_mapa, haversine_m, mapa_de_dados
from rota_segura.roteador import duracao_min

MAPA_TESTE = Path(__file__).resolve().parent.parent / "dados" / "grafo_teste_jardim_helena.json"


def esquina(mapa: Mapa, numero: int) -> dict[str, float]:
    ponto = mapa.coordenadas[numero]
    return {"lat": ponto.lat, "lng": ponto.lng}


def abrigo(mapa: Mapa, identificador: int, numero_esquina: int, vagas: int = 10) -> dict[str, Any]:
    return {"id": identificador, "name": f"Abrigo {identificador}", **esquina(mapa, numero_esquina), "availableSpots": vagas}


def area(mapa: Mapa, numero_esquina: int, raio: float, area_id: int = 1) -> dict[str, Any]:
    return {"areaId": area_id, **esquina(mapa, numero_esquina), "radiusM": raio}


@pytest.fixture(scope="module")
def mapa() -> Mapa:
    return carregar_mapa(MAPA_TESTE)


@pytest.fixture()
def cliente(mapa: Mapa) -> TestClient:
    return TestClient(criar_app(mapa))


def pedir(cliente: TestClient, origem: dict[str, float], abrigos: list[dict[str, Any]], areas: list[dict[str, Any]]) -> Any:
    return cliente.post("/route", json={"origin": origem, "shelters": abrigos, "floodedAreas": areas})


def mapa_de_duas_esquinas(distancia_m: float) -> Mapa:
    return mapa_de_dados(
        {
            "esquinas": [{"id": 1, "lat": -23.49, "lng": -46.41}, {"id": 2, "lat": -23.49, "lng": -46.40}],
            "trechos": [{"origem": 1, "destino": 2, "distanciaM": distancia_m}],
        }
    )


def test_rota_nao_passa_por_area_alagada(cliente: TestClient, mapa: Mapa) -> None:
    # Esquinas 1 a 5 formam a linha de baixo; a esquina 3 (meio) fica alagada.
    direta = pedir(cliente, esquina(mapa, 1), [abrigo(mapa, 1, 5)], []).json()
    assert direta["status"] == "ROUTE_FOUND"

    centro = area(mapa, 3, 30)
    resposta = pedir(cliente, esquina(mapa, 1), [abrigo(mapa, 1, 5)], [centro])
    corpo = resposta.json()
    assert resposta.status_code == 200 and corpo["status"] == "ROUTE_FOUND"
    centro_ponto = Ponto(centro["lat"], centro["lng"])
    assert all(haversine_m(centro_ponto, Ponto(p["lat"], p["lng"])) > 30 for p in corpo["path"])
    assert corpo["distanceMeters"] > direta["distanceMeters"]  # desvio mais longo


def test_abrigo_lotado_e_ignorado(cliente: TestClient, mapa: Mapa) -> None:
    perto_lotado = abrigo(mapa, 1, 14, vagas=0)
    longe_com_vaga = abrigo(mapa, 2, 5, vagas=3)
    corpo = pedir(cliente, esquina(mapa, 13), [perto_lotado, longe_com_vaga], []).json()
    assert corpo["status"] == "ROUTE_FOUND"
    assert corpo["shelter"]["id"] == 2


def test_sem_rota_segura_responde_200_no_safe_route(cliente: TestClient, mapa: Mapa) -> None:
    resposta = pedir(cliente, esquina(mapa, 13), [abrigo(mapa, 1, 5)], [area(mapa, 13, 150)])
    assert resposta.status_code == 200
    assert resposta.json() == {
        "status": "NO_SAFE_ROUTE",
        "shelter": None,
        "distanceMeters": None,
        "durationMin": None,
        "path": [],
        "message": "Não há rota segura agora. Vá para um local alto e aguarde ajuda.",
    }
    todos_lotados = pedir(cliente, esquina(mapa, 13), [abrigo(mapa, 1, 14, vagas=0)], [])
    assert todos_lotados.status_code == 200 and todos_lotados.json()["status"] == "NO_SAFE_ROUTE"


def test_origem_longe_do_mapa_responde_400(cliente: TestClient, mapa: Mapa) -> None:
    resposta = pedir(cliente, {"lat": -23.60, "lng": -46.50}, [abrigo(mapa, 1, 5)], [])
    assert resposta.status_code == 400
    corpo = resposta.json()
    assert corpo["error"] == "INVALID_INPUT" and "origin" in corpo["message"] and "300 m" in corpo["message"]


def test_desempate_por_distancia_e_id() -> None:
    # Desempate por distância: mesmo custo (100 m × 1,5 = 150 m × 1,0), vence a rota mais curta.
    curta_arriscada = mapa_de_dados(
        {
            "esquinas": [
                {"id": 1, "lat": -23.49, "lng": -46.41},
                {"id": 2, "lat": -23.49, "lng": -46.40},
                {"id": 3, "lat": -23.49, "lng": -46.42},
            ],
            "trechos": [
                {"origem": 1, "destino": 2, "distanciaM": 100, "fatorRisco": 0.5},
                {"origem": 1, "destino": 3, "distanciaM": 150},
            ],
        }
    )
    cliente = TestClient(criar_app(curta_arriscada))
    por_distancia = pedir(
        cliente, esquina(curta_arriscada, 1), [abrigo(curta_arriscada, 1, 3), abrigo(curta_arriscada, 9, 2)], []
    ).json()
    assert por_distancia["shelter"]["id"] == 9 and por_distancia["distanceMeters"] == 100

    # Desempate por id: custo e distância iguais; ganha o menor ID do abrigo, não a menor esquina.
    mapa = carregar_mapa(MAPA_TESTE)
    cliente_grade = TestClient(criar_app(mapa))
    esquerda, direita = abrigo(mapa, 7, 12), abrigo(mapa, 2, 14)
    por_id = pedir(cliente_grade, esquina(mapa, 13), [esquerda, direita], []).json()
    assert por_id["shelter"]["id"] == 2
    invertido = pedir(cliente_grade, esquina(mapa, 13), [direita, esquerda], []).json()
    assert invertido["shelter"]["id"] == 2


def test_duracao_usa_70_metros_por_minuto() -> None:
    assert [duracao_min(d) for d in (0, 1, 70, 71, 140, 141)] == [0, 1, 1, 2, 2, 3]
    for distancia, esperado in ((140, 2), (141, 3)):
        mapa = mapa_de_duas_esquinas(distancia)
        cliente = TestClient(criar_app(mapa))
        corpo = pedir(cliente, esquina(mapa, 1), [abrigo(mapa, 1, 2)], []).json()
        assert corpo["distanceMeters"] == distancia and corpo["durationMin"] == esperado


def test_caminho_comeca_na_origem_e_termina_no_abrigo(cliente: TestClient, mapa: Mapa) -> None:
    origem = {"lat": mapa.coordenadas[1].lat + 0.0001, "lng": mapa.coordenadas[1].lng}  # ~11 m da esquina 1
    destino = abrigo(mapa, 1, 5)
    corpo = pedir(cliente, origem, [destino], []).json()
    assert corpo["path"][0] == origem
    assert corpo["path"][-1] == {"lat": destino["lat"], "lng": destino["lng"]}
    assert corpo["shelter"] == {"id": 1, "name": "Abrigo 1", "lat": destino["lat"], "lng": destino["lng"]}
    assert corpo["message"] == "Siga a rota até Abrigo 1."
    assert corpo["distanceMeters"] >= 400 + 10  # grafo (~400 m) + perna da origem (~11 m)


@pytest.mark.parametrize(
    ("alteracao", "campo"),
    [
        ({"floodedAreas": [{"areaId": 1, "lat": -23.49, "lng": -46.41, "radiusM": 0}]}, "raio"),
        ({"shelters": [{"id": 1, "name": "A", "lat": -23.49, "lng": -46.41, "availableSpots": -1}]}, "vagas"),
        ({"origin": {"lat": 120.0, "lng": -46.41}}, "origin"),
    ],
)
def test_entrada_invalida_responde_400(cliente: TestClient, mapa: Mapa, alteracao: dict[str, Any], campo: str) -> None:
    corpo = {"origin": esquina(mapa, 1), "shelters": [abrigo(mapa, 1, 5)], "floodedAreas": []} | alteracao
    resposta = cliente.post("/route", json=corpo)
    assert resposta.status_code == 400 and resposta.json()["error"] == "INVALID_INPUT"
    assert campo in resposta.json()["message"]


def test_json_fora_do_esquema_responde_422(cliente: TestClient) -> None:
    assert cliente.post("/route", json={"origin": {"lat": "x"}}).status_code == 422


def test_sem_mapa_responde_503_map_unavailable() -> None:
    cliente = TestClient(criar_app(None))
    resposta = cliente.post("/route", json={"origin": {"lat": -23.49, "lng": -46.41}, "shelters": [], "floodedAreas": []})
    assert resposta.status_code == 503 and resposta.json()["error"] == "MAP_UNAVAILABLE"


def test_health_informa_mapa_e_modelo(mapa: Mapa) -> None:
    assert TestClient(criar_app(mapa)).get("/health").json() == {
        "status": "ok",
        "mapLoaded": True,
        "modelLoaded": False,
        "version": "2.0.0",
    }
    assert TestClient(criar_app(None)).get("/health").json()["mapLoaded"] is False


def test_falha_inesperada_responde_500(mapa: Mapa, monkeypatch: pytest.MonkeyPatch) -> None:
    app = criar_app(mapa)

    def quebrar(*_: object, **__: object) -> None:
        raise RuntimeError("segredo interno")

    monkeypatch.setattr(app.state.roteador, "rota", quebrar)
    cliente = TestClient(app, raise_server_exceptions=False)
    resposta = cliente.post("/route", json={"origin": esquina(mapa, 1), "shelters": [], "floodedAreas": []})
    assert resposta.status_code == 500
    assert resposta.json()["error"] == "INTERNAL_ERROR" and "segredo" not in resposta.json()["message"]


def test_memoizacao_reaproveita_o_cenario(mapa: Mapa) -> None:
    app = criar_app(mapa)
    cliente = TestClient(app)
    for origem in (1, 2, 3):
        pedir(cliente, esquina(mapa, origem), [abrigo(mapa, 1, 25)], [area(mapa, 13, 30)])
    calculadora = app.state.roteador.calculadora
    assert (calculadora.calculos, calculadora.acertos) == (1, 2)
    pedir(cliente, esquina(mapa, 1), [abrigo(mapa, 1, 25)], [])  # outro cenário (sem alagado)
    assert calculadora.calculos == 2
