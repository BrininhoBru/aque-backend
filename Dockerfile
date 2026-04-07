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

WORKDIR /app

# Copia o JAR gerado no stage anterior
COPY --from=build /app/target/*.jar app.jar

# Porta da aplicação
EXPOSE 8080

# Flags de memória adequadas para o Raspberry Pi 3B (1GB RAM)
# -Xmx256m  → máximo de heap para o Spring Boot
# -Xms128m  → heap inicial
# -XX:+UseSerialGC → GC mais leve para dispositivos com pouca RAM
ENTRYPOINT ["java", \
  "-Xmx256m", \
  "-Xms128m", \
  "-XX:+UseSerialGC", \
  "-jar", "app.jar"]