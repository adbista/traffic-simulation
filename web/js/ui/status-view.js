/**
 * Single Responsibility: manages connection/init status badges and button enabled states.
 */
export class StatusView {
    constructor({ connectionStatusElement, initStatusElement, controls }) {
        this._conn   = connectionStatusElement;
        this._init   = initStatusElement;
        this._ctrls  = controls;
    }

    setConnected(connected) {
        this._conn.textContent = connected ? 'Połączono' : 'Rozłączono';
        this._conn.className   = connected ? 'status status-online' : 'status status-offline';

        this._ctrls.connectBtn.disabled    = connected;
        this._ctrls.disconnectBtn.disabled = !connected;
        this._ctrls.initBtn.disabled       = !connected;

        if (!connected) this.setInitialized(false);
    }

    setInitialized(initialized) {
        this._init.textContent = initialized ? 'Zainicjalizowano' : 'Nie zainicjalizowano';
        this._init.className   = initialized ? 'status status-online' : 'status status-offline';

        this._ctrls.addVehicleBtn.disabled = !initialized;
        this._ctrls.stepBtn.disabled       = !initialized;
        this._ctrls.stopBtn.disabled       = !initialized;
    }
}
