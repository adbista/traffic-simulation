import type { WebSocketHandlers } from '../types.js';

export class WebSocketClient {
    #socket: WebSocket | null = null;
    readonly #handlers: Required<WebSocketHandlers>;

    constructor(handlers: WebSocketHandlers = {}) {
        this.#handlers = {
            onOpen:    handlers.onOpen    ?? (() => {}),
            onClose:   handlers.onClose   ?? (() => {}),
            onMessage: handlers.onMessage ?? (() => {}),
            onError:   handlers.onError   ?? (() => {}),
        };
    }

    connect(url: string): void {
        if (this.#socket && this.#socket.readyState === WebSocket.OPEN) {
            throw new Error('WebSocket jest już połączony.');
        }
        this.#socket = new WebSocket(url);
        this.#socket.addEventListener('open',    () => this.#handlers.onOpen());
        this.#socket.addEventListener('message', (event) => this.#handlers.onMessage(event.data as string));
        this.#socket.addEventListener('error',   (event) => this.#handlers.onError(event));
        this.#socket.addEventListener('close',   () => this.#handlers.onClose());
    }

    sendJson(payload: unknown): void {
        if (!this.isConnected()) {
            throw new Error('Brak połączenia WebSocket.');
        }
        this.#socket!.send(JSON.stringify(payload));
    }

    disconnect(): void {
        this.#socket?.close();
    }

    isConnected(): boolean {
        return this.#socket !== null && this.#socket.readyState === WebSocket.OPEN;
    }
}
