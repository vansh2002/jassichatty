# Stage 1: Build the app using Maven and JDK 17
FROM maven:3.9.3-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cache optimization)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the jar (skip tests to speed up)
RUN mvn clean package -DskipTests

# Stage 2: Run the app with lightweight JDK 17 image
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy the built jar from build stage (replace with your actual jar name!)
COPY --from=build /app/target/chatapp-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080 (matches your server.port)
EXPOSE 8080

# Environment variables for your sensitive configs
ENV YOUTUBE_API_KEY="AIzaSyDcOSEhx8LpaG0MhheUC6EDY-bA3Tpp_nA"
# ENV TWILIO_ACCOUNT_SID="AC9e01c888714f7777dee5bfb5884a3ed2"
# ENV TWILIO_AUTH_TOKEN="faca96eb4d2c42fdb52945d8016dca8f"
# ENV TWILIO_PHONE_NUMBER="+17154849490"
ENV SPRING_MAIL_USERNAME="chotabheemdholakpurwala@gmail.com"
ENV SPRING_MAIL_PASSWORD="ijtvmcdpdrvvlrun"

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
