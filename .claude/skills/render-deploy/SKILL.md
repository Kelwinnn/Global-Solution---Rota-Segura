---
name: render-deploy
description: Prepara o deploy da API Java no Render via Dockerfile (Dockerfile multi-stage, variáveis de ambiente, checklist e instruções de execução para o PDF). Use só quando o Kelwin pedir para preparar ou revisar o deploy.
disable-model-invocation: true
---

# Deploy da API no Render

O deploy vale 30 dos 70 pontos da parte técnica de DDD Java. Comece cedo.

## 1. Confirmar antes

Leia `docs/decisions.md`. Estes itens podem estar em aberto; pergunte antes de seguir:
- nomes das variáveis de ambiente;
- origens de CORS (front local e front em produção), que precisam aceitar credenciais por causa do login com cookie;
- versão exata do Spring Boot e do JDK da imagem (o decidido é Java 21).

## 2. Gerar

- `domain-driven-design-java/backend/Dockerfile` multi-stage: estágio de build com Maven e JDK 21, estágio final só com o JRE 21 e o `.jar`.
- `domain-driven-design-java/backend/.dockerignore` (target/, .git, .env, IDE).
- A aplicação lê a porta de `PORT` (o Render define essa variável) e as credenciais do Oracle e do login de variáveis de ambiente. Nada de credencial no repositório.
- `domain-driven-design-java/docs/como-executar.md`, para a seção de instruções do PDF (5 pontos): ferramentas usadas, versões (JDK 21, Maven, Oracle), link do GitHub, link do deploy (deixar marcado para preencher), passo a passo local e passo a passo no Render, lista de variáveis de ambiente (só os nomes, sem valores).

## 3. Testar localmente

Mostre os comandos `docker build` e `docker run` com `--env-file` para o Kelwin rodar. Peça confirmação antes de rodar Docker.

## 4. Checklist do Render

Depois do deploy, teste o login a partir do front publicado: o cookie de sessão precisa chegar com `SameSite=None` e `Secure`, e a chamada seguinte precisa continuar autenticada. Web Service, ambiente Docker, raiz `domain-driven-design-java/backend/`, variáveis cadastradas no painel, health check (ex.: Swagger ou um GET simples) e conferir se o Render consegue acessar o Oracle da FIAP. Se não conseguir, pare e avise: é um risco para a nota que a equipe precisa resolver com o professor.
