"""Parser da página diária de alagamentos do CGE (Prefeitura de SP).

Estrutura da página (verificada em 05/02/2016, 10/03/2019 e 29/01/2023):
    h1.tit-bairros                      -> zona (Zona Norte, Zona Leste...)
    table.tb-pontos-de-alagamentos      -> uma subprefeitura
        td.bairro                       -> nome da subprefeitura
        td.total-pts                    -> "5 pts."
        div.ponto-de-alagamento         -> um ponto
            li.(ativo|inativo)-(transitavel|intransitavel)  -> status
            li.col-local                -> "De HH:MM a HH:MM" + logradouro
            li (último)                 -> "Sentido: ..." + "Referência: ..."

Regra de dado faltante: só a frase "Não há registros..." vira 0 pontos.
Página vazia, sem a frase e sem pontos, ou com estrutura diferente => ok=False.
"""

from __future__ import annotations

import re
import unicodedata
from dataclasses import dataclass
from datetime import date

from bs4 import BeautifulSoup, Tag

MOTIVO_PAGINA_VAZIA = "pagina_vazia"
MOTIVO_LAYOUT = "layout_diferente"
MOTIVO_INCONSISTENTE = "inconsistente"

FRASE_SEM_REGISTROS = "nao ha registros de alagamentos"

EXTREMO_LESTE = frozenset({"itaim paulista", "sao miguel", "ermelino matarazzo", "guaianases"})
ZONA_LESTE = EXTREMO_LESTE | frozenset(
    {
        "penha",
        "itaquera",
        "cidade tiradentes",
        "sao mateus",
        "mooca",
        "aricanduva",
        "vila prudente",
        "sapopemba",
    }
)

_RE_HORARIO = re.compile(r"De\s+(\d{1,2}:\d{2})\s+a\s+(\d{1,2}:\d{2})")
_RE_STATUS = re.compile(r"^(ativo|inativo)-(transitavel|intransitavel)$")
_RE_TOTAL = re.compile(r"(\d+)\s*pts?\.?")
_RE_SENTIDO = re.compile(r"Sentido:\s*(.*)")
_RE_REFERENCIA = re.compile(r"Refer[^:]*:\s*(.*)")


@dataclass(frozen=True)
class Ponto:
    data: date
    zona: str
    subprefeitura: str
    hora_inicio: str
    hora_fim: str
    logradouro: str
    sentido: str
    referencia: str
    atividade: str  # "ativo" ou "inativo"
    transito: str  # "transitavel" ou "intransitavel"


@dataclass(frozen=True)
class ResultadoPagina:
    ok: bool
    motivo: str | None
    pontos: list[Ponto]


def normalizar_nome(texto: str) -> str:
    """Minúsculas, sem acento, espaços colapsados."""
    sem_acento = unicodedata.normalize("NFKD", texto).encode("ascii", "ignore").decode("ascii")
    return re.sub(r"\s+", " ", sem_acento).strip().lower()


def nome_canonico(subprefeitura: str) -> str:
    """Agrupa variações de nome da subprefeitura ("Aricanduva/ Vila Formosa", "São Miguel Paulista")."""
    nome = normalizar_nome(subprefeitura)
    if nome.startswith("aricanduva"):
        return "aricanduva"
    if nome.startswith("sao miguel"):
        return "sao miguel"
    return nome


def e_extremo_leste(subprefeitura: str) -> bool:
    return nome_canonico(subprefeitura) in EXTREMO_LESTE


def e_zona_leste(subprefeitura: str) -> bool:
    return nome_canonico(subprefeitura) in ZONA_LESTE


def _falha(motivo: str) -> ResultadoPagina:
    return ResultadoPagina(ok=False, motivo=motivo, pontos=[])


def _texto_linhas(li: Tag) -> list[str]:
    return [p.strip() for p in li.get_text("\n").split("\n") if p.strip()]


def _parsear_ponto(div: Tag, data: date, zona: str, subprefeitura: str) -> Ponto | None:
    status = None
    for li in div.find_all("li"):
        for classe in li.get("class") or []:
            achado = _RE_STATUS.match(classe)
            if achado:
                status = achado
    local = div.select_one("li.col-local")
    itens = div.find_all("li")
    if status is None or local is None or len(itens) < 2:
        return None
    linhas_local = _texto_linhas(local)
    horario = _RE_HORARIO.search(linhas_local[0]) if linhas_local else None
    if horario is None or len(linhas_local) < 2:
        return None
    detalhe = " \n".join(_texto_linhas(itens[-1]))
    sentido = _RE_SENTIDO.search(detalhe)
    referencia = _RE_REFERENCIA.search(detalhe)
    if sentido is None or referencia is None:
        return None
    return Ponto(
        data=data,
        zona=zona,
        subprefeitura=subprefeitura,
        hora_inicio=horario.group(1),
        hora_fim=horario.group(2),
        logradouro=linhas_local[1],
        sentido=sentido.group(1).strip(),
        referencia=referencia.group(1).strip(),
        atividade=status.group(1),
        transito=status.group(2),
    )


def parsear_pagina(html: str, data: date) -> ResultadoPagina:
    """Extrai os pontos de uma página. O ponto que passa da meia-noite fica na data consultada."""
    if not html.strip():
        return _falha(MOTIVO_PAGINA_VAZIA)
    sopa = BeautifulSoup(html, "lxml")
    tabelas = sopa.select("table.tb-pontos-de-alagamentos")
    if not tabelas:
        if FRASE_SEM_REGISTROS in normalizar_nome(sopa.get_text(" ")):
            return ResultadoPagina(ok=True, motivo=None, pontos=[])
        return _falha(MOTIVO_LAYOUT)

    pontos: list[Ponto] = []
    for tabela in tabelas:
        celula_nome = tabela.select_one("td.bairro")
        celula_total = tabela.select_one("td.total-pts")
        if celula_nome is None or celula_total is None:
            return _falha(MOTIVO_LAYOUT)
        subprefeitura = celula_nome.get_text(" ", strip=True)
        total = _RE_TOTAL.search(celula_total.get_text(" ", strip=True))
        titulo = tabela.find_previous("h1", class_="tit-bairros")
        zona = titulo.get_text(" ", strip=True) if titulo is not None else ""
        divs = tabela.select("div.ponto-de-alagamento")
        if total is None or not subprefeitura:
            return _falha(MOTIVO_LAYOUT)
        lidos: list[Ponto] = []
        for div in divs:
            ponto = _parsear_ponto(div, data, zona, subprefeitura)
            if ponto is None:
                return _falha(MOTIVO_LAYOUT)
            lidos.append(ponto)
        if int(total.group(1)) != len(lidos):
            return _falha(MOTIVO_INCONSISTENTE)
        pontos.extend(lidos)
    return ResultadoPagina(ok=True, motivo=None, pontos=pontos)
