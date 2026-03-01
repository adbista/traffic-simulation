export class SimulationController {
    constructor({ client, commandFactory, logger, statusView, vehicleStore }) {
        this.client = client;
        this.commandFactory = commandFactory;
        this.logger = logger;
        this.statusView = statusView;
        this.vehicleStore = vehicleStore ?? null;
        this.initialized = false;
        /** Callback invoked after processStep, receives the history entry. */
        this.onStepProcessed = null;
    }

    connect(url) {
        this.client.connect(url);
    }

    disconnect() {
        this.client.disconnect();
    }

    initialize(rawInitJson) {
        const payload = this.commandFactory.createInitRequest(rawInitJson);
        this.client.sendJson(payload);
        this.initialized = true;
        this.statusView.setInitialized(true);
        this.logger.success("Wysłano wiadomość inicjalizacyjną.");
    }

    addVehicle({ vehicleId, startRoad, endRoad, lane }) {
        this.ensureInitialized();
        const command = this.commandFactory.createAddVehicleCommand({ vehicleId, startRoad, endRoad, lane });
        this.client.sendJson(command);
        this.vehicleStore?.add(command.vehicleId, command.startRoad, command.endRoad, command.lane);
        this.logger.info(`Wysłano addVehicle dla ${command.vehicleId}.`);
    }

    step() {
        this.ensureInitialized();
        this.client.sendJson(this.commandFactory.createStepCommand());
        this.logger.info("Wysłano step.");
    }

    stop() {
        this.ensureInitialized();
        this.client.sendJson(this.commandFactory.createStopCommand());
        this.logger.info("Wysłano stop.");
    }

    onConnected() {
        this.statusView.setConnected(true);
        this.statusView.setInitialized(false);
        this.initialized = false;
        this.vehicleStore?.reset();
        this.logger.success('Połączono z serwerem WebSocket.');
    }

    onClosed() {
        this.statusView.setConnected(false);
        this.statusView.setInitialized(false);
        this.initialized = false;
        this.logger.info('Połączenie zostało zamknięte.');
    }

    onError() {
        this.logger.error("Błąd transportu WebSocket.");
    }

    onServerMessage(rawMessage) {
        try {
            const payload = JSON.parse(rawMessage);

            if (payload.error) {
                this.logger.error(`Błąd serwera: ${payload.error}`);
                return;
            }

            const left = payload.leftVehicles ?? [];
            const entry = this.vehicleStore?.processStep(left)
                ?? { step: '?', leftVehicles: left };

            this.onStepProcessed?.(entry);
            this.logger.success(
                `Odebrano StepStatus #${entry.step}: [${left.join(', ') || 'brak'}]`
            );
        } catch {
            this.logger.info(`Odebrano nie-JSON: ${rawMessage}`);
        }
    }

    ensureInitialized() {
        if (!this.initialized) {
            throw new Error("Najpierw wyślij init dla sesji WebSocket.");
        }
    }
}
