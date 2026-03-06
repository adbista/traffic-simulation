import { ROAD_COLOR } from './intersection-config.js';
import type { Vehicle, StepEntry, Road } from '../types.js';

/**
 * Owns all mutable simulation state — vehicle positions, lane queues,
 * and provides the current step-history.
 */
export class VehicleStore {
    #vehicles  = new Map<string, Vehicle>();
    #history: StepEntry[] = [];
    #stepCount = 0;

    add(id: string, startRoad: string, endRoad: string, lane?: number): void {
        const laneIdx  = lane ?? 0;
        const queueLen = this.#waitingCountOnLane(startRoad, laneIdx);
        this.#vehicles.set(id, {
            id,
            startRoad,
            endRoad,
            lane:       laneIdx,
            state:      'waiting',
            queueIndex: queueLen,
            color:      ROAD_COLOR[startRoad as Road] ?? '#e2e8f0',
            animStartTime: null,
        });
    }

    processStep(leftVehicles: string[]): StepEntry {
        this.#stepCount++;

        const departures = leftVehicles
            .map(id => this.#vehicles.get(id))
            .filter((v): v is Vehicle => v !== undefined)
            .map(v => ({ road: v.startRoad, lane: v.lane ?? 0 }));

        for (const id of leftVehicles) {
            const v = this.#vehicles.get(id);
            if (v) { v.state = 'leaving'; v.animStartTime = null; }
        }
        this.#reindexQueues();

        const entry: StepEntry = { step: this.#stepCount, leftVehicles: [...leftVehicles], departures };
        this.#history.push(entry);
        return entry;
    }

    evict(id: string): void   { this.#vehicles.delete(id); }
    getAll(): Vehicle[]       { return [...this.#vehicles.values()]; }
    getHistory(): StepEntry[] { return [...this.#history]; }
    reset(): void             { this.#vehicles.clear(); this.#history = []; this.#stepCount = 0; }

    #waitingCountOnLane(road: string, laneIndex: number): number {
        let n = 0;
        for (const v of this.#vehicles.values()) {
            if (v.startRoad === road && (v.lane ?? 0) === laneIndex && v.state === 'waiting') n++;
        }
        return n;
    }

    #reindexQueues(): void {
        for (const road of ['north', 'south', 'east', 'west'] as Road[]) {
            const byLane = new Map<number, number>();
            for (const v of this.#vehicles.values()) {
                if (v.startRoad !== road || v.state !== 'waiting') continue;
                const ln = v.lane ?? 0;
                if (!byLane.has(ln)) byLane.set(ln, 0);
                v.queueIndex = byLane.get(ln)!;
                byLane.set(ln, byLane.get(ln)! + 1);
            }
        }
    }
}
