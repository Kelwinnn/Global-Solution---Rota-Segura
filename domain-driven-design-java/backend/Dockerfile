# Contexto do build: domain-driven-design-java/ (raiz do servico no Render)
# Local: docker build -f backend/Dockerfile -t rota-segura-api .   (rodar dentro de domain-driven-design-java/)

# ---- Estagio 1: compila o .jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Baixa as dependencias primeiro (camada reaproveitada se so o codigo mudar)
COPY backend/pom.xml .
RUN mvn -q -B dependency:go-offline

COPY backend/src ./src
RUN mvn -q -B package -DskipTests

# ---- Estagio 2: so o JRE e o .jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Roda sem ser root
RUN useradd --system --uid 1001 appuser
USER appuser

COPY --from=build /app/target/rota-segura-api.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
