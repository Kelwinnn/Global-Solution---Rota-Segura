# Rota Segura — Global Solution FIAP 2026.2 (CitizenTech)

Você é o par de programação do Kelwin neste monorepo. Leia este arquivo inteiro antes de qualquer tarefa.

## O projeto

- **Ideia:** a Rota Segura transforma o alerta de chuva em ação. Sensores de rua medem chuva e nível d'água, um modelo estima o risco, o morador recebe um alerta com a rota até o abrigo e responde "Estou seguro". Quem precisa de ajuda e não responde aparece num painel de prioridade de resgate.
- **Cidadão:** morador de área de risco de alagamento ou deslizamento, com foco em quem não consegue sair sozinho (idosos, acamados, pessoas com deficiência).
- **Território do MVP:** todo o Alto Tietê, mais Guarulhos, mais 5 distritos da Zona Leste de São Paulo: Jardim Pantanal, São Miguel Paulista, Itaim Paulista, Jardim Helena e Itaim Biacica.
- **Prazo:** 19/11/2026 às 23h55. **Commits no GitHub depois do prazo zeram a parte técnica de DDD Java.**

## Equipe e escopo deste Claude Code

| Integrante | Partes |
|---|---|
| Kelwin Silva Bastos (RM 566348) | Java (API), Database, Agile |
| João Paulo Santana Basta (RM 565383) | Data Science, Dynamic Programming, serviço Python |
| Vitor Mantovani Camilo (RM 556537) | AR/VR, Network |
| Os três | Front completo |

Só o Kelwin usa o Claude Code. Você trabalha **apenas** em:
- `domain-driven-design-java/` (API em `backend/`, front em `frontend/`, material do PDF em `docs/`)
- `database-design/`
- `agile-methodology/`
- `docs/` e `README.md` na raiz

Nunca edite `data-science/`, `dynamic-programming/` (inclui o serviço Python), `ar-vr-modelling/` nem `network-architect-solutions/`. Se uma tarefa exigir mudar algo lá, pare e diga ao Kelwin o que precisaria mudar e para quem pedir. A estrutura completa está em `docs/decisions.md`.

## Regra número 1: não decida arquitetura sozinho

`docs/decisions.md` lista o que já foi decidido e o que está em aberto.

- Siga à risca o que está em **Decidido**.
- Antes de tocar em qualquer item de **Em aberto**, ou em qualquer decisão que não esteja no arquivo (nova dependência, nome de pacote, estrutura de pastas, formato de contrato, padrão de tabela, biblioteca), **pare e pergunte ao Kelwin**. Apresente 2 ou 3 opções com prós e contras curtos e espere a resposta.
- Depois que ele decidir, atualize `docs/decisions.md` (mova o item para Decidido, com a data).
- Não invente fatos: requisitos, regras de negócio, números, endpoints do serviço Python ou critérios de avaliação que não estejam neste repositório. Se faltar informação, pergunte.

## Modo de trabalho: "gera e explica"

1. Antes de escrever código para uma tarefa média ou grande, mostre um plano curto (arquivos que vai criar ou alterar) e espere o "ok".
2. Escreva o código.
3. Explique **linha a linha** o que foi feito, em português, agrupando por bloco. Destaque anotações do Spring, decisões de design e qualquer coisa nova para ele.
4. Quando usar algo que ele provavelmente não conhece (anotação, API, padrão novo), pergunte primeiro "o que você acha que isso faz?" e só depois explique.
5. Rode os testes e o build quando fizer sentido e mostre o resultado.
6. **Nunca faça `git commit` nem `git push`.** O Kelwin revisa e commita. No fim, sugira a mensagem de commit.

## Stack decidida (resumo; detalhes em `docs/decisions.md`)

- **Backend:** Java 21, Maven, Spring Boot 3.x, Spring Data JPA, banco Oracle da FIAP, springdoc (Swagger UI), deploy no Render via Dockerfile.
- **Pacote base:** `br.com.rotasegura`, com os pacotes `config`, `controller`, `bo`, `dao`, `repository`, `model`, `dto`, `exception`, `security` e `util`.
- **Fluxo das camadas:** `controller → bo → dao → repository`. Nenhuma camada pula a seguinte.
  - `controller`: recebe e devolve só DTOs; entidades nunca saem da API.
  - `bo` (`@Service`): toda regra de negócio fica aqui, nunca no controller nem no DAO.
  - `dao` (`@Component`): acesso a dados, usando o repository; sem regra de negócio.
  - `repository`: interfaces `JpaRepository`.
  - `exception`: exceções de domínio e o `@RestControllerAdvice`.
- **Tabelas:** prefixo `T_RS_` (ex.: `T_RS_SHELTER`) e chave primária com `IDENTITY` (`@GeneratedValue(strategy = GenerationType.IDENTITY)`).
- **Segurança:** login simples com Spring Security; usuário fixo com credenciais em variáveis de ambiente. POST, PUT e DELETE exigem login; GET e Swagger ficam abertos. Todo endpoint em que o cidadão responde durante a emergência fica sem login: hoje, o check-in "Estou seguro" (RF09) e as leituras do sensor (RF03). O login usa sessão com cookie: tela de login no React, cookie com `SameSite=None` e `Secure`, e CORS com credenciais só para as origens do front. CSRF ligado: o front lê o token e envia em cada POST, PUT e DELETE; endpoints liberados ficam fora do CSRF. Sessão de 30 minutos; o logout encerra só a sessão na página, e alertas e mensagens continuam sendo enviados.
- **Testes:** JUnit 5 + Mockito nos BOs; MockMvc nos controllers.
- **Frontend:** React + Vite + TypeScript + Tailwind.
- **Serviço Python:** FastAPI, construído pelo João Paulo. Ele faz a rota segura e o modelo de risco; a API Java chama esse serviço. O contrato ainda está em aberto: não invente endpoints dele.
- **Nomes no código:** em inglês (classes, métodos, tabelas, colunas). Documentação e explicações em português.

## Requisitos do MVP (os 10 "Must")

| ID | Requisito |
|---|---|
| RF01 | CRUD de áreas de risco |
| RF02 | Cadastrar sensores e vincular cada um a uma área |
| RF03 | Registrar leituras do sensor (chuva em mm, nível d'água em cm, data e hora) |
| RF04 | Calcular o nível de risco da área (baixo, médio, alto) |
| RF05 | Gerar alerta para a área quando o risco for alto |
| RF06 | Cadastrar abrigos com endereço e capacidade |
| RF07 | Calcular a rota mais segura até o abrigo (via serviço Python) |
| RF08 | Cadastrar pessoa assistida com seu responsável |
| RF09 | Registrar a resposta de cada assistido a um alerta: "Estou seguro", "Estou a caminho de um local seguro" ou "Ainda não saí" |
| RF12 | Enviar o alerta e as perguntas ao cidadão por SMS simulado (o sistema registra e o front mostra como se fosse o celular) |

Should (só se sobrar tempo): RF10 painel de prioridade de resgate, RF11 avisar o responsável, RF13 avisar voluntários e pessoas que podem ajudar (RN08). Could: RF14 mapa interativo. Won't: RF15 integração real com Defesa Civil.

## Regras de negócio

- **RN01:** risco alto quando o nível d'água passa do limite cadastrado do sensor ou quando o modelo indica alta probabilidade de alagamento. Os limites exatos a equipe define.
- **RN02:** só um alerta ativo por área; ele encerra quando o risco volta a baixo.
- **RN03:** todo assistido tem ao menos um responsável e uma área vinculada.
- **RN04:** assistido sem resposta em até 15 min após o alerta entra no painel como prioridade de resgate.
- **RN05:** a rota nunca passa por rua alagada; sem rota segura, orienta ficar em local alto e aciona o painel.
- **RN06:** o abrigo indicado é o mais próximo pela rota que ainda tenha vaga.
- **RN07:** quem responde "Estou a caminho de um local seguro" recebe uma nova pergunta 15 min depois.
- **RN08:** as respostas "Estou a caminho", "Ainda não saí" e a falta de resposta em 15 min acionam mensagem para voluntários e pessoas que podem ajudar (parte do RF13, Should).

## Regras que valem sempre

- **Dados:** nada de dados pessoais reais (exigência do briefing). Dados de exemplo são simulados; dados públicos têm a fonte citada.
- **Segredos:** usuário, senha e URL do Oracle e as credenciais do login ficam em variáveis de ambiente. Nunca escreva credencial em código, `application.properties` versionado, README ou teste.
- **Acessibilidade no front** (exigência do briefing): letra legível, bom contraste, linguagem simples, funciona em tela de celular.
- **prompts.md:** o briefing exige registrar os prompts de IA. Um hook já grava cada prompt automaticamente; não apague nem reescreva esse arquivo.
- **Coerência entre entregas:** o modelo JPA, os scripts SQL em `database-design/`, o diagrama de classes e o DER precisam bater. Ao mudar uma entidade, avise o que mais precisa ser atualizado.

## Skills e agentes deste repositório

- Skills (`/nome`): `crud-entity`, `sql-scripts`, `endpoints-table`, `diagrams`, `react-screen`, `agile-docs`, `render-deploy`, `pre-delivery`.
- Agentes: `rubric-checker`, `java-reviewer`, `sql-reviewer`, `ui-reviewer`. Use-os para revisar antes de o Kelwin commitar.
