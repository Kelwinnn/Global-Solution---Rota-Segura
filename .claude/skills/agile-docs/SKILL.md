---
name: agile-docs
description: Produz os textos da entrega de Agile Methodology (apresentação da ideia, personas, casos de uso em PlantUML, backlog com MoSCoW e story points, comentários de UX das telas) a partir dos requisitos e das entrevistas reais. Use quando o Kelwin pedir algum entregável de Agile.
argument-hint: "ideia | personas | casos-de-uso | backlog | prototipo | tudo"
---

# Entregáveis de Agile Methodology

Arquivos ficam em `agile-methodology/`. No fim, tudo vai para **um único PDF** (mais de um PDF ou link externo = menos 10 pontos).

## Fontes permitidas

- `CLAUDE.md` (ideia, requisitos RF01 a RF15, regras RN01 a RN06).
- Respostas das entrevistas em `agile-methodology/entrevistas.md`. Se o arquivo não existir ou estiver vazio, peça ao Kelwin as respostas antes de escrever personas. **Nunca invente fala de entrevistado.**
- Nada de dados pessoais reais: personas com nome fictício, só bairro, idade aproximada e contexto.

## Entregáveis e pontos

1. **Apresentação da ideia (10):** `ideia.md` com problema, foco, público-alvo e solução, justificando as escolhas.
2. **Personas (10):** `personas.md` com descrição narrativa. Sugestão a confirmar: idosa acamada, filha cuidadora, agente comunitário de saúde, agente da Defesa Civil. Use frases reais das entrevistas quando houver.
3. **Casos de uso (20):** `casos-de-uso.puml` em PlantUML com todos os atores e o escopo total (Must, Should e Could). Coerente com o backlog.
4. **Backlog (20):** `backlog.md` com histórias no formato "Como <ator>, quero <ação>, para <benefício>", critérios de aceite, MoSCoW, coluna de Story Points e o MVP destacado (os 10 Must).
   - **Story Points:** o Planning Poker precisa ser feito pela equipe. Deixe a coluna vazia ou marcada "a estimar" e gere um roteiro para a sessão. Não invente estimativas.
5. **Protótipo (20):** `prototipo.md` com no mínimo 3 telas e comentários sobre a intenção e a adequação da UX. Sugestão: alerta com rota até o abrigo, check-in "Estou seguro", painel de prioridade de resgate.
6. **Vídeo pitch (20):** roteiro de até 3 minutos em `pitch.md` (problema, solução, quem ajuda), com a narração feita pelo grupo.

## Coerência

Cada caso de uso tem ao menos uma história no backlog, e cada história tem um caso de uso. Liste divergências ao final. Rode o agente `rubric-checker` para Agile antes de montar o PDF.
