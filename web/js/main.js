/**
 * Composition root — wires all modules together and owns the animation loop.
 * No business logic lives here; this file only connects dependencies.
 */
import { WebSocketClient }       from './core/websocket-client.js';
import { CommandFactory }        from './app/command-factory.js';
import { SimulationController }  from './app/simulation-controller.js';
import { VehicleStore }          from './app/vehicle-store.js';
import { LogView }               from './ui/log-view.js';
import { StatusView }            from './ui/status-view.js';
import { IntersectionCanvas }    from './ui/intersection-canvas.js';
import { VehicleLayer }          from './ui/vehicle-layer.js';
import { StepHistoryView }       from './ui/step-history-view.js';
import { getDomElements }        from './ui/dom-elements.js';

// ── DOM ──────────────────────────────────────────────────────────────────────
const dom = getDomElements();

// ── Services ─────────────────────────────────────────────────────────────────
const logger       = new LogView(dom.eventLog);
const historyView  = new StepHistoryView(dom.stepHistory);
const vehicleStore = new VehicleStore();

// ── Canvas renderers ─────────────────────────────────────────────────────────
const intersectionRenderer = new IntersectionCanvas(dom.intersectionCanvas);
const vehicleRenderer      = new VehicleLayer(dom.vehicleCanvas, {
    onVehicleGone: (id) => vehicleStore.evict(id),
});

// ── Animation loop ────────────────────────────────────────────────────────────
let lastPhase = undefined;

function tick(timestamp) {
    const phase = vehicleStore.getPhase();
    if (phase !== lastPhase) {
        lastPhase = phase;
        intersectionRenderer.draw(phase);
    }
    vehicleRenderer.draw(vehicleStore.getAll(), timestamp);
    requestAnimationFrame(tick);
}

intersectionRenderer.draw(null); // draw static intersection immediately
requestAnimationFrame(tick);

// ── Status view ───────────────────────────────────────────────────────────────
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

// ── Controller (created last so callbacks can reference it via closure) ───────
let controller;

const client = new WebSocketClient({
    onOpen:    () => controller.onConnected(),
    onClose:   () => controller.onClosed(),
    onMessage: (data) => controller.onServerMessage(data),
    onError:   () => controller.onError(),
});

controller = new SimulationController({
    client,
    commandFactory: new CommandFactory(),
    logger,
    statusView,
    vehicleStore,
});

controller.onStepProcessed = (entry) => historyView.addEntry(entry);

statusView.setConnected(false);
logger.info('Aplikacja gotowa — połącz się z backendem i wyślij init.');

// ── Input handlers ────────────────────────────────────────────────────────────
function safely(action) {
    try {
        action();
    } catch (err) {
        logger.error(err.message ?? 'Nieznany błąd.');
    }
}

dom.connectBtn.addEventListener('click', () =>
    safely(() => controller.connect(dom.wsUrl.value.trim())));

dom.disconnectBtn.addEventListener('click', () =>
    safely(() => controller.disconnect()));

dom.initBtn.addEventListener('click', () =>
    safely(() => {
        controller.initialize(dom.initPayload.value);
        historyView.clear();
        vehicleStore.reset();
    }));

dom.addVehicleForm.addEventListener('submit', (e) => {
    e.preventDefault();
    safely(() => controller.addVehicle({
        vehicleId: dom.vehicleId.value,
        startRoad: dom.startRoad.value,
        endRoad:   dom.endRoad.value,
        lane:      dom.lane.value,
    }));
});

dom.stepBtn.addEventListener('click', () => safely(() => controller.step()));
dom.stopBtn.addEventListener('click', () => safely(() => controller.stop()));
