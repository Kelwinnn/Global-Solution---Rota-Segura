# Rota Segura — backlog do produto

**Equipe:** João Paulo Santana Basta (RM 565383), Kelwin Silva Bastos (RM 566348) e Vitor Mantovani Camilo (RM 556537).

Histórias no formato "Como <ator>, quero <ação>, para <benefício>", priorizadas por **MoSCoW**. Cada história corresponde a um caso de uso do diagrama (`casos-de-uso.puml`), com o mesmo número.

- **MVP:** as 11 histórias **Must** (marcadas com ★).
- **Story Points:** estimados pela equipe em Planning Poker, na sequência de Fibonacci (roteiro em `planning-poker.md`). _A estimar._
- **Situação:** pronto (API publicada no Render); pronto na API (a API já faz, mas faltam o SMS e a tela); a fazer; fora do escopo (Won't).
- **Morador no MVP:** quem recebe o alerta e o SMS são as pessoas assistidas e os responsáveis cadastrados (H08), os únicos com telefone no sistema. O cadastro de qualquer morador da área fica para uma próxima versão.

## Resumo

| ID | Caso de uso | RF | História (resumo) | MoSCoW | MVP | Story Points | Situação |
|---|---|---|---|---|---|---|---|
| H01 | UC01 | RF01 | Gerenciar áreas de risco | Must | ★ | a estimar | pronto |
| H02 | UC02 | RF02 | Cadastrar sensor numa área | Must | ★ | a estimar | pronto |
| H03 | UC03 | RF03 | Enviar leitura do sensor | Must | ★ | a estimar | pronto |
| H04 | UC04 | RF04 | Consultar nível de risco da área | Must | ★ | a estimar | pronto |
| H05 | UC05 | RF05 | Receber alerta e atualizações | Must | ★ | a estimar | a fazer |
| H06 | UC06 | RF06 | Cadastrar abrigo | Must | ★ | a estimar | pronto |
| H07 | UC07 | RF07 | Receber rota segura até o abrigo | Must | ★ | a estimar | pronto na API (consulta com login; falta o SMS e a tela) |
| H08 | UC08 | RF08 | Cadastrar pessoa assistida e responsáveis | Must | ★ | a estimar | pronto |
| H09 | UC09 | RF09 | Responder ao check-in | Must | ★ | a estimar | a fazer |
| H10 | UC10 | RF12 | Receber mensagens por SMS | Must | ★ | a estimar | a fazer |
| H11 | UC11 | RF14 | Ver mapa interativo | Must | ★ | a estimar | a fazer |
| H12 | UC12 | RF10 | Acompanhar painel de prioridade de resgate | Should | | a estimar | a fazer |
| H13 | UC13 | RF11 | Receber aviso sobre o assistido | Should | | a estimar | a fazer |
| H14 | UC14 | RF13 | Receber pedido de ajuda | Should | | a estimar | a fazer |
| — | UC15 | RF15 | Integrar com a Defesa Civil | Won't | | — | fora do escopo |

**Total do MVP:** a estimar. **Total geral:** a estimar.

## Histórias e critérios de aceite

### ★ H01 — Gerenciar áreas de risco (RF01, Must)

Como **operador do painel**, quero cadastrar, consultar, alterar e excluir as áreas de risco, para saber onde acompanhar a chuva.

- A área tem nome, cidade, tipo de risco (alagamento ou deslizamento), ponto central e raio em metros; o bairro é opcional.
- O nome não se repete na mesma cidade, sem diferenciar maiúsculas.
- Área com sensores ou pessoas assistidas não pode ser excluída.
- Consultar é aberto; cadastrar, alterar e excluir exigem login.

### ★ H02 — Cadastrar sensor numa área (RF02, Must)

Como **operador do painel**, quero cadastrar os sensores e vincular cada um a uma área, para saber de onde vem cada medição.

- O sensor tem código único, posição e situação (ativo ou inativo).
- A área informada precisa existir.
- Sensor com leituras não pode ser excluído, só inativado.

### ★ H03 — Enviar leitura do sensor (RF03, Must)

Como **sensor de rua**, quero enviar a chuva em mm, o nível d'água em cm e a hora da medição, para o sistema acompanhar a área em tempo real.

- O envio não exige login.
- Sensor com código desconhecido é recusado; sensor inativo não envia leitura nova.
- Não há duas leituras do mesmo sensor no mesmo instante.
- Hora até 5 minutos no futuro é aceita (relógio adiantado); além disso, é recusada.
- O histórico de leituras de cada sensor pode ser consultado.

### ★ H04 — Consultar nível de risco da área (RF04, Must)

Como **operador do painel**, quero ver o nível de risco atual de cada área, para saber onde a situação é mais urgente.

- Cinco níveis pelo nível d'água em cm (RN01): baixo, médio, alto, muito alto e muito urgente.
- Vale a leitura mais recente de cada sensor ativo na última 1 hora, e a área fica com a pior delas.
- Área sem leitura recente aparece como "sem dados".

### ★ H05 — Receber alerta e atualizações (RF05, Must)

Como **morador**, quero receber um alerta quando o risco da minha área chegar a médio, com atualizações a cada 15 minutos, para saber quando sair.

- O alerta sai a partir do nível médio, para as pessoas assistidas e os responsáveis cadastrados na área.
- Só um alerta ativo por área; ele encerra quando o risco volta a baixo (RN02).
- Enquanto o alerta durar, o morador recebe o nível de risco a cada 15 minutos; quem ainda não está seguro recebe também a pergunta do check-in (RN07).
- O gatilho principal do alerta é o nível d'água (em cm). Como complemento, quando o modelo de Data Science indicar alta chance de alagamento (`highProbability`), sai antes um SMS de preparação. Em Guarulhos, que não tem estação de chuva nem validação do modelo, vale só o nível d'água.

### ★ H06 — Cadastrar abrigo (RF06, Must)

Como **operador do painel**, quero cadastrar os abrigos com endereço e capacidade, para indicar às pessoas para onde ir.

- O abrigo tem nome, endereço, posição, capacidade e vagas ocupadas.
- As vagas ocupadas nunca passam da capacidade.
- A consulta mostra as vagas livres de cada abrigo.

### ★ H07 — Receber rota segura até o abrigo (RF07, Must)

Como **morador**, quero receber a rota mais segura até o abrigo com vaga mais próximo, para chegar sem passar por rua alagada.

- A rota nunca passa por rua alagada (RN05).
- O destino é o abrigo mais próximo pela rota que ainda tenha vaga (RN06).
- Sem rota segura, a orientação é ficar em local alto (e, quando houver o painel da H12, a pessoa entra nele).
- No MVP, a rota só é calculada no bairro-piloto (Jardim Helena); fora dele, a orientação é ir ao abrigo mais próximo ou a um local mais alto.
- Se o serviço de rota estiver fora do ar, o alerta sai mesmo assim, orientando ir ao abrigo ou a um local mais alto.

### ★ H08 — Cadastrar pessoa assistida e responsáveis (RF08, Must)

Como **operador do painel**, quero cadastrar cada pessoa assistida com seus responsáveis, para saber quem não consegue sair sozinho e quem avisar.

- A pessoa tem nome, telefone, área, onde mora (posição no mapa) e necessidade especial (idosa, acamada, cadeira de rodas ou outra); observações são opcionais.
- Toda pessoa tem ao menos um responsável e uma área (RN03).
- Os dados só aparecem com login, porque são pessoais (privacidade).

### ★ H09 — Responder ao check-in (RF09, Must)

Como **pessoa assistida**, quero responder ao alerta com "Estou seguro", "Estou a caminho de um local seguro" ou "Ainda não saí", para avisar como estou.

- A resposta não exige login.
- São três opções de resposta.
- Quem responde "Estou seguro" passa a receber só as atualizações (RN07).

### ★ H10 — Receber mensagens por SMS (RF12, Must)

Como **morador**, quero receber o alerta e as perguntas por SMS, para ser avisado mesmo sem internet.

- No MVP o SMS é simulado: o sistema registra cada mensagem, e o front mostra como se fosse o celular.
- A mensagem usa linguagem simples e traz o nível de risco e a rota.

### ★ H11 — Ver mapa interativo (RF14, Must)

Como **morador**, quero ver num mapa as áreas de risco, o nível de cada uma e os abrigos, para entender a situação perto de mim.

- É a tela inicial do front.
- Mostra o nível atual de cada área e os abrigos com vagas.
- Funciona no celular, com letra legível, bom contraste e linguagem simples.

### H12 — Acompanhar painel de prioridade de resgate (RF10, Should)

Como **operador do painel**, quero um painel de prioridade de resgate, para atender primeiro quem está em perigo.

- Pessoa assistida sem resposta em até 15 minutos depois do alerta entra no painel como prioridade (RN04).
- Quem não tem rota segura também entra no painel (RN05).
- O operador aciona o resgate de cada pessoa direto da lista.

### H13 — Receber aviso sobre o assistido (RF11, Should)

Como **responsável**, quero ser avisado sobre a pessoa que acompanho durante o alerta, para poder ajudá-la.

- Quando sai o alerta na área da pessoa assistida, cada responsável recebe um SMS (simulado) com o nível de risco e a área.
- Se a pessoa não responder em 15 minutos, ou responder "Ainda não saí", o responsável recebe um aviso de que ela ainda não está segura.
- Quando a pessoa responde "Estou seguro", o responsável é avisado.

### H14 — Receber pedido de ajuda (RF13, Should)

Como **voluntário**, quero ser avisado quando alguém responder "Estou a caminho", "Ainda não saí" ou não responder em 15 minutos, para ir ajudar.

- Esses três casos acionam a mensagem para voluntários e pessoas que podem ajudar (RN08).

### Fora do escopo — Integrar com a Defesa Civil (RF15, Won't)

A integração real com os sistemas da Defesa Civil não entra nesta versão: depende de acesso e acordo com o órgão. O MVP simula o canal (SMS) e o painel.

## Coerência com o diagrama de casos de uso

| Ator | Casos de uso | Histórias |
|---|---|---|
| Operador do painel | UC01, UC02, UC04, UC06, UC08, UC12 | H01, H02, H04, H06, H08, H12 |
| Sensor de rua | UC03 | H03 |
| Morador | UC05, UC07, UC10, UC11 | H05, H07, H10, H11 |
| Pessoa assistida (também é morador) | UC09 | H09 |
| Responsável | UC13 | H13 |
| Voluntário | UC14 | H14 |
| Serviço de rota e risco (sistema) | UC05, UC07 | H05, H07 |
| Defesa Civil | UC15 | fora do escopo (Won't) |
