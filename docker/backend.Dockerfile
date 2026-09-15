FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml ./pom.xml
RUN mvn -B dependency:go-offline
COPY backend/src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S sinapse && adduser -S sinapse -G sinapse
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER sinapse
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
