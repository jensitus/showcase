# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline -q
COPY src src
RUN mvn -B package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/cibdemo-*.jar app.jar
EXPOSE 7001
ENTRYPOINT ["java", "-jar", "app.jar"]
