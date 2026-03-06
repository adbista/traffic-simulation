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
var _StatusView_conn, _StatusView_init, _StatusView_ctrls;
export class StatusView {
    constructor({ connectionStatusElement, initStatusElement, controls }) {
        _StatusView_conn.set(this, void 0);
        _StatusView_init.set(this, void 0);
        _StatusView_ctrls.set(this, void 0);
        __classPrivateFieldSet(this, _StatusView_conn, connectionStatusElement, "f");
        __classPrivateFieldSet(this, _StatusView_init, initStatusElement, "f");
        __classPrivateFieldSet(this, _StatusView_ctrls, controls, "f");
    }
    setConnected(connected) {
        __classPrivateFieldGet(this, _StatusView_conn, "f").textContent = connected ? 'Connected' : 'Disconnected';
        __classPrivateFieldGet(this, _StatusView_conn, "f").className = connected ? 'status status-online' : 'status status-offline';
        __classPrivateFieldGet(this, _StatusView_ctrls, "f").connectBtn.disabled = connected;
        __classPrivateFieldGet(this, _StatusView_ctrls, "f").disconnectBtn.disabled = !connected;
        __classPrivateFieldGet(this, _StatusView_ctrls, "f").initBtn.disabled = !connected;
        if (!connected)
            this.setInitialized(false);
    }
    setInitialized(initialized) {
        __classPrivateFieldGet(this, _StatusView_init, "f").textContent = initialized ? 'Initialized' : 'Not initialized';
        __classPrivateFieldGet(this, _StatusView_init, "f").className = initialized ? 'status status-online' : 'status status-offline';
        __classPrivateFieldGet(this, _StatusView_ctrls, "f").addVehicleBtn.disabled = !initialized;
        __classPrivateFieldGet(this, _StatusView_ctrls, "f").stepBtn.disabled = !initialized;
        __classPrivateFieldGet(this, _StatusView_ctrls, "f").stopBtn.disabled = !initialized;
    }
}
_StatusView_conn = new WeakMap(), _StatusView_init = new WeakMap(), _StatusView_ctrls = new WeakMap();
