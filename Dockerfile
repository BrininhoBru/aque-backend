# =============================================================================
# Aque — Backend Dockerfile (multi-stage, ARM64)
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build — compila o JAR com Maven
# -----------------------------------------------------------------------------
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copia apenas o pom.xml primeiro para aproveitar cache de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copia o código fonte e compila
COPY src ./src
RUN mvn clean package -DskipTests -q

# -----------------------------------------------------------------------------
# Stage 2: Runtime — imagem mínima para rodar no Raspberry Pi (ARM64)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre

# Instala curl para o healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:+UseSerialGC", "-jar", "app.jar"]