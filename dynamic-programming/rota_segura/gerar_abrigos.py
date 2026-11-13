"""Gera docs/abrigos_sugeridos.json: abrigos candidatos do OSM no distrito Jardim Helena, para o Kelwin cadastrar.

    pip install -e ".[grafo]"
    python -m rota_segura.gerar_abrigos

Fonte: OpenStreetMap (escolas, centros comunitários, ginásios e igrejas), © colaboradores do OSM, ODbL.
A CAPACIDADE É SIMULADA (o OSM não tem lotação): escola 200, CEU 400, outros 100. Máximo de 15 abrigos,
escolas e CEUs primeiro, escolhidos para ficarem espalhados pelo bairro (ponto mais distante dos já escolhidos).
Igreja só entra se o prédio tiver mais de 400 m² (o OSM não diz "igreja grande"; este corte é uma escolha nossa).
"""

import json
import sys
import time
from collections.abc import Mapping, Sequence
from dataclasses import dataclass
from datetime import date
from pathlib import Path
from typing import Any

from rota_segura.gerar_grafo import FONTE, RELACAO_OSM, distrito_e_margem
from rota_segura.mapa import Ponto, haversine_m

SAIDA_PADRAO = Path("docs/abrigos_sugeridos.json")
MAXIMO = 15
AREA_MINIMA_IGREJA_M2 = 400.0
DISTANCIA_DUPLICADO_M = 80.0
CAPACIDADE = {"CEU": 400, "escola": 200}
CAPACIDADE_OUTROS = 100
SERVIDORES_OVERPASS = (
    "https://overpass-api.de/api",
    "https://overpass.kumi.systems/api",
    "https://overpass.private.coffee/api",
)
TENTATIVAS_POR_SERVIDOR = 2
ESPERA_S = 30
TAGS = {
    "amenity": ["school", "community_centre", "place_of_worship"],
    "leisure": ["sports_centre"],
}


@dataclass(frozen=True, slots=True)
class Candidato:
    osm_id: str
    nome: str
    ponto: Ponto
    tipo: str
    endereco: str | None

    @property
    def prioritario(self) -> bool:
        return self.tipo in CAPACIDADE


def capacidade_simulada(tipo: str) -> int:
    return CAPACIDADE.get(tipo, CAPACIDADE_OUTROS)


def endereco_de(tags: Mapping[str, Any]) -> str | None:
    """'Rua X, 123' (addr:street + addr:housenumber). Só a rua se faltar o número; None se faltar a rua."""
    rua, numero = _texto(tags.get("addr:street")), _texto(tags.get("addr:housenumber"))
    if rua is None:
        return None
    return f"{rua}, {numero}" if numero else rua


def _texto(valor: Any) -> str | None:
    if valor is None or (isinstance(valor, float) and valor != valor):  # None ou NaN do pandas
        return None
    texto = str(valor).strip()
    return texto or None


def classificar(tags: Mapping[str, Any], area_m2: float) -> str | None:
    """escola | CEU | centro_comunitario | ginasio | igreja, ou None se não serve como abrigo."""
    nome = (_texto(tags.get("name")) or "").upper()
    e_ceu = nome.startswith("CEU") or "CENTRO EDUCACIONAL UNIFICADO" in nome
    amenity, lazer = _texto(tags.get("amenity")), _texto(tags.get("leisure"))
    if amenity == "school":
        return "CEU" if e_ceu else "escola"
    if amenity == "community_centre":
        return "CEU" if e_ceu else "centro_comunitario"
    if lazer == "sports_centre":
        return "ginasio"
    if amenity == "place_of_worship":
        return "igreja" if area_m2 >= AREA_MINIMA_IGREJA_M2 else None
    return None


def sem_duplicados(candidatos: Sequence[Candidato]) -> list[Candidato]:
    """Mesmo nome e a menos de 80 m (ex.: o polígono e o ponto da mesma escola): fica o primeiro."""
    mantidos: list[Candidato] = []
    for c in candidatos:
        if not any(c.nome == m.nome and haversine_m(c.ponto, m.ponto) < DISTANCIA_DUPLICADO_M for m in mantidos):
            mantidos.append(c)
    return mantidos


def mais_distante(candidatos: Sequence[Candidato], escolhidos: Sequence[Candidato]) -> Candidato:
    """Candidato cuja distância ao escolhido mais próximo é a maior. Empate: nome e id (resultado repetível)."""
    return max(
        candidatos,
        key=lambda c: (min(haversine_m(c.ponto, e.ponto) for e in escolhidos), c.nome, c.osm_id),
    )


def selecionar_distribuidos(candidatos: Sequence[Candidato], centro: Ponto, maximo: int = MAXIMO) -> list[Candidato]:
    """Escolas e CEUs primeiro (CEU antes, depois o mais perto do centro), espalhados; só então os outros tipos."""
    escolhidos: list[Candidato] = []
    for grupo in ([c for c in candidatos if c.prioritario], [c for c in candidatos if not c.prioritario]):
        restantes = list(grupo)
        while restantes and len(escolhidos) < maximo:
            if not escolhidos:
                proximo = min(restantes, key=lambda c: (c.tipo != "CEU", haversine_m(c.ponto, centro), c.osm_id))
            else:
                proximo = mais_distante(restantes, escolhidos)
            escolhidos.append(proximo)
            restantes.remove(proximo)
    return escolhidos


def abrigos_em_json(escolhidos: Sequence[Candidato]) -> list[dict[str, Any]]:
    return [
        {
            "id": i,
            "nome": c.nome,
            "endereco": c.endereco,
            "lat": round(c.ponto.lat, 6),
            "lng": round(c.ponto.lng, 6),
            "tipo": c.tipo,
            "capacidade": capacidade_simulada(c.tipo),
            "capacidadeSimulada": True,
            "osmId": c.osm_id,
        }
        for i, c in enumerate(escolhidos, start=1)
    ]


def _baixar_com_tentativas(ox: Any, distrito: Any) -> Any:
    """O Overpass público fica lotado às vezes (502): tenta de novo com espera crescente e troca de servidor."""
    ultimo_erro: Exception | None = None
    for servidor in SERVIDORES_OVERPASS:
        ox.settings.overpass_url = servidor
        for tentativa in range(1, TENTATIVAS_POR_SERVIDOR + 1):
            try:
                # Consulta pela caixa do distrito (mais leve para o servidor que pelo polígono inteiro);
                # o filtro pelo polígono é feito depois, em candidatos_do_osm.
                oeste, sul, leste, norte = distrito.bounds
                return ox.features_from_bbox((oeste, sul, leste, norte), TAGS)
            except Exception as erro:  # 502, 429, timeout: o osmnx lança tipos variados
                ultimo_erro = erro
                print(f"aviso: {servidor} falhou ({type(erro).__name__}), tentativa {tentativa}", file=sys.stderr)
                time.sleep(ESPERA_S * tentativa)
    raise RuntimeError(f"nenhum servidor Overpass respondeu: {ultimo_erro}")


def candidatos_do_osm() -> tuple[list[Candidato], Ponto]:
    """Baixa os pontos de interesse dentro do distrito (sem a margem). Precisa de rede e do extra 'grafo'."""
    import osmnx as ox

    distrito, _, _ = distrito_e_margem()
    tabela = _baixar_com_tentativas(ox, distrito)
    areas = tabela.to_crs(tabela.estimate_utm_crs()).geometry.area
    candidatos: list[Candidato] = []
    for (tipo_osm, identificador), linha in tabela.iterrows():
        centro = linha.geometry.centroid
        tipo = classificar(linha.to_dict(), float(areas.loc[(tipo_osm, identificador)]))
        if tipo is None or not distrito.contains(centro):
            continue
        nome = _texto(linha.get("name")) or f"{tipo.capitalize()} sem nome (OSM {tipo_osm}/{identificador})"
        candidatos.append(
            Candidato(f"{tipo_osm}/{identificador}", nome, Ponto(centro.y, centro.x), tipo, endereco_de(linha.to_dict()))
        )
    candidatos.sort(key=lambda c: (c.osm_id.startswith("node/"), c.osm_id))  # polígonos antes dos pontos
    c = distrito.centroid
    return sem_duplicados(candidatos), Ponto(c.y, c.x)


def main(argumentos: Sequence[str] | None = None) -> int:
    saida = Path(argumentos[0]) if argumentos else SAIDA_PADRAO
    candidatos, centro = candidatos_do_osm()
    escolhidos = selecionar_distribuidos(candidatos, centro)
    abrigos = abrigos_em_json(escolhidos)
    conteudo = {
        "fonte": FONTE,
        "relacaoOsm": RELACAO_OSM,
        "geradoEm": date.today().isoformat(),
        "aviso": "Capacidade SIMULADA (escola 200, CEU 400, outros 100): o OSM não informa lotação. "
        "Endereço vem de addr:street + addr:housenumber e é null quando o OSM não tem.",
        "abrigos": abrigos,
    }
    saida.parent.mkdir(parents=True, exist_ok=True)
    saida.write_text(json.dumps(conteudo, ensure_ascii=False, indent=2), encoding="utf-8")
    por_tipo: dict[str, int] = {}
    for a in abrigos:
        por_tipo[a["tipo"]] = por_tipo.get(a["tipo"], 0) + 1
    sem_endereco = sum(1 for a in abrigos if a["endereco"] is None)
    print(f"candidatos no OSM: {len(candidatos)} | escolhidos: {len(abrigos)} {por_tipo}")
    print(f"sem endereço (null): {sem_endereco} de {len(abrigos)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
