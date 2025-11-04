# Etapa 1: Build da aplicação
# Funciona para o application-dev habilitado.
# [Choice] Java version:
ARG VARIANT=17
FROM maven:3.9.6-eclipse-temurin-17 AS build
LABEL maintainer="Pablo Correia"

WORKDIR /app
COPY . .
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

# Etapa 2: Imagem final com o JAR
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/h2gas-backend-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]
