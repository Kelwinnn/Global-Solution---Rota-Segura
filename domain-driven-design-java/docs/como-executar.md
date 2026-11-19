# Rota Segura API — como executar

Instruções para rodar a API Java localmente e no Render (seção "Instruções de execução" do PDF de Domain Driven Design Java).

## Ferramentas e versões

| Ferramenta | Versão |
|---|---|
| Java (JDK) | 21 |
| Maven | 3.9.12 (via Maven Wrapper `./mvnw`, já no projeto; não precisa instalar) |
| Spring Boot | 3.4.1 |
| Banco de dados | Oracle da FIAP (driver `ojdbc11`) |
| Documentação da API | springdoc (Swagger UI) 2.7.0 |
| Container | Docker (imagem `eclipse-temurin:21-jre`) |
| Hospedagem | Render (Web Service com Docker) |

## Links

- **Repositório da entrega de DDD Java:** https://github.com/Kelwinnn/Global-Solution---Rota-Segura-DDD-Java (API em `backend/`, scripts em `docs/sql/`)
- **Repositório completo do projeto:** https://github.com/Kelwinnn/Global-Solution---Rota-Segura (API em `domain-driven-design-java/backend/`, scripts em `database-design/`; é dele que o Render faz o deploy)
- **API publicada:** https://global-solution-rota-segura.onrender.com
- **Swagger UI:** https://global-solution-rota-segura.onrender.com/swagger-ui.html

## Variáveis de ambiente

Nenhuma credencial fica no código. A API lê estas variáveis (só os nomes; os valores ficam no `.env` local e no painel do Render):

| Variável | Para quê | Obrigatória |
|---|---|---|
| `DB_URL` | URL JDBC do Oracle da FIAP | sim |
| `DB_USERNAME` | Usuário do Oracle | sim |
| `DB_PASSWORD` | Senha do Oracle | sim |
| `ADMIN_USERNAME` | Usuário do login do painel | sim |
| `ADMIN_PASSWORD` | Senha do login do painel | sim |
| `ROUTE_SERVICE_URL` | URL do serviço Python de rota e risco (RF07) | **sim no Render** (`https://rota-segura-python.onrender.com`); localmente o padrão é `http://localhost:8000`. Sem o Python no endereço, a rota responde `ROUTE_UNAVAILABLE`. Para testar o RF07 localmente, suba o Python (`dynamic-programming/README.md`) ou aponte para o Render |
| `CORS_ALLOWED_ORIGINS` | Origens do front com permissão de CORS, separadas por vírgula | não (padrão `http://localhost:5173`) |
| `PORT` | Porta HTTP | não (padrão `8080`; o Render define sozinho) |

O modelo está em `domain-driven-design-java/backend/.env.example`.

## 1. Criar as tabelas no Oracle

A API **não cria tabelas**: ela sobe com `spring.jpa.hibernate.ddl-auto=validate` e só confere se as tabelas batem com as entidades. Por isso, antes da primeira execução:

1. Abra o SQL Developer conectado ao Oracle da FIAP.
2. Abra o `ddl.sql` (`docs/sql/` no repositório de DDD; `database-design/` no repositório completo) e rode com **Executar Script (F5)**.
3. Abra o `dml.sql`, na mesma pasta, e rode com **Executar Script (F5)** (dados de exemplo simulados).
4. Opcional: o `dql.sql` traz as três consultas de exemplo.

Os arquivos estão em UTF-8 (Ferramentas > Preferências > Ambiente > Codificação = UTF-8).

## 2. Rodar localmente (sem Docker)

Dentro de `domain-driven-design-java/backend/`:

```bash
# 1. Criar o .env a partir do modelo e preencher os valores
cp .env.example .env

# 2. Carregar as variáveis no terminal e subir a API
set -a; source .env; set +a
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd spring-boot:run` e defina as variáveis no terminal (ou na configuração de execução da IDE).

A API sobe em `http://localhost:8080`. Testes rápidos:

- `http://localhost:8080/swagger-ui.html` — documentação interativa
- `http://localhost:8080/api/areas` — lista as áreas de risco (aberto, sem login)

Para rodar os testes automatizados (não precisam do Oracle):

```bash
./mvnw test
```

## 3. Rodar localmente com Docker

Dentro de `domain-driven-design-java/` (a pasta acima de `backend/`, porque é o contexto do build):

```bash
docker build -f backend/Dockerfile -t rota-segura-api .
docker run --rm -p 8080:8080 --env-file backend/.env rota-segura-api
```

## 4. Publicar no Render

1. No Render, crie um **Web Service** ligado ao repositório do GitHub.
2. Configure:
   - **Language / Runtime:** Docker
   - **Root Directory:** `domain-driven-design-java`
   - **Dockerfile Path:** `backend/Dockerfile` (relativo ao Root Directory; confira na tela do Render)
   - **Docker Build Context Directory:** `.` (o próprio Root Directory)
   - **Health Check Path:** `/v3/api-docs` (responde sem depender do banco)
3. Em **Environment**, cadastre as variáveis da tabela acima (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`) e `ROUTE_SERVICE_URL` com o endereço do serviço Python. Não cadastre `PORT`: o Render define sozinho.
4. Crie o serviço e acompanhe o log do build. A API está no ar quando aparecer `Started RotaSeguraApplication`.
5. Abra `<endereço do Render>/swagger-ui.html`.

Observações:

- No plano gratuito, o serviço "dorme" depois de um tempo sem uso; a primeira chamada depois disso demora alguns segundos.
- Se o log mostrar erro de conexão com o Oracle, o Render pode não estar conseguindo acessar o banco da FIAP: veja a seção "Verificações depois do deploy".

## 5. Verificações depois do deploy

1. `GET /api/areas` devolve as áreas do `dml.sql` (prova que a API acessa o Oracle), e `GET /api/shelters` os 6 abrigos (um deles lotado, com `availableSpots` 0).
2. No Swagger: `POST /api/auth/login` com o usuário e a senha do `.env`, depois `GET /api/auth/csrf`, copiar o `token` e colar em **Authorize**.
3. Um `POST /api/areas` autenticado devolve 201.
4. O cookie de sessão chega com `SameSite=None` e `Secure` (ferramentas do navegador > Aplicação > Cookies).

## 6. Demonstração com o Postman

O nível de risco (RF04) só considera leituras da última 1 hora, e as leituras do `dml.sql` são de 15/11. Por isso, na demonstração, as leituras são enviadas na hora pela coleção `domain-driven-design-java/docs/rota-segura-demo.postman_collection.json` (dados simulados):

1. **Leituras dos sensores (RF03):** 11 leituras, com a hora do momento (agora, há 15 e há 30 minutos).
2. **Sensor inativo:** a leitura do `SEN-JP-002` é recusada com 409.
3. **Mapa de risco (RF04):** `GET /api/areas/risk` confere o nível de cada área (Jardim Helena alto, Jardim Pantanal muito alto, São Miguel baixo, Itaim Paulista médio, Guarulhos muito urgente) e o histórico do `SEN-JH-001`.

Para rodar:

- **No Postman:** File > Import, escolher o arquivo, ajustar a variável `baseUrl` da coleção (padrão `http://localhost:8080`; para o Render, o endereço publicado) e usar **Run collection**.
- **No terminal (Postman CLI):**

  ```bash
  postman collection run domain-driven-design-java/docs/rota-segura-demo.postman_collection.json --env-var "baseUrl=http://localhost:8080"
  ```

Pode rodar mais de uma vez: cada execução usa a hora do momento. Antes, o banco precisa ter as áreas e os sensores do `dml.sql`.

### Testes da API publicada (login, RF07 e cenário de chuva)

A coleção `domain-driven-design-java/docs/rota-segura-testes.postman_collection.json` (29 requisições, com testes automáticos) confere as rotas abertas, o login com sessão e CSRF, a privacidade das pessoas assistidas, a rota até o abrigo (RF07) nos três resultados (rota encontrada, fora do bairro-piloto e sem rota segura) e o logout. A pasta 4 grava leituras de teste e as desfaz no fim; a pasta 5 cria e apaga uma área de teste. O usuário e a senha nunca ficam na coleção:

```bash
postman collection run domain-driven-design-java/docs/rota-segura-testes.postman_collection.json --env-var "user=<ADMIN_USERNAME>" --env-var "pass=<ADMIN_PASSWORD>"
```

Antes, feche o SQL Developer e a API local (o Oracle aceita uma única sessão).

## Como usar a API autenticada (resumo)

1. `POST /api/auth/login` com `{"username": "...", "password": "..."}` (abre a sessão por cookie).
2. `GET /api/auth/csrf` devolve `{"headerName": "X-CSRF-TOKEN", "token": "..."}`.
3. Em todo `POST`, `PUT` e `DELETE` protegido, enviar o cabeçalho `X-CSRF-TOKEN` com o token.
4. `POST /api/auth/logout` encerra a sessão (depois dele, buscar um token novo).

Exceção: as rotas de pessoas assistidas (`/api/assisted-people`) exigem login em todos os verbos, **inclusive no `GET` e no `HEAD`**, porque mostram dados pessoais de gente vulnerável.

O envio de leituras do sensor (`POST /api/sensor-readings`) fica aberto, sem login e sem token. O check-in do morador (RF09) seguirá a mesma regra quando for implementado.
