FROM node:20-bookworm AS frontend-builder
WORKDIR /frontend

COPY web/package*.json ./
RUN npm ci

COPY web ./
RUN npm run build


FROM eclipse-temurin:17-jdk-jammy AS backend-builder
WORKDIR /workspace

COPY . .

RUN rm -rf /workspace/src/main/resources/static/*
COPY --from=frontend-builder /frontend/index.html /workspace/src/main/resources/static/index.html
COPY --from=frontend-builder /frontend/js /workspace/src/main/resources/static/js
COPY --from=frontend-builder /frontend/styles.css /workspace/src/main/resources/static/styles.css

RUN chmod +x ./gradlew && ./gradlew --no-daemon clean bootJar


FROM gcr.io/distroless/java17-debian12:nonroot
WORKDIR /app

COPY --from=backend-builder /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]