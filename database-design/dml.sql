-- ============================================================
-- Rota Segura - Global Solution FIAP 2026.2 (CitizenTech)
-- DML: dados de exemplo (rodar depois do ddl.sql)
--
-- DADOS SIMULADOS. Os nomes das áreas são fictícios e as
-- coordenadas e os raios são aproximados e ilustrativos: indicam
-- a região de cada localidade do território do MVP, mas NÃO são
-- um mapeamento oficial de áreas de risco. Nenhum dado pessoal.
--
-- Os INSERTs não informam o ID: a coluna é GENERATED ALWAYS AS
-- IDENTITY, então quem numera é o Oracle.
--
-- Arquivo em UTF-8 (os textos têm acentos). No SQL Developer:
-- Ferramentas > Preferências > Ambiente > Codificação = UTF-8,
-- e rodar com "Executar Script" (F5).
--
-- Rodar uma vez, com a tabela vazia: o nome de cada área é único
-- por cidade, então uma segunda execução falha com ORA-00001.
-- Para rodar de novo, descomente e rode antes:
-- DELETE FROM T_RS_AREA;
-- ============================================================

-- ------------------------------------------------------------
-- T_RS_AREA: 10 áreas em 10 localidades do território do MVP
-- (5 da Zona Leste de São Paulo, Guarulhos e 4 cidades do Alto Tietê).
-- ------------------------------------------------------------

-- Zona Leste de São Paulo
INSERT INTO T_RS_AREA (NAME, CITY, DISTRICT, RISK_TYPE, LATITUDE, LONGITUDE, RADIUS_M)
VALUES ('Várzea do Jardim Helena', 'São Paulo', 'Jardim Helena', 'FLOOD', -23.482000, -46.415000, 600);

INSERT INTO T_RS_AREA (NAME, CITY, DISTRICT, RISK_TYPE, LATITUDE, LONGITUDE, RADIUS_M)
VALUES ('Beira-Rio do Jardim Pantanal', 'São Paulo', 'Jardim Pantanal', 'FLOOD', -23.477000, -46.398000, 800);

INSERT INTO T_RS_AREA (NAME, CITY, DISTRICT, RISK_TYPE, LATITUDE, LONGITUDE, RADIUS_M)
VALUES ('Baixada de São Miguel', 'São Paulo', 'São Miguel Paulista', 'FLOOD', -23.493000, -46.444000, 500);

INSERT INTO T_RS_AREA (NAME, CITY, DISTRICT, RISK_TYPE, LATITUDE, LONGITUDE, RADIUS_M)
VALUES ('Córrego do Itaim Paulista', 'São Paulo', 'Itaim Paulista', 'FLOOD', -23.502000, -46.400000, 450);

INSERT INTO T_RS_AREA (NAME, CITY, DISTRICT, RISK_TYPE, LATITUDE, LONGITUDE, RADIUS_M)
VALUES ('Várzea do Itaim Biacica', 'São Paulo', 'Itaim Biacica', 'FLOOD', -23.489000, -46.385000, 700);

-- Guarulhos
INSERT INTO T_RS_AREA (NAME, CITY, DISTRICT, RISK_TYPE, LATITUDE, LONGITUDE, RADIUS_M)
VALUES ('Baixada Leste de Guarulhos', 'Guarulhos', 'Pimentas', 'FLOOD', -23.455000, -46.420000, 650);

-- Alto Tietê
INSERT INTO T_RS_AREA (NAME, CITY, DISTRICT, RISK_TYPE, LATITUDE, LONGITUDE, RADIUS_M)
VALUES ('Encosta Norte de Mogi', 'Mogi das Cruzes', 'Centro', 'LANDSLIDE', -23.515000, -46.185000, 400);

INSERT INTO T_RS_AREA (NAME, CITY, DISTRICT, RISK_TYPE, LATITUDE, LONGITUDE, RADIUS_M)
VALUES ('Várzea do Tietê em Suzano', 'Suzano', 'Centro', 'FLOOD', -23.535000, -46.311000, 550);

INSERT INTO T_RS_AREA (NAME, CITY, DISTRICT, RISK_TYPE, LATITUDE, LONGITUDE, RADIUS_M)
VALUES ('Baixada de Itaquaquecetuba', 'Itaquaquecetuba', 'Centro', 'FLOOD', -23.487000, -46.349000, 500);

-- Sem bairro: DISTRICT é opcional
INSERT INTO T_RS_AREA (NAME, CITY, DISTRICT, RISK_TYPE, LATITUDE, LONGITUDE, RADIUS_M)
VALUES ('Morro de Poá', 'Poá', NULL, 'LANDSLIDE', -23.528000, -46.345000, 300);

COMMIT;

-- Conferência: devem vir 10 linhas, com os acentos corretos.
-- SELECT ID_AREA, NAME, CITY, DISTRICT, RISK_TYPE FROM T_RS_AREA ORDER BY ID_AREA;
