/**
 * Intersection configuration singleton.
 * Parses WsInitRequest.config (SimConfig), computes dynamic road geometry,
 * and tracks real-time traffic-light states.
 *
 * All geometry is expressed in the 720×720 canvas coordinate system.
 */

export const CANVAS_SIZE = 720;
export const CX = 360;
export const CY = 360;
export const LANE_W  = 28;   // usable width of one lane (px)
export const DIVIDER = 4;    // width of center divider between inbound/outbound
export const QUEUE_STEP      = 38;    // px between adjacent queued vehicles
export const ANIM_DURATION_MS = 1600; // vehicle leave-animation duration (ms)

export const ROAD_COLOR = {
    north: '#f87171',
    south: '#60a5fa',
    east:  '#fbbf24',
    west:  '#4ade80',
};

// ── Internal mutable config class ─────────────────────────────────────────────
class IntersectionConfig {
    constructor() {
        this._init(null);
    }

    /** Re-initialize from a SimConfig object (or null for defaults). */
    reset(simConfig) {
        this._init(simConfig);
    }

    _init(simConfig) {
        /** @type {{ north: LaneEntry[], south: LaneEntry[], east: LaneEntry[], west: LaneEntry[] }} */
        this.lanes = { north: [], south: [], east: [], west: [] };
        /** trafficLightId → LightState */
        this.lights = new Map();
        this.timing = { minGreen: 1, maxGreen: 5, yellow: 0, red: 0 };
        /** phase id → Array<{road, laneIndex}> */
        this.phases = new Map();
        this.activePhaseId    = null;
        this.activePhaseState = null;   // 'GREEN' | 'YELLOW' | 'RED'

        if (simConfig?.timing) {
            Object.assign(this.timing, simConfig.timing);
        }

        if (simConfig?.laneDeclarations?.length) {
            this._parseDeclarations(simConfig.laneDeclarations);
        } else {
            this._defaultConfig();
        }

        this._computeGeometry();
    }

    _parseDeclarations(decls) {
        for (const d of decls) {
            const road = d.road?.toLowerCase();
            if (!this.lanes[road]) continue;

            // Grow lane array to accommodate this lane index
            while (this.lanes[road].length <= d.lane) {
                this.lanes[road].push({
                    laneIndex: this.lanes[road].length,
                    signals: [],
                    priority: null,
                });
            }
            const laneEntry = this.lanes[road][d.lane];
            laneEntry.priority = d.priority || null;

            // Group movements by trafficLightId
            const byId = new Map();
            for (const m of d.movements || []) {
                const tid = m.trafficLightId || `${road}-${d.lane}-default`;
                if (!byId.has(tid)) {
                    byId.set(tid, {
                        id: tid,
                        movements: [],
                        type: (m.type || 'GENERIC').toUpperCase(),
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
                        type: sig.type,       // 'GENERIC' | 'PROTECTED'
                        state: 'RED',         // 'RED' | 'YELLOW' | 'GREEN'
                    });
                }
            }
        }
    }

    _defaultConfig() {
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
    }

    _computeGeometry() {
        const nNS = Math.max(this.lanes.north.length, this.lanes.south.length, 1);
        const nEW = Math.max(this.lanes.east.length,  this.lanes.west.length,  1);

        // road half = (inbound lanes + 1 outbound lane) * LANE_W + DIVIDER
        this.roadHalfNS = (nNS + 1) * LANE_W + DIVIDER;
        this.roadHalfEW = (nEW + 1) * LANE_W + DIVIDER;

        this.INT = {
            left:   CX - this.roadHalfNS,
            right:  CX + this.roadHalfNS,
            top:    CY - this.roadHalfEW,
            bottom: CY + this.roadHalfEW,
        };
    }

    // ── Geometry helpers ────────────────────────────────────────────────────

    /** X (for N/S) or Y (for E/W) coordinate of inbound lane centre. */
    laneCenter(road, laneIndex = 0) {
        const k = laneIndex ?? 0;
        switch (road.toLowerCase()) {
            case 'north': return CX - DIVIDER / 2 - (k + 0.5) * LANE_W;
            case 'south': return CX + DIVIDER / 2 + (k + 0.5) * LANE_W;
            case 'east':  return CY + DIVIDER / 2 + (k + 0.5) * LANE_W;
            case 'west':  return CY - DIVIDER / 2 - (k + 0.5) * LANE_W;
            default: return CX;
        }
    }

    /** X (for N/S) or Y (for E/W) coordinate of the outbound lane. */
    outboundCenter(road) {
        switch (road.toLowerCase()) {
            case 'north': return CX + DIVIDER / 2 + LANE_W / 2;
            case 'south': return CX - DIVIDER / 2 - LANE_W / 2;
            case 'east':  return CY - DIVIDER / 2 - LANE_W / 2;
            case 'west':  return CY + DIVIDER / 2 + LANE_W / 2;
            default: return CX;
        }
    }

    getLaneCount(road) {
        return Math.max(this.lanes[road?.toLowerCase()]?.length || 0, 1);
    }

    // ── Signal state management ──────────────────────────────────────────────

    setAllRed() {
        for (const l of this.lights.values()) l.state = 'RED';
    }

    setGreen(id) {
        const l = this.lights.get(id);
        if (l) l.state = 'GREEN';
    }

    setYellow(id) {
        const l = this.lights.get(id);
        if (l) l.state = 'YELLOW';
    }

    /**
     * Store phase definitions received from the server after init.
     * phases: [{id, lanes: [{road, laneIndex}]}]
     */
    setPhases(phases) {
        /** @type {Map<string, Array<{road: string, laneIndex: number}>>} */
        this.phases = new Map(phases.map(p => [p.id, p.lanes]));
        this.activePhaseId    = null;
        this.activePhaseState = null;
    }

    /**
     * Apply server-authoritative phase state.
     * phaseState: 'GREEN' | 'YELLOW' | 'RED'
     */
    applyPhaseState(activePhaseId, phaseState) {
        this.activePhaseId    = activePhaseId;
        this.activePhaseState = phaseState;

        this.setAllRed();
        if (phaseState === 'RED' || !activePhaseId) return;

        const lanes = this.phases?.get(activePhaseId);
        if (!lanes) return;

        for (const { road, laneIndex } of lanes) {
            const laneEntry = this.lanes[road.toLowerCase()]?.[laneIndex ?? 0];
            if (!laneEntry) continue;
            for (const sig of laneEntry.signals) {
                if (phaseState === 'GREEN') this.setGreen(sig.id);
                else if (phaseState === 'YELLOW') this.setYellow(sig.id);
            }
        }
    }

    /**
     * Returns all phases as an ordered array with active-state annotation.
     * [{id, lanes, active, state}]
     */
    get phaseList() {
        return [...this.phases.entries()].map(([id, lanes]) => ({
            id,
            lanes,
            active: id === this.activePhaseId,
            state:  id === this.activePhaseId ? (this.activePhaseState ?? 'RED') : 'RED',
        }));
    }

    /**
     * Given an array of {road, lane} for departed vehicles, mark the
     * corresponding signal groups GREEN and everything else RED.
     * Kept as fallback when no server phase info is available.
     */
    applyDepartures(vehicles) {
        this.setAllRed();
        for (const { road, lane } of vehicles) {
            const laneEntry = this.lanes[road.toLowerCase()]?.[lane ?? 0];
            if (!laneEntry) {
                // Fallback – green all lights on that road
                for (const l of this.lights.values()) {
                    if (l.road === road.toLowerCase()) this.setGreen(l.id);
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

// ── Shared singleton ──────────────────────────────────────────────────────────
/** Shared singleton used by all renderer/logic modules. Mutate via cfg.reset(). */
export const cfg = new IntersectionConfig();
