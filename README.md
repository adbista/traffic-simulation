# Traffic Simulation

Projekt symuluje ruch na skrzyżowaniu i udostępnia API REST oraz WebSocket.

## Uruchomienie przez Docker

### 1) Zbuduj obraz

```bash
docker build -t trafficsim:latest .
```

### 2) Uruchom kontener

```bash
docker run --rm -p 8080:8080 --name trafficsim trafficsim:latest
```

Aplikacja będzie dostępna pod adresem:
- `http://localhost:8080`
- WebSocket: `ws://localhost:8080/v1/ws/simulation`

## Web klient

Frontend WebSocket znajduje się w katalogu `web/`.

Szybki start:
1. Uruchom backend (lokalnie lub w Dockerze).
2. W osobnym terminalu wystartuj serwer:
   ```bash
   python -m http.server 5500 --directory web
   ```
3. Otwórz: `http://localhost:5500`

Szczegóły: `web/README.md`.

## Testy E2E (Docker Compose)

Testy end-to-end uruchamiają aplikację w osobnym kontenerze na porcie **9090**
i przepuszczają przez niego pełny scenariusz WebSocket (addVehicle → step → asercja).

### Uruchomienie lokalne

```bash
# 1. Zbuduj obraz aplikacji
docker build -t trafficsim:latest .

# 2. Uruchom suitę E2E
docker compose -f docker-compose.e2e.yml up --abort-on-container-exit --exit-code-from e2e

# 3. Posprzątaj
docker compose -f docker-compose.e2e.yml down --remove-orphans
```

Co sprawdza test (`e2e/run.js`):
1. Czeka na `GET /health` → `{"status":"UP"}` (retry max 30 s).
2. Łączy się przez WebSocket.
3. Wysyła init `{}`, dwa `addVehicle`, dwa `step`, `stop`.
4. Asertuje, że pierwszy `StepStatus.leftVehicles` zawiera oba pojazdy.

## GitHub Actions — `workflow.yml`

Plik: `.github/workflows/workflow.yml`

Workflow uruchamia się na:
- `push` do `main`/`master`
- `pull_request` do `main`/`master`
- manualnie (`workflow_dispatch`)

### Job 1 — `build`
1. Ustawia JDK 17, uruchamia `./gradlew clean build`.
2. Publikuje artefakty: `app-jar`, `test-reports`, `web-client`.

### Job 2 — `e2e` (zależy od `build`)
1. Buduje obraz Docker aplikacji.
2. Uruchamia `docker-compose.e2e.yml` z kontenerem testującym.
3. W razie awarii zrzuca logi obu kontenerów.
