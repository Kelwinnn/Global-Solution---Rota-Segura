"""Gera dados/grafo_jardim_helena.json a partir do OpenStreetMap. Roda uma vez, fora do serviço.

    pip install -e ".[grafo]"
    python -m rota_segura.gerar_grafo

Passos: polígono do distrito Jardim Helena (relação OSM R3049447; se o geocode falhar, retângulo de reserva
em volta do distrito) + margem de 300 m, rede "walk" do osmnx, trechos nos dois sentidos, só o maior
componente conectado. Trechos paralelos viram um (o mais curto); laços e comprimento 0 são descartados.
Dados © colaboradores do OpenStreetMap, licença ODbL (https://www.openstreetmap.org/copyright).
O osmnx é importado só na hora de baixar: a conversão (grafo_para_mapa) não precisa dele nem de rede.
"""

import json
import sys
from collections.abc import Sequence
from datetime import date
from pathlib import Path
from typing import Any

from rota_segura.mapa import Ponto, haversine_m, mapa_de_dados
from rota_segura.modelos import chave_trecho

RELACAO_OSM = "R3049447"  # "Jardim Helena, São Paulo" no Nominatim (suburb)
ANCORA = Ponto(-23.4802, -46.4213)  # ponto que o distrito precisa conter
RETANGULO_RESERVA = (-46.4409, -23.4941, -46.3784, -23.4704)  # oeste, sul, leste, norte (caixa do Nominatim)
MARGEM_M = 300
SAIDA_PADRAO = Path("dados/grafo_jardim_helena.json")
FONTE = "© colaboradores do OpenStreetMap (ODbL)"


def distrito_e_margem() -> tuple[Any, Any, str]:
    """(polígono do distrito, polígono com margem, origem do polígono). Precisa de rede e do extra 'grafo'."""
    import osmnx as ox
    from shapely.geometry import Point, box

    origem = f"geocode OSM {RELACAO_OSM}"
    try:
        distrito = ox.geocode_to_gdf(RELACAO_OSM, by_osmid=True).geometry.iloc[0]
        if distrito.geom_type not in ("Polygon", "MultiPolygon") or not distrito.contains(Point(ANCORA.lng, ANCORA.lat)):
            raise ValueError("geometria inesperada")
    except Exception as erro:  # sem rede, relação mudou, geometria que não contém o ponto de referência
        print(f"aviso: geocode falhou ({erro}); usando o retângulo de reserva", file=sys.stderr)
        distrito, origem = box(*RETANGULO_RESERVA), "retângulo de reserva"
    projetado, crs = ox.projection.project_geometry(distrito)
    com_margem, _ = ox.projection.project_geometry(projetado.buffer(MARGEM_M), crs=crs, to_latlong=True)
    return distrito, com_margem, origem


def grafo_para_mapa(grafo: Any, metadados: dict[str, Any]) -> dict[str, Any]:
    """Converte um grafo do networkx/osmnx (nós com x=lng e y=lat, arestas com length) no formato do mapa do serviço."""
    import networkx as nx

    nao_direcionado = grafo.to_undirected() if grafo.is_directed() else grafo
    maior = max(nx.connected_components(nao_direcionado), key=len)
    nos = {n: nao_direcionado.nodes[n] for n in maior}

    menores: dict[tuple[int, int], float] = {}
    for u, v, dados in nao_direcionado.subgraph(maior).edges(data=True):
        if u == v:
            continue
        comprimento = dados.get("length")
        if comprimento is None:
            comprimento = haversine_m(Ponto(nos[u]["y"], nos[u]["x"]), Ponto(nos[v]["y"], nos[v]["x"]))
        if comprimento <= 0:
            continue
        chave = chave_trecho(int(u), int(v))
        menores[chave] = min(menores.get(chave, float("inf")), float(comprimento))

    com_trecho = {e for chave in menores for e in chave}
    esquinas = [
        {"id": int(n), "lat": round(float(d["y"]), 6), "lng": round(float(d["x"]), 6)}
        for n, d in sorted(nos.items()) if n in com_trecho
    ]
    trechos = [
        {"origem": a, "destino": b, "distanciaM": round(d, 1), "fatorRisco": 0}
        for (a, b), d in sorted(menores.items())
    ]
    return {"metadados": metadados, "esquinas": esquinas, "trechos": trechos}


def gerar(saida: Path = SAIDA_PADRAO) -> dict[str, Any]:
    import osmnx as ox

    ox.settings.use_cache = True
    ox.settings.cache_folder = str(saida.parent / ".cache_osmnx")
    distrito, com_margem, origem = distrito_e_margem()
    grafo = ox.graph_from_polygon(com_margem, network_type="walk", simplify=True, retain_all=True)
    oeste, sul, leste, norte = com_margem.bounds
    metadados = {
        "fonte": FONTE,
        "licenca": "ODbL 1.0",
        "baixadoEm": date.today().isoformat(),
        "area": f"distrito Jardim Helena, São Paulo ({origem}) + margem de {MARGEM_M} m",
        "caixa": {"oeste": round(oeste, 5), "sul": round(sul, 5), "leste": round(leste, 5), "norte": round(norte, 5)},
        "rede": "walk (a pé), trechos nos dois sentidos, maior componente conectado",
        "fatorRisco": "0 em todos os trechos (MVP)",
    }
    mapa = grafo_para_mapa(grafo, metadados)
    mapa_de_dados(mapa)  # confere que o serviço consegue ler o que foi gerado
    saida.parent.mkdir(parents=True, exist_ok=True)
    saida.write_text(json.dumps(mapa, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    return mapa


def main(argumentos: Sequence[str] | None = None) -> int:
    saida = Path(argumentos[0]) if argumentos else SAIDA_PADRAO
    mapa = gerar(saida)
    tamanho = saida.stat().st_size
    print(f"{saida}: {len(mapa['esquinas'])} esquinas, {len(mapa['trechos'])} trechos, {tamanho / 1e6:.2f} MB")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
