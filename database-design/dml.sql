-- ============================================================
-- Rota Segura - Global Solution FIAP 2026.2 (CitizenTech)
-- DML: dados de exemplo (rodar depois do ddl.sql)
--
-- DADOS SIMULADOS. Os nomes das áreas são fictícios e as
-- coordenadas e os raios são aproximados e ilustrativos: indicam
-- a região de cada localidade do território do MVP, mas NÃO são
-- um mapeamento oficial de áreas de risco. Nenhum dado pessoal real:
-- pessoas assistidas e responsáveis são fictícios.
-- Exceção: nome, endereço e coordenadas dos abrigos são dados
-- públicos do OpenStreetMap (ver a seção T_RS_SHELTER); a capacidade
-- e a ocupação deles são simuladas.
--
-- Os INSERTs não informam o ID: a coluna é GENERATED ALWAYS AS
-- IDENTITY, então quem numera é o Oracle. Por isso cada sensor
-- acha o ID da sua área por uma subconsulta (nome + cidade), e cada
-- leitura acha o ID do seu sensor pelo código.
--
-- Arquivo em UTF-8 (os textos têm acentos). No SQL Developer:
-- Ferramentas > Preferências > Ambiente > Codificação = UTF-8,
-- e rodar com "Executar Script" (F5).
--
-- Rodar uma vez, com as tabelas vazias: o nome de cada área é único
-- por cidade, o código de cada sensor é único e cada sensor tem só
-- uma leitura por instante, então uma segunda execução falha com
-- ORA-00001 nessas tabelas. Os abrigos não têm nome único: numa segunda
-- execução eles seriam gravados de novo, duplicados. As pessoas assistidas
-- também não: elas seriam duplicadas, e os INSERTs dos responsáveis
-- falhariam com ORA-01427 (a subconsulta pelo nome devolveria 2 linhas),
-- deixando cópias sem responsável. Para rodar de
-- novo, descomente e rode antes (das tabelas filhas para as tabelas pai):
-- DELETE FROM T_RS_GUARDIAN;
-- DELETE FROM T_RS_ASSISTED_PERSON;
-- DELETE FROM T_RS_SHELTER;
-- DELETE FROM T_RS_SENSOR_READING;
-- DELETE FROM T_RS_SENSOR;
-- DELETE FROM T_RS_AREA;
-- ============================================================

-- ============================================================
-- T_RS_AREA: 10 áreas em 10 localidades do território do MVP
-- (5 da Zona Leste de São Paulo, Guarulhos e 4 cidades do Alto Tietê).
-- ============================================================

-- Zona Leste de São Paulo
INSERT INTO T_RS_AREA (NAME, CITY, DISTRICT, RISK_TYPE, LATITUDE, LONGITUDE, RADIUS_M)
VALUES ('Várzea do Jardim Helena', 'São Paulo', 'Jardim Helena', 'FLOOD', -23.482000, -46.415000, 550);

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


-- ============================================================
-- T_RS_SENSOR: 8 sensores em 6 das 10 áreas (códigos fictícios).
-- Quatro áreas ficam sem sensor de propósito, e dois sensores estão
-- INACTIVE, para as consultas do dql.sql terem o que mostrar.
-- ============================================================

INSERT INTO T_RS_SENSOR (ID_AREA, CODE, LATITUDE, LONGITUDE, STATUS)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Várzea do Jardim Helena' AND CITY = 'São Paulo'),
        'SEN-JH-001', -23.481500, -46.414200, 'ACTIVE');

INSERT INTO T_RS_SENSOR (ID_AREA, CODE, LATITUDE, LONGITUDE, STATUS)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Várzea do Jardim Helena' AND CITY = 'São Paulo'),
        'SEN-JH-002', -23.483100, -46.416300, 'ACTIVE');

INSERT INTO T_RS_SENSOR (ID_AREA, CODE, LATITUDE, LONGITUDE, STATUS)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Beira-Rio do Jardim Pantanal' AND CITY = 'São Paulo'),
        'SEN-JP-001', -23.476400, -46.397100, 'ACTIVE');

INSERT INTO T_RS_SENSOR (ID_AREA, CODE, LATITUDE, LONGITUDE, STATUS)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Beira-Rio do Jardim Pantanal' AND CITY = 'São Paulo'),
        'SEN-JP-002', -23.478200, -46.399500, 'INACTIVE');

INSERT INTO T_RS_SENSOR (ID_AREA, CODE, LATITUDE, LONGITUDE, STATUS)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Baixada de São Miguel' AND CITY = 'São Paulo'),
        'SEN-SM-001', -23.492600, -46.443500, 'ACTIVE');

INSERT INTO T_RS_SENSOR (ID_AREA, CODE, LATITUDE, LONGITUDE, STATUS)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Córrego do Itaim Paulista' AND CITY = 'São Paulo'),
        'SEN-IP-001', -23.501700, -46.400400, 'ACTIVE');

INSERT INTO T_RS_SENSOR (ID_AREA, CODE, LATITUDE, LONGITUDE, STATUS)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Baixada Leste de Guarulhos' AND CITY = 'Guarulhos'),
        'SEN-GRU-001', -23.455300, -46.419600, 'ACTIVE');

INSERT INTO T_RS_SENSOR (ID_AREA, CODE, LATITUDE, LONGITUDE, STATUS)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Várzea do Tietê em Suzano' AND CITY = 'Suzano'),
        'SEN-SUZ-001', -23.534600, -46.311400, 'INACTIVE');


-- ============================================================
-- T_RS_SENSOR_READING: 12 leituras simuladas. Onze são de uma tarde
-- de chuva (15/11/2026, das 18:00 às 18:30 no horário de Brasília);
-- uma é antiga (10/11). Os valores são ilustrativos: não vêm de
-- medição real.
-- As horas estão em UTC (+00:00), como a API grava: 18:00 em
-- Brasília (-03:00) é 21:00 em UTC.
-- ============================================================

-- Jardim Helena: a água sobe ao longo da tarde
INSERT INTO T_RS_SENSOR_READING (ID_SENSOR, RAIN_MM, WATER_LEVEL_CM, MEASURED_AT)
VALUES ((SELECT ID_SENSOR FROM T_RS_SENSOR WHERE CODE = 'SEN-JH-001'),
        5.20, 0.80, TIMESTAMP '2026-11-15 21:00:00 +00:00');

INSERT INTO T_RS_SENSOR_READING (ID_SENSOR, RAIN_MM, WATER_LEVEL_CM, MEASURED_AT)
VALUES ((SELECT ID_SENSOR FROM T_RS_SENSOR WHERE CODE = 'SEN-JH-001'),
        12.80, 2.40, TIMESTAMP '2026-11-15 21:15:00 +00:00');

INSERT INTO T_RS_SENSOR_READING (ID_SENSOR, RAIN_MM, WATER_LEVEL_CM, MEASURED_AT)
VALUES ((SELECT ID_SENSOR FROM T_RS_SENSOR WHERE CODE = 'SEN-JH-001'),
        21.40, 4.60, TIMESTAMP '2026-11-15 21:30:00 +00:00');

INSERT INTO T_RS_SENSOR_READING (ID_SENSOR, RAIN_MM, WATER_LEVEL_CM, MEASURED_AT)
VALUES ((SELECT ID_SENSOR FROM T_RS_SENSOR WHERE CODE = 'SEN-JH-002'),
        4.90, 0.60, TIMESTAMP '2026-11-15 21:00:00 +00:00');

INSERT INTO T_RS_SENSOR_READING (ID_SENSOR, RAIN_MM, WATER_LEVEL_CM, MEASURED_AT)
VALUES ((SELECT ID_SENSOR FROM T_RS_SENSOR WHERE CODE = 'SEN-JH-002'),
        11.70, 1.90, TIMESTAMP '2026-11-15 21:15:00 +00:00');

-- Jardim Pantanal
INSERT INTO T_RS_SENSOR_READING (ID_SENSOR, RAIN_MM, WATER_LEVEL_CM, MEASURED_AT)
VALUES ((SELECT ID_SENSOR FROM T_RS_SENSOR WHERE CODE = 'SEN-JP-001'),
        8.30, 3.20, TIMESTAMP '2026-11-15 21:00:00 +00:00');

INSERT INTO T_RS_SENSOR_READING (ID_SENSOR, RAIN_MM, WATER_LEVEL_CM, MEASURED_AT)
VALUES ((SELECT ID_SENSOR FROM T_RS_SENSOR WHERE CODE = 'SEN-JP-001'),
        15.60, 5.80, TIMESTAMP '2026-11-15 21:15:00 +00:00');

-- Leitura antiga de um sensor hoje INACTIVE: o histórico continua guardado
INSERT INTO T_RS_SENSOR_READING (ID_SENSOR, RAIN_MM, WATER_LEVEL_CM, MEASURED_AT)
VALUES ((SELECT ID_SENSOR FROM T_RS_SENSOR WHERE CODE = 'SEN-JP-002'),
        2.10, 0.40, TIMESTAMP '2026-11-10 17:00:00 +00:00');

-- São Miguel, Itaim Paulista e Guarulhos
INSERT INTO T_RS_SENSOR_READING (ID_SENSOR, RAIN_MM, WATER_LEVEL_CM, MEASURED_AT)
VALUES ((SELECT ID_SENSOR FROM T_RS_SENSOR WHERE CODE = 'SEN-SM-001'),
        3.40, 0.20, TIMESTAMP '2026-11-15 21:00:00 +00:00');

INSERT INTO T_RS_SENSOR_READING (ID_SENSOR, RAIN_MM, WATER_LEVEL_CM, MEASURED_AT)
VALUES ((SELECT ID_SENSOR FROM T_RS_SENSOR WHERE CODE = 'SEN-IP-001'),
        6.80, 1.50, TIMESTAMP '2026-11-15 21:00:00 +00:00');

INSERT INTO T_RS_SENSOR_READING (ID_SENSOR, RAIN_MM, WATER_LEVEL_CM, MEASURED_AT)
VALUES ((SELECT ID_SENSOR FROM T_RS_SENSOR WHERE CODE = 'SEN-GRU-001'),
        18.90, 9.40, TIMESTAMP '2026-11-15 21:00:00 +00:00');

INSERT INTO T_RS_SENSOR_READING (ID_SENSOR, RAIN_MM, WATER_LEVEL_CM, MEASURED_AT)
VALUES ((SELECT ID_SENSOR FROM T_RS_SENSOR WHERE CODE = 'SEN-GRU-001'),
        27.50, 11.30, TIMESTAMP '2026-11-15 21:15:00 +00:00');

-- ============================================================
-- T_RS_SHELTER: 6 abrigos do bairro-piloto (Jardim Helena).
-- Fonte: © colaboradores do OpenStreetMap (licença ODbL), lista em
-- dynamic-programming/docs/abrigos_sugeridos.json (relação OSM
-- R3049447, gerada em 12/11/2026). Nomes, endereços
-- e coordenadas são do OSM; a CAPACIDADE e a OCUPAÇÃO são SIMULADAS.
-- Outros 4 abrigos da lista entram quando os endereços forem
-- completados (o OSM não traz endereço para eles).
-- Um abrigo está lotado, de propósito, para a rota (RN06) ter um
-- abrigo sem vaga para pular.
-- ============================================================

INSERT INTO T_RS_SHELTER (NAME, ADDRESS, LATITUDE, LONGITUDE, CAPACITY, OCCUPIED_SPOTS)
VALUES ('Centro Educacional Unificado Três Pontes', 'Rua Capachos, 400', -23.478226, -46.381098, 400, 0);

INSERT INTO T_RS_SHELTER (NAME, ADDRESS, LATITUDE, LONGITUDE, CAPACITY, OCCUPIED_SPOTS)
VALUES ('Escola Estadual Heckel Tavares', 'Avenida Doutor José Artur Nova, 2737', -23.474775, -46.424981, 200, 0);

INSERT INTO T_RS_SHELTER (NAME, ADDRESS, LATITUDE, LONGITUDE, CAPACITY, OCCUPIED_SPOTS)
VALUES ('Escola Municipal de Educação Infantil Professora Célia Ribeiro Landim', 'Praça Craveiro do Campo', -23.479068, -46.419903, 200, 0);

-- Lotado (simulado)
INSERT INTO T_RS_SHELTER (NAME, ADDRESS, LATITUDE, LONGITUDE, CAPACITY, OCCUPIED_SPOTS)
VALUES ('Escola Estadual Doutor Diogo de Faria', 'Rua Santa Rosa de Lima, 591', -23.486630, -46.428978, 200, 200);

INSERT INTO T_RS_SHELTER (NAME, ADDRESS, LATITUDE, LONGITUDE, CAPACITY, OCCUPIED_SPOTS)
VALUES ('Escola Estadual Professor José Bonifácio Andrada e Silva Jardim', 'Rua André Furtado de Mendonça, 473', -23.479673, -46.384704, 200, 35);

INSERT INTO T_RS_SHELTER (NAME, ADDRESS, LATITUDE, LONGITUDE, CAPACITY, OCCUPIED_SPOTS)
VALUES ('Escola Municipal de Educação Fundamental Murures', 'Rua dos Murures', -23.476716, -46.418575, 200, 0);

-- ============================================================
-- T_RS_ASSISTED_PERSON: 6 pessoas assistidas (RF08).
-- DADOS FICTÍCIOS: nomes inventados e telefones com DDD 00, que não
-- existe, para nenhum número ser de uma pessoa real. As posições são
-- ilustrativas (não são casas reais) e ficam dentro do raio da área de
-- cada pessoa. Cada pessoa acha a área pela subconsulta nome + cidade.
-- ============================================================

INSERT INTO T_RS_ASSISTED_PERSON (ID_AREA, NAME, PHONE, LATITUDE, LONGITUDE, SPECIAL_NEED, NOTES)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Várzea do Jardim Helena' AND CITY = 'São Paulo'),
        'Benedita Moraes Lima', '(00) 00000-0101', -23.480500, -46.413800, 'BEDRIDDEN', 'Acamada; precisa de maca para sair de casa');

INSERT INTO T_RS_ASSISTED_PERSON (ID_AREA, NAME, PHONE, LATITUDE, LONGITUDE, SPECIAL_NEED, NOTES)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Várzea do Jardim Helena' AND CITY = 'São Paulo'),
        'Joaquim Pereira Santos', '(00) 00000-0102', -23.483500, -46.416200, 'ELDERLY', '92 anos; anda devagar, com bengala');

INSERT INTO T_RS_ASSISTED_PERSON (ID_AREA, NAME, PHONE, LATITUDE, LONGITUDE, SPECIAL_NEED, NOTES)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Várzea do Jardim Helena' AND CITY = 'São Paulo'),
        'Lúcia Ferreira Rocha', '(00) 00000-0103', -23.481200, -46.417100, 'WHEELCHAIR', NULL);

INSERT INTO T_RS_ASSISTED_PERSON (ID_AREA, NAME, PHONE, LATITUDE, LONGITUDE, SPECIAL_NEED, NOTES)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Beira-Rio do Jardim Pantanal' AND CITY = 'São Paulo'),
        'Severino Alves Costa', '(00) 00000-0104', -23.476200, -46.399100, 'ELDERLY', 'Mora sozinho');

INSERT INTO T_RS_ASSISTED_PERSON (ID_AREA, NAME, PHONE, LATITUDE, LONGITUDE, SPECIAL_NEED, NOTES)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Baixada Leste de Guarulhos' AND CITY = 'Guarulhos'),
        'Marta Ribeiro Dias', '(00) 00000-0105', -23.455800, -46.418900, 'OTHER', 'Deficiência visual');

INSERT INTO T_RS_ASSISTED_PERSON (ID_AREA, NAME, PHONE, LATITUDE, LONGITUDE, SPECIAL_NEED, NOTES)
VALUES ((SELECT ID_AREA FROM T_RS_AREA WHERE NAME = 'Morro de Poá' AND CITY = 'Poá'),
        'Antônio Carvalho Nunes', '(00) 00000-0106', -23.527600, -46.344600, 'WHEELCHAIR', NULL);

-- ============================================================
-- T_RS_GUARDIAN: 8 responsáveis (RN03: toda pessoa assistida tem ao
-- menos um). Também fictícios. Cada um acha a pessoa pelo nome.
-- ============================================================

INSERT INTO T_RS_GUARDIAN (ID_ASSISTED_PERSON, NAME, PHONE, RELATIONSHIP)
VALUES ((SELECT ID_ASSISTED_PERSON FROM T_RS_ASSISTED_PERSON WHERE NAME = 'Benedita Moraes Lima'),
        'Ana Moraes Lima', '(00) 00000-0201', 'filha');

INSERT INTO T_RS_GUARDIAN (ID_ASSISTED_PERSON, NAME, PHONE, RELATIONSHIP)
VALUES ((SELECT ID_ASSISTED_PERSON FROM T_RS_ASSISTED_PERSON WHERE NAME = 'Benedita Moraes Lima'),
        'Carlos Lima Souza', '(00) 00000-0202', 'genro');

INSERT INTO T_RS_GUARDIAN (ID_ASSISTED_PERSON, NAME, PHONE, RELATIONSHIP)
VALUES ((SELECT ID_ASSISTED_PERSON FROM T_RS_ASSISTED_PERSON WHERE NAME = 'Joaquim Pereira Santos'),
        'Rita Santos Oliveira', '(00) 00000-0203', 'neta');

INSERT INTO T_RS_GUARDIAN (ID_ASSISTED_PERSON, NAME, PHONE, RELATIONSHIP)
VALUES ((SELECT ID_ASSISTED_PERSON FROM T_RS_ASSISTED_PERSON WHERE NAME = 'Lúcia Ferreira Rocha'),
        'Paulo Rocha Teixeira', '(00) 00000-0204', 'irmão');

INSERT INTO T_RS_GUARDIAN (ID_ASSISTED_PERSON, NAME, PHONE, RELATIONSHIP)
VALUES ((SELECT ID_ASSISTED_PERSON FROM T_RS_ASSISTED_PERSON WHERE NAME = 'Severino Alves Costa'),
        'Dona Cida Mendes', '(00) 00000-0205', 'vizinha');

INSERT INTO T_RS_GUARDIAN (ID_ASSISTED_PERSON, NAME, PHONE, RELATIONSHIP)
VALUES ((SELECT ID_ASSISTED_PERSON FROM T_RS_ASSISTED_PERSON WHERE NAME = 'Marta Ribeiro Dias'),
        'Juliana Dias Prado', '(00) 00000-0206', 'filha');

INSERT INTO T_RS_GUARDIAN (ID_ASSISTED_PERSON, NAME, PHONE, RELATIONSHIP)
VALUES ((SELECT ID_ASSISTED_PERSON FROM T_RS_ASSISTED_PERSON WHERE NAME = 'Antônio Carvalho Nunes'),
        'Roberto Nunes Barros', '(00) 00000-0207', 'filho');

INSERT INTO T_RS_GUARDIAN (ID_ASSISTED_PERSON, NAME, PHONE, RELATIONSHIP)
VALUES ((SELECT ID_ASSISTED_PERSON FROM T_RS_ASSISTED_PERSON WHERE NAME = 'Antônio Carvalho Nunes'),
        'Agente comunitário da UBS', '(00) 00000-0208', 'agente de saúde');

COMMIT;

-- Conferência: devem vir 10 áreas, 8 sensores, 12 leituras, 6 abrigos, 6 pessoas assistidas e 8 responsáveis, com os acentos corretos.
-- SELECT ID_AREA, NAME, CITY, DISTRICT, RISK_TYPE FROM T_RS_AREA ORDER BY ID_AREA;
-- SELECT ID_SENSOR, ID_AREA, CODE, STATUS FROM T_RS_SENSOR ORDER BY CODE;
-- SELECT ID_SENSOR, RAIN_MM, WATER_LEVEL_CM, MEASURED_AT FROM T_RS_SENSOR_READING ORDER BY ID_SENSOR, MEASURED_AT;
-- SELECT ID_SHELTER, NAME, CAPACITY, OCCUPIED_SPOTS FROM T_RS_SHELTER ORDER BY NAME;
-- SELECT p.NAME, p.SPECIAL_NEED, g.NAME AS GUARDIAN, g.RELATIONSHIP FROM T_RS_ASSISTED_PERSON p JOIN T_RS_GUARDIAN g ON g.ID_ASSISTED_PERSON = p.ID_ASSISTED_PERSON ORDER BY p.NAME, g.NAME;
