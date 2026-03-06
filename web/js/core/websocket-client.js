var __classPrivateFieldSet = (this && this.__classPrivateFieldSet) || function (receiver, state, value, kind, f) {
    if (kind === "m") throw new TypeError("Private method is not writable");
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a setter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot write private member to an object whose class did not declare it");
    return (kind === "a" ? f.call(receiver, value) : f ? f.value = value : state.set(receiver, value)), value;
};
var __classPrivateFieldGet = (this && this.__classPrivateFieldGet) || function (receiver, state, kind, f) {
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a getter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot read private member from an object whose class did not declare it");
    return kind === "m" ? f : kind === "a" ? f.call(receiver) : f ? f.value : state.get(receiver);
};
var _WebSocketClient_socket, _WebSocketClient_handlers;
export class WebSocketClient {
    constructor(handlers = {}) {
        _WebSocketClient_socket.set(this, null);
        _WebSocketClient_handlers.set(this, void 0);
        __classPrivateFieldSet(this, _WebSocketClient_handlers, {
            onOpen: handlers.onOpen ?? (() => { }),
            onClose: handlers.onClose ?? (() => { }),
            onMessage: handlers.onMessage ?? (() => { }),
            onError: handlers.onError ?? (() => { }),
        }, "f");
    }
    connect(url) {
        if (__classPrivateFieldGet(this, _WebSocketClient_socket, "f") && __classPrivateFieldGet(this, _WebSocketClient_socket, "f").readyState === WebSocket.OPEN) {
            throw new Error('WebSocket jest już połączony.');
        }
        __classPrivateFieldSet(this, _WebSocketClient_socket, new WebSocket(url), "f");
        __classPrivateFieldGet(this, _WebSocketClient_socket, "f").addEventListener('open', () => __classPrivateFieldGet(this, _WebSocketClient_handlers, "f").onOpen());
        __classPrivateFieldGet(this, _WebSocketClient_socket, "f").addEventListener('message', (event) => __classPrivateFieldGet(this, _WebSocketClient_handlers, "f").onMessage(event.data));
        __classPrivateFieldGet(this, _WebSocketClient_socket, "f").addEventListener('error', (event) => __classPrivateFieldGet(this, _WebSocketClient_handlers, "f").onError(event));
        __classPrivateFieldGet(this, _WebSocketClient_socket, "f").addEventListener('close', () => __classPrivateFieldGet(this, _WebSocketClient_handlers, "f").onClose());
    }
    sendJson(payload) {
        if (!this.isConnected()) {
            throw new Error('Brak połączenia WebSocket.');
        }
        __classPrivateFieldGet(this, _WebSocketClient_socket, "f").send(JSON.stringify(payload));
    }
    disconnect() {
        __classPrivateFieldGet(this, _WebSocketClient_socket, "f")?.close();
    }
    isConnected() {
        return __classPrivateFieldGet(this, _WebSocketClient_socket, "f") !== null && __classPrivateFieldGet(this, _WebSocketClient_socket, "f").readyState === WebSocket.OPEN;
    }
}
_WebSocketClient_socket = new WeakMap(), _WebSocketClient_handlers = new WeakMap();
