FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

RUN apt-get update && apt-get install -y --no-install-recommends nodejs npm && rm -rf /var/lib/apt/lists/*

COPY . .

RUN chmod +x ./gradlew && ./gradlew --no-daemon clean bootJar

FROM gcr.io/distroless/java17-debian12:nonroot
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
