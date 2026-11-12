from rota_segura.gerar_abrigos import (
    Candidato,
    abrigos_em_json,
    capacidade_simulada,
    classificar,
    endereco_de,
    selecionar_distribuidos,
    sem_duplicados,
)
from rota_segura.mapa import Ponto

CENTRO = Ponto(-23.480, -46.420)


def _c(osm_id: str, tipo: str, lat: float, lng: float, nome: str | None = None) -> Candidato:
    return Candidato(osm_id, nome or osm_id, Ponto(lat, lng), tipo, None)


def test_classificacao_por_tipo_e_capacidade_simulada() -> None:
    assert classificar({"amenity": "school", "name": "EMEF Fulano"}, 0) == "escola"
    assert classificar({"amenity": "school", "name": "CEU Jambeiro"}, 0) == "CEU"
    assert classificar({"amenity": "community_centre", "name": "Centro Educacional Unificado X"}, 0) == "CEU"
    assert classificar({"amenity": "community_centre", "name": "Salão"}, 0) == "centro_comunitario"
    assert classificar({"leisure": "sports_centre", "name": "Ginásio"}, 0) == "ginasio"
    assert [capacidade_simulada(t) for t in ("escola", "CEU", "ginasio", "igreja")] == [200, 400, 100, 100]


def test_igreja_so_entra_se_o_predio_for_grande() -> None:
    assert classificar({"amenity": "place_of_worship"}, 399.0) is None
    assert classificar({"amenity": "place_of_worship"}, 400.0) == "igreja"
    assert classificar({"amenity": "parking"}, 5000.0) is None


def test_endereco_vem_de_rua_e_numero_ou_fica_nulo() -> None:
    assert endereco_de({"addr:street": "Rua das Flores", "addr:housenumber": "123"}) == "Rua das Flores, 123"
    assert endereco_de({"addr:street": "Rua das Flores"}) == "Rua das Flores"
    assert endereco_de({"addr:housenumber": "123"}) is None
    assert endereco_de({"addr:street": float("nan")}) is None
    assert endereco_de({}) is None


def test_duplicado_e_o_mesmo_nome_a_menos_de_80_m() -> None:
    a = _c("way/1", "escola", -23.4800, -46.4200, "Escola A")
    perto = _c("node/2", "escola", -23.4801, -46.4200, "Escola A")  # ~11 m
    longe = _c("node/3", "escola", -23.4900, -46.4200, "Escola A")  # outra escola homônima
    assert sem_duplicados([a, perto, longe]) == [a, longe]


def test_selecao_prioriza_escolas_e_ceus_e_espalha_pelo_bairro() -> None:
    escolas = [_c(f"way/{i}", "escola", -23.480 - 0.001 * (i % 3), -46.420 + 0.01 * i) for i in range(20)]
    outros = [_c("way/100", "ginasio", -23.48, -46.42), _c("way/101", "igreja", -23.48, -46.43)]
    ceu = _c("way/200", "CEU", -23.49, -46.40)
    escolhidos = selecionar_distribuidos(outros + escolas + [ceu], CENTRO, maximo=15)
    assert len(escolhidos) == 15
    assert escolhidos[0] == ceu  # CEU primeiro
    assert not any(c.tipo in ("ginasio", "igreja") for c in escolhidos)  # há escolas de sobra
    assert escolhidos == selecionar_distribuidos(outros + escolas + [ceu], CENTRO, maximo=15)  # repetível
    # espalhada: as duas pontas da fileira de escolas entram
    ids = {c.osm_id for c in escolhidos}
    assert {"way/0", "way/19"} <= ids


def test_outros_tipos_completam_quando_faltam_escolas() -> None:
    escolas = [_c("way/1", "escola", -23.48, -46.42), _c("way/2", "escola", -23.49, -46.41)]
    outros = [_c("way/3", "ginasio", -23.47, -46.43)]
    assert [c.tipo for c in selecionar_distribuidos(escolas + outros, CENTRO)] == ["escola", "escola", "ginasio"]


def test_json_marca_a_capacidade_como_simulada_e_numera_a_partir_de_1() -> None:
    itens = abrigos_em_json([_c("way/1", "CEU", -23.48, -46.42), _c("way/2", "escola", -23.49, -46.41)])
    assert [(a["id"], a["capacidade"], a["capacidadeSimulada"], a["endereco"]) for a in itens] == [
        (1, 400, True, None),
        (2, 200, True, None),
    ]
