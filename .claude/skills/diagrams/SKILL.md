---
name: diagrams
description: Gera o diagrama de classes (a partir do código Java) e o DER (a partir do ddl.sql) em Mermaid, para os PDFs de DDD Java e Database Design. Use quando o Kelwin pedir diagrama de classes, DER ou modelo de dados.
argument-hint: "classes | der | ambos"
---

# Diagrama de classes e DER

## Diagrama de classes (DDD Java, 10 pontos)

1. Leia todos os pacotes em `domain-driven-design-java/backend/src/main/java/br/com/rotasegura/` (`controller`, `bo`, `dao`, `repository`, `model`, `dto`, `exception`, `security`, `config`, `util`).
2. Gere `domain-driven-design-java/docs/class-diagram.md` com um bloco ` ```mermaid classDiagram `:
   - Entidades com atributos e tipos; relacionamentos com cardinalidade.
   - Controller → BO → DAO → Repository (dependências), DTOs ligados ao controller.
   - Se ficar grande demais para ler, gere dois: um só do modelo e outro das camadas.
3. O diagrama tem de refletir o código exatamente. Não inclua classe que não existe.

## DER (Database Design e DDD Java)

1. Leia `database-design/ddl.sql`.
2. Gere `database-design/der.md` (e uma cópia em `domain-driven-design-java/docs/der.md`, porque o PDF de DDD também pede o DER) com ` ```mermaid erDiagram `: tabelas, colunas com tipo, PK, FK e cardinalidade.

## Depois

- Diga como exportar a imagem para o PDF (ex.: Mermaid Live Editor ou a extensão do VS Code).
- Lembre que o **modelo lógico da rubrica de Database Design precisa ser feito no Oracle Data Modeler**; o Mermaid serve de referência e para o PDF de DDD Java.
- Aponte divergências entre código, DDL e diagramas.
