import { ROAD_COLOR } from './intersection-config.js';

/**
 * Owns all mutable simulation state — vehicle positions, lane queues,
 * and provides the current step-history. Does not know about DOM or canvas.
 */
export class VehicleStore {
    constructor() {
        this._vehicles   = new Map();
        this._history    = [];
        this._stepCount  = 0;
    }

    /** Register a vehicle waiting on its approach road / lane. */
    add(id, startRoad, endRoad, lane) {
        const laneIdx  = lane ?? 0;
        const queueLen = this._waitingCountOnLane(startRoad, laneIdx);
        this._vehicles.set(id, {
            id,
            startRoad,
            endRoad,
            lane:       laneIdx,
            state:      'waiting',
            queueIndex: queueLen,
            color:      ROAD_COLOR[startRoad] ?? '#e2e8f0',
            animStartTime: null,
        });
    }

    /**
     * Mark vehicles as leaving, reindex queues.
     * @returns {{ step: number, leftVehicles: string[], departures: {road,lane}[] }}
     */
    processStep(leftVehicles) {
        this._stepCount++;

        const departures = leftVehicles
            .map(id => this._vehicles.get(id))
            .filter(Boolean)
            .map(v => ({ road: v.startRoad, lane: v.lane ?? 0 }));

        for (const id of leftVehicles) {
            const v = this._vehicles.get(id);
            if (v) { v.state = 'leaving'; v.animStartTime = null; }
        }
        this._reindexQueues();

        const entry = { step: this._stepCount, leftVehicles: [...leftVehicles], departures };
        this._history.push(entry);
        return entry;
    }

    evict(id)   { this._vehicles.delete(id); }
    getAll()    { return [...this._vehicles.values()]; }
    getHistory(){ return [...this._history]; }
    reset()     { this._vehicles.clear(); this._history = []; this._stepCount = 0; }

    _waitingCountOnLane(road, laneIndex) {
        let n = 0;
        for (const v of this._vehicles.values()) {
            if (v.startRoad === road && (v.lane ?? 0) === laneIndex && v.state === 'waiting') n++;
        }
        return n;
    }

    _reindexQueues() {
        for (const road of ['north', 'south', 'east', 'west']) {
            const byLane = new Map();
            for (const v of this._vehicles.values()) {
                if (v.startRoad !== road || v.state !== 'waiting') continue;
                const ln = v.lane ?? 0;
                if (!byLane.has(ln)) byLane.set(ln, 0);
                v.queueIndex = byLane.get(ln);
                byLane.set(ln, byLane.get(ln) + 1);
            }
        }
    }
}
