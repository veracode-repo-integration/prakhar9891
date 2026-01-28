# Build stage
FROM maven:3.8.8-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# Run stage
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV PORT=4000
EXPOSE 4000
ENTRYPOINT ["java", "-jar", "app.jar"]
