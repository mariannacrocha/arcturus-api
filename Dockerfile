# --- Etapa 1: Construção (Build) ---
# Usamos uma imagem que já tem o Maven e o Java instalados
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Define a pasta de trabalho dentro do container
WORKDIR /app

# Copia todo o seu projeto para dentro do container
COPY . .

# Roda o comando do Maven para gerar o arquivo .jar (pulando os testes para ser mais rápido)
RUN mvn clean package -DskipTests

# --- Etapa 2: Execução (Runtime) ---
# Usamos uma imagem mais leve apenas para rodar o Java
FROM eclipse-temurin:21-jre-alpine

# Define a pasta de trabalho
WORKDIR /app

# Pega o arquivo .jar que foi gerado na Etapa 1 e copia para cá
COPY --from=build /app/target/*.jar app.jar

# Avisa ao Render que a porta 8080 deve ficar aberta
EXPOSE 8080

# O comando final que inicia o seu backend
ENTRYPOINT ["java", "-jar", "app.jar"]