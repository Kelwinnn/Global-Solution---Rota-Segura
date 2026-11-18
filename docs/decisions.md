# Decisões de arquitetura

Regra: o Claude Code segue o que está em **Decidido** e **pergunta antes** de qualquer item de **Em aberto** ou de qualquer decisão que não esteja aqui. Ao decidir, mover o item para Decidido com a data.

## Decidido

| Data | Tema | Decisão |
|---|---|---|
| 09/11 | Repositório | Monorepo único, uma pasta por matéria, em kebab-case em inglês (estrutura abaixo) |
| 09/11 | Front | Fica dentro de `domain-driven-design-java/`, ao lado da API |
| 09/11 | Serviço Python | Fica dentro de `dynamic-programming/`; dono: João Paulo |
| 09/11 | Java | Java 21 + Maven + Spring Boot 3.x |
| 09/11 | Pacote base | `br.com.rotasegura` |
| 09/11 | Pacotes do Java | `config`, `controller`, `bo`, `dao`, `repository`, `model`, `dto`, `exception`, `security`, `util` (papéis abaixo) |
| 09/11 | Banco | Oracle da FIAP, acesso com Spring Data JPA |
| 09/11 | Tabelas | Prefixo `T_RS_` (ex.: `T_RS_SHELTER`) e chave primária com `IDENTITY` |
| 09/11 | Controller | Recebe e devolve DTOs (records) com Bean Validation; entidades não saem da API |
| 09/11 | Segurança | Login simples com Spring Security e usuário fixo, cujas credenciais vêm de variáveis de ambiente. POST, PUT e DELETE exigem login; GET fica aberto |
| 09/11 | Mecanismo do login | Tela de login no React; sessão com cookie no servidor (Spring Security). Mesmo com o front servido junto do Java em produção, a configuração foi mantida (em desenvolvimento o Vite roda em outra porta); o cookie usa `SameSite=None` e `Secure`, e o CORS libera credenciais só para as origens do front |
| 09/11 | CSRF | Token mantido. O front lê o token e envia em cada POST, PUT e DELETE. Endpoints liberados (sem login) ficam fora do CSRF, porque não têm sessão (11/11: só no POST deles) |
| 09/11 | Sessão | Expira em 30 minutos. O logout encerra só a sessão na página; alertas e mensagens continuam sendo enviados normalmente |
| 09/11 | Canal com o cidadão | SMS simulado: o sistema registra a mensagem e o front mostra como se fosse o celular |
| 09/11 | Respostas do check-in | Três opções: "Estou seguro", "Estou a caminho de um local seguro" e "Ainda não saí" |
| 09/11 | Endpoints liberados | Todo endpoint em que o cidadão precisa responder durante a emergência fica sem login. Liberados: check-in "Estou seguro" (RF09) e envio de leituras do sensor (RF03) |
| 09/11 | Documentação da API | springdoc (Swagger UI), aberto sem login |
| 09/11 | Testes | JUnit 5 + Mockito nos BOs; MockMvc nos controllers. 13/11: `@DataJpaTest` com H2 nos repositories |
| 09/11 | Deploy da API | Render, via Dockerfile |
| 09/11 | Frontend | React + Vite + TypeScript + Tailwind, feito pelos três |
| 09/11 | Rota e risco | Serviço Python em FastAPI, chamado pela API Java |
| 09/11 | Idioma do código | Inglês (classes, métodos, tabelas, colunas) |
| 09/11 | Modo de trabalho | Claude gera e explica linha a linha; o Kelwin revisa e commita |
| 09/11 | Base do Data Science | Cemaden (ver 11/11) |
| 09/11 | Versão do Spring Boot | 3.4.1 |
| 09/11 | Nome das colunas | Chave primária no padrão `ID_<ENTIDADE>` (ex.: `ID_SHELTER` na tabela `T_RS_SHELTER`) |
| 09/11 | Front: chamadas à API | `fetch` nativo (sem axios) |
| 09/11 | Front: mapa | Google Maps (a chave da API fica em variável de ambiente do Vite, nunca no código) |
| 09/11 | Front: pastas | Estrutura por componentes |
| 09/11 | Deploy do front | Junto do Java: a API Spring serve o front já compilado (mesmo serviço no Render) |
| 09/11 | Chamada Java → Python | `RestClient` (Spring). Timeout e comportamento se o serviço cair: ver as linhas de 10/11 |
| 09/11 | Front: roteador | React Router |
| 09/11 | CORS, cookie e CSRF | Mantidos como decidido (cookie `SameSite=None` + `Secure`, CORS com credenciais, CSRF ligado), mesmo com o front junto do Java. Origem permitida no desenvolvimento: `http://localhost:5173` (Vite); API em `http://localhost:8080`. Origens ficam em variável de ambiente |
| 09/11 | Nome das FKs | Repetem o nome da PK referenciada (ex.: `ID_AREA` em `T_RS_SENSOR`) |
| 09/11 | Níveis de risco (RF04/RN01) | 5 níveis pelo nível d'água em cm, iguais para todos os sensores: baixo 0 a 1; médio 1,1 a 2,9; alto 3 a 5; muito alto 5,1 a 10; muito urgente acima de 10. Os níveis dizem o quão urgente é ir para um abrigo ou procurar um local mais alto |
| 09/11 | Quando gera alerta (RF05/RN07) | A partir do nível médio. Enquanto o alerta durar, o morador recebe a atualização do nível a cada 15 min; a pergunta do check-in se repete só para quem ainda não está seguro. Quem respondeu "Estou seguro" recebe só as atualizações |
| 10/11 | Mapa de ruas | Fica no serviço Python (OpenStreetMap). O Java não guarda mapa: manda só a posição do morador, os abrigos com vaga e as áreas alagadas |
| 10/11 | Coordenadas | Área de risco com ponto central (latitude e longitude) e raio em metros; abrigo com latitude e longitude. O Java envia as áreas alagadas ao Python como lista com `areaId`, `lat`, `lng` e `radiusM` |
| 10/11 | Timeout da chamada ao Python | 10 s. Um ping no `/health` a cada poucos minutos evita que o Render gratuito hiberne o serviço |
| 10/11 | Se o Python estiver fora | O alerta não depende dele: o Java calcula o nível em cm e alerta a partir do médio. Sem o Python, o SMS informa que a rota está indisponível e orienta ir ao abrigo ou a um local mais alto |
| 10/11 | Bordas das faixas de risco | O valor fica no nível cujo número inicial já alcançou: baixo < 1,1; médio de 1,1 a < 3; alto de 3 a < 5,1; muito alto de 5,1 a 10; muito urgente > 10 (1,05 é baixo; 10,01 é muito urgente). Quem classifica o nível é o Java |
| 10/11 | Rota | Para quem vai a pé: o grafo usa só ruas caminháveis, cerca de 70 m por minuto |
| 09/11 | Probabilidade de alagamento | O serviço Python informa a alta probabilidade de alagamento antes de acontecer (alerta antecipado). É um sinal separado dos níveis em cm, que medem a urgência |
| 10/11 | Variáveis do Oracle | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, lidas no `application.properties` com `${...}`. Porta da API em `PORT` (padrão 8080) |
| 10/11 | Criação das tabelas | `spring.jpa.hibernate.ddl-auto=validate`: as tabelas vêm do `ddl.sql` de `database-design/` e o Hibernate só confere se as entidades batem com elas |
| 10/11 | Build do Docker | `Dockerfile` em `domain-driven-design-java/backend/`, com contexto de build em `domain-driven-design-java/` (raiz do serviço no Render), para depois entrar um estágio Node que compila o front |
| 10/11 | Coordenadas Maven | `groupId` `br.com.rotasegura`, `artifactId` `rota-segura-api`; Maven Wrapper (`./mvnw`) no projeto; springdoc 2.7.0 |
| 10/11 | Variáveis do login e do CORS | `ADMIN_USERNAME`, `ADMIN_PASSWORD` e `CORS_ALLOWED_ORIGINS` (várias origens separadas por vírgula; padrão `http://localhost:5173`) |
| 10/11 | Endpoint de login | `POST /api/auth/login` com JSON `{username, password}`, tratado no `AuthController` (escolha do Kelwin: mais legível que o `formLogin`). Resposta 200 com o usuário ou 401. Logout em `POST /api/auth/logout` (204), feito pelo filtro do Spring Security. `GET /api/auth/me` devolve o usuário da sessão ou 401 |
| 10/11 | Token CSRF no front | `GET /api/auth/csrf` devolve `{headerName, token}`; o front envia o token no cabeçalho indicado (`X-CSRF-TOKEN`) em cada POST, PUT e DELETE. O login não precisa de token (está fora do CSRF). Depois do login e do logout, buscar um token novo (11/11: o login passou a descartar o token anterior). As rotas liberadas ficam fora do CSRF só no POST |
| 10/11 | Prefixo das rotas | Toda a API fica em `/api/...`. Rotas liberadas reservadas: `POST /api/auth/login`, `POST /api/sensor-readings` (RF03) e `POST /api/check-ins` (RF09) |
| 10/11 | `AuthController` sem BO | Exceção ao fluxo `controller → bo → dao`: autenticação não é regra de negócio, então o controller fala direto com o Spring Security |
| 10/11 | Nome das demais colunas | Em inglês, `SNAKE_CASE` em maiúsculas (ex.: `WATER_LEVEL_CM`, `CREATED_AT`), além do padrão de PK e FK |
| 10/11 | Bairro-piloto do grafo de ruas | Jardim Helena. Os demais bairros ficam cadastrados, sem grafo. O grafo e a estação do INMET são do João Paulo: avisar a escolha |
| 10/11 | RF14 (mapa interativo) | Sobe de Could para **Must** e é a tela inicial do front |
| 11/11 | Proxy do Render | `server.forward-headers-strategy=framework`: o Java lê `X-Forwarded-*` e sabe que a chamada original era `https`. Sem isso, o front servido pelo próprio Java levaria 403 de CORS em todo POST. Limitação: o filtro aceita esses cabeçalhos de qualquer cliente, então o IP de `getRemoteAddr()` pode ser forjado; não usar esse IP em limite de taxa, bloqueio ou auditoria. O mesmo vale para o `X-Forwarded-Prefix`: alguns valores inválidos (sem `/` no começo, ou com `%` malformado) são recusados pelo próprio Spring com 500, antes do nosso código, e só afetam a requisição de quem forjou o cabeçalho |
| 11/11 | 401 ou 403 | Sem login, inclusive com a sessão expirada, a resposta é 401 (o front manda para o login). Com login e sem permissão ou sem token CSRF válido, 403 (`RestAccessDeniedHandler`) |
| 11/11 | Swagger com CSRF | Esquema `csrf` (cabeçalho `X-CSRF-TOKEN`) no botão Authorize, aplicado só a POST, PUT e DELETE protegidos. Fluxo: login em `POST /api/auth/login` → token em `GET /api/auth/csrf` → Authorize |
| 11/11 | HEAD | Aberto como o GET (monitores de disponibilidade). `/api/auth/me` exige login em qualquer método |
| 11/11 | Memória da JVM | `-XX:MaxRAMPercentage=75` no Dockerfile (o plano gratuito do Render tem 512 MB) |
| 11/11 | Leituras do sensor sem autenticação | **Risco aceito do MVP:** `POST /api/sensor-readings` fica aberto, então uma leitura falsa pode disparar alerta e SMS. Motivo (Kelwin): na emergência, nada pode depender de cadastro ou login, e os sensores do MVP são simulados. Citar no PDF como limitação conhecida; numa versão real, cada sensor teria uma chave própria |
| 11/11 | Formato do JSON de erro | `ProblemDetail` do Spring (RFC 7807), com `title` e `detail` em português. Erros de validação vão num campo extra `errors`: lista de `{field, message}`. O `GlobalExceptionHandler` estende `ResponseEntityExceptionHandler`. Erros do próprio Spring MVC (405, 415, 406, rota inexistente) têm título e texto em português em `messages.properties`. Erro inesperado vira 500 "Erro interno", sem detalhes na resposta (só no log) |
| 11/11 | Status de erro | 400 dados inválidos; 404 não encontrado (`ResourceNotFoundException`); 409 regra de negócio violada (`BusinessRuleException`) ou conflito no banco (`DataIntegrityViolationException`) |
| 11/11 | Onde a entidade vira DTO | No BO, dentro da transação (`@Transactional`): o BO recebe e devolve DTOs (`Response.from(entity)`), e o controller nunca toca em entidade. Resolve o `LAZY` com `open-in-view=false` |
| 11/11 | Área de risco (RF01) | Classe `RiskArea` na tabela `T_RS_AREA` (`ID_AREA`). Campos: `NAME` (100, obrigatório), `CITY` (60, obrigatório), `DISTRICT` (60, opcional), `RISK_TYPE` (`FLOOD` ou `LANDSLIDE`, `VARCHAR2(20 CHAR)`, obrigatório), `LATITUDE` e `LONGITUDE` (`NUMBER(9,6)`, obrigatórios), `RADIUS_M` (inteiro > 0, sem limite de negócio, `NUMBER(10)` no DDL, obrigatório; 13/11: o `CHECK` do banco vai de 1 a 2147483647, o maior valor que o `Integer` do Java lê; número com casas decimais no JSON dá 400, por `spring.jackson.deserialization.accept-float-as-int=false`). O nível de risco atual não fica na área: sai das leituras (RF03/RF04). Rotas em `/api/areas` |
| 11/11 | Regra da área | Nome único por cidade, sem diferenciar maiúsculas (409 se repetir) |
| 12/11 | Nome das constraints e índices | Prefixo do tipo + `RS` + tabela (+ colunas): `PK_RS_AREA`, `FK_RS_SENSOR_AREA`, `CK_RS_AREA_RISK_TYPE`, `UX_RS_AREA_NAME_CITY` (índice único). Nada de nome gerado pelo Oracle |
| 12/11 | Texto no DDL | `VARCHAR2(n CHAR)`: o tamanho conta caracteres, não bytes (acentos ocupam 2 bytes) |
| 12/11 | Nome único da área no banco | Índice único `UX_RS_AREA_NAME_CITY` em `UPPER(NAME), UPPER(CITY)`, além da checagem no BO (protege contra duas requisições ao mesmo tempo; o erro do banco vira 409) |
| 12/11 | 401 e 403 em ProblemDetail | A segurança responde no mesmo formato dos outros erros: 401 "Login necessário" (sem login ou sessão expirada), 403 "Token CSRF inválido" (logado sem token válido) e 403 "Acesso negado". Login recusado: 401 "Login não realizado", com o mesmo texto para usuário inexistente e senha errada (`InvalidCredentialsException`). O 403 de origem não permitida (CORS) continua sendo texto simples do filtro do Spring |
| 12/11 | Inteiros no JSON | `accept-float-as-int=false` vale para todo JSON que o Java lê, inclusive as respostas do serviço Python. Nos DTOs do Python, campo que pode vir com casas decimais (ex.: `12.0`) usa `Double`, não `Integer`. 13/11: `fail-on-numbers-for-enums=true`, então número no lugar do texto de um enum (ex.: `"status": 1`) dá 400, em vez de ser lido pela posição no enum |
| 12/11 | Erros no Swagger | Toda resposta 4xx e 5xx declarada num controller aparece com o formato `ProblemDetail` (`application/problem+json`, com o campo `errors`). As operações protegidas ganham sozinhas as respostas 401 e 403. Tudo aplicado de uma vez pelo `OpenApiConfig` |
| 12/11 | Campo `instance` dos erros | Preenchido por `RequestUris.toUri`, que nunca lança exceção: um caminho com caractere inválido (ex.: `X-Forwarded-Prefix` forjado) é codificado e, se nem assim virar URI, o `instance` fica `/`. Assim o erro mantém o status certo, em vez de virar 500 |
| 12/11 | Geração do id no banco | `NUMBER(19) GENERATED ALWAYS AS IDENTITY` (cabe o `Long` do Java). Só o Oracle numera: o `dml.sql` não informa ids e, nas tabelas filhas, o `INSERT` busca o id do pai por subconsulta. Evita conflito com os ids que a API gera |
| 12/11 | `NOT NULL` com nome | `NN_RS_<TABELA>_<COLUNA>` (ex.: `NN_RS_AREA_NAME`), no mesmo padrão das outras constraints |
| 12/11 | Dados do `dml.sql` | Simulados: nomes de áreas fictícios e coordenadas aproximadas e ilustrativas, 10 áreas em 10 localidades do território (5 da Zona Leste, Guarulhos e 4 cidades do Alto Tietê) e, desde 13/11, 8 sensores com códigos fictícios em 6 dessas áreas (2 deles `INACTIVE`) e, desde 16/11, 12 leituras simuladas (onze de uma tarde de chuva em 15/11, uma antiga de um sensor `INACTIVE`), gravadas em UTC como a API; e 6 abrigos do OSM no Jardim Helena (capacidade e ocupação simuladas, um deles lotado); e 6 pessoas assistidas com 8 responsáveis, todos fictícios. Não são mapeamento oficial de risco; o arquivo diz isso no cabeçalho |
| 12/11 | `dql.sql` | Criado em 13/11, com a segunda tabela (Sensor): as três consultas da rubrica, cada uma com a pergunta de negócio e o resultado esperado para os dados do `dml.sql` (em que área está cada sensor; quantos sensores ativos por área; áreas de alagamento sem sensor) |
| 13/11 | Sensor (RF02) | Classe `Sensor` na tabela `T_RS_SENSOR` (`ID_SENSOR`). Campos: `ID_AREA` (FK `FK_RS_SENSOR_AREA`, obrigatório), `CODE` (`VARCHAR2(30 CHAR)`, a etiqueta do aparelho, obrigatório), `LATITUDE` e `LONGITUDE` (`NUMBER(9,6)`, obrigatórios), `STATUS` (`ACTIVE` ou `INACTIVE`, `VARCHAR2(20 CHAR)`, obrigatório). Sem tipo de sensor: toda leitura traz chuva e nível d'água (RF03). Relacionamento só do lado do sensor (`@ManyToOne` `LAZY`); a área não guarda lista de sensores |
| 13/11 | Regras do sensor | Código único, sem diferenciar maiúsculas (409 se repetir; índice único `UX_RS_SENSOR_CODE` em `UPPER(CODE)`). A área informada precisa existir (404 se não existir). Excluir uma área com sensores é bloqueado com 409, sem cascata |
| 13/11 | Rotas do sensor | `/api/sensors`, com filtro opcional `GET /api/sensors?areaId=1` (área inexistente devolve lista vazia). A resposta traz `areaId` e `areaName` |
| 13/11 | Índice comum | Prefixo `IX_RS_` + tabela + tabela referenciada, como no nome da FK. Toda chave estrangeira ganha um índice (ex.: `IX_RS_SENSOR_AREA` em `T_RS_SENSOR(ID_AREA)`, ao lado de `FK_RS_SENSOR_AREA`), pensando em crescimento: acelera o filtro por área e a checagem ao excluir uma área |
| 13/11 | Teste dos repositories | `@DataJpaTest` com H2 em memória (dependência só de teste, fora do `.jar`): executa as consultas de cada repository, para um nome de método errado aparecer no `./mvnw test` e não só ao subir no Oracle. O H2 não é o Oracle: o DDL real continua sendo conferido pelo `validate` na subida da API |
| 16/11 | Leitura de sensor `INACTIVE` (RF03) | Sensor inativo está desligado: `POST /api/sensor-readings` recusa leitura nova dele (409, regra de negócio). O histórico de leituras que ele já tem continua consultável |
| 16/11 | Excluir sensor | Sensor com leituras não pode ser excluído (409): só dá para inativá-lo (`PUT` com `status` `INACTIVE`). Sensor sem leituras pode ser excluído. No banco, a FK da leitura para o sensor fica sem cascata |
| 16/11 | Leitura do sensor (RF03) | Classe `SensorReading` na tabela `T_RS_SENSOR_READING` (`ID_SENSOR_READING`). Campos: `ID_SENSOR` (FK sem cascata, obrigatório), `RAIN_MM` (`NUMBER(6,2)`, ≥ 0), `WATER_LEVEL_CM` (`NUMBER(7,2)`, ≥ 0), `MEASURED_AT` (`TIMESTAMP WITH TIME ZONE`, a hora da medição), todos obrigatórios. Nomes no JSON seguem o `/risk` do contrato: `rainMm`, `waterLevelCm` |
| 16/11 | Envio da leitura | `POST /api/sensor-readings` (aberto, sem CSRF) com `sensorCode` (a etiqueta do aparelho) e `measuredAt` em ISO 8601 com fuso, enviado pelo sensor. Data no futuro é recusada (400). Código desconhecido: 404. Sensor `INACTIVE`: 409 |
| 16/11 | Leituras são histórico | Só `POST` e `GET`: leitura não se altera nem se exclui |
| 16/11 | Consulta das leituras | `GET /api/sensor-readings?sensorId=3` (obrigatório), com `from` e `to` opcionais (ISO 8601), mais recentes primeiro; sensor inexistente devolve lista vazia. Sem `to`, vai até agora mais a folga de 5 minutos (16/11), para aparecer toda leitura que a API aceitou. Mais `GET /api/sensor-readings/{id}` |
| 16/11 | Nomes da tabela de leituras | Nas constraints e índices de `T_RS_SENSOR_READING`, a tabela é abreviada para `READING`, para todo nome caber em 30 caracteres (limite do Oracle até 12.1): `PK_RS_READING`, `FK_RS_READING_SENSOR`, `NN_RS_READING_<COLUNA>`, `CK_RS_READING_<COLUNA>`, `UX_RS_READING_SENSOR_TIME` |
| 16/11 | Leitura repetida | O mesmo sensor não pode ter duas leituras no mesmo instante: 409. O BO confere antes de gravar, e o índice único `UX_RS_READING_SENSOR_TIME` em `(ID_SENSOR, SYS_EXTRACT_UTC(MEASURED_AT))` garante no banco (o Oracle não aceita constraint `UNIQUE` em `TIMESTAMP WITH TIME ZONE`, ORA-02329, por isso a regra é um índice único sobre o instante em UTC). Esse índice começa pelo `ID_SENSOR`, então também cumpre a regra de toda FK ter índice e serve à busca das leituras de cada sensor: não há outro índice na tabela |
| 16/11 | Hora da leitura | O sensor envia com o fuso dele (ex.: `-03:00`); a API converte para UTC antes de gravar e responde em UTC (`...Z`). O instante é o mesmo; o front mostra no horário de Brasília. A hora é cortada em microssegundos (o que o banco guarda) e os valores ficam com 2 casas decimais |
| 16/11 | Relógio da aplicação | Bean `Clock` (UTC) em `config/ClockConfig`: o código pede o `Clock` em vez de ler a hora do sistema direto, e os testes usam um relógio fixo |
| 16/11 | Folga para relógio adiantado | Leitura com data até 5 minutos no futuro é aceita (relógio do aparelho um pouco adiantado); além disso, 400. A folga vale para toda validação de data da API e usa o bean `Clock` (`config/ValidationConfig`). Atenção: num `@Future` ela inverte, e aceita até 5 minutos no passado |
| 16/11 | Zeros à direita nos números da leitura | `rainMm` e `waterLevelCm` aceitam zeros à direita (`12.400`, `2.300000`): eles são retirados antes da validação (`util/StripTrailingZerosDeserializer`). Casa decimal de verdade além da segunda (`12.345`) continua dando 400 |
| 16/11 | Nível de risco da área (RF04) | Calculado na hora, não gravado. Para cada sensor `ACTIVE` da área vale a leitura mais recente da última 1 hora (até agora mais a folga de 5 min); a área fica com a **pior** delas (maior nível d'água). Desempate (16/11): com o mesmo nível d'água, vale a leitura mais recente; se ainda empatar, a do sensor de código maior na ordem alfabética (só muda qual sensor e qual hora aparecem; o nível é o mesmo). Sem nenhuma leitura recente: `riskLevel` vazio e `hasData: false` (o mapa mostra "sem dados"). Códigos: `LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`, `VERY_URGENT`, nas faixas de 09/11 com as bordas de 10/11 (2 casas: `LOW` < 1,10; `MEDIUM` < 3,00; `HIGH` < 5,10; `VERY_HIGH` até 10,00; `VERY_URGENT` acima de 10,00) |
| 16/11 | Rotas do nível de risco | `GET /api/areas/risk` (todas as áreas, para o mapa do RF14) e `GET /api/areas/{id}/risk`, abertas. Respondem nível, nível d'água, sensor e hora da leitura que definiu o resultado. As rotas de `/api/areas` que já existiam não mudam |
| 16/11 | Dados da demonstração | Coleção do Postman em `domain-driven-design-java/docs/rota-segura-demo.postman_collection.json`: envia as leituras com a hora do momento (a janela do RF04 é de 1 hora, e as leituras do `dml.sql` são de 15/11), mostra o 409 do sensor inativo e confere o mapa de risco. Roda contra qualquer endereço (variável `baseUrl`) |
| 16/11 | Abrigo (RF06) | Classe `Shelter` na tabela `T_RS_SHELTER` (`ID_SHELTER`). Campos: `NAME` (`VARCHAR2(150 CHAR)`), `ADDRESS` (`VARCHAR2(200 CHAR)`, obrigatório, como pede o RF06), `LATITUDE` e `LONGITUDE` (`NUMBER(9,6)`), `CAPACITY` (inteiro > 0) e `OCCUPIED_SPOTS` (inteiro de 0 até a capacidade, 409 se passar), todos obrigatórios. A resposta traz `availableSpots` = capacidade - ocupadas (o que o `/route` do Python recebe). O painel atualiza a ocupação pelo `PUT`. O abrigo não fica ligado a uma área: quem escolhe o mais próximo com vaga é a rota (RN06). Rotas em `/api/shelters` |
| 16/11 | Abrigos do piloto | Base: `dynamic-programming/docs/abrigos_sugeridos.json` (OpenStreetMap, ODbL; capacidade simulada). Entram no `dml.sql` os 6 que têm endereço; os 4 sem endereço entram quando o Kelwin completar os endereços |
| 16/11 | Prioridade até a entrega | Pela nota: RF06 e RF08 (completam a camada model), depois diagrama de classes, DER e tabela de endpoints, documentos de Agile, protótipos de 3 telas (Figma) e README com a Ficha. RF05, RF09, RF12, RF07 e o front em React só se sobrar tempo. Uma rodada de revisão por requisito |
| 16/11 | Pool de conexões | A conta do Oracle da FIAP aceita uma única sessão (`SESSIONS_PER_USER` = 1; o deploy do RF04 falhou com ORA-02391). Hikari com no máximo 1 conexão; nenhuma parada aberta (`minimum-idle=0`: fechada entre 10 e 40 s depois do último uso); espera máxima de 10 s por uma conexão; na subida, novas tentativas por até 60 s (`initialization-fail-timeout`). Consequências: a API do Render, a API local e o SQL Developer não podem usar o banco ao mesmo tempo; e a chamada ao serviço Python (RF07) não pode ficar dentro de um `@Transactional`, para não segurar a única conexão por até 10 s |
| 16/11 | Pessoa assistida (RF08) | Classe `AssistedPerson` na tabela `T_RS_ASSISTED_PERSON` (`ID_ASSISTED_PERSON`). Campos: `NAME` (150), `PHONE` (20, para o SMS simulado do RF12), `ID_AREA` (FK, obrigatória pela RN03), `LATITUDE` e `LONGITUDE` (onde a pessoa mora: origem da rota do RF07), `SPECIAL_NEED` (`ELDERLY`, `BEDRIDDEN`, `WHEELCHAIR` ou `OTHER`), todos obrigatórios, e `NOTES` (500, opcional) |
| 16/11 | Responsável (RF08) | Classe `Guardian` na tabela `T_RS_GUARDIAN` (`ID_GUARDIAN`): `NAME` (150), `PHONE` (20), `RELATIONSHIP` (50, ex.: filha, vizinho), todos obrigatórios. Cada responsável pertence a um assistido (1:N, `ID_ASSISTED_PERSON`). Assistido e responsáveis são cadastrados juntos: o JSON do assistido traz a lista `guardians`, e lista vazia é recusada (RN03). O `PUT` troca a lista inteira. Excluir o assistido exclui os responsáveis dele (cascata no JPA e `ON DELETE CASCADE` no banco) |
| 16/11 | Privacidade do assistido | Exceção à regra de 09/11 (GET aberto): **todo** acesso a `/api/assisted-people`, inclusive `GET`, exige login, porque mostra nome, telefone e endereço de pessoas vulneráveis (LGPD), mesmo com dados simulados |
| 16/11 | Rotas do assistido | `/api/assisted-people`, com filtro opcional `GET /api/assisted-people?areaId=1` (o alerta do RF05 vai precisar dos assistidos de uma área) |
| 16/11 | Área com assistidos | Excluir uma área que tem assistidos é bloqueado (409), como com sensores: pela RN03 todo assistido tem área |
| 16/11 | Nomes das tabelas do RF08 | Nas constraints e índices, `T_RS_ASSISTED_PERSON` aparece como `ASSISTED` (para caber em 30 caracteres, como `READING`): `PK_RS_ASSISTED`, `FK_RS_ASSISTED_AREA`, `IX_RS_ASSISTED_AREA`, `FK_RS_GUARDIAN_ASSISTED`, `IX_RS_GUARDIAN_ASSISTED` e `NN_RS_GUARDIAN_ASSISTED` (NOT NULL da coluna `ID_ASSISTED_PERSON`) |
| 16/11 | Dados do assistido no `dml.sql` | Todos fictícios: nomes inventados e telefones com DDD `00`, que não existe, para nenhum número ser de uma pessoa real |
| 17/11 | Quem recebe o SMS no MVP | As pessoas assistidas e os responsáveis cadastrados (RF08), os únicos com telefone no sistema. O "morador" dos requisitos (RF05, RF12, RN07), no MVP, é a pessoa assistida. O cadastro de qualquer morador da área para receber alertas fica para uma próxima versão (sem RF no backlog) |
| 18/11 | Aviso antecipado (RF05) | Confirmado pelo João Paulo: entra como critério de aceite do RF05, **complementar**. O gatilho principal do alerta é o nível d'água em cm; o `highProbability` do `/risk` só antecipa um SMS de preparação. Limite 0,572 (validação temporal em 2019–2023). No teste em 2024–2025: avisou 75,7% dos dias de alagamento (28 de 37), com precisão de 40%; a regra "chuva ≥ 23 mm" avisa 56,8%. Detalhes na linha "Probabilidade alta", que o João Paulo atualiza |
| 18/11 | Guarulhos | Sem base de alagamentos nem estação de chuva: em Guarulhos o MVP usa só os sensores (nível d'água em cm), sem a probabilidade do modelo. Limitação a citar nos PDFs (João Paulo) |
| 18/11 | Deploy do serviço Python | O Kelwin e o Claude sobem juntos no Render, usando o `Dockerfile` que já está em `dynamic-programming/`. O Claude não edita arquivos dessa pasta: se algo precisar mudar lá, o pedido vai para o João Paulo |
| 11/11 | `.gitignore` na raiz | Só `.idea/` e `*.iml` (arquivos do IntelliJ). Cada pasta mantém o próprio `.gitignore` |
| 11/11 | Contrato do serviço Python | v2 em `dynamic-programming/docs/contrato.md`: `GET /health`, `POST /route`, `POST /risk`, camelCase, seguindo as decisões de 10/11 (áreas alagadas com `areaId`, `lat`, `lng`, `radiusM`; abrigos enviados pelo Java; 70 m/min). Sem rota segura = 200 com `status: NO_SAFE_ROUTE`. Desempate: custo, distância, ID do abrigo. Origem a mais de 300 m da rua mais próxima = 400 (João Paulo; validar com o Kelwin) |
| 11/11 | `/risk` | Recebe as leituras da área dos últimos 8 dias e devolve `floodProbability`, `highProbability` e `lowData`. O nível em cm continua no Java (João Paulo; validar com o Kelwin) |
| 11/11 | Deploy do serviço Python | Serviço separado no Render, Dockerfile em `dynamic-programming/`. Variáveis `PORT`, `GRAFO_PATH`, `MODELO_PATH`. URL em desenvolvimento: `http://localhost:8000` (João Paulo; validar com o Kelwin) |
| 11/11 | Probabilidade alta | Regra: limite de maior precisão entre os que avisam pelo menos 70% dos dias de alagamento na validação (2023). O número sai do treino até 17/11 (João Paulo; validar com o Kelwin) |
| 11/11 | Alerta antecipado | Quando `highProbability` for `true`, SMS: "Há alta chance de alagamento na sua área hoje. Prepare-se para ir ao abrigo." (João Paulo; validar com o Kelwin) |
| 11/11 | Base do Data Science | Chuva do Cemaden (PED), estação Itaim Paulista (355030881A), 2016–2025. Rótulo: alagamentos do CGE (Extremo Leste e Zona Leste), 2016–2025. Alto Tietê: modelo aplicado com a chuva de Mogi das Cruzes e Itaquaquecetuba e validado contra as ocorrências do Reindesc (Cemaden). Substitui o INMET de 09/11 (João Paulo; validar com o Kelwin) |
| 11/11 | Modelos de Data Science | Regressão logística = alagou no dia (é o `/risk`); regressão linear = número de pontos de alagamento no dia. Divisão por tempo: treino 2016–2022, validação 2023, teste 2024–2025 (João Paulo; validar com o Kelwin) |

### Estrutura do monorepo

```
/
├── CLAUDE.md
├── README.md                          ← Ficha da CitizenTech no topo
├── prompts.md                         ← gravado pelo hook
├── docs/decisions.md
├── agile-methodology/                 ← Kelwin
├── database-design/                   ← Kelwin (ddl.sql, dml.sql, dql.sql, modelo)
├── domain-driven-design-java/         ← Kelwin
│   ├── backend/                       ← API Spring Boot
│   ├── frontend/                      ← React (os três)
│   └── docs/                          ← material do PDF de DDD
├── data-science/                      ← João Paulo
├── dynamic-programming/               ← João Paulo (inclui o serviço Python)
├── ar-vr-modelling/                   ← Vitor
└── network-architect-solutions/       ← Vitor
```

### Papel de cada pacote Java (`br.com.rotasegura`)

| Pacote | O que fica |
|---|---|
| `controller` | `@RestController`; recebe DTOs, chama o BO, devolve DTOs |
| `bo` | `@Service` com as regras de negócio (RN01 a RN08); chama o DAO |
| `dao` | Classes `@Component` de acesso a dados; usam o repository; sem regra de negócio |
| `repository` | Interfaces `JpaRepository` |
| `model` | Entidades JPA |
| `dto` | Records de request e response com Bean Validation |
| `exception` | Exceções de domínio e o `@RestControllerAdvice` |
| `security` | Configuração do Spring Security (login simples) |
| `config` | Configurações gerais (CORS, springdoc, relógio da aplicação (`Clock`), validação, cliente do serviço Python) |
| `util` | Funções auxiliares sem estado |

Fluxo: `controller → bo → dao → repository`. Nenhuma camada pula a seguinte.

## Em aberto (perguntar antes de decidir)

- [ ] **Front: pacote do Google Maps** (ex.: `@vis.gl/react-google-maps`) e conta/chave da API. Urgente: o RF14 virou Must e depende disso.
- [ ] **Número da equipe** para a faixa 192.168.X.0/24 de Network. Kelwin informa em 10/11.
- [ ] **Endereço de 4 abrigos do piloto:** Colégio Cavlac, "Escola sem nome (OSM way/622202838)", EMEF José Bento de Assis e EE Maria de Loudes Vieira não têm endereço no OSM. Kelwin completa; depois eles entram no `dml.sql`.
- [ ] **Cenário da demonstração:** perto da várzea o mapa é esparso e uma área alagada de 150 m pode isolar a origem (NO_SAFE_ROUTE). Definir juntos uma origem e uma área alagada que mostrem a rota desviando, e outra que mostre o NO_SAFE_ROUTE. (João Paulo, 12/11)
