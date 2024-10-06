# Stage 1: Build the application
FROM gradle:8.10.1-jdk21 AS builder

WORKDIR /app

COPY . .

# Build the application and create the JAR
RUN gradle clean bootJar -x test


FROM amazoncorretto:21

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar

COPY --from=builder /app/build/libs/*.jar /application.jar


ENV DB_HOST=taxool-postgres-db.cpaogi0wwer1.eu-central-1.rds.amazonaws.com

ENV DB_NAME=taxooldb


EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/application.jar"]
