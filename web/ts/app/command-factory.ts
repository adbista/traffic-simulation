import type { AddVehicleParams, AddVehicleCommand } from '../types.js';

function parseOptionalLane(rawLane: string): number | undefined {
    if (!rawLane || rawLane.trim() === '') return undefined;
    const lane = Number.parseInt(rawLane, 10);
    if (Number.isNaN(lane) || lane < 0) {
        throw new Error('Pole lane musi być liczbą całkowitą >= 0.');
    }
    return lane;
}

export class CommandFactory {
    createInitRequest(rawJson: string): Record<string, unknown> {
        const trimmed = rawJson.trim();
        if (trimmed.length === 0) return {};

        const parsed: unknown = JSON.parse(trimmed);
        if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
            throw new Error('Init payload musi być obiektem JSON.');
        }
        const obj = parsed as Record<string, unknown>;

        if (obj['config'] !== undefined) return { config: obj['config'] };
        if (obj['laneDeclarations'] !== undefined) return { config: { laneDeclarations: obj['laneDeclarations'] } };
        return obj;
    }

    createAddVehicleCommand({ vehicleId, startRoad, endRoad, lane }: AddVehicleParams): AddVehicleCommand {
        if (!vehicleId || vehicleId.trim() === '') throw new Error('vehicleId jest wymagane.');
        if (!startRoad || !endRoad)                throw new Error('startRoad i endRoad są wymagane.');

        const command: AddVehicleCommand = {
            type: 'addVehicle',
            vehicleId: vehicleId.trim(),
            startRoad,
            endRoad,
        };

        const parsedLane = parseOptionalLane(lane ?? '');
        if (parsedLane !== undefined) command.lane = parsedLane;

        return command;
    }

    createStepCommand(): { type: 'step' } {
        return { type: 'step' };
    }

    createStopCommand(): { type: 'stop' } {
        return { type: 'stop' };
    }
}
