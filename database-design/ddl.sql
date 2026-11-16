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
--   T_RS_SENSOR_READING aparece como READING nesses nomes, para caber em 30 caracteres
--   texto        VARCHAR2(n CHAR): o tamanho conta caracteres, não bytes
--
-- Arquivo em UTF-8. No SQL Developer, rodar com "Executar Script" (F5).
-- ============================================================

-- Para recriar do zero, descomente e rode antes dos CREATE
-- (primeiro a tabela filha, depois a tabela pai):
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
