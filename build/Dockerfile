# Use an official OpenJDK runtime as a parent image
FROM registry.access.redhat.com/ubi8/openjdk-11:1.11

# Set the working directory in the container
WORKDIR /app

# Copy the packaged jar file into the container
COPY ../target/migration-1.0-SNAPSHOT.jar my-app.jar

# Run the jar file
ENTRYPOINT ["java", "-jar", "my-app.jar"]