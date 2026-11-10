---
name: endpoints-table
description: Gera a tabela de endpoints da API (método, rota, descrição, corpo, respostas) para o PDF de DDD Java, lendo os controllers do backend. Use quando o Kelwin pedir a tabela de endpoints ou a documentação da API.
---

# Tabela de endpoints para o PDF de DDD Java

1. Leia todos os arquivos em `domain-driven-design-java/backend/**/controller/` e os DTOs que eles usam.
2. Monte uma tabela Markdown em `domain-driven-design-java/docs/endpoints.md` com as colunas:
   `Método | Rota | O que faz | Corpo da requisição | Respostas (status)`.
   - Agrupe por recurso.
   - Corpo: nome do DTO e campos obrigatórios.
   - Respostas: os status reais que o código devolve (ex.: 200, 201, 204, 400, 404), incluindo os do `@RestControllerAdvice`.
3. Abaixo da tabela, inclua um exemplo de requisição e resposta JSON para um POST e um GET, com dados simulados.
4. Confira que existem GET, POST, PUT e DELETE no conjunto (exigência da rubrica) e avise se faltar algum.
5. Informe o endereço do Swagger UI (`/swagger-ui/index.html`), que pode ser citado no PDF junto com o link do deploy.

Use só o que está no código. Não documente endpoint que não existe.
