# Contrato do serviço Python — Rota Segura (v2)

> Substitui o v1 (GET /saude, POST /rotas com o mapa enviado pelo Java).
> Itens marcados **[ABERTO]** dependem de resposta do Kelwin ou de decisão do grupo.
> Responsável pelo serviço: João Paulo Santana Basta (RM 565383).

## 1. Visão geral

- Serviço FastAPI separado, chamado pela API Java.
- **O mapa fica no Python**: grafo de ruas do OpenStreetMap, perfil **a pé**, recortado no bairro-piloto **Jardim Helena** (decidido em 10/11).
- O Java manda, a cada chamada: a posição da pessoa, os abrigos com vaga e as áreas alagadas (coordenadas + raio).
- O Python devolve a rota mais segura até o abrigo mais próximo com vaga (RN05, RN06) e a probabilidade de alagamento por área. **Quem classifica o nível de risco em cm é o Java**, não o Python (decidido em 10/11).

| Endpoint | Método | Uso |
|---|---|---|
| `/health` | GET | Verificação de disponibilidade antes das chamadas |
| `/route` | POST | Rota segura até o abrigo (RF07) |
| `/risk` | POST | Probabilidade de alagamento (alerta antecipado) |

## 2. Convenções

- JSON com nomes em **camelCase**, endpoints em inglês.
- Coordenadas em graus decimais (WGS 84): `lat`, `lng`.
- Distâncias em metros, tempo em minutos, chuva em mm.
- Datas e horas em ISO 8601 com fuso (`2026-11-19T18:30:00-03:00`).
- URLs: `http://localhost:8000` em desenvolvimento; serviço próprio no Render (Dockerfile) em produção.

## 3. GET /health

Resposta `200`:

```json
{
  "status": "ok",
  "mapLoaded": true,
  "modelLoaded": true,
  "version": "2.0.0"
}
```

`mapLoaded` ou `modelLoaded` falsos significam que o serviço subiu, mas não consegue responder `/route` ou `/risk`. O Java trata como indisponível.

## 4. POST /route

### Requisição

```json
{
  "origin": { "lat": -23.4890, "lng": -46.4120 },
  "shelters": [
    { "id": 1, "name": "Escola Estadual A", "lat": -23.4850, "lng": -46.4080, "availableSpots": 40 },
    { "id": 2, "name": "Ginásio B",        "lat": -23.4930, "lng": -46.4170, "availableSpots": 0 }
  ],
  "floodedAreas": [
    { "areaId": 7, "lat": -23.4875, "lng": -46.4100, "radiusM": 150 }
  ]
}
```

- `shelters`: o Java pode mandar todos; o Python ignora os com `availableSpots` ≤ 0 (RN06). Os abrigos (com latitude e longitude) ficam cadastrados no Java, que os envia a cada chamada (decidido em 10/11).
- `floodedAreas`: formato decidido em 10/11 (`areaId`, `lat`, `lng`, `radiusM`). Todo trecho do grafo com algum ponto dentro do círculo sai do grafo (RN05).
- `origin` é convertida na esquina mais próxima do grafo.

### Resposta `200` — rota encontrada

```json
{
  "status": "ROUTE_FOUND",
  "shelter": { "id": 1, "name": "Escola Estadual A", "lat": -23.4850, "lng": -46.4080 },
  "distanceMeters": 620,
  "durationMin": 8,
  "path": [
    { "lat": -23.4890, "lng": -46.4120 },
    { "lat": -23.4872, "lng": -46.4095 },
    { "lat": -23.4850, "lng": -46.4080 }
  ],
  "message": "Siga a rota até Escola Estadual A."
}
```

### Resposta `200` — sem rota segura

Não é erro: é um resultado normal (RN05).

```json
{
  "status": "NO_SAFE_ROUTE",
  "shelter": null,
  "distanceMeters": null,
  "durationMin": null,
  "path": [],
  "message": "Não há rota segura agora. Vá para um local alto e aguarde ajuda."
}
```

O Java usa esse status para mandar a pessoa ao painel de prioridade (RN05).

### Regras do cálculo

- Custo do trecho = distância × (1 + fatorRisco). `fatorRisco` vem do modelo de Data Science, calculado no Python a partir da última estimativa de risco da área do trecho; sem estimativa, vale 0.
- Algoritmo: Dijkstra multi-origem a partir dos abrigos com vaga, com resultado memoizado por cenário (áreas alagadas + abrigos com vaga). Bellman-Ford tabular usado como verificação nos testes.
- Desempate: menor custo, depois menor distância, depois menor ID do abrigo.
- `durationMin` = distância ÷ 70 m/min (a pé, decidido em 10/11), arredondado para cima. O grafo usa só ruas caminháveis.
- Origem a mais de 300 m da rua mais próxima do grafo: `400` (fora do bairro-piloto).

## 5. POST /risk

Serve só para o **alerta antecipado**: a chance de alagar antes de acontecer. É um sinal separado dos níveis em cm (decidido em 09/11), que o Java classifica sozinho.

### Requisição

```json
{
  "areaId": 3,
  "lat": -23.4890,
  "lng": -46.4120,
  "readings": [
    { "timestamp": "2026-11-19T18:00:00-03:00", "rainMm": 12.4, "waterLevelCm": 2.3 },
    { "timestamp": "2026-11-19T18:10:00-03:00", "rainMm": 8.1,  "waterLevelCm": 2.8 }
  ]
}
```

- `readings`: leituras dos sensores da área dos **últimos 8 dias** (dia atual + 7 anteriores, que o modelo usa como chuva acumulada), em ordem crescente de horário. Com menos de 8 dias, o Python devolve `floodProbability` calculada com o que houver e `lowData: true`.
- O modelo usa a chuva acumulada; `waterLevelCm` vai junto para uso futuro e para validação.

### Resposta `200`

```json
{
  "areaId": 3,
  "floodProbability": 0.82,
  "highProbability": true,
  "lowData": false,
  "modelVersion": "2026-11-17"
}
```

- `floodProbability`: saída da regressão logística (0 a 1).
- `highProbability`: `floodProbability` ≥ limite. Regra: o limite é o de maior precisão entre os que avisam ao menos 70% dos dias de alagamento na validação (2023). O valor numérico entra aqui depois do treino (até 17/11).
- Texto do alerta antecipado (o Java monta o SMS quando `highProbability` for `true`): "Há alta chance de alagamento na sua área hoje. Prepare-se para ir ao abrigo."

## 6. Erros

| Código | Quando | Corpo |
|---|---|---|
| `400` | Entrada inválida (coordenada fora do mapa, raio ≤ 0, vagas negativas, leituras vazias ou fora de ordem) | `{ "error": "INVALID_INPUT", "message": "texto claro" }` |
| `422` | JSON fora do esquema (validação automática do FastAPI) | formato padrão do FastAPI |
| `500` | Falha inesperada | `{ "error": "INTERNAL_ERROR", "message": "..." }` |

Internamente, a entrada inválida gera `EntradaInvalidaError`, convertida em `400`.

## 7. Comportamento do Java (decidido em 10/11)

- Timeout de **10 s** por chamada. Ping em `/health` a cada poucos minutos para o Render gratuito não hibernar o serviço.
- O alerta não depende do Python: o Java calcula o nível em cm e alerta a partir do nível médio.
- Se o Python estiver fora (timeout, `500` ou `/health` com `mapLoaded`/`modelLoaded` falso), o SMS informa que a rota está indisponível e orienta ir ao abrigo ou a um local mais alto.

## 8. Configuração do serviço

| Variável | Uso |
|---|---|
| `PORT` | Porta HTTP (o Render define) |
| `GRAFO_PATH` | Caminho do grafo do bairro-piloto (arquivo gerado a partir do OSM) |
| `MODELO_PATH` | Caminho do modelo treinado (`.joblib`) |

## 9. Fontes de dados

- Mapa: © colaboradores do OpenStreetMap, licença ODbL.
- Chuva: DADOS DA REDE OBSERVACIONAL DO CEMADEN/MCTIC (Plataforma de Entrega de Dados, ped.cemaden.gov.br).
- Alagamentos (rótulo do modelo): CGE — Prefeitura de São Paulo, histórico diário de pontos de alagamento (2016–2025, raspagem de cgesp.org/v3/alagamentos.jsp). Validação no Alto Tietê: Cemaden, Reindesc.

## 10. Histórico

| Versão | Data | Mudança |
|---|---|---|
| v1 | 09/11/2026 | GET /saude, POST /rotas com o mapa enviado pelo Java |
| v2 | 11/11/2026 | Mapa no Python (OSM, a pé, Jardim Helena); /health, /route, /risk; camelCase; áreas alagadas com `areaId`, `lat`, `lng`, `radiusM`; /risk só com probabilidade (nível em cm fica no Java) |
