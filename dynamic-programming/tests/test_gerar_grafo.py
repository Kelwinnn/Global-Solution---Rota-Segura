import json
from pathlib import Path

import networkx as nx

from rota_segura.gerar_grafo import grafo_para_mapa
from rota_segura.mapa import mapa_de_dados

META = {"fonte": "© colaboradores do OpenStreetMap (ODbL)"}


def _grafo() -> nx.MultiDiGraph:
    """Como o osmnx devolve: dirigido, com trechos nos dois sentidos, paralelos e um laço."""
    g: nx.MultiDiGraph = nx.MultiDiGraph()
    for no, (lat, lng) in {1: (-23.480, -46.420), 2: (-23.481, -46.420), 3: (-23.482, -46.420), 4: (-23.500, -46.400)}.items():
        g.add_node(no, y=lat, x=lng)
    g.add_edge(1, 2, length=110.0)
    g.add_edge(2, 1, length=110.0)
    g.add_edge(2, 3, length=90.0)
    g.add_edge(2, 3, length=120.0)  # paralelo mais longo: descartado
    g.add_edge(3, 3, length=15.0)  # laço: descartado
    g.add_edge(1, 3, length=0.0)  # comprimento 0: descartado
    g.add_node(5, y=-23.6, x=-46.5)  # componente menor
    g.add_node(6, y=-23.601, x=-46.5)
    g.add_edge(5, 6, length=50.0)
    return g


def test_grafo_do_osm_vira_o_mapa_do_servico() -> None:
    mapa = grafo_para_mapa(_grafo(), META)
    assert [e["id"] for e in mapa["esquinas"]] == [1, 2, 3]  # maior componente; o nó 4 isolado e o par 5-6 saem
    assert mapa["trechos"] == [
        {"origem": 1, "destino": 2, "distanciaM": 110.0, "fatorRisco": 0},
        {"origem": 2, "destino": 3, "distanciaM": 90.0, "fatorRisco": 0},
    ]
    assert mapa["metadados"] == META
    assert mapa["esquinas"][0] == {"id": 1, "lat": -23.48, "lng": -46.42}


def test_o_leitor_do_servico_le_o_mapa_gerado_e_ignora_metadados(tmp_path: Path) -> None:
    mapa = grafo_para_mapa(_grafo(), META)
    arquivo = tmp_path / "grafo.json"
    arquivo.write_text(json.dumps(mapa, ensure_ascii=False), encoding="utf-8")
    lido = mapa_de_dados(json.loads(arquivo.read_text(encoding="utf-8")))
    assert lido.bairro.esquinas == {1, 2, 3}
    assert len(lido.bairro.trechos) == 2
    assert all(t.fator_risco == 0.0 for t in lido.bairro.trechos)


def test_sem_comprimento_no_osm_usa_o_haversine() -> None:
    g: nx.Graph = nx.Graph()
    g.add_node(1, y=0.0, x=0.0)
    g.add_node(2, y=0.0, x=0.01)
    g.add_edge(1, 2)
    distancia = grafo_para_mapa(g, {})["trechos"][0]["distanciaM"]
    assert 1100 < distancia < 1120  # ~1.112 m por 0,01 grau no equador
