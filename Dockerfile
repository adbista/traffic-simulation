FROM node:24-bookworm-slim AS frontend-builder
WORKDIR /frontend

COPY web/package*.json ./
RUN npm ci

COPY web ./
RUN npm run build

FROM eclipse-temurin:17-jdk-jammy AS backend-builder
WORKDIR /workspace

COPY . .

RUN rm -rf /workspace/web/js /workspace/web/css
COPY --from=frontend-builder /frontend/index.html /workspace/web/index.html
COPY --from=frontend-builder /frontend/js /workspace/web/js
COPY --from=frontend-builder /frontend/css /workspace/web/css

RUN chmod +x ./gradlew && ./gradlew --no-daemon clean bootJar

FROM gcr.io/distroless/java17-debian12:nonroot
WORKDIR /app

COPY --from=backend-builder /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]