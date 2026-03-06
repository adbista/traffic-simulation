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
var _SimulationController_instances, _SimulationController_client, _SimulationController_commandFactory, _SimulationController_logger, _SimulationController_statusView, _SimulationController_vehicleStore, _SimulationController_initialized, _SimulationController_ensureInitialized;
import { cfg } from './intersection-config.js';
export class SimulationController {
    constructor({ client, commandFactory, logger, statusView, vehicleStore }) {
        _SimulationController_instances.add(this);
        _SimulationController_client.set(this, void 0);
        _SimulationController_commandFactory.set(this, void 0);
        _SimulationController_logger.set(this, void 0);
        _SimulationController_statusView.set(this, void 0);
        _SimulationController_vehicleStore.set(this, void 0);
        _SimulationController_initialized.set(this, false);
        this.onStepProcessed = null;
        this.onConfigChanged = null;
        __classPrivateFieldSet(this, _SimulationController_client, client, "f");
        __classPrivateFieldSet(this, _SimulationController_commandFactory, commandFactory, "f");
        __classPrivateFieldSet(this, _SimulationController_logger, logger, "f");
        __classPrivateFieldSet(this, _SimulationController_statusView, statusView, "f");
        __classPrivateFieldSet(this, _SimulationController_vehicleStore, vehicleStore ?? null, "f");
    }
    connect(url) { __classPrivateFieldGet(this, _SimulationController_client, "f").connect(url); }
    disconnect() { __classPrivateFieldGet(this, _SimulationController_client, "f").disconnect(); }
    initialize(rawInitJson) {
        const payload = __classPrivateFieldGet(this, _SimulationController_commandFactory, "f").createInitRequest(rawInitJson);
        cfg.reset(payload['config'] ?? null);
        __classPrivateFieldGet(this, _SimulationController_client, "f").sendJson(payload);
        __classPrivateFieldSet(this, _SimulationController_initialized, true, "f");
        __classPrivateFieldGet(this, _SimulationController_statusView, "f").setInitialized(true);
        __classPrivateFieldGet(this, _SimulationController_logger, "f").success('Init message sent.');
        this.onConfigChanged?.();
    }
    addVehicle(params) {
        __classPrivateFieldGet(this, _SimulationController_instances, "m", _SimulationController_ensureInitialized).call(this);
        const command = __classPrivateFieldGet(this, _SimulationController_commandFactory, "f").createAddVehicleCommand(params);
        __classPrivateFieldGet(this, _SimulationController_client, "f").sendJson(command);
        __classPrivateFieldGet(this, _SimulationController_vehicleStore, "f")?.add(command.vehicleId, command.startRoad, command.endRoad, command.lane);
        __classPrivateFieldGet(this, _SimulationController_logger, "f").info(`addVehicle sent for ${command.vehicleId}.`);
    }
    step() {
        __classPrivateFieldGet(this, _SimulationController_instances, "m", _SimulationController_ensureInitialized).call(this);
        __classPrivateFieldGet(this, _SimulationController_client, "f").sendJson(__classPrivateFieldGet(this, _SimulationController_commandFactory, "f").createStepCommand());
        __classPrivateFieldGet(this, _SimulationController_logger, "f").info('Step sent.');
    }
    stop() {
        __classPrivateFieldGet(this, _SimulationController_instances, "m", _SimulationController_ensureInitialized).call(this);
        __classPrivateFieldGet(this, _SimulationController_client, "f").sendJson(__classPrivateFieldGet(this, _SimulationController_commandFactory, "f").createStopCommand());
        __classPrivateFieldGet(this, _SimulationController_logger, "f").info('Stop sent.');
    }
    onConnected() {
        __classPrivateFieldGet(this, _SimulationController_statusView, "f").setConnected(true);
        __classPrivateFieldGet(this, _SimulationController_statusView, "f").setInitialized(false);
        __classPrivateFieldSet(this, _SimulationController_initialized, false, "f");
        __classPrivateFieldGet(this, _SimulationController_vehicleStore, "f")?.reset();
        __classPrivateFieldGet(this, _SimulationController_logger, "f").success('Connected to WebSocket server.');
    }
    onClosed() {
        __classPrivateFieldGet(this, _SimulationController_statusView, "f").setConnected(false);
        __classPrivateFieldGet(this, _SimulationController_statusView, "f").setInitialized(false);
        __classPrivateFieldSet(this, _SimulationController_initialized, false, "f");
        __classPrivateFieldGet(this, _SimulationController_logger, "f").info('Connection closed.');
    }
    onError() {
        __classPrivateFieldGet(this, _SimulationController_logger, "f").error('WebSocket transport error.');
    }
    onServerMessage(rawMessage) {
        try {
            const payload = JSON.parse(rawMessage);
            if (payload.type === 'phases') {
                cfg.setPhases(payload.phases ?? []);
                __classPrivateFieldGet(this, _SimulationController_logger, "f").info(`Received phase definitions: ${payload.phases?.length ?? 0} phase(s).`);
                this.onConfigChanged?.();
                return;
            }
            if (payload.error) {
                __classPrivateFieldGet(this, _SimulationController_logger, "f").error(`Server error: ${payload.error}`);
                return;
            }
            const left = payload.leftVehicles ?? [];
            const entry = __classPrivateFieldGet(this, _SimulationController_vehicleStore, "f")?.processStep(left)
                ?? { step: 0, leftVehicles: left, departures: [] };
            if (payload.activePhaseId) {
                cfg.applyPhaseState(payload.activePhaseId, payload.phaseState ?? 'GREEN');
            }
            else {
                cfg.applyDepartures(entry.departures ?? []);
            }
            this.onStepProcessed?.(entry);
            __classPrivateFieldGet(this, _SimulationController_logger, "f").success(`Step #${entry.step}: [${left.join(', ') || 'no departures'}] phase=${payload.activePhaseId ?? '?'} (${payload.phaseState ?? '?'})`);
        }
        catch {
            __classPrivateFieldGet(this, _SimulationController_logger, "f").info(`Received non-JSON: ${rawMessage}`);
        }
    }
}
_SimulationController_client = new WeakMap(), _SimulationController_commandFactory = new WeakMap(), _SimulationController_logger = new WeakMap(), _SimulationController_statusView = new WeakMap(), _SimulationController_vehicleStore = new WeakMap(), _SimulationController_initialized = new WeakMap(), _SimulationController_instances = new WeakSet(), _SimulationController_ensureInitialized = function _SimulationController_ensureInitialized() {
    if (!__classPrivateFieldGet(this, _SimulationController_initialized, "f"))
        throw new Error('Send init first to start a WebSocket session.');
};
