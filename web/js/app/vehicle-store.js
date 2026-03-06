var __classPrivateFieldGet = (this && this.__classPrivateFieldGet) || function (receiver, state, kind, f) {
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a getter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot read private member from an object whose class did not declare it");
    return kind === "m" ? f : kind === "a" ? f.call(receiver) : f ? f.value : state.get(receiver);
};
var __classPrivateFieldSet = (this && this.__classPrivateFieldSet) || function (receiver, state, value, kind, f) {
    if (kind === "m") throw new TypeError("Private method is not writable");
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a setter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot write private member to an object whose class did not declare it");
    return (kind === "a" ? f.call(receiver, value) : f ? f.value = value : state.set(receiver, value)), value;
};
var _VehicleStore_instances, _VehicleStore_vehicles, _VehicleStore_history, _VehicleStore_stepCount, _VehicleStore_waitingCountOnLane, _VehicleStore_reindexQueues;
import { ROAD_COLOR } from './intersection-config.js';
/**
 * Owns all mutable simulation state — vehicle positions, lane queues,
 * and provides the current step-history.
 */
export class VehicleStore {
    constructor() {
        _VehicleStore_instances.add(this);
        _VehicleStore_vehicles.set(this, new Map());
        _VehicleStore_history.set(this, []);
        _VehicleStore_stepCount.set(this, 0);
    }
    add(id, startRoad, endRoad, lane) {
        const laneIdx = lane ?? 0;
        const queueLen = __classPrivateFieldGet(this, _VehicleStore_instances, "m", _VehicleStore_waitingCountOnLane).call(this, startRoad, laneIdx);
        __classPrivateFieldGet(this, _VehicleStore_vehicles, "f").set(id, {
            id,
            startRoad,
            endRoad,
            lane: laneIdx,
            state: 'waiting',
            queueIndex: queueLen,
            color: ROAD_COLOR[startRoad] ?? '#e2e8f0',
            animStartTime: null,
        });
    }
    processStep(leftVehicles) {
        var _a;
        __classPrivateFieldSet(this, _VehicleStore_stepCount, (_a = __classPrivateFieldGet(this, _VehicleStore_stepCount, "f"), _a++, _a), "f");
        const departures = leftVehicles
            .map(id => __classPrivateFieldGet(this, _VehicleStore_vehicles, "f").get(id))
            .filter((v) => v !== undefined)
            .map(v => ({ road: v.startRoad, lane: v.lane ?? 0 }));
        for (const id of leftVehicles) {
            const v = __classPrivateFieldGet(this, _VehicleStore_vehicles, "f").get(id);
            if (v) {
                v.state = 'leaving';
                v.animStartTime = null;
            }
        }
        __classPrivateFieldGet(this, _VehicleStore_instances, "m", _VehicleStore_reindexQueues).call(this);
        const entry = { step: __classPrivateFieldGet(this, _VehicleStore_stepCount, "f"), leftVehicles: [...leftVehicles], departures };
        __classPrivateFieldGet(this, _VehicleStore_history, "f").push(entry);
        return entry;
    }
    evict(id) { __classPrivateFieldGet(this, _VehicleStore_vehicles, "f").delete(id); }
    getAll() { return [...__classPrivateFieldGet(this, _VehicleStore_vehicles, "f").values()]; }
    getHistory() { return [...__classPrivateFieldGet(this, _VehicleStore_history, "f")]; }
    reset() { __classPrivateFieldGet(this, _VehicleStore_vehicles, "f").clear(); __classPrivateFieldSet(this, _VehicleStore_history, [], "f"); __classPrivateFieldSet(this, _VehicleStore_stepCount, 0, "f"); }
}
_VehicleStore_vehicles = new WeakMap(), _VehicleStore_history = new WeakMap(), _VehicleStore_stepCount = new WeakMap(), _VehicleStore_instances = new WeakSet(), _VehicleStore_waitingCountOnLane = function _VehicleStore_waitingCountOnLane(road, laneIndex) {
    let n = 0;
    for (const v of __classPrivateFieldGet(this, _VehicleStore_vehicles, "f").values()) {
        if (v.startRoad === road && (v.lane ?? 0) === laneIndex && v.state === 'waiting')
            n++;
    }
    return n;
}, _VehicleStore_reindexQueues = function _VehicleStore_reindexQueues() {
    for (const road of ['north', 'south', 'east', 'west']) {
        const byLane = new Map();
        for (const v of __classPrivateFieldGet(this, _VehicleStore_vehicles, "f").values()) {
            if (v.startRoad !== road || v.state !== 'waiting')
                continue;
            const ln = v.lane ?? 0;
            if (!byLane.has(ln))
                byLane.set(ln, 0);
            v.queueIndex = byLane.get(ln);
            byLane.set(ln, byLane.get(ln) + 1);
        }
    }
};
