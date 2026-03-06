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
var _StepHistoryView_container;
export class StepHistoryView {
    constructor(container) {
        _StepHistoryView_container.set(this, void 0);
        __classPrivateFieldSet(this, _StepHistoryView_container, container, "f");
    }
    addEntry({ step, leftVehicles }) {
        const row = document.createElement('div');
        row.className = 'history-row';
        const badge = document.createElement('span');
        badge.className = 'history-step';
        badge.textContent = `Step ${step}`;
        const vehicles = document.createElement('span');
        vehicles.className = leftVehicles.length > 0 ? 'history-vehicles' : 'history-empty';
        vehicles.textContent = leftVehicles.length > 0
            ? leftVehicles.join(', ')
            : 'no vehicles left';
        row.appendChild(badge);
        row.appendChild(vehicles);
        __classPrivateFieldGet(this, _StepHistoryView_container, "f").prepend(row);
    }
    clear() {
        __classPrivateFieldGet(this, _StepHistoryView_container, "f").innerHTML = '';
    }
}
_StepHistoryView_container = new WeakMap();
