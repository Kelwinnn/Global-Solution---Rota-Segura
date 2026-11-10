import json
from pathlib import Path
from typing import Any

import pytest

from rota_segura.carregador import carregar_bairro, carregar_cenario
from rota_segura.modelos import Abrigo, Bairro, EntradaInvalidaError, Trecho

DADOS = Path(__file__).parent / "dados"

BAIRRO_VALIDO: dict[str, Any] = {
    "esquinas": [1, 2, 3],
    "trechos": [
        {"origem": 1, "destino": 2, "distancia_m": 100, "fator_risco": 0.5},
        {"origem": 2, "destino": 3, "distancia_m": 50},
    ],
}


def _salvar(pasta: Path, nome: str, conteudo: Any) -> Path:
    caminho = pasta / nome
    caminho.write_text(json.dumps(conteudo), encoding="utf-8")
    return caminho


def _bairro_valido(pasta: Path) -> Bairro:
    return carregar_bairro(_salvar(pasta, "bairro.json", BAIRRO_VALIDO))


def test_carrega_arquivos_de_exemplo() -> None:
    bairro = carregar_bairro(DADOS / "bairro_exemplo.json")
    cenario = carregar_cenario(DADOS / "cenario_exemplo.json", bairro)

    assert bairro.esquinas == frozenset({1, 2, 3, 4})
    assert Trecho(2, 3, 150.0, 0.5) in bairro.trechos
    assert cenario.trechos_alagados == frozenset({(2, 3)})
    assert cenario.abrigos == (Abrigo(4, 50, 10),)


def test_fator_de_risco_ausente_vale_zero(tmp_path: Path) -> None:
    bairro = _bairro_valido(tmp_path)
    assert Trecho(2, 3, 50.0, 0.0) in bairro.trechos


def test_trecho_alagado_vale_nos_dois_sentidos(tmp_path: Path) -> None:
    bairro = _bairro_valido(tmp_path)
    cenario = carregar_cenario(_salvar(tmp_path, "c.json", {"trechos_alagados": [[3, 2]], "abrigos": []}), bairro)
    assert cenario.trechos_alagados == frozenset({(2, 3)})


@pytest.mark.parametrize("campo", ["origem", "destino"])
def test_trecho_com_esquina_inexistente_e_rejeitado(tmp_path: Path, campo: str) -> None:
    dados = json.loads(json.dumps(BAIRRO_VALIDO))
    dados["trechos"][0][campo] = 99
    with pytest.raises(EntradaInvalidaError, match="esquina 99 não existe"):
        carregar_bairro(_salvar(tmp_path, "bairro.json", dados))


@pytest.mark.parametrize("distancia", [0, -10, 0.0004])
def test_distancia_menor_ou_igual_a_zero_e_rejeitada(tmp_path: Path, distancia: float) -> None:
    dados = json.loads(json.dumps(BAIRRO_VALIDO))
    dados["trechos"][0]["distancia_m"] = distancia
    with pytest.raises(EntradaInvalidaError, match="distância deve ser maior que zero"):
        carregar_bairro(_salvar(tmp_path, "bairro.json", dados))


def test_fator_de_risco_negativo_e_rejeitado(tmp_path: Path) -> None:
    dados = json.loads(json.dumps(BAIRRO_VALIDO))
    dados["trechos"][0]["fator_risco"] = -0.5
    with pytest.raises(EntradaInvalidaError, match="fator de risco não pode ser negativo"):
        carregar_bairro(_salvar(tmp_path, "bairro.json", dados))


def test_capacidade_negativa_e_rejeitada(tmp_path: Path) -> None:
    bairro = _bairro_valido(tmp_path)
    cenario = {"trechos_alagados": [], "abrigos": [{"esquina": 3, "capacidade": -1, "ocupacao": 0}]}
    with pytest.raises(EntradaInvalidaError, match="capacidade não pode ser negativa"):
        carregar_cenario(_salvar(tmp_path, "c.json", cenario), bairro)


def test_ocupacao_negativa_e_rejeitada(tmp_path: Path) -> None:
    bairro = _bairro_valido(tmp_path)
    cenario = {"trechos_alagados": [], "abrigos": [{"esquina": 3, "capacidade": 10, "ocupacao": -2}]}
    with pytest.raises(EntradaInvalidaError, match="ocupação não pode ser negativa"):
        carregar_cenario(_salvar(tmp_path, "c.json", cenario), bairro)


def test_abrigo_em_esquina_inexistente_e_rejeitado(tmp_path: Path) -> None:
    bairro = _bairro_valido(tmp_path)
    cenario = {"trechos_alagados": [], "abrigos": [{"esquina": 42, "capacidade": 10, "ocupacao": 0}]}
    with pytest.raises(EntradaInvalidaError, match="esquina 42 não existe"):
        carregar_cenario(_salvar(tmp_path, "c.json", cenario), bairro)


def test_trecho_alagado_com_esquina_inexistente_e_rejeitado(tmp_path: Path) -> None:
    bairro = _bairro_valido(tmp_path)
    with pytest.raises(EntradaInvalidaError, match="esquina 7 não existe"):
        carregar_cenario(_salvar(tmp_path, "c.json", {"trechos_alagados": [[1, 7]], "abrigos": []}), bairro)


def test_trecho_alagado_que_nao_existe_no_mapa_e_rejeitado(tmp_path: Path) -> None:
    bairro = _bairro_valido(tmp_path)
    with pytest.raises(EntradaInvalidaError, match="não existe trecho entre as esquinas 1 e 3"):
        carregar_cenario(_salvar(tmp_path, "c.json", {"trechos_alagados": [[1, 3]], "abrigos": []}), bairro)


def test_campo_obrigatorio_ausente_e_rejeitado(tmp_path: Path) -> None:
    with pytest.raises(EntradaInvalidaError, match="campo obrigatório 'trechos' ausente"):
        carregar_bairro(_salvar(tmp_path, "bairro.json", {"esquinas": [1]}))


def test_tipo_errado_e_rejeitado(tmp_path: Path) -> None:
    dados = json.loads(json.dumps(BAIRRO_VALIDO))
    dados["trechos"][0]["distancia_m"] = "100"
    with pytest.raises(EntradaInvalidaError, match="esperado número finito"):
        carregar_bairro(_salvar(tmp_path, "bairro.json", dados))


def test_json_malformado_e_rejeitado(tmp_path: Path) -> None:
    caminho = tmp_path / "bairro.json"
    caminho.write_text("{ isto não é json", encoding="utf-8")
    with pytest.raises(EntradaInvalidaError, match="não é um JSON válido"):
        carregar_bairro(caminho)


def test_arquivo_inexistente_e_rejeitado(tmp_path: Path) -> None:
    with pytest.raises(EntradaInvalidaError, match="não foi possível abrir"):
        carregar_bairro(tmp_path / "nao_existe.json")
