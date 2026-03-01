function getById(id) {
    const element = document.getElementById(id);
    if (!element) throw new Error(`Missing DOM element: #${id}`);
    return element;
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
        // commands
        addVehicleForm:    getById('addVehicleForm'),
        vehicleId:         getById('vehicleId'),
        startRoad:         getById('startRoad'),
        endRoad:           getById('endRoad'),
        lane:              getById('lane'),
        addVehicleBtn:     getById('addVehicleBtn'),
        stepBtn:           getById('stepBtn'),
        stopBtn:           getById('stopBtn'),
        // visualisation
        intersectionCanvas: getById('intersectionCanvas'),
        vehicleCanvas:      getById('vehicleCanvas'),
        stepHistory:        getById('stepHistory'),
        // log
        eventLog:           getById('eventLog'),
    };
}
