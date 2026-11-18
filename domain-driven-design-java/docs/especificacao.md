# Rota Segura API — especificação do projeto

Seção "Especificação do projeto" do PDF de Domain Driven Design Java: descritivo, justificativa e objetivos.

## Descritivo

A Rota Segura é um sistema de alerta de alagamento e deslizamento que transforma o alerta de chuva em ação. Sensores de rua medem a chuva e o nível d'água; o sistema calcula o risco de cada área; a pessoa recebe por SMS a rota segura até o abrigo com vaga mais próximo e responde se está segura; e quem não responde aparece num painel de prioridade de resgate.

Esta entrega é a **API em Java** que sustenta o sistema. Ela foi feita em Spring Boot, grava os dados no banco Oracle da FIAP e está publicada no Render. Cuida de:

- **áreas de risco** (RF01): cadastro com cidade, tipo de risco (alagamento ou deslizamento), ponto central e raio;
- **sensores** (RF02), cada um vinculado a uma área e com situação ativo ou inativo;
- **leituras dos sensores** (RF03): chuva em mm, nível d'água em cm e a hora da medição, enviadas pelo próprio aparelho, sem login;
- **nível de risco de cada área** (RF04): baixo, médio, alto, muito alto ou muito urgente, calculado na hora a partir das leituras da última 1 hora dos sensores ativos (RN01);
- **abrigos** (RF06): endereço, capacidade, vagas ocupadas e vagas livres;
- **pessoas assistidas e responsáveis** (RF08): quem não consegue sair sozinho, com a necessidade especial e ao menos um responsável (RN03). Os dados só aparecem com login, porque são pessoais.

A API segue as camadas `controller → bo → dao → repository`:
- os controllers recebem e devolvem só DTOs;
- as regras de negócio ficam nos BOs;
- o acesso a dados fica nos DAOs e repositories (Spring Data JPA).

Os erros são tratados num único `GlobalExceptionHandler`, que responde no formato `ProblemDetail` com mensagens em português.

A segurança usa login com sessão e token CSRF nas operações que alteram dados. As rotas que precisam funcionar na emergência ficam abertas, sem login: hoje, o envio de leituras do sensor.

As próximas etapas, já desenhadas no backlog, são:
- o alerta com atualização a cada 15 minutos (RF05);
- a rota até o abrigo, calculada pelo serviço Python da equipe (RF07);
- o check-in "Estou seguro" (RF09);
- o SMS simulado (RF12);
- o mapa interativo em React (RF14).

## Justificativa

Quando chove forte na Zona Leste de São Paulo e no Alto Tietê, o alerta de risco chega a todos, mas não diz para onde ir nem como chegar sem passar por rua alagada. E, depois que o alerta sai, ninguém sabe quem conseguiu sair e quem ficou para trás. Para quem não consegue sair sozinho, como uma pessoa idosa, acamada ou em cadeira de rodas, esse é o maior perigo.

A API resolve a base desse problema: guarda **onde** está o risco (áreas e sensores), **quanto** risco há agora (leituras e nível calculado), **para onde ir** (abrigos com vagas livres) e **quem precisa de ajuda** (pessoas assistidas e responsáveis).

Algumas escolhas técnicas seguem diretamente o problema:
- **Nível de risco calculado na hora,** e não gravado: o mapa sempre mostra a situação atual, pela pior leitura recente da área.
- **Leituras sem login:** na emergência, nada pode depender de cadastro ou senha. O risco dessa escolha, aceito para o MVP, está registrado em `docs/decisions.md`.
- **Dados das pessoas assistidas só com login:** são pessoas vulneráveis, e seus dados pessoais ficam protegidos, mesmo sendo fictícios nos exemplos (LGPD).
- **Vagas livres calculadas pela API:** a rota (RN06) precisa saber quais abrigos ainda têm vaga.

## Objetivos

**Objetivo geral:** oferecer uma API confiável, publicada e documentada, que concentre os dados de risco, abrigos e pessoas assistidas da Rota Segura e sirva o front e o serviço de rota da equipe.

**Objetivos específicos:**

1. Permitir o cadastro completo (GET, POST, PUT e DELETE) de áreas de risco, sensores, abrigos e pessoas assistidas com seus responsáveis.
2. Receber as leituras dos sensores sem login e recusar leituras inválidas: sensor desconhecido ou inativo, hora no futuro ou leitura repetida.
3. Calcular o nível de risco atual de cada área pelas regras de negócio (RN01), para o mapa do front.
4. Garantir as regras de negócio no BO e no banco, por exemplo:
   - nome de área único por cidade;
   - sensor com leituras só pode ser inativado, não excluído;
   - vagas ocupadas nunca passam da capacidade do abrigo;
   - toda pessoa assistida tem ao menos um responsável e uma área.
5. Responder todo erro num formato único (`ProblemDetail`), em português, com o status HTTP correto (400, 401, 403, 404, 409).
6. Proteger as operações que alteram dados com login e token CSRF, e os dados pessoais com login até na leitura.
7. Publicar a API no Render, com o banco Oracle da FIAP e a documentação interativa no Swagger UI.
8. Garantir a qualidade com testes automatizados: JUnit 5 e Mockito nos BOs, MockMvc nos controllers e `@DataJpaTest` nos repositories, com 264 testes passando.
