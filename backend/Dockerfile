# Etapa 1: compila el jar con Maven (no hace falta Java instalado en el equipo).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -q package -DskipTests

# Etapa 2: imagen liviana que solo corre el jar, con un usuario sin privilegios.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 superapp
COPY --from=build /app/target/*.jar app.jar
USER superapp
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
