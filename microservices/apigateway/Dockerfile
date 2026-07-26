# --- Etapa 1: Build ---
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# 1. Copiar primero el POM para descargar y cachear dependencias
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# 2. Copiar el código fuente y compilar
COPY src ./src
RUN mvn -q -B package -DskipTests

# --- Etapa 2: Runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]