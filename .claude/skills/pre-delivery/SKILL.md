---
name: pre-delivery
description: Checklist final antes de enviar as entregas da Global Solution (17 e 18/11). Confere rubricas, coerência entre partes, nomes e RMs, links, README com a Ficha, prompts.md e o risco de commit depois do prazo. Use só quando o Kelwin pedir a revisão final.
disable-model-invocation: true
---

# Revisão final antes da entrega

Prazo: 19/11/2026 às 23h55. 20/11 é feriado. Quem envia é o João Paulo (Amigo 1); o Kelwin repassa os arquivos para ele.

## 1. Rubricas

Rode o agente `rubric-checker` três vezes: DDD Java, Database Design e Agile. Junte os itens "faltando" e "parcial" numa lista única ordenada por pontos em risco.

## 2. Coerência entre partes

- Entidades JPA, `database-design/ddl.sql`, diagrama de classes e DER descrevem as mesmas tabelas e relacionamentos.
- Casos de uso, backlog e telas do protótipo contam a mesma história.
- A tabela de endpoints bate com os controllers.

## 3. Identificação

Os três nomes completos e RMs aparecem em toda capa e arquivo de identificação:
Kelwin Silva Bastos (566348), João Paulo Santana Basta (565383), Vitor Mantovani Camilo (556537).
Database Design pede os nomes em **ordem alfabética**.

## 4. Repositório

- README com a Ficha da CitizenTech no topo e uma pasta por parte.
- `prompts.md` presente e preenchido.
- Nenhuma credencial versionada (procure senha, usuário e URL do Oracle).
- Nada de dados pessoais reais em seeds, testes e documentos.

## 5. Links e deploy

- O deploy no Render responde (abra o Swagger ou um GET).
- Vídeos do YouTube abrem em janela anônima (pitch de Agile não listado, até 3 min; vídeo de AR/VR de 30 a 60 s).

## 6. Prazo do GitHub

Lembre em destaque: **o último commit precisa ser antes de 19/11 às 23h55**. Commit depois disso zera a parte técnica de DDD Java.

## Formato

Uma lista de pendências: o que falta, quem resolve (Kelwin, João Paulo ou Vitor) e quanto vale.
