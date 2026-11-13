import json
from pathlib import Path
from typing import Any

import pytest

from rota_segura.mapa import (
    AreaAlagada,
    Ponto,
    carregar_mapa,
    esquina_mais_proxima,
    haversine_m,
    mapa_de_dados,
    trechos_alagados,
)
from rota_segura.modelos import EntradaInvalidaError

# Linha de 3 esquinas a ~1.000 m uma da outra, no equador, para a conta ser fácil de conferir.
UM_GRAU_LNG_M = 111_194.9  # R·π/180 com R = 6.371.008,8 m


def _mapa() -> Any:
    return mapa_de_dados(
        {
            "esquinas": [{"id": i, "lat": 0.0, "lng": i * 0.01} for i in (1, 2, 3)],
            "trechos": [{"origem": 1, "destino": 2}, {"origem": 2, "destino": 3, "fatorRisco": 0.2}],
        }
    )


def test_haversine_de_um_grau_no_equador() -> None:
    assert haversine_m(Ponto(0, 0), Ponto(0, 1)) == pytest.approx(UM_GRAU_LNG_M, rel=1e-4)
    assert haversine_m(Ponto(-23.5, -46.4), Ponto(-23.5, -46.4)) == 0


def test_trecho_sem_distancia_usa_haversine() -> None:
    primeiro = _mapa().bairro.trechos[0]
    assert primeiro.distancia_m == pytest.approx(0.01 * UM_GRAU_LNG_M, rel=1e-4)
    assert primeiro.fator_risco == 0.0 and _mapa().bairro.trechos[1].fator_risco == 0.2


def test_esquina_mais_proxima_e_empate_pelo_menor_id() -> None:
    mapa = _mapa()
    assert esquina_mais_proxima(mapa, Ponto(0.0, 0.0199))[0] == 2
    assert esquina_mais_proxima(mapa, Ponto(0.0, 0.015))[0] == 1  # exatamente no meio: menor id


def test_trecho_alagado_pela_ponta_ou_pelo_ponto_medio() -> None:
    mapa = _mapa()
    # Raio de 100 m em volta da esquina 1: pega o trecho 1-2 pela ponta.
    assert trechos_alagados(mapa, [AreaAlagada(1, Ponto(0, 0.01), 100)]) == {(1, 2)}
    # Centro no meio do trecho 2-3, raio pequeno: nenhuma ponta dentro, só o ponto médio.
    assert trechos_alagados(mapa, [AreaAlagada(1, Ponto(0, 0.025), 50)]) == {(2, 3)}
    # Longe de tudo: nenhum.
    assert trechos_alagados(mapa, [AreaAlagada(1, Ponto(1.0, 1.0), 100)]) == frozenset()


@pytest.mark.parametrize(
    ("dados", "trecho_da_mensagem"),
    [
        ({"esquinas": [], "trechos": []}, "mapa.esquinas"),
        ({"esquinas": [{"id": 1, "lat": 0, "lng": 0}, {"id": 1, "lat": 0, "lng": 1}], "trechos": []}, "esquinas[1].id"),
        ({"esquinas": [{"id": 1, "lat": 99, "lng": 0}], "trechos": []}, "esquinas[0]"),
        ({"esquinas": [{"id": 1, "lat": 0, "lng": 0}], "trechos": [{"origem": 1, "destino": 2}]}, "trechos[0].destino"),
        (
            {"esquinas": [{"id": 1, "lat": 0, "lng": 0}, {"id": 2, "lat": 0, "lng": 1}],
             "trechos": [{"origem": 1, "destino": 2, "distanciaM": 0}]},
            "trechos[0].distanciaM",
        ),
        (
            {"esquinas": [{"id": 1, "lat": 0, "lng": 0}, {"id": 2, "lat": 0, "lng": 1}],
             "trechos": [{"origem": 1, "destino": 2, "fatorRisco": 1.5}]},
            "trechos[0].fatorRisco",
        ),
    ],
)
def test_mapa_invalido_aponta_o_campo(dados: dict[str, Any], trecho_da_mensagem: str) -> None:
    with pytest.raises(EntradaInvalidaError, match=trecho_da_mensagem.replace("[", r"\[").replace("]", r"\]")):
        mapa_de_dados(dados)


def test_carregar_mapa_de_arquivo(tmp_path: Path) -> None:
    arquivo = tmp_path / "mapa.json"
    arquivo.write_text(json.dumps({"esquinas": [{"id": 1, "lat": 0, "lng": 0}], "trechos": []}), encoding="utf-8")
    assert carregar_mapa(arquivo).bairro.esquinas == {1}
    with pytest.raises(EntradaInvalidaError, match="não foi possível abrir"):
        carregar_mapa(tmp_path / "nao_existe.json")
    arquivo.write_text("{", encoding="utf-8")
    with pytest.raises(EntradaInvalidaError, match="JSON válido"):
        carregar_mapa(arquivo)
