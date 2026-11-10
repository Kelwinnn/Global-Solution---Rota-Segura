# Rota Segura: rota até o abrigo (Dynamic Programming)

**NOMES E RMS: A DEFINIR**

**FONTE DO MAPA: A DEFINIR**

Parte de Dynamic Programming da Global Solution FIAP 2026.2 (CitizenTech). O código calcula a rota segura de cada pessoa até o abrigo mais próximo que ainda tenha vaga.

## O problema

A Rota Segura é um sistema de alerta de alagamento. Sensores de chuva e de nível d'água definem o risco de cada área. Quando sai o alerta, cada pessoa recebe a rota até um abrigo e, ao chegar, faz o check-in "Estou seguro".

Este módulo responde **para onde ir** (RF07):

- a rota leva ao abrigo **com vaga** mais próximo; abrigo lotado é ignorado, mesmo que esteja mais perto (RN06);
- a rota **nunca** passa por trecho alagado (RN05);
- trechos com mais risco custam mais: custo do trecho = distância × (1 + fator_risco);
- se não houver rota possível, a resposta é `sem_rota_segura` com a orientação **"Fique em local alto"**. Isso é um resultado normal, não um erro;
- o desempate é sempre o mesmo: menor custo, depois menor distância, depois menor ID.

## Como o mapa virou grafo

| Mundo real | Grafo | Arquivo |
| --- | --- | --- |
| Esquina | Vértice (ID inteiro) | `bairro.json` → `esquinas` |
| Rua entre duas esquinas | Aresta de mão dupla (rota a pé) | `bairro.json` → `trechos` |
| Comprimento e risco da rua | Peso = `distancia_m × (1 + fator_risco)` | `bairro.json` → `distancia_m`, `fator_risco` (padrão 0) |
| Rua alagada | Aresta removida (RN05) | `cenario.json` → `trechos_alagados` |
| Abrigo com vaga | Origem da busca | `cenario.json` → `abrigos` (`ocupacao < capacidade`) |
| Abrigo lotado | Esquina comum: dá para passar, não para parar (RN06) | `cenario.json` → `abrigos` |

Exemplo (`tests/dados/`):

```jsonc
// bairro.json
{"esquinas": [1, 2, 3, 4],
 "trechos": [{"origem": 1, "destino": 2, "distancia_m": 100, "fator_risco": 0.0},
             {"origem": 2, "destino": 3, "distancia_m": 150, "fator_risco": 0.5},
             {"origem": 3, "destino": 4, "distancia_m": 80,  "fator_risco": 0.0}]}

// cenario.json
{"trechos_alagados": [[2, 3]],
 "abrigos": [{"esquina": 4, "capacidade": 50, "ocupacao": 10}]}
```

O bairro (o mapa, que quase não muda) fica separado do cenário (o que muda durante o alerta). Por isso o mesmo mapa serve para qualquer cenário.

**Validação na entrada** (`carregador.py`): a leitura falha com `EntradaInvalidaError` e uma mensagem que aponta o campo quando há esquina inexistente, distância ≤ 0 (o mínimo é 1 mm), fator de risco negativo, capacidade ou ocupação negativa, trecho alagado que não existe no mapa, campo ausente, tipo errado ou JSON malformado. O algoritmo nunca recebe dado inválido.

**Custos em milímetros inteiros** (`grafo.py`): em ponto flutuante, `100 × 1,1 = 110,00000000000001`, e um empate real (110 m contra 110 m) seria decidido por erro de arredondamento. Com inteiros em mm, empate é empate e o desempate segue a regra.

## Técnicas

Cada esquina recebe um **rótulo** `(custo, distância, abrigo)`. A comparação de tuplas já aplica o desempate: menor custo, depois menor distância, depois menor ID do abrigo. Entre dois vizinhos que geram o mesmo rótulo, a próxima esquina do caminho é a de menor ID. Assim o caminho é único.

### 1. Dijkstra multi-origem (algoritmo principal), `dijkstra.py`

Em vez de rodar um Dijkstra por pessoa, a fila de prioridade (`heapq`) começa com **todos os abrigos com vaga** com rótulo zero. Como as ruas são de mão dupla, a distância do abrigo até a esquina é igual à da esquina até o abrigo. Uma única execução dá, para **todas** as esquinas, o abrigo com vaga mais próximo e a próxima esquina do caminho. A rota é reconstruída seguindo `proximo[v]` até chegar ao abrigo.

Recorrência (resolvida em ordem crescente de rótulo, o que vale porque todo custo é positivo):

```
d(a) = (0, 0, a)                                   para cada abrigo a com vaga
d(v) = min sobre trechos (v,u) de  d(u) ⊕ trecho(v,u)
```

em que `⊕` soma custo e distância do trecho e mantém o abrigo.

- Tempo: **O((V + E) log V)**. Memória: **O(V + E)**.

### 2. Memoização por cenário, `memoizacao.py`

Um alerta gera muitas consultas iguais. A árvore do Dijkstra já responde a todas as esquinas, então `CalculadoraRotas` a guarda num dicionário:

```
memo[(trechos_alagados, abrigos_com_vaga)] = árvore de rotas
```

Mudou o cenário (alagou um trecho, lotou ou liberou um abrigo), a chave muda e a árvore é recalculada. Se só a ocupação mudou, sem lotar nenhum abrigo, a chave é a mesma e o cálculo é reaproveitado.

- 1ª consulta do cenário: **O((V + E) log V)**.
- Consultas seguintes: **O(A + L)**, com A = abrigos + trechos alagados (para montar a chave) e L = tamanho do caminho.
- Memória: **O(V)** por cenário guardado.

### 3. Bellman-Ford em tabela (tabulação), `bellman_ford.py`

É a programação dinâmica explícita: a tabela `dp[k][v]` guarda o melhor rótulo de `v` usando **no máximo k trechos**.

```
dp[0][v] = (0, 0, v)   se v é abrigo com vaga, senão ∞
dp[k][v] = min( dp[k-1][v],  min sobre trechos (v,u) de dp[k-1][u] ⊕ trecho(v,u) )
```

Com custos positivos, a melhor rota não repete esquina e tem no máximo V − 1 trechos, então `dp[V-1]` é a resposta.

- Tempo: **O(V · E)**. Memória: **O(V²)**, porque a tabela inteira é guardada para deixar a recorrência visível.

Ele serve de **referência**: um teste com `hypothesis` gera 500 mapas aleatórios (com e sem empates, trechos alagados e abrigos lotados) e confirma que Dijkstra e Bellman-Ford dão os mesmos rótulos, as mesmas próximas esquinas e as mesmas rotas. O custo O(V·E) contra O((V+E) log V) é o argumento de eficiência, medido na tabela abaixo.

## Funcionalidades

- Leitura de `bairro.json` e `cenario.json` com validação e mensagens claras.
- Rota até o abrigo com vaga mais próximo, ponderada pelo risco, sem trecho alagado.
- Resposta `sem_rota_segura` com "Fique em local alto" quando não há rota: origem isolada, todos os abrigos lotados ou nenhum abrigo.
- Quem já está num abrigo com vaga recebe caminho `[abrigo]` e distância 0.
- Memoização por cenário.
- Comando de terminal com saída JSON, que pode ser salva no aparelho (RNF06).
- Script de medição de tempo em mapas sintéticos.

## Como rodar

Requer Python 3.11+. Na raiz do repositório:

```bash
pip install -r requirements.txt
cd dynamic-programming
```

Calcular uma rota:

```bash
python -m rota_segura --bairro tests/dados/bairro_exemplo.json --cenario tests/dados/cenario_exemplo.json --origem 3
```

```json
{
  "status": "rota_encontrada",
  "caminho": [3, 4],
  "distancia_m": 80.0,
  "orientacao": "Siga a rota até o abrigo na esquina 4"
}
```

Com `--origem 1`, o trecho 2-3 alagado isola a pessoa:

```json
{"status": "sem_rota_segura", "caminho": [], "distancia_m": 0.0, "orientacao": "Fique em local alto"}
```

(Na saída real, o JSON vem indentado.) A saída é JSON em ASCII puro: acentos aparecem como `é`, e qualquer leitor de JSON os converte de volta. Assim o arquivo salvo vale em qualquer terminal, inclusive no Windows. Dado inválido imprime `erro: ...` em stderr e sai com código 1. Sem rota não é erro e sai com código 0.

## Como testar

```bash
python -m pytest        # 62 testes
python -m mypy --strict # configuração em pyproject.toml
python -m rota_segura.medicao
```

| Arquivo | O que verifica |
| --- | --- |
| `test_carregador.py` | Validação: esquina inexistente, distância ≤ 0, capacidade/ocupação/risco negativos, trecho alagado inexistente, JSON malformado |
| `test_bellman_ford.py` | Significado da tabela `dp[k][v]`, custo com risco, RN05, RN06, desempates |
| `test_dijkstra.py` | Caminho reconstruído, RN05 nos dois sentidos, RN06, distância 0 no abrigo, desempates |
| `test_memoizacao.py` | Reaproveita com o mesmo cenário; recalcula quando alaga um trecho ou lota um abrigo |
| `test_equivalencia.py` | `hypothesis`: Dijkstra = Bellman-Ford em 500 mapas aleatórios |
| `test_sem_rota.py` | `SEM_ROTA_SEGURA`, caminho vazio e "Fique em local alto" (RN05, RN06, comando) |
| `test_comando.py` | Saída JSON e códigos de saída do comando |
| `test_medicao.py` | Gerador de mapas sintéticos e a tabela de tempos |

## Tempos medidos

`python -m rota_segura.medicao`, em mapas em grade (cerca de 2 trechos por esquina, como um bairro), com 5% dos trechos alagados e 1 abrigo a cada 50 esquinas, um terço deles lotado. A semente é fixa. Dijkstra: melhor de 5 execuções, incluindo a montagem do grafo. Consulta memoizada: média de 1.000 origens. Bellman-Ford: 1 execução, cujo resultado o script confere com o do Dijkstra.

Máquina: Windows 11, Python 3.12.1, processador Intel 64 (Family 6 Model 141), em 08/11/2026.

| Esquinas (V) | Trechos (E) | Dijkstra, 1ª consulta (ms) | Consulta memoizada (µs) | Bellman-Ford (ms) |
| ---: | ---: | ---: | ---: | ---: |
| 100 | 180 | 0,26 | 2,1 | 6 |
| 500 | 955 | 1,10 | 2,9 | 161 |
| 1.000 | 1.936 | 2,31 | 4,1 | 633 |
| 2.000 | 3.910 | 5,71 | 5,8 | 2.767 |
| 5.000 | 9.858 | 17,62 | 12,6 | não medido |
| 10.000 | 19.800 | 41,20 | 22,1 | não medido |

Leitura:

- Com 2.000 esquinas, o Bellman-Ford é cerca de **480× mais lento** que o Dijkstra.
- De 1.000 para 2.000 esquinas, o Bellman-Ford fica cerca de 4,4× mais lento, o que bate com O(V·E) quando E ≈ 2V (≈ 4×). O Dijkstra fica cerca de 2,5× mais lento, perto do esperado para O((V+E) log V).
- O Bellman-Ford não foi medido acima de 2.000 esquinas por causa da memória da tabela, que cresce com V². Pelo `tracemalloc`, ela ocupou 37 MB com 1.000 esquinas e 148 MB com 2.000. Com 10 mil seriam cerca de 3,7 GB.
- A consulta memoizada leva microssegundos: só monta a chave e percorre o caminho.

Os números variam com a máquina. A proporção entre os algoritmos é o que importa.

## Limitações

- Trechos são de mão dupla (rota a pé). Para rota de carro, a mão de direção mudaria o grafo.
- A memoização vale dentro de um processo, como num serviço que atende muitas consultas. O comando de terminal roda um processo por consulta, então ali ela não ganha nada. O dicionário também não tem limite de tamanho.
- Na resposta `sem_rota_segura`, `distancia_m` vale 0,0, porque não há caminho. Quem consome a saída deve olhar o `status`, não a distância.
- O mapa do bairro real ainda não foi modelado (ver FONTE DO MAPA).
