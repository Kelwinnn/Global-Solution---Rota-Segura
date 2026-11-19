# Rota Segura

## Ficha da CitizenTech

| Campo | Resposta |
|---|---|
| **Nome da CitizenTech** | Rota Segura |
| **Cidade e bairro** | São Paulo (SP), com o Jardim Helena como bairro-piloto. O território do MVP inclui também Jardim Pantanal, São Miguel Paulista, Itaim Paulista e Itaim Biacica (Zona Leste), Guarulhos e o Alto Tietê |
| **Cidadão beneficiado** | Morador de área de risco de alagamento ou deslizamento, com foco em quem não consegue sair sozinho: idosos, acamados e pessoas com deficiência |
| **Problema, em uma frase** | O alerta de chuva chega, mas não diz para onde ir, e ninguém fica sabendo quem ficou para trás |
| **Como funciona** | Sensores de rua medem a chuva e o nível d'água, e o sistema calcula o risco de cada área. A partir do risco médio, o morador recebe por SMS um alerta com a rota segura até o abrigo com vaga mais próximo e responde "Estou seguro", "Estou a caminho de um local seguro" ou "Ainda não saí". Quem precisa de ajuda e não responde em 15 minutos aparece num painel de prioridade de resgate |
| **O que cada parte faz** | Ver a tabela [Partes do projeto](#partes-do-projeto) |
| **Link do repositório** | https://github.com/Kelwinnn/Global-Solution---Rota-Segura |
| **Indicador de impacto** | Percentual de pessoas assistidas que confirmam "Estou seguro" em até 30 minutos depois do alerta. **Meta do piloto: 90%** (meta definida pela equipe; o sistema registra cada resposta e a hora em que ela chegou) |
| **ODS conectados** | **ODS 11** (Cidades e comunidades sustentáveis), meta 11.5: reduzir as mortes e as pessoas afetadas por desastres, inclusive os relacionados à água, protegendo as pessoas em situação de vulnerabilidade. **ODS 13** (Ação contra a mudança global do clima), meta 13.1: reforçar a resiliência a riscos relacionados ao clima. **ODS 10** (Redução das desigualdades): o foco em idosos e pessoas com deficiência |

## Equipe

| Integrante | RM | Partes |
|---|---|---|
| João Paulo Santana Basta | 565383 | Data Science, Dynamic Programming, serviço Python |
| Kelwin Silva Bastos | 566348 | Domain Driven Design Java (API), Database Design, Agile Methodology |
| Vitor Mantovani Camilo | 556537 | AR/VR, Network |

O front é feito pelos três.

## Partes do projeto

| Pasta | Disciplina | O que faz |
|---|---|---|
| [`domain-driven-design-java/`](domain-driven-design-java/) | Domain Driven Design Java | API em Spring Boot (áreas de risco, sensores, leituras, nível de risco, abrigos, pessoas assistidas e a rota segura até o abrigo pelo serviço Python), publicada no Render. O front em React fica em `frontend/` |
| [`database-design/`](database-design/) | Database Design | Modelo de dados e scripts Oracle: `ddl.sql`, `dml.sql` e `dql.sql` |
| [`agile-methodology/`](agile-methodology/) | Agile Methodology | Ideia, personas, casos de uso, backlog e protótipo |
| [`data-science/`](data-science/) | Data Science | Modelo que estima a chance de alagamento a partir da chuva do Cemaden e dos alagamentos registrados pelo CGE |
| [`dynamic-programming/`](dynamic-programming/) | Dynamic Programming | Rota mais segura até o abrigo com vaga, sem passar por rua alagada (serviço em FastAPI) |
| [`ar-vr-modelling/`](ar-vr-modelling/) | AR/VR | _A preencher (Vitor)_ |
| [`network-architect-solutions/`](network-architect-solutions/) | Network | _A preencher (Vitor)_ |
| [`docs/`](docs/) | Todas | Decisões de arquitetura (`decisions.md`) |

## Links

- **Repositório da entrega de DDD Java:** https://github.com/Kelwinnn/Global-Solution---Rota-Segura-DDD-Java
- **Vídeo pitch:** https://www.youtube.com/watch?v=taJKTqr1J7I
- **API publicada:** https://global-solution-rota-segura.onrender.com
- **Swagger UI:** https://global-solution-rota-segura.onrender.com/swagger-ui.html
- **Serviço Python (rota segura e risco de alagamento):** https://rota-segura-python.onrender.com (`GET /health`, `POST /route`, `POST /risk`; documentação em `/docs`)
- **Como executar a API:** [`domain-driven-design-java/docs/como-executar.md`](domain-driven-design-java/docs/como-executar.md)

## Dados

Nenhum dado pessoal real. Pessoas assistidas, responsáveis, áreas de risco, sensores e leituras de exemplo são simulados. Dados públicos usados:

- **Abrigos do bairro-piloto:** © colaboradores do OpenStreetMap (licença ODbL); capacidade e ocupação simuladas.
- **Chuva:** Cemaden, Plataforma de Estações (PED).
- **Alagamentos:** Centro de Gerenciamento de Emergências Climáticas (CGE) da Prefeitura de São Paulo.

## Uso de IA

Os prompts usados com IA ficam registrados em [`prompts.md`](prompts.md).
