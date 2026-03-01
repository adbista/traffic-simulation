import { ROAD_COLOR, queuePos } from '../ui/layout.js';

/**
 * Single Responsibility: owns all mutable simulation state —
 * vehicle positions, queues, and inferred traffic-light phase.
 *
 * Does NOT know about canvas or DOM.
 */
export class VehicleStore {
    constructor() {
        /** @type {Map<string, VehicleState>} */
        this._vehicles = new Map();
        /** @type {'NS' | 'EW' | null} */
        this._phase = null;
        /** @type {Array<{step: number, leftVehicles: string[]}>} */
        this._history = [];
        this._stepCount = 0;
    }

    /** Register a vehicle waiting on its approach road. */
    add(id, startRoad, endRoad, lane) {
        const queueLen = this._waitingCountOnRoad(startRoad);
        this._vehicles.set(id, {
            id,
            startRoad,
            endRoad,
            lane: lane ?? 0,
            state: 'waiting',    // 'waiting' | 'leaving' | 'gone'
            queueIndex: queueLen,
            color: ROAD_COLOR[startRoad] ?? '#e2e8f0',
            animStartTime: null,
        });
    }

    /**
     * Mark vehicles as leaving, infer phase, reindex queues.
     * @returns {{ step: number, leftVehicles: string[] }} history entry
     */
    processStep(leftVehicles) {
        this._stepCount++;
        const roads = new Set(
            leftVehicles.map(id => this._vehicles.get(id)?.startRoad).filter(Boolean)
        );
        if (roads.has('north') || roads.has('south')) this._phase = 'NS';
        else if (roads.has('east') || roads.has('west')) this._phase = 'EW';

        for (const id of leftVehicles) {
            const v = this._vehicles.get(id);
            if (v) { v.state = 'leaving'; v.animStartTime = null; }
        }

        this._reindexQueues();

        const entry = { step: this._stepCount, leftVehicles: [...leftVehicles] };
        this._history.push(entry);
        return entry;
    }

    evict(id) { this._vehicles.delete(id); }

    getAll()     { return [...this._vehicles.values()]; }
    getPhase()   { return this._phase; }
    getHistory() { return [...this._history]; }

    reset() {
        this._vehicles.clear();
        this._history = [];
        this._phase = null;
        this._stepCount = 0;
    }

    _waitingCountOnRoad(road) {
        let n = 0;
        for (const v of this._vehicles.values()) {
            if (v.startRoad === road && v.state === 'waiting') n++;
        }
        return n;
    }

    _reindexQueues() {
        for (const road of ['north', 'south', 'east', 'west']) {
            let i = 0;
            for (const v of this._vehicles.values()) {
                if (v.startRoad === road && v.state === 'waiting') v.queueIndex = i++;
            }
        }
    }
}
