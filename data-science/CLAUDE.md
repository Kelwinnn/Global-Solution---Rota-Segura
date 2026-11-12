# Data Science (João Paulo)

## Quem está trabalhando aqui

Esta pasta é do **João Paulo Santana Basta (RM 565383)**: Dynamic Programming, Data Science e o serviço Python do projeto Rota Segura (FIAP Global Solution 2026.2, entrega em **19/11/2026, 23h55**). Nesta pasta, as restrições do `CLAUDE.md` da raiz (escritas para o Kelwin: "só edite as pastas do Kelwin", "nunca faça commit") **não se aplicam**. Valem as regras abaixo.

- Decisões do grupo: `docs/decisions.md` (siga "Decidido"; pergunte antes de qualquer item fora dele). Contrato do serviço: `dynamic-programming/docs/contrato.md` (v2, fonte da verdade).
- Trabalhe só em `data-science/` e `dynamic-programming/`. Fora disso (incluindo `docs/decisions.md`), só com pedido explícito.
- Git: branch `main`. Pode commitar quando eu pedir, com o meu usuário do git e **sem `Co-Authored-By` nem nenhum trailer**. **Nunca faça push.** Antes de commitar, rode pytest e mypy --strict da pasta.
- Antes de tarefa média ou grande, mostre um plano curto (arquivos) e espere meu ok. Diga sempre o que rodou e o resultado.
- Python 3.11+, type hints em tudo, `mypy --strict`, `pytest`. Testes nomeados pela regra de negócio. Identificadores e README em português; JSON e endpoints do serviço em inglês e camelCase.
- Entrada inválida: `EntradaInvalidaError` com mensagem que aponta o campo.
- Sem dados pessoais reais. Toda fonte citada no README e no notebook.
- Não apague nem reescreva `prompts.md` da raiz; eu registro os meus prompts.


## Escopo desta pasta

- Pergunta: dada a chuva do dia, (1) alaga hoje? → regressão logística (matriz de confusão, precisão, recall, F1, PR-AUC); (2) quantos pontos de alagamento? → regressão linear (MAE, RMSE, R², interpretação). Uma linha = um dia (America/Sao_Paulo).
- Chuva: Cemaden/PED, `dados/ped/PED_Cemaden_<código>.csv` (não versionados). Itaim Paulista 355030881A (principal), Suzano 355250201A (apoio, sem rótulo), Mogi das Cruzes e Itaquaquecetuba (validação no Alto Tietê). Sensor 10 = mm acumulados no intervalo; UTC → Brasília; hora sem leitura = NaN, dia com < 20 horas de leitura fica fora.
- Rótulo: CGE (`rota_dados/cge_*`, cache em `dados/cge/raw/`, saída `dados/cge_diario.csv`), filtrado por subprefeitura: Extremo Leste (Itaim Paulista, São Miguel, Ermelino Matarazzo, Guaianases) e Zona Leste. Só "Não há registros…" vira zero; página com erro = faltante. Validação no Alto Tietê: ocorrências `Hidro` do Reindesc (Cemaden).
- Regras do modelo: divisão por tempo (treino 2016–2022, validação 2023, teste 2024–2025), **nunca aleatória**; variáveis só olham para trás; normalização dentro de `Pipeline`; `random_state` fixo; referência simples ao lado de cada modelo. Limite de "alta probabilidade": o de maior precisão com recall ≥ 0,70 na validação.
- Recomendações com limitações e impactos valem 3,0 de 10: rótulo do CGE só cobre vias monitoradas (subnotifica Jardim Helena/várzea), uma estação para a região, granularidade diária, lacunas, mudança no registro ao longo dos anos, Alto Tietê sem rótulo diário.
- Entrega: `.ipynb` executado, CSV (ou link) com a fonte citada, TXT com nomes e RMs.
