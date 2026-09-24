FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml ./pom.xml
RUN mvn -B dependency:go-offline
COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r helpdesk && useradd -r -g helpdesk helpdesk
COPY --from=build /workspace/target/helpdesk-backend-0.1.0-SNAPSHOT.jar app.jar
USER helpdesk
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
