function parseOptionalLane(rawLane) {
    if (!rawLane || rawLane.trim() === '')
        return undefined;
    const lane = Number.parseInt(rawLane, 10);
    if (Number.isNaN(lane) || lane < 0) {
        throw new Error('Pole lane musi być liczbą całkowitą >= 0.');
    }
    return lane;
}
export class CommandFactory {
    createInitRequest(rawJson) {
        const trimmed = rawJson.trim();
        if (trimmed.length === 0)
            return {};
        const parsed = JSON.parse(trimmed);
        if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
            throw new Error('Init payload musi być obiektem JSON.');
        }
        const obj = parsed;
        if (obj['config'] !== undefined)
            return { config: obj['config'] };
        if (obj['laneDeclarations'] !== undefined)
            return { config: { laneDeclarations: obj['laneDeclarations'] } };
        return obj;
    }
    createAddVehicleCommand({ vehicleId, startRoad, endRoad, lane }) {
        if (!vehicleId || vehicleId.trim() === '')
            throw new Error('vehicleId jest wymagane.');
        if (!startRoad || !endRoad)
            throw new Error('startRoad i endRoad są wymagane.');
        const command = {
            type: 'addVehicle',
            vehicleId: vehicleId.trim(),
            startRoad,
            endRoad,
        };
        const parsedLane = parseOptionalLane(lane ?? '');
        if (parsedLane !== undefined)
            command.lane = parsedLane;
        return command;
    }
    createStepCommand() {
        return { type: 'step' };
    }
    createStopCommand() {
        return { type: 'stop' };
    }
}
