# Stage 1: Build Java application
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S zakat && adduser -S zakat -G zakat

COPY --from=build /app/target/*.jar app.jar
RUN chown -R zakat:zakat /app

USER zakat

EXPOSE 8080

ENV TZ=Asia/Kolkata
ENTRYPOINT ["java", "-Duser.timezone=Asia/Kolkata", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
