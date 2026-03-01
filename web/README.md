# Web UI (WebSocket)

Prosty klient do sterowania symulacją przez WebSocket (`/v1/ws/simulation`).

## Struktura

- `index.html` — układ aplikacji
- `css/styles.css` — style
- `js/main.js` — kompozycja zależności
- `js/core/` — infrastruktura transportu WebSocket
- `js/app/` — logika biznesowa (komendy, orkiestracja)
- `js/ui/` — warstwa widoku (DOM, status, log)

## Jak uruchomić

1. Uruchom backend Spring Boot (w katalogu projektu):
   - `./gradlew bootRun`
2. Udostępnij katalog `web/` przez lokalny serwer HTTP (przykład):
   - `python -m http.server 5500 --directory web`
3. Otwórz:
   - `http://localhost:5500`
4. W UI:
   - kliknij **Połącz**,
   - wyślij **init** (domyślnie `{}`),
   - używaj `addVehicle`, `step`, `stop`.

## Uwagi

- Pierwsza wiadomość po połączeniu musi być `WsInitRequest`.
- Odpowiedzi serwera dla `step` są wyświetlane jako JSON w sekcji „Odpowiedź serwera”.
- Błędy backendu (`{"error":"..."}`) trafiają do logu zdarzeń.
