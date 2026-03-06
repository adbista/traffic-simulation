export interface StatusViewControls {
    connectBtn:    HTMLButtonElement;
    disconnectBtn: HTMLButtonElement;
    initBtn:       HTMLButtonElement;
    addVehicleBtn: HTMLButtonElement;
    stepBtn:       HTMLButtonElement;
    stopBtn:       HTMLButtonElement;
}

export interface StatusViewDeps {
    connectionStatusElement: HTMLElement;
    initStatusElement:       HTMLElement;
    controls:                StatusViewControls;
}

export class StatusView {
    #conn:  HTMLElement;
    #init:  HTMLElement;
    #ctrls: StatusViewControls;

    constructor({ connectionStatusElement, initStatusElement, controls }: StatusViewDeps) {
        this.#conn  = connectionStatusElement;
        this.#init  = initStatusElement;
        this.#ctrls = controls;
    }

    setConnected(connected: boolean): void {
        this.#conn.textContent = connected ? 'Connected' : 'Disconnected';
        this.#conn.className   = connected ? 'status status-online' : 'status status-offline';

        this.#ctrls.connectBtn.disabled    = connected;
        this.#ctrls.disconnectBtn.disabled = !connected;
        this.#ctrls.initBtn.disabled       = !connected;

        if (!connected) this.setInitialized(false);
    }

    setInitialized(initialized: boolean): void {
        this.#init.textContent = initialized ? 'Initialized' : 'Not initialized';
        this.#init.className   = initialized ? 'status status-online' : 'status status-offline';

        this.#ctrls.addVehicleBtn.disabled = !initialized;
        this.#ctrls.stepBtn.disabled       = !initialized;
        this.#ctrls.stopBtn.disabled       = !initialized;
    }
}
