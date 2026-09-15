FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system sinapse && useradd --system --gid sinapse sinapse
COPY --from=build /app/target/sinapse-api-0.1.0.jar app.jar
USER sinapse
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
