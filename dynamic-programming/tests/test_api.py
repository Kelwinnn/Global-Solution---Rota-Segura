import copy
from typing import Any

import pytest
from fastapi.testclient import TestClient

from rota_segura.api import app, criar_app
from rota_segura.servico import CacheArvores

# Mapa consistente: 1 -(120, risco 0,1)- 2 -(80)- 3 -(60, risco 0,3)- 4, e 2 -(50)- 5.
MAPA: dict[str, Any] = {
    "esquinas": [1, 2, 3, 4, 5],
    "trechos": [
        {"origem": 1, "destino": 2, "distanciaM": 120.0, "fatorRisco": 0.1},
        {"origem": 2, "destino": 3, "distanciaM": 80.0},
        {"origem": 3, "destino": 4, "distanciaM": 60.0, "fatorRisco": 0.3},
        {"origem": 2, "destino": 5, "distanciaM": 50.0},
    ],
}
ABRIGO_4: dict[str, Any] = {"esquina": 4, "capacidade": 50, "ocupacao": 10}
ABRIGO_5_LOTADO: dict[str, Any] = {"esquina": 5, "capacidade": 20, "ocupacao": 20}


def _requisicao(
    origens: list[int],
    alagados: list[list[int]] | None = None,
    abrigos: list[dict[str, Any]] | None = None,
    mapa: dict[str, Any] | None = None,
) -> dict[str, Any]:
    return {
        "mapa": copy.deepcopy(MAPA if mapa is None else mapa),
        "cenario": {
            "trechosAlagados": alagados or [],
            "abrigos": copy.deepcopy([ABRIGO_4] if abrigos is None else abrigos),
        },
        "origens": origens,
    }


@pytest.fixture
def cache() -> CacheArvores:
    return CacheArvores()


@pytest.fixture
def cliente(cache: CacheArvores) -> TestClient:
    return TestClient(criar_app(cache))


def test_saude_responde_ok() -> None:
    resposta = TestClient(app).get("/saude")
    assert resposta.status_code == 200
    assert resposta.json() == {"status": "ok"}


def test_rota_valida_segue_o_caminho_de_menor_custo_ate_o_abrigo(cliente: TestClient) -> None:
    resposta = cliente.post("/rotas", json=_requisicao([1]))

    assert resposta.status_code == 200
    assert resposta.json() == {
        "rotas": [
            {
                "origem": 1,
                "status": "rota_encontrada",
                "caminho": [1, 2, 3, 4],
                "distanciaM": 260.0,
                "orientacao": "Siga a rota até o abrigo na esquina 4",
            }
        ]
    }


def test_trecho_alagado_fica_fora_do_grafo_nos_dois_sentidos_rn05(cliente: TestClient) -> None:
    abrigos = [ABRIGO_4, {"esquina": 5, "capacidade": 20, "ocupacao": 0}]

    livre = cliente.post("/rotas", json=_requisicao([1], abrigos=abrigos)).json()["rotas"][0]
    alagado = cliente.post("/rotas", json=_requisicao([1], alagados=[[5, 2]], abrigos=abrigos)).json()["rotas"][0]

    assert livre["caminho"] == [1, 2, 5]
    assert alagado["caminho"] == [1, 2, 3, 4]  # o trecho 2-5 foi informado como [5, 2] e saiu do grafo


def test_abrigo_lotado_e_ignorado_mesmo_sendo_o_mais_proximo_rn06(cliente: TestClient) -> None:
    resposta = cliente.post("/rotas", json=_requisicao([1], abrigos=[ABRIGO_4, ABRIGO_5_LOTADO]))

    rota = resposta.json()["rotas"][0]
    assert rota["caminho"] == [1, 2, 3, 4]


def test_sem_rota_segura_responde_200_com_orientacao_de_ficar_em_local_alto(cliente: TestClient) -> None:
    resposta = cliente.post("/rotas", json=_requisicao([1], alagados=[[2, 3]]))

    assert resposta.status_code == 200
    assert resposta.json()["rotas"] == [
        {
            "origem": 1,
            "status": "sem_rota_segura",
            "caminho": [],
            "distanciaM": 0.0,
            "orientacao": "Fique em local alto",
        }
    ]


def test_varias_origens_usam_uma_unica_busca_e_devolvem_uma_rota_por_origem(
    cliente: TestClient, cache: CacheArvores
) -> None:
    resposta = cliente.post("/rotas", json=_requisicao([1, 3, 4, 5]))

    rotas = resposta.json()["rotas"]
    assert [r["origem"] for r in rotas] == [1, 3, 4, 5]
    assert [r["caminho"] for r in rotas] == [[1, 2, 3, 4], [3, 4], [4], [5, 2, 3, 4]]
    assert rotas[2]["distanciaM"] == 0.0  # origem que já é abrigo com vaga
    assert cache.calculos == 1


def test_resposta_usa_camel_case_e_requisicao_so_aceita_camel_case(cliente: TestClient) -> None:
    rota = cliente.post("/rotas", json=_requisicao([1])).json()["rotas"][0]
    assert set(rota) == {"origem", "status", "caminho", "distanciaM", "orientacao"}

    snake = _requisicao([1])
    snake["mapa"]["trechos"][0] = {"origem": 1, "destino": 2, "distancia_m": 120.0}
    resposta = cliente.post("/rotas", json=snake)
    assert resposta.status_code == 400
    assert resposta.json() == {"erro": "entrada_invalida", "mensagem": "mapa.trechos[0].distanciaM: campo obrigatório ausente"}


def test_fator_de_risco_ausente_vale_zero(cliente: TestClient) -> None:
    mapa = {"esquinas": [1, 2], "trechos": [{"origem": 1, "destino": 2, "distanciaM": 100.0}]}
    requisicao = _requisicao([1], abrigos=[{"esquina": 2, "capacidade": 5, "ocupacao": 0}], mapa=mapa)

    rota = cliente.post("/rotas", json=requisicao).json()["rotas"][0]

    assert rota["distanciaM"] == 100.0


def test_cache_e_reaproveitado_com_mesmo_mapa_e_cenario(cliente: TestClient, cache: CacheArvores) -> None:
    cliente.post("/rotas", json=_requisicao([1]))
    mapa_reordenado = copy.deepcopy(MAPA)
    mapa_reordenado["trechos"].reverse()
    mapa_reordenado["trechos"][0]["origem"], mapa_reordenado["trechos"][0]["destino"] = 5, 2  # mesmo trecho, outro sentido
    cliente.post("/rotas", json=_requisicao([4], mapa=mapa_reordenado))
    # só a ocupação mudou, sem lotar o abrigo: o cenário é o mesmo
    cliente.post("/rotas", json=_requisicao([1], abrigos=[{**ABRIGO_4, "ocupacao": 49}]))

    assert cache.calculos == 1
    assert cache.acertos == 2


def test_cache_e_recalculado_quando_o_mapa_muda(cliente: TestClient, cache: CacheArvores) -> None:
    cliente.post("/rotas", json=_requisicao([1]))
    mapa_novo = copy.deepcopy(MAPA)
    mapa_novo["trechos"][1]["distanciaM"] = 81.0

    rota = cliente.post("/rotas", json=_requisicao([1], mapa=mapa_novo)).json()["rotas"][0]

    assert cache.calculos == 2
    assert rota["distanciaM"] == 261.0


def test_cache_e_recalculado_quando_o_trecho_alagado_ou_o_abrigo_com_vaga_muda(
    cliente: TestClient, cache: CacheArvores
) -> None:
    cliente.post("/rotas", json=_requisicao([1]))
    cliente.post("/rotas", json=_requisicao([1], alagados=[[2, 5]]))
    assert cache.calculos == 2

    cliente.post("/rotas", json=_requisicao([1], abrigos=[{**ABRIGO_4, "ocupacao": 50}]))  # lotou
    assert cache.calculos == 3


def test_cache_descarta_o_menos_recente_ao_passar_do_limite() -> None:
    cache = CacheArvores(capacidade=1)
    cliente = TestClient(criar_app(cache))

    cliente.post("/rotas", json=_requisicao([1]))
    cliente.post("/rotas", json=_requisicao([1], alagados=[[2, 5]]))
    cliente.post("/rotas", json=_requisicao([1]))  # a primeira árvore já saiu do cache

    assert cache.calculos == 3
    assert cache.acertos == 0


def _requisicao_com(caminho: list[str | int], valor: Any) -> dict[str, Any]:
    requisicao = _requisicao([1])
    alvo: Any = requisicao
    for parte in caminho[:-1]:
        alvo = alvo[parte]
    alvo[caminho[-1]] = valor
    return requisicao


@pytest.mark.parametrize(
    ("caminho", "valor", "trecho_da_mensagem"),
    [
        (["mapa", "trechos", 0, "destino"], 9, "mapa.trechos[0].destino: esquina 9 não existe no mapa"),
        (["mapa", "trechos", 1, "distanciaM"], 0, "mapa.trechos[1].distanciaM: a distância deve ser maior que zero"),
        (["mapa", "trechos", 0, "fatorRisco"], 1.5, "mapa.trechos[0].fatorRisco: o fator de risco deve ser no máximo 1"),
        (["cenario", "abrigos", 0, "capacidade"], -1, "cenario.abrigos[0].capacidade: a capacidade não pode ser negativa"),
        (["cenario", "abrigos", 0, "ocupacao"], -1, "cenario.abrigos[0].ocupacao: a ocupação não pode ser negativa"),
        (["cenario", "trechosAlagados"], [[1, 3]], "cenario.trechosAlagados[0]: não existe trecho entre as esquinas 1 e 3"),
        (["origens"], [1, 42], "origens[1]: esquina 42 não existe no mapa"),
        (["mapa", "trechos", 0, "distanciaM"], "120", "mapa.trechos[0].distanciaM: valor inválido"),
        (["origens"], "1", "origens: valor inválido"),
    ],
)
def test_entrada_invalida_responde_400_apontando_o_campo(
    cliente: TestClient, caminho: list[str | int], valor: Any, trecho_da_mensagem: str
) -> None:
    resposta = cliente.post("/rotas", json=_requisicao_com(caminho, valor))

    assert resposta.status_code == 400
    corpo = resposta.json()
    assert corpo["erro"] == "entrada_invalida"
    assert trecho_da_mensagem in corpo["mensagem"]


def test_corpo_que_nao_e_json_responde_400(cliente: TestClient) -> None:
    resposta = cliente.post("/rotas", content="{ isto não é json", headers={"Content-Type": "application/json"})

    assert resposta.status_code == 400
    assert resposta.json()["erro"] == "entrada_invalida"


def test_falha_inesperada_responde_500_no_formato_do_contrato(
    cache: CacheArvores, monkeypatch: pytest.MonkeyPatch
) -> None:
    def quebrar(*_: Any) -> None:
        raise RuntimeError("segredo interno")

    monkeypatch.setattr(cache, "arvore", quebrar)
    cliente = TestClient(criar_app(cache), raise_server_exceptions=False)

    resposta = cliente.post("/rotas", json=_requisicao([1]))

    assert resposta.status_code == 500
    assert resposta.json() == {"erro": "erro_interno", "mensagem": "Falha inesperada ao calcular a rota."}
