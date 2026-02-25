# --- Etapa 1: Construção (Build) ---
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# --- Etapa 2: Execução (Runtime) ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-Xmx512M", "-Xms256M", "-XX:+UseSerialGC", "-jar", "app.jar"]