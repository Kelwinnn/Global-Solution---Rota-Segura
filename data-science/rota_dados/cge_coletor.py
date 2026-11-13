"""Coletor do histórico diário de alagamentos do CGE (raspagem educada e retomável).

1 requisição a cada 4 s. Só guarda no cache (HTML bruto) respostas HTTP 200 com corpo.
Erro de rede ou HTTP não grava nada: a próxima execução tenta de novo.

Uso: python -m rota_dados.cge_coletor --inicio 2016-01-01 --fim 2025-12-31
"""

from __future__ import annotations

import argparse
import sys
import time
from collections.abc import Callable, Iterator, Sequence
from datetime import date, datetime, timedelta
from pathlib import Path

import requests

from rota_dados.erros import EntradaInvalidaError

URL = "https://www.cgesp.org/v3/alagamentos.jsp"
USER_AGENT = "FIAP-GS2026-RotaSegura (uso academico)"
TIMEOUT_S = 60
PAUSA_S = 4.0
PASTA_RAW_PADRAO = Path("dados/cge/raw")


def intervalo_datas(inicio: date, fim: date) -> Iterator[date]:
    if fim < inicio:
        raise EntradaInvalidaError(f"fim ({fim}) anterior ao início ({inicio})")
    dia = inicio
    while dia <= fim:
        yield dia
        dia += timedelta(days=1)


def caminho_cache(pasta: Path, dia: date) -> Path:
    return pasta / f"{dia.isoformat()}.html"


def baixar_dia(sessao: requests.Session, dia: date) -> bytes:
    """Baixa a página do dia. Levanta requests.RequestException em erro de rede ou HTTP != 200."""
    resposta = sessao.get(
        URL,
        params={"dataBusca": dia.strftime("%d/%m/%Y"), "enviaBusca": "Buscar"},
        headers={"User-Agent": USER_AGENT},
        timeout=TIMEOUT_S,
    )
    resposta.raise_for_status()
    if resposta.status_code != 200 or not resposta.content.strip():
        raise requests.RequestException(
            f"resposta inválida: HTTP {resposta.status_code}, {len(resposta.content)} bytes"
        )
    return resposta.content


def coletar(
    inicio: date,
    fim: date,
    pasta: Path = PASTA_RAW_PADRAO,
    pausa_s: float = PAUSA_S,
    baixar: Callable[[date], bytes] | None = None,
    dormir: Callable[[float], None] = time.sleep,
) -> tuple[int, int, int]:
    """Devolve (baixados, ja_em_cache, falhas)."""
    pasta.mkdir(parents=True, exist_ok=True)
    sessao = requests.Session()
    funcao = baixar if baixar is not None else (lambda d: baixar_dia(sessao, d))
    dias = list(intervalo_datas(inicio, fim))
    baixados = em_cache = falhas = 0
    for i, dia in enumerate(dias, start=1):
        destino = caminho_cache(pasta, dia)
        if destino.exists() and destino.stat().st_size > 0:
            em_cache += 1
            continue
        try:
            conteudo = funcao(dia)
        except requests.RequestException as erro:
            falhas += 1
            print(f"[{datetime.now():%H:%M:%S}] FALHA {dia}: {erro}", flush=True)
        else:
            temporario = destino.with_suffix(".tmp")
            temporario.write_bytes(conteudo)
            temporario.replace(destino)
            baixados += 1
        if i % 25 == 0:
            print(
                f"[{datetime.now():%H:%M:%S}] {i}/{len(dias)} "
                f"(baixados {baixados}, em cache {em_cache}, falhas {falhas})",
                flush=True,
            )
        dormir(pausa_s)
    return baixados, em_cache, falhas


def _data(texto: str) -> date:
    try:
        return datetime.strptime(texto, "%Y-%m-%d").date()
    except ValueError as erro:
        raise argparse.ArgumentTypeError(f"data inválida '{texto}' (use AAAA-MM-DD)") from erro


def main(argumentos: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Coleta o histórico de alagamentos do CGE.")
    parser.add_argument("--inicio", type=_data, default=date(2016, 1, 1))
    parser.add_argument("--fim", type=_data, default=date(2025, 12, 31))
    parser.add_argument("--pasta", type=Path, default=PASTA_RAW_PADRAO)
    parser.add_argument("--pausa", type=float, default=PAUSA_S, help="segundos entre requisições")
    args = parser.parse_args(argumentos)
    try:
        baixados, em_cache, falhas = coletar(args.inicio, args.fim, args.pasta, args.pausa)
    except KeyboardInterrupt:
        print("interrompido; rode de novo para retomar.", file=sys.stderr)
        return 130
    except EntradaInvalidaError as erro:
        print(f"erro: {erro}", file=sys.stderr)
        return 1
    print(f"fim: baixados {baixados}, em cache {em_cache}, falhas {falhas}")
    if falhas:
        print("rode o mesmo comando de novo para tentar as falhas.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
