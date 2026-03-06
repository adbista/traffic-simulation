function getById(id) {
    const el = document.getElementById(id);
    if (!el) throw new Error(`Missing DOM element: #${id}`);
    return el;
}

export function getDomElements() {
    return {
        // connection
        wsUrl:             getById('wsUrl'),
        connectBtn:        getById('connectBtn'),
        disconnectBtn:     getById('disconnectBtn'),
        connectionStatus:  getById('connectionStatus'),
        // init
        initPayload:       getById('initPayload'),
        initBtn:           getById('initBtn'),
        initStatus:        getById('initStatus'),
        scenarioSelect:    getById('scenarioSelect'),
        // vehicle add
        addVehicleForm:    getById('addVehicleForm'),
        vehicleId:         getById('vehicleId'),
        startRoad:         getById('startRoad'),
        endRoad:           getById('endRoad'),
        lane:              getById('lane'),
        addVehicleBtn:     getById('addVehicleBtn'),
        // step controls
        stepBtn:           getById('stepBtn'),
        stopBtn:           getById('stopBtn'),
        autoStepCheck:     getById('autoStepCheck'),
        autoStepInterval:  getById('autoStepInterval'),
        // canvas
        intersectionCanvas: getById('intersectionCanvas'),
        vehicleCanvas:      getById('vehicleCanvas'),
        // panels
        phasePanel:         getById('phasePanel'),
        stepHistory:        getById('stepHistory'),
        eventLog:           getById('eventLog'),
        statsSteps:         getById('statsSteps'),
        statsLeft:          getById('statsLeft'),
        statsQueued:        getById('statsQueued'),
    };
}
