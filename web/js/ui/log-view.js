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
var _LogView_instances, _LogView_logElement, _LogView_append;
export class LogView {
    constructor(logElement) {
        _LogView_instances.add(this);
        _LogView_logElement.set(this, void 0);
        __classPrivateFieldSet(this, _LogView_logElement, logElement, "f");
    }
    info(message) { __classPrivateFieldGet(this, _LogView_instances, "m", _LogView_append).call(this, 'info', message); }
    success(message) { __classPrivateFieldGet(this, _LogView_instances, "m", _LogView_append).call(this, 'success', message); }
    error(message) { __classPrivateFieldGet(this, _LogView_instances, "m", _LogView_append).call(this, 'error', message); }
}
_LogView_logElement = new WeakMap(), _LogView_instances = new WeakSet(), _LogView_append = function _LogView_append(level, message) {
    const line = document.createElement('div');
    line.className = `log-line ${level}`;
    const time = new Date().toLocaleTimeString('en-GB', { hour12: false });
    line.textContent = `[${time}] ${message}`;
    __classPrivateFieldGet(this, _LogView_logElement, "f").prepend(line);
};
