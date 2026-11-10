import json
import subprocess
import sys
from pathlib import Path

import pytest

from rota_segura.__main__ import main

DADOS = Path(__file__).parent / "dados"
RAIZ = Path(__file__).parent.parent
ARGS_EXEMPLO = ["--bairro", str(DADOS / "bairro_exemplo.json"), "--cenario", str(DADOS / "cenario_exemplo.json")]


def test_comando_imprime_rota_em_json(capsys: pytest.CaptureFixture[str]) -> None:
    assert main([*ARGS_EXEMPLO, "--origem", "3"]) == 0
    assert json.loads(capsys.readouterr().out) == {
        "status": "rota_encontrada",
        "caminho": [3, 4],
        "distancia_m": 80.0,
        "orientacao": "Siga a rota até o abrigo na esquina 4",
    }


def test_comando_com_origem_inexistente_sai_com_erro(capsys: pytest.CaptureFixture[str]) -> None:
    assert main([*ARGS_EXEMPLO, "--origem", "99"]) == 1
    saida = capsys.readouterr()
    assert saida.out == ""
    assert "esquina 99 não existe" in saida.err


def test_comando_com_arquivo_invalido_sai_com_erro(tmp_path: Path, capsys: pytest.CaptureFixture[str]) -> None:
    ruim = tmp_path / "bairro.json"
    ruim.write_text('{"esquinas": [1], "trechos": [{"origem": 1, "destino": 1, "distancia_m": 0}]}', encoding="utf-8")
    codigo = main(["--bairro", str(ruim), "--cenario", str(DADOS / "cenario_exemplo.json"), "--origem", "1"])
    assert codigo == 1
    assert "distância deve ser maior que zero" in capsys.readouterr().err


def test_comando_roda_como_modulo() -> None:
    processo = subprocess.run(
        [sys.executable, "-m", "rota_segura", *ARGS_EXEMPLO, "--origem", "4"],
        cwd=RAIZ,
        capture_output=True,
        text=True,
        check=False,
    )
    assert processo.returncode == 0, processo.stderr
    assert processo.stdout.isascii()
    assert json.loads(processo.stdout)["orientacao"] == "Siga a rota até o abrigo na esquina 4"
