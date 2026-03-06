import { cfg } from './intersection-config.js';
import type { CommandFactory } from './command-factory.js';
import type { VehicleStore } from './vehicle-store.js';
import type { LogView } from '../ui/log-view.js';
import type { StatusView } from '../ui/status-view.js';
import type { WebSocketClient } from '../core/websocket-client.js';
import type { StepEntry, LightState, AddVehicleParams } from '../types.js';

interface SimulationControllerDeps {
    client:         WebSocketClient;
    commandFactory: CommandFactory;
    logger:         LogView;
    statusView:     StatusView;
    vehicleStore?:  VehicleStore | null;
}

interface ServerMessage {
    type?: string;
    phases?: Array<{ id: string; lanes: Array<{ road: string; laneIndex: number }> }>;
    error?: string;
    leftVehicles?: string[];
    activePhaseId?: string;
    phaseState?: LightState;
}

export class SimulationController {
    #client:         WebSocketClient;
    #commandFactory: CommandFactory;
    #logger:         LogView;
    #statusView:     StatusView;
    #vehicleStore:   VehicleStore | null;
    #initialized = false;

    onStepProcessed: ((entry: StepEntry) => void) | null = null;
    onConfigChanged:  (() => void) | null = null;

    constructor({ client, commandFactory, logger, statusView, vehicleStore }: SimulationControllerDeps) {
        this.#client         = client;
        this.#commandFactory = commandFactory;
        this.#logger         = logger;
        this.#statusView     = statusView;
        this.#vehicleStore   = vehicleStore ?? null;
    }

    connect(url: string):    void { this.#client.connect(url); }
    disconnect():            void { this.#client.disconnect(); }

    initialize(rawInitJson: string): void {
        const payload = this.#commandFactory.createInitRequest(rawInitJson);
        cfg.reset((payload['config'] as Parameters<typeof cfg.reset>[0]) ?? null);
        this.#client.sendJson(payload);
        this.#initialized = true;
        this.#statusView.setInitialized(true);
        this.#logger.success('Init message sent.');
        this.onConfigChanged?.();
    }

    addVehicle(params: AddVehicleParams): void {
        this.#ensureInitialized();
        const command = this.#commandFactory.createAddVehicleCommand(params);
        this.#client.sendJson(command);
        this.#vehicleStore?.add(command.vehicleId, command.startRoad, command.endRoad, command.lane);
        this.#logger.info(`addVehicle sent for ${command.vehicleId}.`);
    }

    step(): void {
        this.#ensureInitialized();
        this.#client.sendJson(this.#commandFactory.createStepCommand());
        this.#logger.info('Step sent.');
    }

    stop(): void {
        this.#ensureInitialized();
        this.#client.sendJson(this.#commandFactory.createStopCommand());
        this.#logger.info('Stop sent.');
    }

    onConnected(): void {
        this.#statusView.setConnected(true);
        this.#statusView.setInitialized(false);
        this.#initialized = false;
        this.#vehicleStore?.reset();
        this.#logger.success('Connected to WebSocket server.');
    }

    onClosed(): void {
        this.#statusView.setConnected(false);
        this.#statusView.setInitialized(false);
        this.#initialized = false;
        this.#logger.info('Connection closed.');
    }

    onError(): void {
        this.#logger.error('WebSocket transport error.');
    }

    onServerMessage(rawMessage: string): void {
        try {
            const payload = JSON.parse(rawMessage) as ServerMessage;

            if (payload.type === 'phases') {
                cfg.setPhases(payload.phases ?? []);
                this.#logger.info(`Received phase definitions: ${payload.phases?.length ?? 0} phase(s).`);
                this.onConfigChanged?.();
                return;
            }

            if (payload.error) {
                this.#logger.error(`Server error: ${payload.error}`);
                return;
            }

            const left  = payload.leftVehicles ?? [];
            const entry = this.#vehicleStore?.processStep(left)
                ?? { step: 0, leftVehicles: left, departures: [] };

            if (payload.activePhaseId) {
                cfg.applyPhaseState(payload.activePhaseId, payload.phaseState ?? 'GREEN');
            } else {
                cfg.applyDepartures(entry.departures ?? []);
            }

            this.onStepProcessed?.(entry);
            this.#logger.success(
                `Step #${entry.step}: [${left.join(', ') || 'no departures'}] phase=${payload.activePhaseId ?? '?'} (${payload.phaseState ?? '?'})`,
            );
        } catch {
            this.#logger.info(`Received non-JSON: ${rawMessage}`);
        }
    }

    #ensureInitialized(): void {
        if (!this.#initialized) throw new Error('Send init first to start a WebSocket session.');
    }
}
