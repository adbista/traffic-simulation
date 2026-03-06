function getById(id) {
    const el = document.getElementById(id);
    if (!el)
        throw new Error(`Missing DOM element: #${id}`);
    return el;
}
export function getDomElements() {
    return {
        wsUrl: getById('wsUrl'),
        connectBtn: getById('connectBtn'),
        disconnectBtn: getById('disconnectBtn'),
        connectionStatus: getById('connectionStatus'),
        initPayload: getById('initPayload'),
        initBtn: getById('initBtn'),
        initStatus: getById('initStatus'),
        scenarioSelect: getById('scenarioSelect'),
        addVehicleForm: getById('addVehicleForm'),
        vehicleId: getById('vehicleId'),
        startRoad: getById('startRoad'),
        endRoad: getById('endRoad'),
        lane: getById('lane'),
        addVehicleBtn: getById('addVehicleBtn'),
        stepBtn: getById('stepBtn'),
        stopBtn: getById('stopBtn'),
        autoStepCheck: getById('autoStepCheck'),
        autoStepInterval: getById('autoStepInterval'),
        intersectionCanvas: getById('intersectionCanvas'),
        vehicleCanvas: getById('vehicleCanvas'),
        phasePanel: getById('phasePanel'),
        stepHistory: getById('stepHistory'),
        eventLog: getById('eventLog'),
        statsSteps: getById('statsSteps'),
        statsLeft: getById('statsLeft'),
        statsQueued: getById('statsQueued'),
    };
}
