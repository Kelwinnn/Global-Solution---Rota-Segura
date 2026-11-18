# Rota Segura — roteiro do PDF de Domain Driven Design Java

Ordem das seções do PDF e de onde vem cada uma. A rubrica pede **um PDF** (30 pontos). A entrega vai num ZIP com o PDF, o link do GitHub e o link do deploy, pelo Portal do Aluno.

## Capa

- **Projeto:** Rota Segura
- **Disciplina:** Domain Driven Design Java — Global Solution FIAP 2026.2 (CitizenTech)
- **Integrantes:**
  - João Paulo Santana Basta — RM 565383
  - Kelwin Silva Bastos — RM 566348
  - Vitor Mantovani Camilo — RM 556537

## Índice

Gerado pelo editor (Word ou Google Docs: inserir índice a partir dos títulos).

## Seções

| Nº | Seção do PDF | Item da rubrica | Pontos | De onde vem |
|---|---|---|---|---|
| 1 | Especificação do projeto: descritivo, justificativa e objetivos | Especificação | 5 (com a capa e o índice) | `especificacao.md` |
| 2 | Diagrama de classes | Diagrama de classes | 10 | `class-diagram.md`: as 4 partes, como imagem |
| 3 | Protótipos de tela | Protótipos, endpoints e DER | 10 (os três juntos) | Figma (https://www.figma.com/design/EyUuqW4eIANQGnDVRNSYdR): as 4 telas em PNG, com uma frase de cada, tirada de `agile-methodology/prototipo.md` |
| 4 | Tabela de endpoints | (mesmo item) | | `endpoints.md`: as tabelas e os exemplos JSON |
| 5 | DER | (mesmo item) | | `der.md`: o diagrama como imagem e as tabelas de relacionamentos e índices |
| 6 | Instruções de execução: ferramentas, versões, links e passo a passo | Instruções | 5 | `como-executar.md` |

Na seção 6, os links precisam aparecer por extenso:
- **GitHub:** https://github.com/Kelwinnn/Global-Solution---Rota-Segura
- **Deploy:** https://global-solution-rota-segura.onrender.com
- **Swagger UI:** https://global-solution-rota-segura.onrender.com/swagger-ui.html

## Como montar

1. Crie o documento no Word ou no Google Docs, com estilos de título (Título 1 para as seções), para o índice sair sozinho.
2. Copie o texto dos arquivos `.md` de cada seção. As tabelas do Markdown podem ser coladas a partir da pré-visualização do IntelliJ ou do GitHub.
3. **Imagens dos diagramas:** gerar junto com os outros diagramas (combinado para depois). Até lá, deixe um espaço reservado na seção.
4. **Telas:** no Figma, selecione cada frame (1 a 4) e use **Export > PNG 2x**.
5. Exporte em PDF e confira se os links abrem.

## Antes de enviar

- [ ] O deploy responde (abra o Swagger). No plano gratuito do Render, a primeira chamada demora alguns segundos.
- [ ] O banco Oracle tem as tabelas e os dados do `ddl.sql` e do `dml.sql`. O `GET /api/areas` devolve as 10 áreas.
- [ ] O último commit foi feito **antes de 19/11 às 23h55**. Commit depois disso zera a parte técnica.
- [ ] O ZIP tem o PDF, o link do GitHub e o link do deploy.
