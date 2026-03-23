# Use OpenJDK as the base image
FROM eclipse-temurin:21

# Set working directory
WORKDIR /app

# Copy the application JAR file
COPY target/dp-http-*.jar app.jar

# Run the application
CMD ["java", "-jar", "app.jar"]

