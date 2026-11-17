# Rota Segura — protótipo de telas

**Equipe:** João Paulo Santana Basta (RM 565383), Kelwin Silva Bastos (RM 566348) e Vitor Mantovani Camilo (RM 556537).

**Arquivo no Figma:** https://www.figma.com/design/EyUuqW4eIANQGnDVRNSYdR

Quatro telas: três do morador e da pessoa assistida, no celular (390 × 844), e uma do operador, no computador (1280 × 800). Todos os nomes e números são fictícios. Os abrigos são do bairro-piloto, com nomes do OpenStreetMap e vagas simuladas.

## Decisões de UX que valem para todas as telas

- **Linguagem simples:** frases curtas, na segunda pessoa ("Vá agora para o abrigo"), sem termos técnicos. O nível de risco aparece em palavras (baixo, médio, alto, muito alto, muito urgente), e não em números.
- **Cor nunca sozinha:** cada nível de risco e cada resposta tem cor **e** texto, para quem não distingue cores. Todos os pares de cor de fundo e texto têm contraste AA ou melhor.
- **Letra legível:** fonte Inter, texto de no mínimo 16 px no celular e títulos de 26 a 30 px.
- **Toque fácil:** os botões principais têm 64 px de altura e ocupam a largura da tela, pensando em pessoas idosas e com pouca firmeza nas mãos.
- **Uma ação por tela:** cada tela tem uma ação principal clara (ver a rota, responder, enviar resgate).

## Tela 1 — Mapa (tela inicial)

Histórias H11 (RF14) e H04 (RF04): o mapa mostra o nível de risco calculado pelo UC04, que o UC11 inclui.

- **Intenção:** responder em poucos segundos "estou em perigo?" e "para onde vou?".
- **O que tem:** o mapa com as áreas coloridas pelo nível de risco, os abrigos com vaga e o ponto "Você está aqui"; a legenda dos cinco níveis; e um cartão com a área do morador, o nível d'água, o abrigo com vaga mais perto **pela rota** (RN06) e o botão "Ver rota até o abrigo".
- **Adequação:**
  - o cartão de baixo resume o mapa para quem tem dificuldade de ler mapas;
  - a legenda fica sempre visível, sem precisar abrir menu;
  - os rótulos das áreas trazem o nível escrito, e não só a cor.

## Tela 2 — Alerta por SMS com a rota

Histórias H05 (RF05), H07 (RF07) e H10 (RF12).

- **Intenção:** fazer a pessoa sair, e sair pelo caminho certo.
- **O que tem:**
  - a primeira mensagem diz o nível de risco e o nível d'água, o abrigo e o endereço, a distância e o tempo a pé;
  - a prévia da rota mostra que o caminho evita a rua alagada;
  - a segunda mensagem traz a pergunta do check-in, com respostas por número.
- **Adequação:**
  - o SMS funciona em qualquer celular, sem internet e sem instalar aplicativo, o que importa para o público idoso;
  - a informação mais importante (o risco e para onde ir) vem na primeira linha, em destaque;
  - o tempo a pé (10 min) é mais fácil de entender do que a distância sozinha;
  - responder com um número funciona até em celular sem tela de toque.
- No MVP o SMS é simulado: o sistema registra a mensagem, e o front mostra a conversa como nesta tela.

## Tela 3 — Check-in "Estou seguro"

História H09 (RF09).

- **Intenção:** saber quem está seguro e quem ainda precisa de ajuda, com o menor esforço possível para quem responde.
- **Onde roda:** é a página que abre pelo link do SMS, para quem tem internet. Sem internet, a mesma resposta vai pelo próprio SMS, com o número da opção (tela 2). No MVP, as duas formas aparecem no celular simulado do front.
- **O que tem:** uma faixa com o alerta ativo; a pergunta "Como você está?"; três botões grandes ("Estou seguro", "Estou a caminho de um local seguro", "Ainda não saí"); o aviso de que as atualizações chegam a cada 15 minutos; e os telefones de socorro.
- **Adequação:**
  - não pede login nem cadastro: na emergência, nada pode atrasar a resposta;
  - os três botões têm cores e textos bem diferentes e ficam um embaixo do outro, para evitar toque errado;
  - a frase "Sua resposta avisa a equipe de resgate" mostra para que serve responder;
  - os números 193 e 199 ficam visíveis para quem precisa de ajuda imediata.

## Tela 4 — Painel de prioridade de resgate

História H12 (RF10, Should). Entra no protótipo porque mostra o destino das respostas da tela 3.

- **Intenção:** que o operador saiba em segundos quem atender primeiro.
- **O que tem:** o alerta ativo e o nível da área; quatro contadores (sem resposta há mais de 15 min, ainda não saíram, a caminho e seguros); e a lista das pessoas assistidas, ordenada por prioridade, com a necessidade, a resposta, o tempo desde o alerta e o botão "Enviar resgate".
- **Adequação:**
  - quem não respondeu em 15 minutos fica no topo, com a linha destacada (RN04);
  - a necessidade (ex.: "Acamada") e as observações do cadastro (ex.: "Precisa de maca para sair de casa") aparecem ao lado do nome, porque mudam o tipo de resgate;
  - quem já está seguro fica no fim, sem ação, para não tirar a atenção dos outros;
  - os dados pessoais só aparecem com login, como na API.

## Coerência com o backlog

| Tela | Histórias | Casos de uso |
|---|---|---|
| 1 · Mapa | H11, H04 | UC11, UC04 |
| 2 · Alerta por SMS com a rota | H05, H07, H10 | UC05, UC07, UC10 |
| 3 · Check-in | H09 | UC09 |
| 4 · Painel de prioridade de resgate | H12 | UC12 |
