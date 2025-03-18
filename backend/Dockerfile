# Funciona para o application-dev habilitado.
# [Choice] Java version:
ARG VARIANT=17

#FROM openjdk:${VARIANT}
FROM maven:3.8-openjdk-17-slim AS build
LABEL mantainer="Pablo Correia"

WORKDIR /app

# Copy the current directory contents into the container at /app
COPY . /app

# Garantir permissão de execução do Maven Wrapper
RUN chmod +x ./mvnw

# Build the application with Maven
RUN ./mvnw clean package -DskipTests

# Copiar o JAR gerado da etapa anterior
COPY target/h2gas-backend-0.0.1-SNAPSHOT.jar /app/h2gas-backend-0.0.1-SNAPSHOT.jar

# Define a porta que a aplicação irá escutar
EXPOSE 8080

# Set the default command to run the application when the container starts
CMD ["java", "-jar", "/app/h2gas-backend-0.0.1-SNAPSHOT.jar"]
