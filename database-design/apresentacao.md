# Rota Segura — Database Design

Texto da apresentação do PDF de Database Design: capa, sumário, objetivo e escopo, e funcionalidades. Os outros itens do PDF estão no roteiro, no fim deste arquivo.

## Capa

- **Solução:** Rota Segura
- **Equipe:** Rota Segura
- **Disciplina:** Database Design — Global Solution FIAP 2026.2 (CitizenTech)
- **Integrantes (em ordem alfabética):**
  - João Paulo Santana Basta — RM 565383
  - Kelwin Silva Bastos — RM 566348
  - Vitor Mantovani Camilo — RM 556537

## Sumário

1. Objetivo e escopo
2. Funcionalidades
3. Modelo lógico (Oracle Data Modeler)
4. Script DDL
5. Script DML
6. Script DQL

## 1. Objetivo e escopo

**Objetivo:** guardar, no banco Oracle, os dados de que a Rota Segura precisa para transformar o alerta de chuva em ação:
- onde estão as áreas de risco;
- o que os sensores de rua medem;
- para onde as pessoas podem ir (abrigos com vagas);
- quem não consegue sair sozinho e quem avisar (pessoas assistidas e responsáveis).

**Escopo:** o modelo tem 6 tabelas, todas com o prefixo `T_RS_` e chave primária gerada pelo próprio Oracle (`IDENTITY`):

| Tabela | O que guarda |
|---|---|
| `T_RS_AREA` | Áreas de risco de alagamento ou deslizamento: nome, cidade, bairro, tipo, ponto central e raio |
| `T_RS_SENSOR` | Sensores de rua, cada um numa área, com código único e situação (ativo ou inativo) |
| `T_RS_SENSOR_READING` | Leituras dos sensores: chuva (mm), nível d'água (cm) e hora da medição |
| `T_RS_SHELTER` | Abrigos: endereço, posição, capacidade e vagas ocupadas |
| `T_RS_ASSISTED_PERSON` | Pessoas que não conseguem sair sozinhas: área, onde moram, telefone e necessidade especial |
| `T_RS_GUARDIAN` | Responsáveis por cada pessoa assistida (uma pessoa tem um ou mais) |

**Fora deste escopo:** os alertas, os check-ins e as mensagens SMS ficam para a próxima etapa do projeto e ainda não têm tabela. O nível de risco de cada área não é gravado: a API calcula na hora, a partir das leituras.

**Dados:** todos os dados de exemplo são simulados, e nenhum é de pessoa real. Os telefones usam o DDD `00`, que não existe. Nomes, endereços e posições dos abrigos vêm do OpenStreetMap (© colaboradores do OpenStreetMap, licença ODbL); a capacidade e a ocupação são simuladas.

## 2. Funcionalidades

O que o banco permite fazer, e as regras que ele mesmo garante:

1. **Cadastrar áreas de risco.** O nome não se repete na mesma cidade, sem diferenciar maiúsculas: índice único em `UPPER(NAME), UPPER(CITY)`. O tipo é `FLOOD` ou `LANDSLIDE` (`CHECK`).
2. **Vincular sensores às áreas.** A chave estrangeira impede sensor em área que não existe, e uma área com sensores não pode ser excluída (sem cascata). O código do sensor é único.
3. **Registrar as leituras dos sensores.** Chuva e nível d'água nunca são negativos (`CHECK`). Um sensor não tem duas leituras no mesmo instante: índice único sobre a hora em UTC. Um sensor com leituras não pode ser excluído, só inativado.
4. **Cadastrar abrigos.** As vagas ocupadas ficam entre 0 e a capacidade (`CHECK`).
5. **Cadastrar pessoas assistidas e seus responsáveis.** Toda pessoa tem uma área. Ao excluir a pessoa, os responsáveis dela também são excluídos (`ON DELETE CASCADE`). A necessidade especial é uma de quatro opções (`CHECK`).
6. **Consultar.** O `dql.sql` responde a três perguntas do negócio:
   - em que área está cada sensor (INNER JOIN);
   - quantos sensores ativos tem cada área (INNER JOIN com `COUNT` e `GROUP BY`);
   - quais áreas de alagamento ainda não têm sensor (LEFT OUTER JOIN com `WHERE` e `ORDER BY`).

Toda chave estrangeira tem um índice, para a busca e a exclusão continuarem rápidas quando o banco crescer.

---

## Roteiro do PDF

| Nº | Seção | Item da rubrica | Pontos | De onde vem |
|---|---|---|---|---|
| — | Capa, sumário, objetivo e escopo, funcionalidades | Apresentação | 10 | este arquivo |
| 3 | Modelo lógico | Modelo no Oracle Data Modeler | 20 | imagem exportada do Data Modeler (passo a passo abaixo). **Nada de link para diagrama em nuvem** |
| 4 | DDL | Script DDL | 20 | `ddl.sql` (o arquivo vai junto; no PDF, um trecho ou o script inteiro) |
| 5 | DML | Script DML, no mínimo 5 linhas por tabela | 20 | `dml.sql`: 10 áreas, 8 sensores, 12 leituras, 6 abrigos, 6 pessoas assistidas e 8 responsáveis |
| 6 | DQL | Três consultas | 30 | `dql.sql`: cada consulta com a pergunta, o SQL e um print do resultado no SQL Developer |

### Modelo lógico no Oracle Data Modeler (você faz na ferramenta)

1. Abra o **Oracle SQL Developer Data Modeler**.
2. **File > Import > DDL File**, escolha `database-design/ddl.sql` e selecione o banco **Oracle Database 12c** ou superior. Confirme a importação: o modelo **relacional** aparece com as 6 tabelas.
   - Se aparecer aviso sobre os índices com `UPPER(...)` ou `SYS_EXTRACT_UTC(...)`, pode seguir: as tabelas, as PKs e as FKs entram normalmente.
3. Com o modelo relacional aberto, clique em **Engineer to Logical Model** (o botão com as setas `<<`) e confirme. Assim nasce o modelo **lógico**, com entidades, atributos, identificadores e relacionamentos.
4. Abra o diagrama do modelo lógico e confira:
   - as 6 entidades e as chaves primárias;
   - os 4 relacionamentos: área–sensor, sensor–leitura, área–pessoa assistida, pessoa assistida–responsável;
   - a cardinalidade 1:N de cada um.

   O `der.md` serve de gabarito.
5. Organize o desenho (**Layout > Auto Layout** ajuda) e exporte a imagem: **File > Print Diagram > To Image File** (PNG).
6. Salve o projeto (**File > Save As**) em `database-design/modelo/` e coloque a imagem no PDF.

**Lembrete:** para rodar os scripts no Oracle da FIAP, suspenda o serviço do Render antes. A conta aceita uma sessão só.
