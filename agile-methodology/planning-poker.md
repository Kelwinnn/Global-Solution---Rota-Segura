# Rota Segura — roteiro do Planning Poker

Roteiro para a equipe estimar os **Story Points** do backlog (rubrica de Agile: "Story Points por Planning Poker"). As histórias abaixo saem dos requisitos do `CLAUDE.md`. Os pontos ficam **vazios** até a votação: quem estima é a equipe.

- **Participantes:** Kelwin Silva Bastos (RM 566348), João Paulo Santana Basta (RM 565383) e Vitor Mantovani Camilo (RM 556537).
- **Facilitador:** _definir_ (lê as histórias, controla o tempo e anota os votos).
- **Duração:** cerca de 45 minutos (14 histórias, até 3 minutos cada, mais a abertura).
- **Cartas:** sequência de Fibonacci: 1, 2, 3, 5, 8, 13, 21, mais `?` (não sei) e `☕` (pausa). Podem ser cartas de papel ou qualquer app de Planning Poker.

## 1. Abertura (5 min)

1. O facilitador explica que o ponto mede o **tamanho relativo** da história: esforço, complexidade e incerteza juntos, e não horas.
2. **História de referência:** a equipe escolhe a primeira história a votar e combina o valor dela. Sugestão: a H01 (CRUD de áreas de risco), que já está pronta, todos conhecem e serve de régua para as outras. O valor é a equipe que define.
3. Combinar a regra de desempate do passo 3.

## 2. Para cada história (até 3 min)

1. O facilitador lê a história e os critérios de aceite.
2. Dúvidas rápidas (1 min). Quem conhece a parte técnica responde: o Kelwin para a API e o banco, o João Paulo para a rota e o modelo de risco.
3. **Todos escolhem a carta em segredo e mostram ao mesmo tempo**, para ninguém se influenciar pelo voto do outro.
4. Se os votos forem iguais ou vizinhos na sequência (ex.: 3 e 5), anote o maior.
5. Se ficarem distantes (ex.: 2 e 8), quem votou o **menor** e quem votou o **maior** explicam o motivo em 30 segundos cada, e todos votam de novo.
6. Depois de 3 rodadas sem acordo, fica o **maior** voto da última rodada: é o mais seguro para planejar.
7. Se alguém mostrar `?`, a história volta para dúvidas. Se aparecer um 21, vale perguntar se ela não deveria ser dividida em duas.

## 3. Histórias para votar

Os critérios completos estão em `backlog.md`, que é a fonte única: se algo mudar na votação, atualize lá.

Os atores seguem os casos de uso: **morador** (cidadão em área de risco), **pessoa assistida**, **responsável**, **operador do painel**, **voluntário** e **sensor de rua**. Os nomes das personas entram depois, a partir das entrevistas.

MVP = os 11 **Must**. RF15 (integração real com a Defesa Civil) é **Won't** e fica fora da votação.

| ID | RF | MoSCoW | História | Critérios de aceite |
|---|---|---|---|---|
| H01 | RF01 | Must | Como **operador do painel**, quero cadastrar, consultar, alterar e excluir as áreas de risco, para saber onde acompanhar a chuva. | Área com nome, cidade, tipo (alagamento ou deslizamento), ponto central e raio; nome único por cidade; área com sensores ou pessoas assistidas não pode ser excluída; consultar é aberto. |
| H02 | RF02 | Must | Como **operador do painel**, quero cadastrar os sensores e vincular cada um a uma área, para saber de onde vem cada medição. | Sensor com código único, posição e situação (ativo ou inativo); a área precisa existir; sensor com leituras só pode ser inativado, não excluído. |
| H03 | RF03 | Must | Como **sensor de rua**, quero enviar a chuva em mm, o nível d'água em cm e a hora da medição, para o sistema acompanhar a área em tempo real. | Envio sem login; sensor desconhecido é recusado; sensor inativo não envia leitura nova; não aceita duas leituras do mesmo sensor no mesmo instante; hora até 5 min no futuro é aceita; o histórico pode ser consultado. |
| H04 | RF04 | Must | Como **operador do painel**, quero ver o nível de risco atual de cada área, para saber onde a situação é mais urgente. | Cinco níveis pelo nível d'água (RN01); vale a leitura mais recente de cada sensor ativo na última 1 hora, e a área fica com a pior delas; área sem leitura recente aparece como "sem dados". |
| H05 | RF05 | Must | Como **morador**, quero receber um alerta quando o risco da minha área chegar a médio, com atualizações a cada 15 minutos, para saber quando sair. | Um só alerta ativo por área (RN02), que encerra quando o risco volta a baixo; atualização a cada 15 min; a pergunta do check-in se repete só para quem ainda não está seguro (RN07); aviso antecipado quando o modelo indicar alta chance de alagamento (a confirmar). |
| H06 | RF06 | Must | Como **operador do painel**, quero cadastrar os abrigos com endereço e capacidade, para indicar às pessoas para onde ir. | Abrigo com nome, endereço, posição, capacidade e vagas ocupadas; ocupadas nunca passam da capacidade; a consulta mostra as vagas livres. |
| H07 | RF07 | Must | Como **morador**, quero receber a rota mais segura até o abrigo com vaga mais próximo, para chegar sem passar por rua alagada. | A rota nunca passa por rua alagada (RN05); vai ao abrigo mais próximo que tenha vaga (RN06); sem rota segura, orienta ficar em local alto; se o serviço de rota estiver fora, o alerta sai assim mesmo. |
| H08 | RF08 | Must | Como **operador do painel**, quero cadastrar cada pessoa assistida com seus responsáveis, para saber quem não consegue sair sozinho e quem avisar. | Pessoa com área, onde mora (posição no mapa), telefone e necessidade especial; ao menos um responsável (RN03); dados vistos só com login (privacidade). |
| H09 | RF09 | Must | Como **pessoa assistida**, quero responder ao alerta com "Estou seguro", "Estou a caminho de um local seguro" ou "Ainda não saí", para avisar como estou. | Resposta sem login; as três opções; quem responde "Estou seguro" passa a receber só as atualizações (RN07). |
| H10 | RF12 | Must | Como **morador**, quero receber o alerta e as perguntas por SMS, para ser avisado mesmo sem internet. | No MVP o SMS é simulado: o sistema registra a mensagem e o front mostra como se fosse o celular; linguagem simples, com o nível de risco e a rota. |
| H11 | RF14 | Must | Como **morador**, quero ver num mapa as áreas de risco, o nível de cada uma e os abrigos, para entender a situação perto de mim. | É a tela inicial; mostra o nível atual de cada área e os abrigos; funciona no celular, com letra legível e bom contraste. |
| H12 | RF10 | Should | Como **operador do painel**, quero um painel de prioridade de resgate, para atender primeiro quem está em perigo. | Assistido sem resposta em até 15 min após o alerta entra no painel como prioridade (RN04); quem não tem rota segura também (RN05); o operador aciona o resgate pela lista. |
| H13 | RF11 | Should | Como **responsável**, quero ser avisado sobre a pessoa que acompanho durante o alerta, para poder ajudá-la. | _Detalhar com a equipe antes de votar: quando o aviso sai e o que ele diz._ |
| H14 | RF13 | Should | Como **voluntário**, quero ser avisado quando alguém responder "Estou a caminho", "Ainda não saí" ou não responder em 15 min, para ir ajudar. | Os três casos acionam a mensagem (RN08). |

## 4. Folha de votação

Uma linha por história. Anote o voto de cada um em cada rodada e o valor final.

| ID | Rodada 1 (Kelwin / João Paulo / Vitor) | Rodada 2 | Rodada 3 | Story Points | Observação |
|---|---|---|---|---|---|
| H01 | / / | | | | referência |
| H02 | / / | | | | |
| H03 | / / | | | | |
| H04 | / / | | | | |
| H05 | / / | | | | |
| H06 | / / | | | | |
| H07 | / / | | | | |
| H08 | / / | | | | |
| H09 | / / | | | | |
| H10 | / / | | | | |
| H11 | / / | | | | |
| H12 | / / | | | | |
| H13 | / / | | | | |
| H14 | / / | | | | |

**Total do MVP (H01 a H11):** ___ pontos. **Total geral:** ___ pontos.

## 5. Depois da sessão

1. Passe a folha preenchida para o Claude (pode ser foto ou texto). Os pontos entram na coluna Story Points do `backlog.md`.
2. Guarde uma **evidência** da sessão para o PDF de Agile: foto das cartas na mesa ou print do app, com a data.
3. História que mudou na conversa (dividida, ou com critério novo) também precisa mudar no backlog e no diagrama de casos de uso, para manter a coerência que a rubrica avalia.
