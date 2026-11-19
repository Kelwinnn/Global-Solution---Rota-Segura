# Rota Segura — Domain Driven Design Java

API da **Rota Segura** (Global Solution FIAP 2026.2, CitizenTech): sistema de alerta de alagamento que transforma o alerta de chuva em ação. Os sensores medem chuva e nível d'água, a API calcula o risco de cada área, guarda os abrigos e as pessoas que não conseguem sair sozinhas, e pede ao serviço Python da equipe a rota segura até o abrigo com vaga.

## Integrantes

| Nome | RM |
|---|---|
| João Paulo Santana Basta | 565383 |
| Kelwin Silva Bastos | 566348 |
| Vitor Mantovani Camilo | 556537 |

## Links

- **API publicada (Render):** https://global-solution-rota-segura.onrender.com
- **Swagger UI:** https://global-solution-rota-segura.onrender.com/swagger-ui.html
- **Serviço Python de rota e risco (Render):** https://rota-segura-python.onrender.com
- **Repositório completo do projeto** (todas as disciplinas): https://github.com/Kelwinnn/Global-Solution---Rota-Segura

O deploy no Render é feito a partir do repositório completo, onde esta pasta fica em `domain-driven-design-java/`. O código é o mesmo deste repositório.

## O que tem aqui

| Pasta | Conteúdo |
|---|---|
| `backend/` | API em Java 21 e Spring Boot 3.4.1: camadas `controller → bo → dao → repository`, Oracle (JPA), Spring Security, Swagger, 293 testes, e o `Dockerfile` do Render |
| `docs/Rota-Segura-DDD-Java.pdf` | **Documento da entrega:** especificação, diagrama de classes, protótipos, tabela de endpoints, DER e instruções de execução |
| `docs/` | Os textos e as imagens que formam o PDF, as coleções do Postman (`rota-segura-demo` e `rota-segura-testes`) e o roteiro de execução (`como-executar.md`) |
| `docs/sql/` | Scripts Oracle para criar as tabelas e os dados de exemplo (`ddl.sql`, `dml.sql`, `dql.sql`). São cópias dos scripts de Database Design do repositório completo |

## Requisitos implementados

| RF | O que faz | Rotas |
|---|---|---|
| RF01 | Áreas de risco | `/api/areas` |
| RF02 | Sensores vinculados às áreas | `/api/sensors` |
| RF03 | Leituras dos sensores (sem login) | `/api/sensor-readings` |
| RF04 | Nível de risco atual de cada área | `/api/areas/risk` |
| RF06 | Abrigos com vagas livres | `/api/shelters` |
| RF07 | Rota segura até o abrigo, pelo serviço Python | `/api/assisted-people/{id}/route` |
| RF08 | Pessoas assistidas e responsáveis (com login) | `/api/assisted-people` |

## Como executar

Passo a passo completo em [`docs/como-executar.md`](docs/como-executar.md). Resumo:

1. Rodar `docs/sql/ddl.sql` e `docs/sql/dml.sql` no Oracle (SQL Developer, F5).
2. Em `backend/`: criar o `.env` a partir do `.env.example`, carregar as variáveis e rodar `./mvnw spring-boot:run`.
3. Abrir `http://localhost:8080/swagger-ui.html`.

Neste repositório, a pasta da API está na raiz: onde o `como-executar.md` diz `domain-driven-design-java/`, leia a raiz deste repositório; onde diz `database-design/`, use `docs/sql/`.

Testes automatizados (não precisam do Oracle): `cd backend && ./mvnw test`.

## Dados

Nenhum dado pessoal real: pessoas, telefones (DDD 00), áreas, sensores e leituras são simulados. Os abrigos vêm do OpenStreetMap (© colaboradores do OpenStreetMap, licença ODbL), com capacidade e ocupação simuladas.
