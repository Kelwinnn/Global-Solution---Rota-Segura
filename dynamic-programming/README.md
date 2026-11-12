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

**Validação na entrada** (`carregador.py`): a leitura falha com `EntradaInvalidaError` e uma mensagem que aponta o campo quando há esquina inexistente, distância ≤ 0 (o mínimo é 1 mm), fator de risco fora de 0 a 1, capacidade ou ocupação negativa, trecho alagado que não existe no mapa, campo ausente, tipo errado ou JSON malformado. O algoritmo nunca recebe dado inválido.

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
- Serviço HTTP (FastAPI) do contrato v2: `GET /health`, `POST /route` e `POST /risk`.

## Como rodar

Requer Python 3.11+. Dentro de `dynamic-programming/`:

```bash
pip install -e ".[dev]"
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

## Serviço HTTP (FastAPI)

Implementa o contrato v2 em [`docs/contrato.md`](docs/contrato.md): `GET /health`, `POST /route` e `POST /risk`. JSON em camelCase. **O mapa é fixo**: fica no serviço e é carregado na subida, em vez de chegar em cada requisição.

| Variável | Uso |
| --- | --- |
| `GRAFO_PATH` | JSON do mapa do bairro-piloto (esquinas com `id`, `lat`, `lng`; trechos com `origem`, `destino`, `distanciaM` opcional e `fatorRisco` opcional) |
| `MODELO_PATH` | Modelo de risco (`.joblib`, gerado por `data-science/02_modelos.ipynb`) |
| `PORT` | Porta HTTP (o Render define; no Docker o padrão é 8000) |

Enquanto o grafo real do Jardim Helena (OpenStreetMap) não existe, use o mapa de teste `dados/grafo_teste_jardim_helena.json`: 25 esquinas numa grade de ~100 m com coordenadas **inventadas**, só plausíveis para o bairro. O modelo real vem do notebook. Sem `GRAFO_PATH` ou `MODELO_PATH` (ou com arquivo inválido) o serviço sobe mesmo assim: `/health` mostra `mapLoaded` ou `modelLoaded` falso e a rota ou o risco correspondente respondem `503`.

```bash
# dentro de dynamic-programming/
GRAFO_PATH=dados/grafo_teste_jardim_helena.json MODELO_PATH=modelo.joblib python -m uvicorn rota_segura.api:app --port 8000
```

### Exemplos

```bash
curl http://127.0.0.1:8000/health
# {"status":"ok","mapLoaded":true,"modelLoaded":true,"version":"2.0.0"}
```

```bash
curl -X POST http://127.0.0.1:8000/route -H "Content-Type: application/json" -d '{
  "origin": {"lat": -23.4930, "lng": -46.4150},
  "shelters": [
    {"id": 1, "name": "Escola Estadual A", "lat": -23.4894, "lng": -46.4111, "availableSpots": 40},
    {"id": 2, "name": "Ginasio B", "lat": -23.4912, "lng": -46.4131, "availableSpots": 0}],
  "floodedAreas": [{"areaId": 7, "lat": -23.4921, "lng": -46.4140, "radiusM": 40}]}'
```

```json
{"status": "ROUTE_FOUND",
 "shelter": {"id": 1, "name": "Escola Estadual A", "lat": -23.4894, "lng": -46.4111},
 "distanceMeters": 744, "durationMin": 11,
 "path": [{"lat": -23.493, "lng": -46.415}, {"lat": -23.493, "lng": -46.41402}, "...", {"lat": -23.4894, "lng": -46.4111}],
 "message": "Siga a rota até Escola Estadual A."}
```

(O abrigo 2 está sem vagas e é ignorado; a rota contorna a área alagada.) Sem rota segura, a resposta é `200` com `{"status": "NO_SAFE_ROUTE", "shelter": null, "distanceMeters": null, "durationMin": null, "path": [], "message": "Não há rota segura agora. Vá para um local alto e aguarde ajuda."}`.

```bash
curl -X POST http://127.0.0.1:8000/risk -H "Content-Type: application/json" -d '{
  "areaId": 3, "lat": -23.4890, "lng": -46.4120,
  "readings": [
    {"timestamp": "2026-11-19T18:00:00-03:00", "rainMm": 12.4, "waterLevelCm": 2.3},
    {"timestamp": "2026-11-19T18:10:00-03:00", "rainMm": 8.1, "waterLevelCm": 2.8}]}'
```

```json
{"areaId": 3, "floodProbability": 0.0037, "highProbability": false, "lowData": true, "modelVersion": "2026-11-17"}
```

(O valor de `floodProbability` do exemplo vem de um modelo de brinquedo; o do projeto sai do notebook. Com 2 leituras há só 1 dia com dados, então `lowData` é `true`.)

### Regras do `/route`

- **Esquina mais próxima.** A origem e cada abrigo viram a esquina mais próxima do mapa (haversine; empate: menor id da esquina). Origem a mais de **300 m** da esquina mais próxima: `400 INVALID_INPUT` (fora do bairro-piloto).
- **Abrigo lotado (RN06).** Abrigo com `availableSpots <= 0` é ignorado. `availableSpots` negativo é entrada inválida (`400`).
- **Trecho alagado (RN05).** O trecho sai do grafo se **qualquer ponta ou o ponto médio** (média das coordenadas das pontas) estiver a até `radiusM` metros do centro de **alguma** área alagada. O ponto médio evita que um trecho comprido atravesse um círculo pequeno sem que nenhuma ponta caia nele. `radiusM <= 0` é entrada inválida.
- **Custo e desempate.** Custo = distância × (1 + `fatorRisco`), com o `fatorRisco` do arquivo do mapa (0 se ausente). Desempate: menor custo, depois menor distância, depois **menor id do abrigo** (o id que o Java manda, não a esquina).
- **Resposta.** `path` começa na `origin` exata e termina na coordenada do abrigo, passando pelas esquinas. `distanceMeters` = distância no grafo + a perna da origem até a esquina + a perna da esquina até o abrigo (haversine), arredondada para metro inteiro. `durationMin` = `ceil(distanceMeters / 70)` (a pé, 70 m/min), calculado sobre esse inteiro.
- **Memoização.** A árvore do Dijkstra multi-origem é guardada por cenário (chave = trechos alagados + abrigos com vaga, porque o mapa é fixo), com limite LRU de 128 cenários.

### Regras do `/risk`

- As variáveis são as da seção 1 de `data-science/02_modelos.ipynb`, calculadas por `risco.py` para o dia da **última leitura** no fuso America/Sao_Paulo: `chuva_24h`, `chuva_max_1h`, `chuva_max_3h` (soma móvel de 3 h), `chuva_dia_anterior`, `chuva_72h` (dia + 2 anteriores), `chuva_7d_anteriores` (7 dias antes do dia) e `mes_sen`/`mes_cos`. As leituras de uma mesma hora são somadas.
- `lowData` é `true` se houver **menos de 8 dias do calendário de São Paulo com ao menos uma leitura** ou se alguma variável não puder ser calculada. **Dia (ou hora) sem leitura entra como 0 mm, o que subestima a chuva**: o Java deve tratar `lowData: true` como sinal fraco.
- `highProbability` = `floodProbability >= limite`, com o limite gravado no joblib.
- `modelVersion` vem da chave `versao` do joblib; se não existir, da data de modificação do arquivo.
- O joblib precisa ter as chaves `pipeline`, `variaveis` e `limite` (e, opcionalmente, `versao` e `sklearn`). Se `sklearn` for diferente da versão instalada, o serviço só registra um aviso no log. O `pyproject.toml` fixa `scikit-learn==1.8.0`, a mesma do notebook. **Joblib é pickle: carregue só arquivos de confiança.**

### Erros

Entrada inválida (`EntradaInvalidaError`): `400` com `{"error": "INVALID_INPUT", "message": "origin: a 14719 m da esquina mais próxima (máximo 300 m); fora do bairro-piloto"}`. JSON fora do esquema: `422` padrão do FastAPI. Mapa ou modelo não carregado: `503` com `MAP_UNAVAILABLE` ou `MODEL_UNAVAILABLE`. Falha inesperada: `500` com `{"error": "INTERNAL_ERROR", ...}` (sem detalhes internos).

### Docker

`Dockerfile` em `dynamic-programming/` (`python:3.12-slim`, `uvicorn` na porta `$PORT`, padrão 8000). A imagem **não inclui** o mapa nem o modelo: aponte `GRAFO_PATH` e `MODELO_PATH` para arquivos copiados ou montados no deploy.

```bash
docker build -t rota-segura-dp .
docker run -p 8000:8000 -v "$PWD/dados:/dados" -e GRAFO_PATH=/dados/grafo_teste_jardim_helena.json rota-segura-dp
```

## Como testar

```bash
python -m pytest        # 103 testes
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
| `test_mapa.py` | Haversine, esquina mais próxima, trecho alagado pela ponta ou pelo ponto médio, validação do arquivo do mapa |
| `test_rota_endpoint.py` | `/route` e `/health`: RN05, RN06, sem rota (200), origem longe (400), desempate, 70 m/min, 422, 503, 500, memoização |
| `test_risk_endpoint.py` | `/risk`: 503 sem modelo, `lowData`, variáveis conferidas à mão, fuso, 400, carga do joblib |

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
- A memoização vale dentro de um processo, como num serviço que atende muitas consultas. O comando de terminal roda um processo por consulta, então ali ela não ganha nada. O dicionário é LRU com 128 cenários.
- Na resposta `sem_rota_segura`, `distancia_m` vale 0,0, porque não há caminho. Quem consome a saída deve olhar o `status`, não a distância.
- O grafo real do Jardim Helena (OpenStreetMap) ainda não existe: o serviço usa um mapa de teste com coordenadas inventadas (`dados/grafo_teste_jardim_helena.json`).
