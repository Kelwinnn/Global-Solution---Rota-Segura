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
| 09/11 | CSRF | Token mantido. O front lê o token e envia em cada POST, PUT e DELETE. Endpoints liberados (sem login) ficam fora do CSRF, porque não têm sessão |
| 09/11 | Sessão | Expira em 30 minutos. O logout encerra só a sessão na página; alertas e mensagens continuam sendo enviados normalmente |
| 09/11 | Canal com o cidadão | SMS simulado: o sistema registra a mensagem e o front mostra como se fosse o celular |
| 09/11 | Respostas do check-in | Três opções: "Estou seguro", "Estou a caminho de um local seguro" e "Ainda não saí" |
| 09/11 | Endpoints liberados | Todo endpoint em que o cidadão precisa responder durante a emergência fica sem login. Liberados: check-in "Estou seguro" (RF09) e envio de leituras do sensor (RF03) |
| 09/11 | Documentação da API | springdoc (Swagger UI), aberto sem login |
| 09/11 | Testes | JUnit 5 + Mockito nos BOs; MockMvc nos controllers |
| 09/11 | Deploy da API | Render, via Dockerfile |
| 09/11 | Frontend | React + Vite + TypeScript + Tailwind, feito pelos três |
| 09/11 | Rota e risco | Serviço Python em FastAPI, chamado pela API Java |
| 09/11 | Idioma do código | Inglês (classes, métodos, tabelas, colunas) |
| 09/11 | Modo de trabalho | Claude gera e explica linha a linha; o Kelwin revisa e commita |
| 09/11 | Base do Data Science | INMET |
| 09/11 | Versão do Spring Boot | 3.4.1 |
| 09/11 | Nome das colunas | Chave primária no padrão `ID_<ENTIDADE>` (ex.: `ID_SHELTER` na tabela `T_RS_SHELTER`) |
| 09/11 | Front: chamadas à API | `fetch` nativo (sem axios) |
| 09/11 | Front: mapa | Google Maps (a chave da API fica em variável de ambiente do Vite, nunca no código) |
| 09/11 | Front: pastas | Estrutura por componentes |
| 09/11 | Deploy do front | Junto do Java: a API Spring serve o front já compilado (mesmo serviço no Render) |
| 09/11 | Chamada Java → Python | `RestClient` (Spring). Timeout e comportamento se o serviço cair seguem em aberto |
| 09/11 | Front: roteador | React Router |
| 09/11 | CORS, cookie e CSRF | Mantidos como decidido (cookie `SameSite=None` + `Secure`, CORS com credenciais, CSRF ligado), mesmo com o front junto do Java. Origem permitida no desenvolvimento: `http://localhost:5173` (Vite); API em `http://localhost:8080`. Origens ficam em variável de ambiente |
| 09/11 | Nome das FKs | Repetem o nome da PK referenciada (ex.: `ID_AREA` em `T_RS_SENSOR`) |
| 09/11 | Níveis de risco (RF04/RN01) | 5 níveis pelo nível d'água em cm, iguais para todos os sensores: baixo 0 a 1; médio 1,1 a 2,9; alto 3 a 5; muito alto 5,1 a 10; muito urgente acima de 10. Os níveis dizem o quão urgente é ir para um abrigo ou procurar um local mais alto |
| 09/11 | Quando gera alerta (RF05/RN07) | A partir do nível médio. Enquanto o alerta durar, o morador recebe a atualização do nível a cada 15 min; a pergunta do check-in se repete só para quem ainda não está seguro. Quem respondeu "Estou seguro" recebe só as atualizações |
| 09/11 | Probabilidade de alagamento | O serviço Python informa a alta probabilidade de alagamento antes de acontecer (alerta antecipado). É um sinal separado dos níveis em cm, que medem a urgência |
| 10/11 | Variáveis do Oracle | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, lidas no `application.properties` com `${...}`. Porta da API em `PORT` (padrão 8080) |
| 10/11 | Criação das tabelas | `spring.jpa.hibernate.ddl-auto=validate`: as tabelas vêm do `ddl.sql` de `database-design/` e o Hibernate só confere se as entidades batem com elas |
| 10/11 | Build do Docker | `Dockerfile` em `domain-driven-design-java/backend/`, com contexto de build em `domain-driven-design-java/` (raiz do serviço no Render), para depois entrar um estágio Node que compila o front |
| 10/11 | Coordenadas Maven | `groupId` `br.com.rotasegura`, `artifactId` `rota-segura-api`; Maven Wrapper (`./mvnw`) no projeto; springdoc 2.7.0 |

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
| `bo` | `@Service` com as regras de negócio (RN01 a RN06); chama o DAO |
| `dao` | Classes `@Component` de acesso a dados; usam o repository; sem regra de negócio |
| `repository` | Interfaces `JpaRepository` |
| `model` | Entidades JPA |
| `dto` | Records de request e response com Bean Validation |
| `exception` | Exceções de domínio e o `@RestControllerAdvice` |
| `security` | Configuração do Spring Security (login simples) |
| `config` | Configurações gerais (CORS, springdoc, cliente do serviço Python) |
| `util` | Funções auxiliares sem estado |

Fluxo: `controller → bo → dao → repository`. Nenhuma camada pula a seguinte.

## Em aberto (perguntar antes de decidir)

- [ ] **Padrão das demais colunas** (além de PK e FK): nomes em inglês `SNAKE_CASE` em maiúsculas? Confirmar na primeira entidade.
- [ ] **Contrato do serviço Python:** endpoints, formato de request e response, URL base. Rascunho de proposta no documento "Serviço Python: decisões para o João Paulo"; aguardando as respostas dele.
- [ ] **Chamada ao Python:** timeout e o que fazer se o serviço estiver fora (sugestão: o Java segue só com os limites em cm e avisa que a rota está indisponível).
- [ ] **Deploy do serviço Python** e variáveis de ambiente de cada serviço. Aguardando o João Paulo.
- [ ] **Front: pacote do Google Maps** (ex.: `@vis.gl/react-google-maps`) e conta/chave da API. RF14 passou a ser a tela inicial: confirmar se sobe para Must.
- [ ] **Probabilidade de alagamento:** a partir de qual valor é "alta", e o que o alerta antecipado diz ao morador. Depende do João Paulo.
- [ ] **Onde a entidade vira DTO:** com `open-in-view=false`, relacionamentos `LAZY` só carregam dentro da transação. Decidir na primeira entidade com relacionamento (Sensor → Área) se a conversão fica no BO (`@Transactional`) ou se o DAO já busca o relacionamento junto (ex.: `JOIN FETCH`).
- [ ] **Número da equipe** para a faixa 192.168.X.0/24 de Network. Kelwin informa em 10/11.
