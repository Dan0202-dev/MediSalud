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

COPY --from=build /app/target/agendamiento-*.jar app.jar

# Usuario sin privilegios; /app (y ./data para la BD H2) debe ser escribible por el.
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/data \
    && chown -R app:app /app
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
