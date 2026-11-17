# Rota Segura — DER (modelo de dados)

Gerado a partir de `database-design/ddl.sql` (Oracle). Serve de referência para o modelo lógico do **Oracle Data Modeler** (exigido pela rubrica de Database Design) e para o PDF de DDD Java.

No diagrama, o tipo de cada coluna aparece entre aspas (o Mermaid não aceita vírgula no nome do tipo). Todos os textos são `VARCHAR2(n CHAR)` e todas as chaves primárias são `NUMBER(19) GENERATED ALWAYS AS IDENTITY`. `UK` marca as colunas cobertas por um **índice único** (sobre `UPPER(...)`, para não diferenciar maiúsculas); não são constraints `UNIQUE`, e é assim que aparecem no Data Modeler.

```mermaid
erDiagram
    T_RS_AREA ||--o{ T_RS_SENSOR : "tem"
    T_RS_SENSOR ||--o{ T_RS_SENSOR_READING : "mede"
    T_RS_AREA ||--o{ T_RS_ASSISTED_PERSON : "onde mora"
    T_RS_ASSISTED_PERSON ||--|{ T_RS_GUARDIAN : "tem responsáveis"

    T_RS_AREA {
        NUMBER ID_AREA PK "NUMBER(19) identity"
        VARCHAR2 NAME UK "VARCHAR2(100) NN; índice único com CITY em UPPER"
        VARCHAR2 CITY UK "VARCHAR2(60) NN; índice único com NAME em UPPER"
        VARCHAR2 DISTRICT "VARCHAR2(60) opcional"
        VARCHAR2 RISK_TYPE "VARCHAR2(20) NN; FLOOD ou LANDSLIDE"
        NUMBER LATITUDE "NUMBER(9,6) NN; -90 a 90"
        NUMBER LONGITUDE "NUMBER(9,6) NN; -180 a 180"
        NUMBER RADIUS_M "NUMBER(10) NN; 1 a 2147483647"
    }

    T_RS_SENSOR {
        NUMBER ID_SENSOR PK "NUMBER(19) identity"
        NUMBER ID_AREA FK "NUMBER(19) NN; FK_RS_SENSOR_AREA"
        VARCHAR2 CODE UK "VARCHAR2(30) NN; índice único em UPPER(CODE)"
        NUMBER LATITUDE "NUMBER(9,6) NN"
        NUMBER LONGITUDE "NUMBER(9,6) NN"
        VARCHAR2 STATUS "VARCHAR2(20) NN; ACTIVE ou INACTIVE"
    }

    T_RS_SENSOR_READING {
        NUMBER ID_SENSOR_READING PK "NUMBER(19) identity"
        NUMBER ID_SENSOR FK "NUMBER(19) NN; FK_RS_READING_SENSOR"
        NUMBER RAIN_MM "NUMBER(6,2) NN; >= 0"
        NUMBER WATER_LEVEL_CM "NUMBER(7,2) NN; >= 0"
        TIMESTAMP MEASURED_AT "TIMESTAMP(6) WITH TIME ZONE NN"
    }

    T_RS_SHELTER {
        NUMBER ID_SHELTER PK "NUMBER(19) identity"
        VARCHAR2 NAME "VARCHAR2(150) NN"
        VARCHAR2 ADDRESS "VARCHAR2(200) NN"
        NUMBER LATITUDE "NUMBER(9,6) NN"
        NUMBER LONGITUDE "NUMBER(9,6) NN"
        NUMBER CAPACITY "NUMBER(10) NN; 1 a 2147483647"
        NUMBER OCCUPIED_SPOTS "NUMBER(10) NN; 0 até CAPACITY"
    }

    T_RS_ASSISTED_PERSON {
        NUMBER ID_ASSISTED_PERSON PK "NUMBER(19) identity"
        NUMBER ID_AREA FK "NUMBER(19) NN; FK_RS_ASSISTED_AREA"
        VARCHAR2 NAME "VARCHAR2(150) NN"
        VARCHAR2 PHONE "VARCHAR2(20) NN"
        NUMBER LATITUDE "NUMBER(9,6) NN"
        NUMBER LONGITUDE "NUMBER(9,6) NN"
        VARCHAR2 SPECIAL_NEED "VARCHAR2(20) NN; ELDERLY, BEDRIDDEN, WHEELCHAIR ou OTHER"
        VARCHAR2 NOTES "VARCHAR2(500) opcional"
    }

    T_RS_GUARDIAN {
        NUMBER ID_GUARDIAN PK "NUMBER(19) identity"
        NUMBER ID_ASSISTED_PERSON FK "NUMBER(19) NN; FK_RS_GUARDIAN_ASSISTED, ON DELETE CASCADE"
        VARCHAR2 NAME "VARCHAR2(150) NN"
        VARCHAR2 PHONE "VARCHAR2(20) NN"
        VARCHAR2 RELATIONSHIP "VARCHAR2(50) NN"
    }
```

## Relacionamentos

| De | Para | Cardinalidade | Chave estrangeira | Ao excluir o pai |
|---|---|---|---|---|
| `T_RS_AREA` | `T_RS_SENSOR` | 1 : 0..N | `FK_RS_SENSOR_AREA` (`ID_AREA`) | bloqueado (sem cascata) |
| `T_RS_SENSOR` | `T_RS_SENSOR_READING` | 1 : 0..N | `FK_RS_READING_SENSOR` (`ID_SENSOR`) | bloqueado (sem cascata) |
| `T_RS_AREA` | `T_RS_ASSISTED_PERSON` | 1 : 0..N | `FK_RS_ASSISTED_AREA` (`ID_AREA`) | bloqueado (sem cascata) |
| `T_RS_ASSISTED_PERSON` | `T_RS_GUARDIAN` | 1 : 1..N | `FK_RS_GUARDIAN_ASSISTED` (`ID_ASSISTED_PERSON`) | `ON DELETE CASCADE` |

- `T_RS_SHELTER` não se relaciona com as outras tabelas: a rota (RN06) escolhe o abrigo mais próximo com vaga.
- O "ao menos um responsável" (1..N, RN03) é garantido pela API; o banco, sozinho, aceitaria uma pessoa sem responsável.

## Índices além das chaves primárias

| Índice | Tabela | Colunas | Para quê |
|---|---|---|---|
| `UX_RS_AREA_NAME_CITY` (único) | `T_RS_AREA` | `UPPER(NAME), UPPER(CITY)` | nome único por cidade, sem diferenciar maiúsculas |
| `UX_RS_SENSOR_CODE` (único) | `T_RS_SENSOR` | `UPPER(CODE)` | código do sensor único |
| `IX_RS_SENSOR_AREA` | `T_RS_SENSOR` | `ID_AREA` | sensores de uma área; FK |
| `UX_RS_READING_SENSOR_TIME` (único) | `T_RS_SENSOR_READING` | `ID_SENSOR, SYS_EXTRACT_UTC(MEASURED_AT)` | sem leitura repetida no mesmo instante; serve à FK |
| `IX_RS_ASSISTED_AREA` | `T_RS_ASSISTED_PERSON` | `ID_AREA` | pessoas de uma área; FK |
| `IX_RS_GUARDIAN_ASSISTED` | `T_RS_GUARDIAN` | `ID_ASSISTED_PERSON` | responsáveis de uma pessoa; FK |
