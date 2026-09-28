# Build en dos fases: compilamos con Maven+JDK, pero la imagen final solo lleva el JRE.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Capa de dependencias separada: si solo cambia el código (no el pom.xml),
# Docker reutiliza esta capa y no vuelve a descargar todo Maven Central.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/tienda-online-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
