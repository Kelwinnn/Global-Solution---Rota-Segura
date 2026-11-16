-- ============================================================
-- Rota Segura - Global Solution FIAP 2026.2 (CitizenTech)
-- DQL: consultas (rodar depois do ddl.sql e do dml.sql)
--
-- Três consultas, cada uma respondendo a uma pergunta do negócio.
-- O "resultado esperado" de cada uma vale para os dados do dml.sql.
--
-- Arquivo em UTF-8. No SQL Developer, rodar com "Executar Script" (F5).
-- ============================================================


-- ============================================================
-- 1) INNER JOIN de duas tabelas
-- Pergunta: em que área de risco está instalado cada sensor, e
-- qual é a situação dele?
-- Resultado esperado: 8 linhas, uma por sensor.
-- ============================================================
SELECT s.CODE   AS SENSOR,
       s.STATUS AS SENSOR_STATUS,
       a.NAME   AS AREA,
       a.CITY
FROM T_RS_SENSOR s
INNER JOIN T_RS_AREA a ON a.ID_AREA = s.ID_AREA
ORDER BY a.CITY, a.NAME, s.CODE;


-- ============================================================
-- 2) INNER JOIN com função de grupo e GROUP BY
-- Pergunta: quantos sensores ativos tem cada área monitorada, isto
-- é, cada área com ao menos um sensor ativo? (As áreas com mais
-- sensores ativos são as mais bem monitoradas.)
-- Resultado esperado: 5 linhas. 'Várzea do Jardim Helena' com 2 e
-- as outras quatro com 1. Ficam de fora as quatro áreas sem sensor
-- e a 'Várzea do Tietê em Suzano', cujo único sensor está INACTIVE.
-- ============================================================
SELECT a.NAME             AS AREA,
       a.CITY,
       COUNT(s.ID_SENSOR) AS ACTIVE_SENSORS
FROM T_RS_AREA a
INNER JOIN T_RS_SENSOR s ON s.ID_AREA = a.ID_AREA
WHERE s.STATUS = 'ACTIVE'
GROUP BY a.ID_AREA, a.NAME, a.CITY
ORDER BY ACTIVE_SENSORS DESC, a.NAME;


-- ============================================================
-- 3) LEFT OUTER JOIN com WHERE e ORDER BY
-- Pergunta: quais áreas de alagamento ainda não têm nenhum sensor
-- instalado? (São as áreas que o sistema não consegue monitorar.)
-- O LEFT OUTER JOIN mantém as áreas sem sensor; nelas, as colunas
-- do sensor vêm nulas, e é isso que o WHERE procura. Com INNER JOIN,
-- esta consulta não devolveria nenhuma linha.
-- Resultado esperado: 2 linhas, nesta ordem:
--   'Baixada de Itaquaquecetuba' (Itaquaquecetuba)
--   'Várzea do Itaim Biacica'    (São Paulo)
-- ============================================================
SELECT a.NAME AS AREA,
       a.CITY,
       a.DISTRICT
FROM T_RS_AREA a
LEFT OUTER JOIN T_RS_SENSOR s ON s.ID_AREA = a.ID_AREA
WHERE a.RISK_TYPE = 'FLOOD'
  AND s.ID_SENSOR IS NULL
ORDER BY a.CITY, a.NAME;
