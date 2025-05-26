FROM gradle:7.6.1-jdk17 AS build

WORKDIR /app
COPY . /app/
RUN gradle buildFatJar --no-daemon

FROM openjdk:17-slim

WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/pubsub.jar

# Create volume for H2 database
VOLUME /app/data

# Set environment variables with default values
ENV RABBITMQ_HOST=rabbitmq
ENV RABBITMQ_PORT=5672
ENV RABBITMQ_USERNAME=guest
ENV RABBITMQ_PASSWORD=guest
ENV H2_DB_PATH=/app/data/pubsub-db

# Expose the application port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "/app/pubsub.jar"]
