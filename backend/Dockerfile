FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package \
    && test -f target/backend-0.0.1-SNAPSHOT.jar

FROM eclipse-temurin:25-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN useradd --system --uid 10001 --create-home bonaca

COPY --from=build /workspace/target/backend-0.0.1-SNAPSHOT.jar /app/backend-0.0.1-SNAPSHOT.jar

USER bonaca
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/backend-0.0.1-SNAPSHOT.jar"]
