"""python -m rota_segura --bairro bairro.json --cenario cenario.json --origem 7

Imprime a rota em JSON (a saída pode ser salva no aparelho, RNF06).
Dado inválido: mensagem em stderr e código de saída 1. "Sem rota" não é erro: sai com 0.
"""

import argparse
import json
import sys
from collections.abc import Sequence
from pathlib import Path

from rota_segura.carregador import carregar_bairro, carregar_cenario
from rota_segura.memoizacao import CalculadoraRotas
from rota_segura.modelos import EntradaInvalidaError


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="python -m rota_segura",
        description="Rota segura até o abrigo com vaga mais próximo, sem passar por trecho alagado.",
    )
    parser.add_argument("--bairro", required=True, type=Path, help="arquivo bairro.json (mapa)")
    parser.add_argument("--cenario", required=True, type=Path, help="arquivo cenario.json (alagamentos e abrigos)")
    parser.add_argument("--origem", required=True, type=int, help="esquina onde a pessoa está")
    args = parser.parse_args(argv)

    try:
        bairro = carregar_bairro(args.bairro)
        cenario = carregar_cenario(args.cenario, bairro)
        resultado = CalculadoraRotas(bairro).rota(cenario, args.origem)
    except EntradaInvalidaError as erro:
        print(f"erro: {erro}", file=sys.stderr)
        return 1

    saida = {
        "status": resultado.status.value,
        "caminho": list(resultado.caminho),
        "distancia_m": resultado.distancia_m,
        "orientacao": resultado.orientacao,
    }
    # ASCII puro (acentos viram é): o arquivo salvo é JSON válido em qualquer
    # codificação de terminal, inclusive a cp1252 do Windows.
    print(json.dumps(saida, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
