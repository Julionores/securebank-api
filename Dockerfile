# --- Étape 1 : build (isolée, aucune dépendance de build ne finit dans l'image finale) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
# Cache Docker : les dépendances ne sont retéléchargées que si pom.xml change.
RUN mvn -q -B dependency:go-offline

COPY src src
RUN mvn -q -B clean package -DskipTests

# --- Étape 2 : exécution (image minimale, utilisateur non-root) ---
FROM eclipse-temurin:21-jre-jammy

RUN groupadd -r securebank && useradd -r -g securebank securebank
WORKDIR /app

COPY --from=build /workspace/target/securebank-api-*.jar app.jar
RUN chown -R securebank:securebank /app
USER securebank

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
