# ============================================
# Stage 1: Build the application
# ============================================
FROM eclipse-temurin:22-jdk AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first (for dependency caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make the Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY src src

# Build the application (skip tests for faster builds)
RUN ./mvnw package -DskipTests -B

# ============================================
# Stage 2: Run the application
# ============================================
FROM eclipse-temurin:22-jre

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Run the application
# Render sets the PORT env variable — Spring Boot will pick it up via server.port
ENTRYPOINT java -jar app.jar --server.port=${PORT:-8080}
