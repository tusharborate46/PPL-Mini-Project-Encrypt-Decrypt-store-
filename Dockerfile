# Use a lightweight JDK image
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy everything into the container
COPY . .

# Compile the Java application
RUN javac src/*.java

# Run the application
CMD ["java", "src.WebServer"]
