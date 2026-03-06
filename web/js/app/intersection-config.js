var __classPrivateFieldGet = (this && this.__classPrivateFieldGet) || function (receiver, state, kind, f) {
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a getter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot read private member from an object whose class did not declare it");
    return kind === "m" ? f : kind === "a" ? f.call(receiver) : f ? f.value : state.get(receiver);
};
var _IntersectionConfig_instances, _IntersectionConfig_init, _IntersectionConfig_parseDeclarations, _IntersectionConfig_defaultConfig, _IntersectionConfig_computeGeometry;
export const CANVAS_SIZE = 720;
export const CX = 360;
export const CY = 360;
export const LANE_W = 28; // usable width of one lane (px)
export const DIVIDER = 4; // width of center divider between inbound/outbound
export const QUEUE_STEP = 38; // px between adjacent queued vehicles
export const ANIM_DURATION_MS = 1600; // vehicle leave-animation duration (ms)
export const ROAD_COLOR = {
    north: '#f87171',
    south: '#60a5fa',
    east: '#fbbf24',
    west: '#4ade80',
};
class IntersectionConfig {
    constructor() {
        _IntersectionConfig_instances.add(this);
        __classPrivateFieldGet(this, _IntersectionConfig_instances, "m", _IntersectionConfig_init).call(this, null);
    }
    reset(simConfig) {
        __classPrivateFieldGet(this, _IntersectionConfig_instances, "m", _IntersectionConfig_init).call(this, simConfig);
    }
    laneCenter(road, laneIndex = 0) {
        const k = laneIndex ?? 0;
        switch (road.toLowerCase()) {
            case 'north': return CX - DIVIDER / 2 - (k + 0.5) * LANE_W;
            case 'south': return CX + DIVIDER / 2 + (k + 0.5) * LANE_W;
            case 'east': return CY + DIVIDER / 2 + (k + 0.5) * LANE_W;
            case 'west': return CY - DIVIDER / 2 - (k + 0.5) * LANE_W;
            default: return CX;
        }
    }
    outboundCenter(road) {
        switch (road.toLowerCase()) {
            case 'north': return CX + DIVIDER / 2 + LANE_W / 2;
            case 'south': return CX - DIVIDER / 2 - LANE_W / 2;
            case 'east': return CY - DIVIDER / 2 - LANE_W / 2;
            case 'west': return CY + DIVIDER / 2 + LANE_W / 2;
            default: return CX;
        }
    }
    getLaneCount(road) {
        return Math.max(this.lanes[road?.toLowerCase()]?.length ?? 0, 1);
    }
    setAllRed() {
        for (const l of this.lights.values())
            l.state = 'RED';
    }
    setGreen(id) {
        const l = this.lights.get(id);
        if (l)
            l.state = 'GREEN';
    }
    setYellow(id) {
        const l = this.lights.get(id);
        if (l)
            l.state = 'YELLOW';
    }
    setPhases(phases) {
        this.phases = new Map(phases.map(p => [p.id, p.lanes]));
        this.activePhaseId = null;
        this.activePhaseState = null;
    }
    applyPhaseState(activePhaseId, phaseState) {
        this.activePhaseId = activePhaseId;
        this.activePhaseState = phaseState;
        this.setAllRed();
        if (phaseState === 'RED' || !activePhaseId)
            return;
        const lanes = this.phases.get(activePhaseId);
        if (!lanes)
            return;
        for (const { road, laneIndex } of lanes) {
            const laneEntry = this.lanes[road.toLowerCase()]?.[laneIndex ?? 0];
            if (!laneEntry)
                continue;
            for (const sig of laneEntry.signals) {
                if (phaseState === 'GREEN')
                    this.setGreen(sig.id);
                else if (phaseState === 'YELLOW')
                    this.setYellow(sig.id);
            }
        }
    }
    get phaseList() {
        return [...this.phases.entries()].map(([id, lanes]) => ({
            id,
            lanes,
            active: id === this.activePhaseId,
            state: (id === this.activePhaseId ? (this.activePhaseState ?? 'RED') : 'RED'),
        }));
    }
    applyDepartures(vehicles) {
        this.setAllRed();
        for (const { road, lane } of vehicles) {
            const laneEntry = this.lanes[road.toLowerCase()]?.[lane ?? 0];
            if (!laneEntry) {
                for (const l of this.lights.values()) {
                    if (l.road === road.toLowerCase())
                        this.setGreen(l.id);
                }
                continue;
            }
            for (const sig of laneEntry.signals) {
                this.setGreen(sig.id);
            }
        }
    }
    get lightList() {
        return [...this.lights.values()];
    }
}
_IntersectionConfig_instances = new WeakSet(), _IntersectionConfig_init = function _IntersectionConfig_init(simConfig) {
    this.lanes = { north: [], south: [], east: [], west: [] };
    this.lights = new Map();
    this.timing = { minGreen: 1, maxGreen: 5, yellow: 0, red: 0 };
    this.phases = new Map();
    this.activePhaseId = null;
    this.activePhaseState = null;
    if (simConfig?.timing) {
        Object.assign(this.timing, simConfig.timing);
    }
    if (simConfig?.laneDeclarations?.length) {
        __classPrivateFieldGet(this, _IntersectionConfig_instances, "m", _IntersectionConfig_parseDeclarations).call(this, simConfig.laneDeclarations);
    }
    else {
        __classPrivateFieldGet(this, _IntersectionConfig_instances, "m", _IntersectionConfig_defaultConfig).call(this);
    }
    __classPrivateFieldGet(this, _IntersectionConfig_instances, "m", _IntersectionConfig_computeGeometry).call(this);
}, _IntersectionConfig_parseDeclarations = function _IntersectionConfig_parseDeclarations(decls) {
    for (const d of decls) {
        const road = d.road?.toLowerCase();
        if (!this.lanes[road])
            continue;
        while (this.lanes[road].length <= d.lane) {
            this.lanes[road].push({
                laneIndex: this.lanes[road].length,
                signals: [],
                priority: null,
            });
        }
        const laneEntry = this.lanes[road][d.lane];
        laneEntry.priority = d.priority ?? null;
        const byId = new Map();
        for (const m of d.movements ?? []) {
            const tid = m.trafficLightId ?? `${road}-${d.lane}-default`;
            if (!byId.has(tid)) {
                byId.set(tid, {
                    id: tid,
                    movements: [],
                    type: (m.type ?? 'GENERIC').toUpperCase(),
                    road,
                    laneIndex: d.lane,
                });
            }
            byId.get(tid).movements.push(m.movement.toUpperCase());
        }
        for (const sig of byId.values()) {
            laneEntry.signals.push({ id: sig.id, movements: sig.movements, type: sig.type });
            if (!this.lights.has(sig.id)) {
                this.lights.set(sig.id, {
                    id: sig.id,
                    road: sig.road,
                    laneIndex: sig.laneIndex,
                    movements: sig.movements,
                    type: sig.type,
                    state: 'RED',
                });
            }
        }
    }
}, _IntersectionConfig_defaultConfig = function _IntersectionConfig_defaultConfig() {
    for (const road of ['north', 'south', 'east', 'west']) {
        const id = `${road}-default`;
        this.lanes[road] = [{
                laneIndex: 0,
                signals: [{ id, movements: ['STRAIGHT', 'LEFT', 'RIGHT'], type: 'GENERIC' }],
                priority: null,
            }];
        this.lights.set(id, {
            id, road, laneIndex: 0,
            movements: ['STRAIGHT', 'LEFT', 'RIGHT'],
            type: 'GENERIC',
            state: 'RED',
        });
    }
}, _IntersectionConfig_computeGeometry = function _IntersectionConfig_computeGeometry() {
    const nNS = Math.max(this.lanes.north.length, this.lanes.south.length, 1);
    const nEW = Math.max(this.lanes.east.length, this.lanes.west.length, 1);
    this.roadHalfNS = (nNS + 1) * LANE_W + DIVIDER;
    this.roadHalfEW = (nEW + 1) * LANE_W + DIVIDER;
    this.INT = {
        left: CX - this.roadHalfNS,
        right: CX + this.roadHalfNS,
        top: CY - this.roadHalfEW,
        bottom: CY + this.roadHalfEW,
    };
};
/** Shared singleton used by all renderer/logic modules. Mutate via cfg.reset(). */
export const cfg = new IntersectionConfig();
