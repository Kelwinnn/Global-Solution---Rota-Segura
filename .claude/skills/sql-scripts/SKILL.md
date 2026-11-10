---
name: sql-scripts
description: Gera ou atualiza os scripts Oracle da entrega de Database Design (ddl.sql, dml.sql, dql.sql em database-design/) a partir das entidades JPA, cumprindo a rubrica. Use quando o Kelwin pedir os scripts SQL ou depois de mudar uma entidade.
---

# Scripts SQL de Database Design

## 1. Antes

- Leia `CLAUDE.md`, `docs/decisions.md` e todas as entidades em `domain-driven-design-java/backend/**/model/`.
- O modelo JPA é a fonte da verdade. Padrão decidido: tabelas com prefixo `T_RS_` e chave primária `GENERATED ALWAYS AS IDENTITY` (ou `BY DEFAULT`, se o DML precisar inserir IDs fixos; pergunte). Padrão de nome das colunas está em aberto: pergunte antes de escrever.

## 2. Arquivos (em `database-design/`, texto puro, extensão .sql)

**`ddl.sql` (20 pontos)**
- `DROP TABLE ... CASCADE CONSTRAINTS` comentados no topo, para rodar de novo com facilidade.
- `CREATE TABLE` em ordem que respeite as FKs.
- Constraints com nome explícito: PK, FK, NOT NULL, UNIQUE e CHECK (ex.: nível de risco só em valores válidos, capacidade > 0).
- Comentário curto acima de cada tabela dizendo o que ela guarda.

**`dml.sql` (20 pontos)**
- No mínimo 5 `INSERT` por tabela. Se o domínio tiver menos de 5 valores possíveis, justifique em comentário.
- Dados simulados e coerentes com o território do projeto (Alto Tietê, Guarulhos e os 5 distritos da Zona Leste). Nada de dados pessoais reais: nomes fictícios, sem CPF, sem telefone real.
- `COMMIT;` no final.

**`dql.sql` (30 pontos)** — exatamente estas três, cada uma com um comentário com a pergunta de negócio que responde:
1. INNER JOIN de duas tabelas.
2. INNER JOIN com função de grupo e GROUP BY.
3. LEFT OUTER JOIN com WHERE e ORDER BY.

## 3. Depois

- Chame o agente `sql-reviewer` para conferir rubrica e coerência com o JPA.
- Explique cada script em português e lembre que o modelo lógico precisa ser refeito no Oracle Data Modeler para bater com o DDL (o PDF não pode ter link de diagrama em nuvem).
