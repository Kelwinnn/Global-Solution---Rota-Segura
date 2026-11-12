FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# Dependências primeiro, para o Docker reaproveitar a camada quando só o código muda.
COPY pyproject.toml ./
RUN python -c "import tomllib; print(chr(10).join(tomllib.load(open('pyproject.toml', 'rb'))['project']['dependencies']))" > requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY rota_segura ./rota_segura
# Mapa do Jardim Helena (OpenStreetMap) e modelo de risco: são os caminhos padrão do serviço
# (GRAFO_PATH e MODELO_PATH só são necessários para trocar de arquivo).
COPY dados/grafo_jardim_helena.json ./dados/grafo_jardim_helena.json
COPY modelos/risco_logistica.joblib ./modelos/risco_logistica.joblib

# Se algum dos dois arquivos faltar ou for inválido, o serviço sobe mesmo assim e /health mostra mapLoaded/modelLoaded falsos.
ENV PORT=8000
EXPOSE 8000

CMD ["sh", "-c", "uvicorn rota_segura.api:app --host 0.0.0.0 --port ${PORT}"]
