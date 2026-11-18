# Rota Segura API — tabela de endpoints

Gerada a partir dos controllers em `domain-driven-design-java/backend/src/main/java/br/com/rotasegura/controller/`.

- **Documentação interativa (Swagger UI):** `<endereço da API>/swagger-ui.html` (redireciona para `/swagger-ui/index.html`); o JSON do OpenAPI fica em `/v3/api-docs`.
- **Login:** `GET` e `HEAD` são abertos; `POST`, `PUT` e `DELETE` exigem login (sessão por cookie) e o cabeçalho `X-CSRF-TOKEN`. Exceções: o `POST` de login e o `POST` de leituras do sensor são abertos e sem token; `GET /api/auth/me` e **todas** as rotas de pessoas assistidas exigem login, inclusive o `GET` (dados pessoais).
- **Erros:** sempre em `ProblemDetail` (`application/problem+json`) com `title` e `detail` em português. Status comuns a todas as rotas: **401** sem login nas rotas protegidas (sem login é sempre 401, mesmo que falte também o token), **403** com login mas sem token CSRF válido, **400** quando o `{id}` não é um número, **405** método não aceito, **415** corpo que não é JSON, **500** erro interno.
- **Reservado:** `POST /api/check-ins` (RF09) já está liberado na segurança, mas ainda não existe: hoje responde 404.

## Autenticação

| Método | Rota | O que faz | Corpo da requisição | Respostas (status) |
|---|---|---|---|---|
| POST | `/api/auth/login` | Confere usuário e senha e abre a sessão (cookie) | `LoginRequest`: `username`, `password` | 200, 400, 401 |
| GET | `/api/auth/me` | Usuário da sessão atual | — | 200, 401 |
| GET | `/api/auth/csrf` | Token CSRF e o nome do cabeçalho onde enviá-lo | — | 200 |
| POST | `/api/auth/logout` | Encerra a sessão (feito pelo filtro do Spring Security) | — (com `X-CSRF-TOKEN`) | 204, 401, 403 |

## Áreas de risco (RF01)

| Método | Rota | O que faz | Corpo da requisição | Respostas (status) |
|---|---|---|---|---|
| GET | `/api/areas` | Lista as áreas, em ordem de nome | — | 200 |
| GET | `/api/areas/{id}` | Busca uma área | — | 200, 400, 404 |
| POST | `/api/areas` | Cadastra uma área | `RiskAreaRequest`: `name`, `city`, `riskType` (`FLOOD`/`LANDSLIDE`), `latitude`, `longitude`, `radiusM`; `district` opcional | 201, 400, 401, 403, 409 (nome repetido na cidade) |
| PUT | `/api/areas/{id}` | Atualiza todos os dados da área | `RiskAreaRequest` | 200, 400, 401, 403, 404, 409 |
| DELETE | `/api/areas/{id}` | Exclui a área | — | 204, 400, 401, 403, 404, 409 (tem sensores ou pessoas assistidas) |

## Nível de risco (RF04)

| Método | Rota | O que faz | Corpo da requisição | Respostas (status) |
|---|---|---|---|---|
| GET | `/api/areas/risk` | Nível de risco atual de todas as áreas (para o mapa): em cada área, a pior entre as leituras mais recentes da última 1 hora dos sensores ativos | — | 200 |
| GET | `/api/areas/{id}/risk` | Nível de risco atual de uma área | — | 200, 400, 404 |

## Sensores (RF02)

| Método | Rota | O que faz | Corpo da requisição | Respostas (status) |
|---|---|---|---|---|
| GET | `/api/sensors?areaId=` | Lista os sensores (todos, ou só os de uma área), em ordem de código | — | 200, 400 |
| GET | `/api/sensors/{id}` | Busca um sensor | — | 200, 400, 404 |
| POST | `/api/sensors` | Cadastra um sensor numa área | `SensorRequest`: `areaId`, `code`, `latitude`, `longitude`, `status` (`ACTIVE`/`INACTIVE`) | 201, 400, 401, 403, 404 (área), 409 (código repetido) |
| PUT | `/api/sensors/{id}` | Atualiza o sensor (pode mudar de área) | `SensorRequest` | 200, 400, 401, 403, 404, 409 |
| DELETE | `/api/sensors/{id}` | Exclui o sensor (só se não tiver leituras) | — | 204, 400, 401, 403, 404, 409 (tem leituras: inativar) |

## Leituras dos sensores (RF03)

| Método | Rota | O que faz | Corpo da requisição | Respostas (status) |
|---|---|---|---|---|
| POST | `/api/sensor-readings` | Registra uma leitura enviada pelo aparelho (aberto, sem login e sem token) | `SensorReadingRequest`: `sensorCode`, `rainMm`, `waterLevelCm`, `measuredAt` (ISO 8601 com fuso; até 5 min no futuro) | 201, 400, 404 (código desconhecido), 409 (sensor inativo ou leitura repetida) |
| GET | `/api/sensor-readings?sensorId=&from=&to=` | Histórico de um sensor, mais recentes primeiro (`sensorId` obrigatório) | — | 200, 400 |
| GET | `/api/sensor-readings/{id}` | Busca uma leitura | — | 200, 400, 404 |

Leitura é histórico: não há `PUT` nem `DELETE` (405 com login e token; 403 com login sem token; 401 sem login).

## Abrigos (RF06)

| Método | Rota | O que faz | Corpo da requisição | Respostas (status) |
|---|---|---|---|---|
| GET | `/api/shelters` | Lista os abrigos com as vagas livres (`availableSpots`), em ordem de nome | — | 200 |
| GET | `/api/shelters/{id}` | Busca um abrigo | — | 200, 400, 404 |
| POST | `/api/shelters` | Cadastra um abrigo | `ShelterRequest`: `name`, `address`, `latitude`, `longitude`, `capacity`, `occupiedSpots` | 201, 400, 401, 403, 409 (ocupadas acima da capacidade) |
| PUT | `/api/shelters/{id}` | Atualiza o abrigo, inclusive a ocupação | `ShelterRequest` | 200, 400, 401, 403, 404, 409 |
| DELETE | `/api/shelters/{id}` | Exclui o abrigo | — | 204, 400, 401, 403, 404 |

## Pessoas assistidas e responsáveis (RF08) — todas exigem login

| Método | Rota | O que faz | Corpo da requisição | Respostas (status) |
|---|---|---|---|---|
| GET | `/api/assisted-people?areaId=` | Lista as pessoas (todas, ou só as de uma área) com os responsáveis | — | 200, 400, 401 |
| GET | `/api/assisted-people/{id}` | Busca uma pessoa com os responsáveis | — | 200, 400, 401, 404 |
| POST | `/api/assisted-people` | Cadastra a pessoa junto com os responsáveis (ao menos um, RN03) | `AssistedPersonRequest`: `name`, `phone`, `areaId`, `latitude`, `longitude`, `specialNeed` (`ELDERLY`/`BEDRIDDEN`/`WHEELCHAIR`/`OTHER`), `guardians` (lista de 1 a 10 `GuardianRequest`: `name`, `phone`, `relationship`); `notes` opcional | 201, 400, 401, 403, 404 (área) |
| PUT | `/api/assisted-people/{id}` | Atualiza a pessoa e troca a lista inteira de responsáveis | `AssistedPersonRequest` | 200, 400, 401, 403, 404 |
| DELETE | `/api/assisted-people/{id}` | Exclui a pessoa e os responsáveis dela | — | 204, 400, 401, 403, 404 |

## Rota segura até o abrigo (RF07) — exige login

| Método | Rota | O que faz | Corpo da requisição | Respostas (status) |
|---|---|---|---|---|
| GET | `/api/assisted-people/{id}/route` | Rota a pé de onde a pessoa mora até o abrigo com vaga mais próximo, sem passar por área alagada (RN05, RN06). Calculada pelo serviço Python (`POST /route` do contrato v2) | — | 200, 400, 401, 404 |

A resposta é **sempre 200** quando a pessoa existe; o campo `status` diz o que aconteceu, e `message` traz a orientação em linguagem simples:

| `status` | Quando | O que vem junto |
|---|---|---|
| `ROUTE_FOUND` | Achou rota | `shelter` (com o endereço do cadastro), `distanceMeters`, `durationMin` (a pé, 70 m/min) e `path` (pontos do caminho) |
| `NO_SAFE_ROUTE` | Todo caminho passa por área alagada | Orientação do Python: ir para um local alto e aguardar ajuda |
| `ROUTE_UNAVAILABLE` | Serviço Python fora do ar, com erro ou mais de 10 s | Orientação de ir ao abrigo mais próximo ou a um local mais alto (decidido em 10/11) |
| `OUTSIDE_PILOT_AREA` | A pessoa mora fora do mapa do bairro-piloto (Jardim Helena) | A mesma orientação geral |

Áreas evitadas pela rota: as outras áreas em nível **alto ou acima**, e a área da própria pessoa só em **muito urgente** (decidido em 18/11). Abrigos lotados são ignorados. O timeout é de 10 s para conectar e 10 s para ler a resposta. Pela RN05, quem fica sem rota segura iria para o painel de resgate; isso depende do painel (RF10, Should), que ainda não existe: hoje o `NO_SAFE_ROUTE` não é gravado.

O conjunto tem **GET, POST, PUT e DELETE**, como pede a rubrica (áreas, sensores, abrigos e pessoas assistidas têm os quatro).

## Exemplos (dados simulados)

### POST `/api/areas` (com login e `X-CSRF-TOKEN`)

Requisição:

```json
{
  "name": "Várzea de Exemplo",
  "city": "São Paulo",
  "district": "Jardim Helena",
  "riskType": "FLOOD",
  "latitude": -23.485,
  "longitude": -46.41,
  "radiusM": 300
}
```

Resposta `201 Created` (cabeçalho `Location: <endereço da API>/api/areas/11`; o id depende do banco):

```json
{
  "id": 11,
  "name": "Várzea de Exemplo",
  "city": "São Paulo",
  "district": "Jardim Helena",
  "riskType": "FLOOD",
  "latitude": -23.485,
  "longitude": -46.41,
  "radiusM": 300
}
```

Mesmo POST com `radiusM: 0` → `400 Bad Request`:

```json
{
  "type": "about:blank",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Confira os campos indicados.",
  "instance": "/api/areas",
  "errors": [
    { "field": "radiusM", "message": "O raio deve ser maior que zero" }
  ]
}
```

### GET `/api/assisted-people/1/route` (com login)

Resposta `200 OK` (trecho do `path`):

```json
{
  "assistedPersonId": 1,
  "status": "ROUTE_FOUND",
  "shelter": {
    "id": 6,
    "name": "Escola Municipal de Educação Fundamental Murures",
    "address": "Rua dos Murures",
    "latitude": -23.476716,
    "longitude": -46.418575
  },
  "distanceMeters": 1039,
  "durationMin": 15,
  "path": [
    { "latitude": -23.4805, "longitude": -46.4138 },
    { "latitude": -23.476716, "longitude": -46.418575 }
  ],
  "message": "Siga a rota até Escola Municipal de Educação Fundamental Murures."
}
```

Resultado real do serviço Python em 18/11 para a Benedita (Jardim Helena, dados fictícios do `dml.sql`), com o Jardim Pantanal alagado; o `path` completo tem dezenas de pontos. A rota muda com as áreas alagadas e as vagas do momento. Para quem mora fora do Jardim Helena (ex.: Poá), o status é `OUTSIDE_PILOT_AREA`.

### GET `/api/areas/risk` (aberto)

O exemplo supõe leituras da última hora, como as que a coleção do Postman da demonstração envia. Só com as leituras do `dml.sql` (de 15/11), todas as áreas saem com `hasData: false`.

Resposta `200 OK` (trecho):

```json
[
  {
    "areaId": 6,
    "areaName": "Baixada Leste de Guarulhos",
    "hasData": true,
    "riskLevel": "VERY_URGENT",
    "waterLevelCm": 11.30,
    "sensorCode": "SEN-GRU-001",
    "measuredAt": "2026-11-18T20:15:00Z"
  },
  {
    "areaId": 10,
    "areaName": "Morro de Poá",
    "hasData": false,
    "riskLevel": null,
    "waterLevelCm": null,
    "sensorCode": null,
    "measuredAt": null
  }
]
```
