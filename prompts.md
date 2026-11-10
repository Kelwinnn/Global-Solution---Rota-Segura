# Prompts de IA usados no projeto

Registro automatico (hook do Claude Code), exigido pelo briefing da Global Solution.

## 10/11/2026 09:13 - Claude Code

Crie o esqueleto do backend em domain-driven-design-java/backend/: projeto Maven com Java 21 e Spring Boot 3.4.1, pacote base br.com.rotasegura com os pacotes decididos, dependências para JPA, Oracle, Validation, Security e springdoc, conexão com o Oracle por variáveis de ambiente, .gitignore com .env e um Dockerfile. Mostre o plano antes de criar os arquivos.

## 10/11/2026 09:23 - Claude Code

ok, pode seguir com as recomendações

## 10/11/2026 09:53 - Claude Code

acho que o open-view mantem a conexão aberta até a resposta.

pois ele precisa primeiro baixar as dependecias para depois rodar os arquivos.

## 10/11/2026 09:56 - Claude Code

! printf '# Copie para .env e preencha. O .env nunca vai para o Git.\nPORT=8080\nDB_URL=\nDB_USERNAME=\nDB_PASSWORD=\n' > domain-driven-design-java/backend/.env.example

