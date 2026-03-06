import { WebSocketClient }      from './core/websocket-client.js';
import { CommandFactory }       from './app/command-factory.js';
import { SimulationController } from './app/simulation-controller.js';
import { VehicleStore }         from './app/vehicle-store.js';
import { cfg }                  from './app/intersection-config.js';
import { LogView }              from './ui/log-view.js';
import { StatusView }           from './ui/status-view.js';
import { IntersectionCanvas }   from './ui/intersection-canvas.js';
import { VehicleLayer }         from './ui/vehicle-layer.js';
import { StepHistoryView }      from './ui/step-history-view.js';
import { PhaseView }            from './ui/phase-view.js';
import { getDomElements }       from './ui/dom-elements.js';

// DOM
const dom = getDomElements();
// Auto-set WS URL only when the page is served by the backend itself (same origin).
// When using a separate dev server (e.g. python -m http.server 5500), keep the
// hardcoded default from the HTML attribute so it still points to Spring Boot.
{
    const _proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const defaultBackendPort = '8080';
    const servedByBackend = location.port === defaultBackendPort || location.port === '';
    if (servedByBackend) {
        dom.wsUrl.value = `${_proto}//${location.host}/v1/ws/simulation`;
    }
}

// Services
const logger      = new LogView(dom.eventLog);
const historyView = new StepHistoryView(dom.stepHistory);
const phaseView   = new PhaseView(dom.phasePanel);
const store       = new VehicleStore();

// Renderers
const intRenderer  = new IntersectionCanvas(dom.intersectionCanvas);
const vehRenderer  = new VehicleLayer(dom.vehicleCanvas, {
    onVehicleGone: (id) => store.evict(id),
});

// Stats
let totalLeft = 0;
function updateStats() {
    dom.statsSteps.textContent  = store.getHistory().length;
    dom.statsLeft.textContent   = totalLeft;
    dom.statsQueued.textContent = store.getAll().filter(v => v.state === 'waiting').length;
}

// Animation loop
let needsRedraw = true;

function tick(timestamp) {
    if (needsRedraw) {
        intRenderer.draw();
        phaseView.render();
        needsRedraw = false;
    }
    vehRenderer.draw(store.getAll(), timestamp);
    updateStats();
    requestAnimationFrame(tick);
}
intRenderer.draw();
phaseView.render();
requestAnimationFrame(tick);

// Auto-step
let autoTimer = null;

function startAutoStep() {
    if (autoTimer) return;
    const ms = Math.max(300, parseInt(dom.autoStepInterval.value, 10) || 1000);
    autoTimer = setInterval(() => {
        try { controller.step(); } catch { stopAutoStep(); }
    }, ms);
}
function stopAutoStep() {
    if (autoTimer) { clearInterval(autoTimer); autoTimer = null; }
    dom.autoStepCheck.checked = false;
}

dom.autoStepCheck.addEventListener('change', () => {
    if (dom.autoStepCheck.checked) startAutoStep(); else stopAutoStep();
});
dom.autoStepInterval.addEventListener('change', () => {
    if (autoTimer) { stopAutoStep(); startAutoStep(); }
});

// Scenario presets
const SCENARIOS = {
    'default': '{}',
    'multilane-ns': JSON.stringify({
        config: {
            laneDeclarations: [
                { road: 'north', lane: 0, movements: [{ movement: 'STRAIGHT', type: 'GENERIC', trafficLightId: 'n0' }, { movement: 'RIGHT', type: 'GENERIC', trafficLightId: 'n0' }] },
                { road: 'north', lane: 1, movements: [{ movement: 'LEFT', type: 'GENERIC', trafficLightId: 'n1' }] },
                { road: 'south', lane: 0, movements: [{ movement: 'STRAIGHT', type: 'GENERIC', trafficLightId: 's0' }, { movement: 'RIGHT', type: 'GENERIC', trafficLightId: 's0' }] },
                { road: 'south', lane: 1, movements: [{ movement: 'LEFT', type: 'GENERIC', trafficLightId: 's1' }] },
                { road: 'east',  lane: 0, movements: [{ movement: 'STRAIGHT', type: 'GENERIC', trafficLightId: 'e0' }, { movement: 'LEFT', type: 'GENERIC', trafficLightId: 'e0' }, { movement: 'RIGHT', type: 'GENERIC', trafficLightId: 'e0' }] },
                { road: 'west',  lane: 0, movements: [{ movement: 'STRAIGHT', type: 'GENERIC', trafficLightId: 'w0' }, { movement: 'LEFT', type: 'GENERIC', trafficLightId: 'w0' }, { movement: 'RIGHT', type: 'GENERIC', trafficLightId: 'w0' }] },
            ]
        }
    }, null, 2),
    'protected-left': JSON.stringify({
        config: {
            timing: { minGreen: 1, maxGreen: 2, yellow: 0, red: 1 },
            laneDeclarations: [
                { road: 'north', lane: 0, movements: [{ movement: 'STRAIGHT', type: 'GENERIC', trafficLightId: 'n0' }] },
                { road: 'north', lane: 1, movements: [{ movement: 'LEFT', type: 'PROTECTED', trafficLightId: 'n1-arrow' }] },
                { road: 'south', lane: 0, movements: [{ movement: 'STRAIGHT', type: 'GENERIC', trafficLightId: 's0' }, { movement: 'RIGHT', type: 'GENERIC', trafficLightId: 's0' }, { movement: 'LEFT', type: 'GENERIC', trafficLightId: 's0' }] },
                { road: 'east',  lane: 0, movements: [{ movement: 'STRAIGHT', type: 'GENERIC', trafficLightId: 'e0' }, { movement: 'RIGHT', type: 'GENERIC', trafficLightId: 'e0' }, { movement: 'LEFT', type: 'GENERIC', trafficLightId: 'e0' }] },
                { road: 'west',  lane: 0, movements: [{ movement: 'STRAIGHT', type: 'GENERIC', trafficLightId: 'w0' }, { movement: 'RIGHT', type: 'GENERIC', trafficLightId: 'w0' }, { movement: 'LEFT', type: 'GENERIC', trafficLightId: 'w0' }] },
            ]
        }
    }, null, 2),
    'grouped-signals': JSON.stringify({
        config: {
            timing: { minGreen: 2, maxGreen: 10, yellow: 1, red: 1 },
            laneDeclarations: [
                { road: 'north', lane: 0, movements: [{ movement: 'STRAIGHT', type: 'GENERIC', trafficLightId: 'n0-main' }, { movement: 'RIGHT', type: 'GENERIC', trafficLightId: 'n0-main' }] },
                { road: 'north', lane: 1, movements: [{ movement: 'LEFT', type: 'PROTECTED', trafficLightId: 'n1-arrow' }] },
                { road: 'south', lane: 0, movements: [{ movement: 'STRAIGHT', type: 'GENERIC', trafficLightId: 's0-main' }, { movement: 'RIGHT', type: 'GENERIC', trafficLightId: 's0-main' }, { movement: 'LEFT', type: 'GENERIC', trafficLightId: 's0-main' }] },
                { road: 'east',  lane: 0, movements: [{ movement: 'STRAIGHT', type: 'GENERIC', trafficLightId: 'e0' }, { movement: 'RIGHT', type: 'GENERIC', trafficLightId: 'e0' }, { movement: 'LEFT', type: 'GENERIC', trafficLightId: 'e0' }] },
                { road: 'west',  lane: 0, movements: [{ movement: 'STRAIGHT', type: 'GENERIC', trafficLightId: 'w0' }, { movement: 'RIGHT', type: 'GENERIC', trafficLightId: 'w0' }, { movement: 'LEFT', type: 'GENERIC', trafficLightId: 'w0' }] },
            ]
        }
    }, null, 2),
};

dom.scenarioSelect.addEventListener('change', () => {
    const val = dom.scenarioSelect.value;
    if (SCENARIOS[val] !== undefined) dom.initPayload.value = SCENARIOS[val];
});

// --- Vehicle form: smart lane/endRoad dropdowns ---
// Road cycle matches Java Road enum ordinals: NORTH=0, WEST=1, SOUTH=2, EAST=3
const ROAD_CYCLE = ['north', 'west', 'south', 'east'];
const ROAD_LABEL = { north: '\u2191 north', west: '\u2190 west', south: '\u2193 south', east: '\u2192 east' };
const MOVEMENT_OFFSET = { RIGHT: 1, STRAIGHT: 2, LEFT: 3 };

function movementToEndRoad(fromRoad, movement) {
    const idx = ROAD_CYCLE.indexOf(fromRoad);
    return ROAD_CYCLE[(idx + MOVEMENT_OFFSET[movement]) % 4];
}

function getValidLaneIndices(road) {
    const lanes = cfg.lanes[road] ?? [];
    return lanes.length > 0 ? lanes.map(l => l.laneIndex) : [0];
}

function getValidEndRoads(startRoad, laneIndex) {
    const lanes = cfg.lanes[startRoad] ?? [];
    const laneEntry = lanes[laneIndex];
    if (!laneEntry || laneEntry.signals.length === 0) {
        // Default config: all three directions
        return ROAD_CYCLE.filter(r => r !== startRoad);
    }
    const movements = new Set(laneEntry.signals.flatMap(s => s.movements));
    const ends = [...movements].map(m => movementToEndRoad(startRoad, m));
    return [...new Set(ends)].filter(r => r !== startRoad);
}

function updateEndRoadSelect() {
    const startRoad = dom.startRoad.value;
    const laneIdx = parseInt(dom.lane.value, 10) || 0;
    const validEnds = getValidEndRoads(startRoad, laneIdx);
    const current = dom.endRoad.value;
    dom.endRoad.innerHTML = validEnds.map(r => `<option value="${r}">${ROAD_LABEL[r]}</option>`).join('');
    if (validEnds.includes(current)) dom.endRoad.value = current;
}

function updateVehicleForm() {
    const startRoad = dom.startRoad.value;
    const validLanes = getValidLaneIndices(startRoad);
    const currentLane = parseInt(dom.lane.value, 10);
    dom.lane.innerHTML = validLanes.map(i => `<option value="${i}">${i}</option>`).join('');
    if (validLanes.includes(currentLane)) dom.lane.value = String(currentLane);
    updateEndRoadSelect();
}

let vehicleCounter = 1;
function nextVehicleId() { return `v-${vehicleCounter}`; }

dom.startRoad.addEventListener('change', updateVehicleForm);
dom.lane.addEventListener('change', updateEndRoadSelect);
updateVehicleForm();
dom.vehicleId.value = nextVehicleId();

// Status view
const statusView = new StatusView({
    connectionStatusElement: dom.connectionStatus,
    initStatusElement:       dom.initStatus,
    controls: {
        connectBtn:    dom.connectBtn,
        disconnectBtn: dom.disconnectBtn,
        initBtn:       dom.initBtn,
        addVehicleBtn: dom.addVehicleBtn,
        stepBtn:       dom.stepBtn,
        stopBtn:       dom.stopBtn,
    },
});

// Controller
let controller;

const client = new WebSocketClient({
    onOpen:    () => controller.onConnected(),
    onClose:   () => { controller.onClosed(); stopAutoStep(); },
    onMessage: (data) => controller.onServerMessage(data),
    onError:   () => controller.onError(),
});

controller = new SimulationController({
    client,
    commandFactory: new CommandFactory(),
    logger,
    statusView,
    vehicleStore: store,
});

controller.onConfigChanged = () => {
    needsRedraw = true;
    updateVehicleForm();
};

controller.onStepProcessed = (entry) => {
    totalLeft += entry.leftVehicles.length;
    historyView.addEntry(entry);
    needsRedraw = true;
};

// Button wiring
dom.connectBtn.addEventListener('click', () => {
    try { controller.connect(dom.wsUrl.value.trim()); }
    catch (e) { logger.error(e.message); }
});

dom.disconnectBtn.addEventListener('click', () => {
    stopAutoStep();
    controller.disconnect();
});

dom.initBtn.addEventListener('click', () => {
    try {
        controller.initialize(dom.initPayload.value);
        store.reset();
        totalLeft = 0;
        vehicleCounter = 1;
        historyView.clear();
        dom.vehicleId.value = nextVehicleId();
    } catch (e) { logger.error(e.message); }
});

dom.addVehicleForm.addEventListener('submit', (e) => {
    e.preventDefault();
    try {
        controller.addVehicle({
            vehicleId: dom.vehicleId.value,
            startRoad: dom.startRoad.value,
            endRoad:   dom.endRoad.value,
            lane:      dom.lane.value,
        });
        vehicleCounter++;
        dom.vehicleId.value = nextVehicleId();
    } catch (e) { logger.error(e.message); }
});

dom.stepBtn.addEventListener('click', () => {
    try { controller.step(); } catch (e) { logger.error(e.message); }
});

dom.stopBtn.addEventListener('click', () => {
    try { stopAutoStep(); controller.stop(); } catch (e) { logger.error(e.message); }
});
