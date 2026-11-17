-- ============================================================
-- Rota Segura - Global Solution FIAP 2026.2 (CitizenTech)
-- DDL: criação das tabelas no Oracle
--
-- Espelha as entidades JPA de domain-driven-design-java/backend
-- (pacote br.com.rotasegura.model). A API sobe com ddl-auto=validate:
-- ela só confere estas tabelas, não cria nem altera nada.
--
-- Padrões:
--   tabelas      T_RS_<NOME>
--   chave        ID_<ENTIDADE>, gerada pelo banco (IDENTITY)
--   constraints  PK_RS_, FK_RS_, NN_RS_, CK_RS_ + tabela (+ coluna)
--   índice único UX_RS_ + tabela + colunas
--   índice comum IX_RS_ + tabela + tabela referenciada; toda chave estrangeira tem
--   um índice que começa por ela (na READING, é o próprio UX_RS_READING_SENSOR_TIME)
--   T_RS_SENSOR_READING aparece como READING e T_RS_ASSISTED_PERSON como ASSISTED
--   nesses nomes, para caber em 30 caracteres (o NOT NULL de T_RS_GUARDIAN.ID_ASSISTED_PERSON
--   vira NN_RS_GUARDIAN_ASSISTED pelo mesmo motivo)
--   texto        VARCHAR2(n CHAR): o tamanho conta caracteres, não bytes
--
-- Arquivo em UTF-8. No SQL Developer, rodar com "Executar Script" (F5).
-- ============================================================

-- Para recriar do zero, descomente e rode antes dos CREATE
-- (T_RS_SHELTER é independente; as demais, da tabela filha para a pai):
-- DROP TABLE T_RS_GUARDIAN CASCADE CONSTRAINTS;
-- DROP TABLE T_RS_ASSISTED_PERSON CASCADE CONSTRAINTS;
-- DROP TABLE T_RS_SHELTER CASCADE CONSTRAINTS;
-- DROP TABLE T_RS_SENSOR_READING CASCADE CONSTRAINTS;
-- DROP TABLE T_RS_SENSOR CASCADE CONSTRAINTS;
-- DROP TABLE T_RS_AREA CASCADE CONSTRAINTS;


-- ============================================================
-- T_RS_AREA: áreas de risco de alagamento ou deslizamento (RF01).
-- Cada área é um círculo no mapa: ponto central (latitude e
-- longitude) e raio em metros.
-- ============================================================
CREATE TABLE T_RS_AREA (
    ID_AREA    NUMBER(19)         GENERATED ALWAYS AS IDENTITY,
    NAME       VARCHAR2(100 CHAR) CONSTRAINT NN_RS_AREA_NAME      NOT NULL,
    CITY       VARCHAR2(60 CHAR)  CONSTRAINT NN_RS_AREA_CITY      NOT NULL,
    DISTRICT   VARCHAR2(60 CHAR),
    RISK_TYPE  VARCHAR2(20 CHAR)  CONSTRAINT NN_RS_AREA_RISK_TYPE NOT NULL,
    LATITUDE   NUMBER(9,6)        CONSTRAINT NN_RS_AREA_LATITUDE  NOT NULL,
    LONGITUDE  NUMBER(9,6)        CONSTRAINT NN_RS_AREA_LONGITUDE NOT NULL,
    RADIUS_M   NUMBER(10)         CONSTRAINT NN_RS_AREA_RADIUS_M  NOT NULL,
    CONSTRAINT PK_RS_AREA           PRIMARY KEY (ID_AREA),
    CONSTRAINT CK_RS_AREA_RISK_TYPE CHECK (RISK_TYPE IN ('FLOOD', 'LANDSLIDE')),
    CONSTRAINT CK_RS_AREA_LATITUDE  CHECK (LATITUDE BETWEEN -90 AND 90),
    CONSTRAINT CK_RS_AREA_LONGITUDE CHECK (LONGITUDE BETWEEN -180 AND 180),
    -- Maior que zero e até 2147483647, o maior valor que o Integer do Java consegue ler
    CONSTRAINT CK_RS_AREA_RADIUS_M  CHECK (RADIUS_M BETWEEN 1 AND 2147483647)
);

-- Nome único por cidade, sem diferenciar maiúsculas de minúsculas.
-- Uma constraint UNIQUE comum diferenciaria "Várzea" de "VÁRZEA";
-- por isso a regra é um índice único sobre UPPER(...).
CREATE UNIQUE INDEX UX_RS_AREA_NAME_CITY
    ON T_RS_AREA (UPPER(NAME), UPPER(CITY));


-- ============================================================
-- T_RS_SENSOR: sensores de rua, que medem chuva e nível d'água (RF02).
-- Cada sensor pertence a uma área de risco. Criada depois de
-- T_RS_AREA, porque a chave estrangeira aponta para ela.
-- ============================================================
CREATE TABLE T_RS_SENSOR (
    ID_SENSOR  NUMBER(19)         GENERATED ALWAYS AS IDENTITY,
    ID_AREA    NUMBER(19)         CONSTRAINT NN_RS_SENSOR_ID_AREA   NOT NULL,
    CODE       VARCHAR2(30 CHAR)  CONSTRAINT NN_RS_SENSOR_CODE      NOT NULL,
    LATITUDE   NUMBER(9,6)        CONSTRAINT NN_RS_SENSOR_LATITUDE  NOT NULL,
    LONGITUDE  NUMBER(9,6)        CONSTRAINT NN_RS_SENSOR_LONGITUDE NOT NULL,
    STATUS     VARCHAR2(20 CHAR)  CONSTRAINT NN_RS_SENSOR_STATUS    NOT NULL,
    CONSTRAINT PK_RS_SENSOR           PRIMARY KEY (ID_SENSOR),
    -- Sem ON DELETE CASCADE: o banco recusa excluir uma área que ainda tem sensores
    CONSTRAINT FK_RS_SENSOR_AREA      FOREIGN KEY (ID_AREA) REFERENCES T_RS_AREA (ID_AREA),
    CONSTRAINT CK_RS_SENSOR_STATUS    CHECK (STATUS IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT CK_RS_SENSOR_LATITUDE  CHECK (LATITUDE BETWEEN -90 AND 90),
    CONSTRAINT CK_RS_SENSOR_LONGITUDE CHECK (LONGITUDE BETWEEN -180 AND 180)
);

-- Código único, sem diferenciar maiúsculas de minúsculas.
CREATE UNIQUE INDEX UX_RS_SENSOR_CODE
    ON T_RS_SENSOR (UPPER(CODE));

-- Índice da chave estrangeira: acelera listar os sensores de uma área
-- e a checagem que o banco faz ao excluir uma área.
CREATE INDEX IX_RS_SENSOR_AREA
    ON T_RS_SENSOR (ID_AREA);


-- ============================================================
-- T_RS_SENSOR_READING: leituras dos sensores (RF03), isto é, a
-- chuva em mm e o nível d'água em cm medidos num instante.
-- É histórico: a API só grava e consulta, nunca altera.
-- Criada depois de T_RS_SENSOR, porque a chave estrangeira aponta
-- para ela.
-- ============================================================
CREATE TABLE T_RS_SENSOR_READING (
    ID_SENSOR_READING NUMBER(19)                  GENERATED ALWAYS AS IDENTITY,
    ID_SENSOR         NUMBER(19)                  CONSTRAINT NN_RS_READING_ID_SENSOR      NOT NULL,
    RAIN_MM           NUMBER(6,2)                 CONSTRAINT NN_RS_READING_RAIN_MM        NOT NULL,
    WATER_LEVEL_CM    NUMBER(7,2)                 CONSTRAINT NN_RS_READING_WATER_LEVEL_CM NOT NULL,
    MEASURED_AT       TIMESTAMP(6) WITH TIME ZONE CONSTRAINT NN_RS_READING_MEASURED_AT    NOT NULL,
    CONSTRAINT PK_RS_READING                PRIMARY KEY (ID_SENSOR_READING),
    -- Sem ON DELETE CASCADE: o banco recusa excluir um sensor que já tem leituras
    CONSTRAINT FK_RS_READING_SENSOR         FOREIGN KEY (ID_SENSOR) REFERENCES T_RS_SENSOR (ID_SENSOR),
    CONSTRAINT CK_RS_READING_RAIN_MM        CHECK (RAIN_MM >= 0),
    CONSTRAINT CK_RS_READING_WATER_LEVEL_CM CHECK (WATER_LEVEL_CM >= 0)
);

-- Um sensor não tem duas leituras no mesmo instante.
-- O Oracle não aceita constraint UNIQUE numa coluna TIMESTAMP WITH TIME
-- ZONE (ORA-02329); por isso a regra é um índice único, sobre o instante
-- convertido para UTC (assim 18:00 -03:00 e 21:00 +00:00 contam igual).
-- Como começa por ID_SENSOR, este índice também serve à chave
-- estrangeira e à busca das leituras de cada sensor.
CREATE UNIQUE INDEX UX_RS_READING_SENSOR_TIME
    ON T_RS_SENSOR_READING (ID_SENSOR, SYS_EXTRACT_UTC(MEASURED_AT));


-- ============================================================
-- T_RS_SHELTER: abrigos (RF06), com endereço, posição no mapa e
-- capacidade. As vagas livres (capacidade - ocupadas) são o que a
-- rota (RN06) usa para indicar o abrigo mais próximo com vaga.
-- Não depende de nenhuma outra tabela.
-- ============================================================
CREATE TABLE T_RS_SHELTER (
    ID_SHELTER      NUMBER(19)         GENERATED ALWAYS AS IDENTITY,
    NAME            VARCHAR2(150 CHAR) CONSTRAINT NN_RS_SHELTER_NAME           NOT NULL,
    ADDRESS         VARCHAR2(200 CHAR) CONSTRAINT NN_RS_SHELTER_ADDRESS        NOT NULL,
    LATITUDE        NUMBER(9,6)        CONSTRAINT NN_RS_SHELTER_LATITUDE       NOT NULL,
    LONGITUDE       NUMBER(9,6)        CONSTRAINT NN_RS_SHELTER_LONGITUDE      NOT NULL,
    CAPACITY        NUMBER(10)         CONSTRAINT NN_RS_SHELTER_CAPACITY       NOT NULL,
    OCCUPIED_SPOTS  NUMBER(10)         CONSTRAINT NN_RS_SHELTER_OCCUPIED_SPOTS NOT NULL,
    CONSTRAINT PK_RS_SHELTER                PRIMARY KEY (ID_SHELTER),
    CONSTRAINT CK_RS_SHELTER_LATITUDE       CHECK (LATITUDE BETWEEN -90 AND 90),
    CONSTRAINT CK_RS_SHELTER_LONGITUDE      CHECK (LONGITUDE BETWEEN -180 AND 180),
    -- Maior que zero e até 2147483647, o maior valor que o Integer do Java consegue ler
    CONSTRAINT CK_RS_SHELTER_CAPACITY       CHECK (CAPACITY BETWEEN 1 AND 2147483647),
    -- Ninguém a mais do que cabe: de 0 até a capacidade
    CONSTRAINT CK_RS_SHELTER_OCCUPIED_SPOTS CHECK (OCCUPIED_SPOTS BETWEEN 0 AND CAPACITY)
);


-- ============================================================
-- T_RS_ASSISTED_PERSON: pessoas assistidas (RF08), quem não consegue
-- sair sozinha numa emergência (idosas, acamadas, cadeirantes).
-- Toda pessoa assistida mora numa área de risco (RN03). A latitude e
-- a longitude são de onde ela mora: a origem da rota (RF07).
-- Criada depois de T_RS_AREA, porque a chave estrangeira aponta para ela.
-- ============================================================
CREATE TABLE T_RS_ASSISTED_PERSON (
    ID_ASSISTED_PERSON NUMBER(19)         GENERATED ALWAYS AS IDENTITY,
    ID_AREA            NUMBER(19)         CONSTRAINT NN_RS_ASSISTED_ID_AREA      NOT NULL,
    NAME               VARCHAR2(150 CHAR) CONSTRAINT NN_RS_ASSISTED_NAME         NOT NULL,
    PHONE              VARCHAR2(20 CHAR)  CONSTRAINT NN_RS_ASSISTED_PHONE        NOT NULL,
    LATITUDE           NUMBER(9,6)        CONSTRAINT NN_RS_ASSISTED_LATITUDE     NOT NULL,
    LONGITUDE          NUMBER(9,6)        CONSTRAINT NN_RS_ASSISTED_LONGITUDE    NOT NULL,
    SPECIAL_NEED       VARCHAR2(20 CHAR)  CONSTRAINT NN_RS_ASSISTED_SPECIAL_NEED NOT NULL,
    NOTES              VARCHAR2(500 CHAR),
    CONSTRAINT PK_RS_ASSISTED              PRIMARY KEY (ID_ASSISTED_PERSON),
    -- Sem ON DELETE CASCADE: o banco recusa excluir uma área que ainda tem pessoas assistidas
    CONSTRAINT FK_RS_ASSISTED_AREA         FOREIGN KEY (ID_AREA) REFERENCES T_RS_AREA (ID_AREA),
    CONSTRAINT CK_RS_ASSISTED_LATITUDE     CHECK (LATITUDE BETWEEN -90 AND 90),
    CONSTRAINT CK_RS_ASSISTED_LONGITUDE    CHECK (LONGITUDE BETWEEN -180 AND 180),
    CONSTRAINT CK_RS_ASSISTED_SPECIAL_NEED CHECK (SPECIAL_NEED IN ('ELDERLY', 'BEDRIDDEN', 'WHEELCHAIR', 'OTHER'))
);

-- Índice da chave estrangeira: acelera listar as pessoas de uma área
-- (o alerta do RF05 vai precisar) e a checagem ao excluir uma área.
CREATE INDEX IX_RS_ASSISTED_AREA
    ON T_RS_ASSISTED_PERSON (ID_AREA);


-- ============================================================
-- T_RS_GUARDIAN: responsáveis pelas pessoas assistidas (RF08, RN03).
-- Cada responsável pertence a uma pessoa assistida (1:N).
-- Criada depois de T_RS_ASSISTED_PERSON, porque a chave estrangeira
-- aponta para ela.
-- ============================================================
CREATE TABLE T_RS_GUARDIAN (
    ID_GUARDIAN        NUMBER(19)         GENERATED ALWAYS AS IDENTITY,
    ID_ASSISTED_PERSON NUMBER(19)         CONSTRAINT NN_RS_GUARDIAN_ASSISTED     NOT NULL,
    NAME               VARCHAR2(150 CHAR) CONSTRAINT NN_RS_GUARDIAN_NAME         NOT NULL,
    PHONE              VARCHAR2(20 CHAR)  CONSTRAINT NN_RS_GUARDIAN_PHONE        NOT NULL,
    RELATIONSHIP       VARCHAR2(50 CHAR)  CONSTRAINT NN_RS_GUARDIAN_RELATIONSHIP NOT NULL,
    CONSTRAINT PK_RS_GUARDIAN          PRIMARY KEY (ID_GUARDIAN),
    -- ON DELETE CASCADE: o responsável só existe junto da pessoa assistida dele;
    -- excluir a pessoa exclui os responsáveis (a API faz o mesmo pelo JPA)
    CONSTRAINT FK_RS_GUARDIAN_ASSISTED FOREIGN KEY (ID_ASSISTED_PERSON)
        REFERENCES T_RS_ASSISTED_PERSON (ID_ASSISTED_PERSON) ON DELETE CASCADE
);

-- Índice da chave estrangeira: acelera buscar os responsáveis de uma pessoa.
CREATE INDEX IX_RS_GUARDIAN_ASSISTED
    ON T_RS_GUARDIAN (ID_ASSISTED_PERSON);
