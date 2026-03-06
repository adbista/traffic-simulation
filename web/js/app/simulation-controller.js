import { cfg } from './intersection-config.js';

export class SimulationController {
    constructor({ client, commandFactory, logger, statusView, vehicleStore }) {
        this.client         = client;
        this.commandFactory = commandFactory;
        this.logger         = logger;
        this.statusView     = statusView;
        this.vehicleStore   = vehicleStore ?? null;
        this.initialized    = false;
        /** Called after each step: receives history entry. */
        this.onStepProcessed  = null;
        /** Called after config is applied: no args. */
        this.onConfigChanged  = null;
    }

    connect(url)    { this.client.connect(url); }
    disconnect()    { this.client.disconnect(); }

    initialize(rawInitJson) {
        const payload = this.commandFactory.createInitRequest(rawInitJson);
        // Apply config to the shared singleton
        cfg.reset(payload.config ?? null);
        this.client.sendJson(payload);
        this.initialized = true;
        this.statusView.setInitialized(true);
        this.logger.success('Init message sent.');
        this.onConfigChanged?.();
    }

    addVehicle({ vehicleId, startRoad, endRoad, lane }) {
        this.ensureInitialized();
        const command = this.commandFactory.createAddVehicleCommand({ vehicleId, startRoad, endRoad, lane });
        this.client.sendJson(command);
        this.vehicleStore?.add(command.vehicleId, command.startRoad, command.endRoad, command.lane);
        this.logger.info(`addVehicle sent for ${command.vehicleId}.`);
    }

    step() {
        this.ensureInitialized();
        this.client.sendJson(this.commandFactory.createStepCommand());
        this.logger.info('Step sent.');
    }

    stop() {
        this.ensureInitialized();
        this.client.sendJson(this.commandFactory.createStopCommand());
        this.logger.info('Stop sent.');
    }

    onConnected() {
        this.statusView.setConnected(true);
        this.statusView.setInitialized(false);
        this.initialized = false;
        this.vehicleStore?.reset();
        this.logger.success('Connected to WebSocket server.');
    }

    onClosed() {
        this.statusView.setConnected(false);
        this.statusView.setInitialized(false);
        this.initialized = false;
        this.logger.info('Connection closed.');
    }

    onError() {
        this.logger.error('WebSocket transport error.');
    }

    onServerMessage(rawMessage) {
        try {
            const payload = JSON.parse(rawMessage);

            // Phases-info frame sent once after init
            if (payload.type === 'phases') {
                cfg.setPhases(payload.phases ?? []);
                this.logger.info(`Received phase definitions: ${payload.phases?.length ?? 0} phase(s).`);
                this.onConfigChanged?.();
                return;
            }

            if (payload.error) {
                this.logger.error(`Server error: ${payload.error}`);
                return;
            }

            const left  = payload.leftVehicles ?? [];
            const entry = this.vehicleStore?.processStep(left)
                ?? { step: '?', leftVehicles: left, departures: [] };

            // Apply server-authoritative phase state
            if (payload.activePhaseId) {
                cfg.applyPhaseState(payload.activePhaseId, payload.phaseState ?? 'GREEN');
            } else {
                // Fallback: infer from departures (legacy)
                cfg.applyDepartures(entry.departures ?? []);
            }

            this.onStepProcessed?.(entry);
            this.logger.success(
                `Step #${entry.step}: [${left.join(', ') || 'no departures'}] phase=${payload.activePhaseId ?? '?'} (${payload.phaseState ?? '?'})`
            );
        } catch {
            this.logger.info(`Received non-JSON: ${rawMessage}`);
        }
    }

    ensureInitialized() {
        if (!this.initialized) throw new Error('Send init first to start a WebSocket session.');
    }
}
