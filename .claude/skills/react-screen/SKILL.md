---
name: react-screen
description: Cria ou altera uma tela do frontend React + Vite + TypeScript + Tailwind da Rota Segura, consumindo a API Java com tipos alinhados aos DTOs e seguindo as regras de acessibilidade do briefing. Use quando o Kelwin pedir uma tela, um formulário ou um componente do front.
argument-hint: "<nome da tela> [RF relacionado]"
---

# Criar uma tela do front

## 1. Antes

- Leia `CLAUDE.md` e `docs/decisions.md`. Roteador, cliente HTTP, biblioteca de mapa e estrutura de pastas do front podem estar **em aberto**: se a tela precisar de um deles, pergunte com 2 ou 3 opções antes de instalar ou criar.
- Confirme com o Kelwin: objetivo da tela, quem usa (morador, familiar, agente, Defesa Civil), quais endpoints consome e qual RF atende.
- Leia os DTOs do backend que a tela usa. Os tipos TypeScript devem espelhar exatamente esses DTOs.
- Lembre que o front é feito pelos três: verifique se já existe componente parecido antes de criar outro.

## 2. Regras da tela

- **Linguagem simples**, frases curtas, em português ("Saia agora. Vá para a Escola X").
- **Legível:** texto base de pelo menos 16px; botões principais grandes (alvo de no mínimo 44px).
- **Contraste** AA; risco nunca indicado só por cor (sempre texto ou ícone com rótulo).
- **Celular primeiro:** funciona em 360px de largura.
- **Acessível:** `label` em todo campo, foco visível, HTML semântico, navegação por teclado.
- **Estados:** carregando, erro com orientação e vazio.
- **URL da API** vem de `import.meta.env.VITE_API_URL`, nunca fixa.
- **Chamadas protegidas** enviam o cookie de sessão (`credentials: 'include'`) e o token CSRF em todo POST, PUT e DELETE. Endpoints liberados (check-in e leituras do sensor) não precisam de nenhum dos dois.
- **SMS simulado:** a tela que mostra as mensagens do cidadão imita a conversa no celular, com os três botões de resposta do check-in.
- Tailwind para estilo; sem CSS solto, salvo necessidade.

## 3. Depois

- Rode `npm run build` em `domain-driven-design-java/frontend/`.
- Chame o agente `ui-reviewer`.
- Explique o código por bloco em português e sugira a mensagem de commit. Não faça commit.
