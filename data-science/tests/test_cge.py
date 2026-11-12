from datetime import date
from pathlib import Path

from rota_dados.cge_agregacao import agregar
from rota_dados.cge_parser import (
    MOTIVO_INCONSISTENTE,
    MOTIVO_LAYOUT,
    MOTIVO_PAGINA_VAZIA,
    e_extremo_leste,
    e_zona_leste,
    parsear_pagina,
)

SEM_REGISTROS = "<html><body><p>Não há registros de alagamentos para essa data.</p></body></html>"


def _ponto(inicio: str, fim: str, rua: str, status: str = "inativo-intransitavel") -> str:
    return f"""
    <tr><td colspan="3"><div class="ponto-de-alagamento"><ul>
      <li class="{status}" title="x"></li><li class="espaco"></li>
      <li class="arial-descr-alag col-local">De {inicio} a {fim}<br/>{rua}</li>
      <li class="ln-vert"></li>
      <li class="arial-descr-alag">Sentido: AMBOS<br/>Referência: AV TESTE</li>
    </ul></div></td></tr>"""


def _tabela(subprefeitura: str, pontos: list[str], total: int | None = None) -> str:
    n = len(pontos) if total is None else total
    return f"""
    <table class="tb-pontos-de-alagamentos">
      <tr><td class="bairro arial-bairros-alag">{subprefeitura} <hr/></td>
          <td class="total-pts">{n} pts.</td></tr>
      {''.join(pontos)}
    </table>"""


def _pagina(blocos: list[tuple[str, list[str]]]) -> str:
    """blocos: (zona, tabelas)."""
    corpo = "".join(f'<h1 class="tit-bairros">{zona}</h1>{"".join(t)}' for zona, t in blocos)
    return f"<html><body>{corpo}</body></html>"


DIA = date(2023, 1, 29)


def test_pagina_sem_registros_vira_zero(tmp_path: Path) -> None:
    resultado = parsear_pagina(SEM_REGISTROS, DIA)
    assert resultado.ok and resultado.pontos == []

    pasta = tmp_path / "raw"
    pasta.mkdir()
    (pasta / "2023-01-29.html").write_text(SEM_REGISTROS, encoding="utf-8")
    ag = agregar(DIA, DIA, pasta)
    linha = ag.diario.iloc[0]
    assert linha["n_pontos_extremo_leste"] == 0 and linha["alagou_zona_leste"] == 0
    assert bool(linha["pagina_ok"]) is True
    assert ag.faltantes.empty


def test_pagina_com_erro_vira_faltante(tmp_path: Path) -> None:
    assert parsear_pagina("", DIA).motivo == MOTIVO_PAGINA_VAZIA
    assert parsear_pagina("   \n ", DIA).motivo == MOTIVO_PAGINA_VAZIA
    assert parsear_pagina("<html><body><h1>Erro 500</h1></body></html>", DIA).motivo == MOTIVO_LAYOUT
    quebrada = _pagina([("Zona Leste", [_tabela("Penha", ["<tr><td><div class='ponto-de-alagamento'></div></td></tr>"])])])
    assert parsear_pagina(quebrada, DIA).motivo == MOTIVO_LAYOUT
    inconsistente = _pagina([("Zona Leste", [_tabela("Penha", [_ponto("10:00", "11:00", "R A")], total=3)])])
    assert parsear_pagina(inconsistente, DIA).motivo == MOTIVO_INCONSISTENTE

    pasta = tmp_path / "raw"
    pasta.mkdir()
    (pasta / "2023-01-29.html").write_text("", encoding="utf-8")  # vazia
    # 2023-01-30 sem arquivo no cache
    ag = agregar(DIA, date(2023, 1, 30), pasta)
    assert list(ag.diario["pagina_ok"]) == [False, False]
    assert ag.diario["n_pontos_zona_leste"].isna().all()
    assert ag.diario["alagou_zona_leste"].isna().all()
    assert list(ag.faltantes["motivo"]) == ["pagina_vazia", "sem_cache"]


def test_ponto_apos_meia_noite_fica_na_data_consultada() -> None:
    html = _pagina([("Zona Leste", [_tabela("Itaim Paulista", [_ponto("23:30", "01:10", "R DA MADRUGADA")])])])
    resultado = parsear_pagina(html, DIA)
    assert resultado.ok and len(resultado.pontos) == 1
    ponto = resultado.pontos[0]
    assert ponto.data == DIA
    assert (ponto.hora_inicio, ponto.hora_fim) == ("23:30", "01:10")
    assert ponto.logradouro == "R DA MADRUGADA"
    assert (ponto.atividade, ponto.transito) == ("inativo", "intransitavel")


def test_filtro_usa_subprefeitura_e_nao_zona(tmp_path: Path) -> None:
    # Itaquera aparece sob "Zona Sudeste" e Pinheiros sob "Zona Leste": vale o nome.
    html = _pagina(
        [
            ("Zona Sudeste", [_tabela("Aricanduva/ Vila Formosa", [_ponto("10:00", "11:00", "R A")])]),
            ("Zona Leste", [_tabela("Pinheiros", [_ponto("10:00", "11:00", "R B")])]),
            ("Zona Leste", [_tabela("São Miguel Paulista", [_ponto("10:00", "11:00", "R C")])]),
        ]
    )
    assert e_zona_leste("Aricanduva/ Vila Formosa") and not e_extremo_leste("Aricanduva/ Vila Formosa")
    assert e_extremo_leste("SÃO MIGUEL") and e_extremo_leste("São Miguel Paulista")
    assert not e_zona_leste("Pinheiros")
    pasta = tmp_path / "raw"
    pasta.mkdir()
    (pasta / "2023-01-29.html").write_text(html, encoding="utf-8")
    linha = agregar(DIA, DIA, pasta).diario.iloc[0]
    assert linha["n_pontos_extremo_leste"] == 1
    assert linha["n_pontos_zona_leste"] == 2


def test_soma_por_subprefeitura_bate_com_total() -> None:
    html = _pagina(
        [
            (
                "Zona Leste",
                [
                    _tabela("Itaquera", [_ponto("10:00", "11:00", f"R {i}") for i in range(5)]),
                    _tabela("Itaim Paulista", [_ponto("10:00", "11:00", f"R {i}") for i in range(2)]),
                    _tabela("Penha", [_ponto("10:00", "11:00", "R P")]),
                ],
            )
        ]
    )
    resultado = parsear_pagina(html, DIA)
    assert resultado.ok
    contagem: dict[str, int] = {}
    for p in resultado.pontos:
        contagem[p.subprefeitura] = contagem.get(p.subprefeitura, 0) + 1
    assert contagem == {"Itaquera": 5, "Itaim Paulista": 2, "Penha": 1}
    assert sum(contagem.values()) == len(resultado.pontos) == 8


def test_ponto_repetido_no_dia_seguinte_conta_nos_dois_dias(tmp_path: Path) -> None:
    pagina = _pagina([("Zona Leste", [_tabela("Penha", [_ponto("23:30", "01:10", "R X")])])])
    pasta = tmp_path / "raw"
    pasta.mkdir()
    (pasta / "2023-01-29.html").write_text(pagina, encoding="utf-8")
    (pasta / "2023-01-30.html").write_text(pagina, encoding="utf-8")
    ag = agregar(DIA, date(2023, 1, 30), pasta)
    assert list(ag.diario["n_pontos_zona_leste"]) == [1, 1]
    assert len(ag.repetidos_meia_noite) == 1
