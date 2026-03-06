export interface DomElements {
    // connection
    wsUrl:             HTMLInputElement;
    connectBtn:        HTMLButtonElement;
    disconnectBtn:     HTMLButtonElement;
    connectionStatus:  HTMLElement;
    // init
    initPayload:       HTMLTextAreaElement;
    initBtn:           HTMLButtonElement;
    initStatus:        HTMLElement;
    scenarioSelect:    HTMLSelectElement;
    // vehicle add
    addVehicleForm:    HTMLFormElement;
    vehicleId:         HTMLInputElement;
    startRoad:         HTMLSelectElement;
    endRoad:           HTMLSelectElement;
    lane:              HTMLSelectElement;
    addVehicleBtn:     HTMLButtonElement;
    // step controls
    stepBtn:           HTMLButtonElement;
    stopBtn:           HTMLButtonElement;
    autoStepCheck:     HTMLInputElement;
    autoStepInterval:  HTMLInputElement;
    // canvas
    intersectionCanvas: HTMLCanvasElement;
    vehicleCanvas:      HTMLCanvasElement;
    // panels
    phasePanel:         HTMLElement;
    stepHistory:        HTMLElement;
    eventLog:           HTMLElement;
    statsSteps:         HTMLElement;
    statsLeft:          HTMLElement;
    statsQueued:        HTMLElement;
}

function getById<T extends HTMLElement>(id: string): T {
    const el = document.getElementById(id) as T | null;
    if (!el) throw new Error(`Missing DOM element: #${id}`);
    return el;
}

export function getDomElements(): DomElements {
    return {
        wsUrl:             getById<HTMLInputElement>('wsUrl'),
        connectBtn:        getById<HTMLButtonElement>('connectBtn'),
        disconnectBtn:     getById<HTMLButtonElement>('disconnectBtn'),
        connectionStatus:  getById('connectionStatus'),
        initPayload:       getById<HTMLTextAreaElement>('initPayload'),
        initBtn:           getById<HTMLButtonElement>('initBtn'),
        initStatus:        getById('initStatus'),
        scenarioSelect:    getById<HTMLSelectElement>('scenarioSelect'),
        addVehicleForm:    getById<HTMLFormElement>('addVehicleForm'),
        vehicleId:         getById<HTMLInputElement>('vehicleId'),
        startRoad:         getById<HTMLSelectElement>('startRoad'),
        endRoad:           getById<HTMLSelectElement>('endRoad'),
        lane:              getById<HTMLSelectElement>('lane'),
        addVehicleBtn:     getById<HTMLButtonElement>('addVehicleBtn'),
        stepBtn:           getById<HTMLButtonElement>('stepBtn'),
        stopBtn:           getById<HTMLButtonElement>('stopBtn'),
        autoStepCheck:     getById<HTMLInputElement>('autoStepCheck'),
        autoStepInterval:  getById<HTMLInputElement>('autoStepInterval'),
        intersectionCanvas: getById<HTMLCanvasElement>('intersectionCanvas'),
        vehicleCanvas:      getById<HTMLCanvasElement>('vehicleCanvas'),
        phasePanel:         getById('phasePanel'),
        stepHistory:        getById('stepHistory'),
        eventLog:           getById('eventLog'),
        statsSteps:         getById('statsSteps'),
        statsLeft:          getById('statsLeft'),
        statsQueued:        getById('statsQueued'),
    };
}
