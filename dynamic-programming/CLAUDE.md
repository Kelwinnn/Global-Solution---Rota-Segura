# Dynamic Programming e serviço Python (João Paulo)

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

- Rota segura: Dijkstra multi-origem a partir dos abrigos com vaga; memoização por cenário (áreas alagadas + abrigos com vaga, o mapa é fixo); Bellman-Ford tabular como verificação e argumento de eficiência; teste com `hypothesis` de que Dijkstra e Bellman-Ford dão o mesmo resultado. **Bellman-Ford, memoização e hypothesis estão no escopo de propósito: não remova como "extra".**
- Custo do trecho = distância × (1 + fatorRisco). Trecho dentro de área alagada sai do grafo (RN05). Abrigo sem vaga é ignorado (RN06). Sem rota segura é resultado normal (200, `NO_SAFE_ROUTE`, orientação de ir para local alto), não exceção. Desempate: custo, distância, menor ID do abrigo. A pé, 70 m/min.
- Serviço FastAPI: `GET /health`, `POST /route`, `POST /risk` conforme `docs/contrato.md`. Mapa do bairro-piloto **Jardim Helena** (OpenStreetMap) carregado de `GRAFO_PATH`; modelo de risco de `MODELO_PATH` (gerado por `data-science/02_modelos.ipynb`). Deploy: Render via Dockerfile nesta pasta.
- Validação na carga: esquina inexistente, distância ≤ 0, capacidade negativa → `EntradaInvalidaError`.
- Entrega: código-fonte + README (nomes e RMs, algoritmos, funcionalidades, como rodar).
