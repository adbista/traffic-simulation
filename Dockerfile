FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

COPY . .

RUN chmod +x ./gradlew && ./gradlew --no-daemon clean bootJar

FROM gcr.io/distroless/java17-debian12:nonroot
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
