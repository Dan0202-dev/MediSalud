# ---------- Etapa 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cachear dependencias primero (mejor uso de la capa de Docker)
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Compilar y empaquetar (los tests se ejecutan en CI, no en la imagen)
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/agendamiento-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
