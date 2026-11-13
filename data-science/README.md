# Data Science — Rota Segura (Global Solution FIAP 2026.2)

Código e dados da parte de Data Science. Os notebooks são `01_contexto.ipynb` e `02_modelos.ipynb`.
O pacote `rota_dados/` reúne os scripts que preparam os dados.

## Fontes

- **PED (Cemaden):** Centro Nacional de Monitoramento e Alertas de Desastres Naturais (Cemaden), Plataforma de Estações (PED).
  Arquivos em `dados/ped/` (não versionados): `PED_Cemaden_355030881A.csv` (Itaim Paulista, São Paulo)
  e `PED_Cemaden_355250201A.csv` (Jardim Amazonas, Suzano). Mogi das Cruzes e Itaquaquecetuba entram no mesmo formato.
- **CGE:** PREFEITURA DE SÃO PAULO. Centro de Gerenciamento de Emergências Climáticas (CGE). Histórico de pontos de
  alagamento, https://www.cgesp.org/v3/alagamentos.jsp, 2016–2025, acesso em nov. 2026, raspagem automatizada (1 req./4 s).
- **Reindesc:** TODO (Kelwin/João Paulo): completar a referência completa desta fonte; ela não está descrita no repositório.

## Premissas

- **UTC:** a `datahora` do PED é tratada como UTC. Por causa da conversão UTC → Brasília, só o primeiro e o último dia da
  série ficam incompletos (`dia_parcial`).
- **`dia_com_dado`:** dia com pelo menos 20 horas distintas com leitura do sensor 10. Lacuna é uma sequência de dias consecutivos
  sem `dia_com_dado`.
- **mm:** o sensor 10 informa mm acumulados no intervalo da leitura (60 min sem chuva, 10 min com chuva). Por isso a
  soma diária é comparável entre dias, mesmo com a série de intervalo misto.
- **Falha = NaN:** só a frase "Não há registros de alagamentos para essa data." vira 0 pontos no CGE. Erro HTTP, página vazia ou
  layout diferente ficam vazios (NaN) com `pagina_ok = False`, nunca zero.
- **Pontos que passam da meia-noite** pertencem à data consultada. Se o CGE repetir o ponto no dia seguinte, ele conta nos dois dias.
- **Extremo Leste:** Itaim Paulista, São Miguel (Paulista), Ermelino Matarazzo e Guaianases.
  **Zona Leste:** essas, mais Penha, Itaquera, Cidade Tiradentes, São Mateus, Mooca, Aricanduva/Formosa/Carrão, Vila Prudente e Sapopemba.
  O filtro usa o nome da subprefeitura, não o cabeçalho de zona do CGE.

## Como rodar

Python 3.11+. Dentro de `data-science/`:

```
pip install pandas beautifulsoup4 lxml requests pytest mypy pandas-stubs types-requests types-beautifulsoup4

python -m rota_dados.ped dados/ped/PED_Cemaden_*.csv                       # relatório de cobertura do PED
python -m rota_dados.cge_coletor --inicio 2016-01-01 --fim 2025-12-31       # coleta (~4 h, retomável)
python -m rota_dados.cge_agregacao                                         # gera dados/cge_diario.csv

pytest
mypy
```

A coleta guarda o HTML bruto em `dados/cge/raw/AAAA-MM-DD.html` (não versionado) e pula o que já existe.
A agregação grava `dados/cge_diario.csv` (versionável) e, em `dados/cge/`, `pontos.csv` e `faltantes.csv`.
No fim, ela imprime quantas páginas caíram em cada motivo (`sem_cache`, `pagina_vazia`, `layout_diferente`, `inconsistente`).
