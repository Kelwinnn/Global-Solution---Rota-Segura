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
| 09/11 | Mecanismo do login | Tela de login no React; sessão com cookie no servidor (Spring Security). Como front e API ficam em domínios diferentes, o cookie usa `SameSite=None` e `Secure`, e o CORS libera credenciais só para as origens do front |
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

- [ ] **Versão exata do Spring Boot 3.x.**
- [ ] **Nomes das colunas:** padrão do ID e das FKs (ex.: `ID` ou `ID_SHELTER`).
- [ ] **Contrato do serviço Python:** endpoints, formato de request e response, URL base. O João Paulo vai enviar.
- [ ] **Como o Java chama o Python:** cliente HTTP, timeout e o que fazer se o serviço estiver fora.
- [ ] **Deploy do serviço Python** e variáveis de ambiente de cada serviço.
- [ ] **Front:** roteador, cliente HTTP (fetch ou axios), biblioteca de mapa (se houver RF14) e estrutura de pastas.
- [ ] **CORS:** origens aceitas pela API (local e deploy do front).
- [ ] **Deploy do front:** onde vai ficar hospedado.
- [ ] **Limites de risco (RN01):** valores de nível d'água e de probabilidade que definem risco alto.
- [ ] **Número da equipe** para a faixa 192.168.X.0/24 de Network (aguardando a FIAP).
