export class WebSocketClient {
    constructor({ onOpen, onClose, onMessage, onError } = {}) {
        this.socket = null;
        this.handlers = {
            onOpen: onOpen ?? (() => {}),
            onClose: onClose ?? (() => {}),
            onMessage: onMessage ?? (() => {}),
            onError: onError ?? (() => {})
        };
    }

    connect(url) {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            throw new Error("WebSocket jest już połączony.");
        }

        this.socket = new WebSocket(url);
        this.socket.addEventListener("open", () => this.handlers.onOpen());
        this.socket.addEventListener("message", (event) => this.handlers.onMessage(event.data));
        this.socket.addEventListener("error", (event) => this.handlers.onError(event));
        this.socket.addEventListener("close", () => this.handlers.onClose());
    }

    sendJson(payload) {
        if (!this.isConnected()) {
            throw new Error("Brak połączenia WebSocket.");
        }
        this.socket.send(JSON.stringify(payload));
    }

    disconnect() {
        if (!this.socket) {
            return;
        }
        this.socket.close();
    }

    isConnected() {
        return this.socket && this.socket.readyState === WebSocket.OPEN;
    }
}
