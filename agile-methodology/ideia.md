# Rota Segura — apresentação da ideia

**Equipe:** João Paulo Santana Basta (RM 565383), Kelwin Silva Bastos (RM 566348) e Vitor Mantovani Camilo (RM 556537).
**Desafio:** Global Solution FIAP 2026.2, CitizenTech.

## Problema

Quando chove forte, o alerta de risco chega para todos, mas não diz **para onde ir** nem **como chegar** sem passar por uma rua alagada. E, depois que o alerta sai, ninguém fica sabendo **quem conseguiu sair e quem ficou para trás**.

Para quem não consegue sair sozinho, como uma pessoa idosa, acamada ou em cadeira de rodas, isso é ainda mais grave. Ela depende de alguém que saiba que ela precisa de ajuda, e na hora da emergência essa informação não está em lugar nenhum.

## Foco

Transformar o alerta de chuva em **ação**. A pessoa recebe a rota até um abrigo que ainda tenha vaga, e o sistema acompanha quem já está seguro, para que a ajuda vá primeiro a quem ainda está em perigo.

**Por que esse foco:** um alerta que só informa deixa a decisão inteira para quem está em risco. Juntar alerta, rota e confirmação de segurança fecha o ciclo: avisar, orientar e conferir.

## Público-alvo

- **Cidadão beneficiado:** quem mora em área de risco de alagamento ou deslizamento, com prioridade para quem não consegue sair sozinho (idosos, acamados e pessoas com deficiência).
- **Quem também usa o sistema:**
  - o **responsável** pela pessoa assistida (familiar ou vizinho);
  - o **operador do painel**, que cadastra áreas, sensores, abrigos e pessoas assistidas e acompanha a prioridade de resgate;
  - os **voluntários** que podem ajudar na hora.

**Território do MVP:** o Alto Tietê, Guarulhos e cinco distritos da Zona Leste de São Paulo (Jardim Pantanal, São Miguel Paulista, Itaim Paulista, Jardim Helena e Itaim Biacica). O bairro-piloto é o Jardim Helena.

**Por que esse público e esse território:** são regiões da várzea do Rio Tietê sujeitas a alagamento. Na Zona Leste, o histórico vem do Centro de Gerenciamento de Emergências Climáticas (CGE) da Prefeitura de São Paulo; no Alto Tietê, das ocorrências do Reindesc (Cemaden). As duas bases são usadas na parte de Data Science. **Limitação:** o modelo foi treinado com dados da capital e validado no Alto Tietê (Mogi das Cruzes e Itaquaquecetuba). Guarulhos não tem estação de chuva nem validação, então lá o MVP usa só os sensores (nível d'água em cm). E quem não consegue sair sozinho é quem mais perde quando o alerta não vira ação.

## Solução

1. **Sensores de rua** medem a chuva (mm) e o nível d'água (cm) em cada área de risco.
2. O sistema calcula o **nível de risco** da área: baixo, médio, alto, muito alto ou muito urgente. Um modelo de Data Science estima também a **chance de alagamento antes de acontecer** e, quando ela é alta, manda um SMS de preparação. O gatilho principal do alerta continua sendo o nível d'água medido. No teste com 2024 e 2025, dados que o modelo nunca tinha visto, ele avisou 75,7% dos dias de alagamento (28 de 37), com precisão de 40%. A regra simples "chuva de 23 mm ou mais" avisou 56,8%.
3. A partir do risco médio, o morador recebe um **alerta por SMS**, com atualização a cada 15 minutos enquanto o risco durar.
4. O alerta traz a **rota mais segura até o abrigo com vaga mais próximo**. A rota nunca passa por rua alagada e, se não houver rota segura, orienta procurar um local alto. No MVP, a rota é calculada no bairro-piloto (Jardim Helena); fora dele, a mensagem orienta ir ao abrigo mais próximo ou a um local mais alto.
5. A pessoa responde pelo SMS: **"Estou seguro"**, **"Estou a caminho de um local seguro"** ou **"Ainda não saí"**.
6. **Próxima etapa (Should):** quem não responder em 15 minutos vai entrar no **painel de prioridade de resgate**, e o responsável e os voluntários poderão ser avisados.

Um **mapa interativo** mostra as áreas, o nível de risco de cada uma e os abrigos.

**Quem recebe no MVP:** as pessoas assistidas e os responsáveis cadastrados, que são os que têm telefone no sistema. O cadastro de qualquer morador da área para receber alertas fica para uma próxima versão.

**Por que SMS:** funciona em qualquer celular, sem internet e sem instalar aplicativo, o que importa para o público idoso. No MVP o SMS é simulado: o sistema registra a mensagem e a tela mostra como se fosse o celular.

**Por que sensores próprios, e não só a previsão do tempo:** a previsão diz que vai chover. O nível d'água medido na rua diz se a área já está alagando, e é isso que define a urgência de sair.

## Indicador de impacto

Percentual de pessoas assistidas que confirmam "Estou seguro" em até 30 minutos depois do alerta. **Meta do piloto: 90%.** O próprio sistema mede, porque registra cada resposta e a hora em que chegou.

## Objetivos de Desenvolvimento Sustentável (ODS)

- **ODS 11, meta 11.5:** reduzir as mortes e as pessoas afetadas por desastres, inclusive os relacionados à água, protegendo as pessoas em situação de vulnerabilidade.
- **ODS 13, meta 13.1:** reforçar a resiliência a riscos relacionados ao clima.
- **ODS 10:** o foco em idosos e pessoas com deficiência, que ficam para trás quando o alerta é igual para todos.
