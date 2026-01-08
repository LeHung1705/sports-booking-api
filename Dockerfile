# --- Giai đoạn 1: Build ---
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml và tải dependency
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code và build file jar
COPY src ./src
RUN mvn clean package -DskipTests

# --- Giai đoạn 2: Run ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file jar từ stage build
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
