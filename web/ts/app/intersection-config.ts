/**
 * Intersection configuration singleton.
 * Parses WsInitRequest.config (SimConfig), computes dynamic road geometry,
 * and tracks real-time traffic-light states.
 *
 * All geometry is expressed in the 720×720 canvas coordinate system.
 */
import type {
    Road, LightState, LightConfig, LaneEntry, SimConfig, LaneDeclaration,
    IntersectionBox, PhaseEntry,
} from '../types.js';

export const CANVAS_SIZE = 720;
export const CX = 360;
export const CY = 360;
export const LANE_W           = 28;   // usable width of one lane (px)
export const DIVIDER          = 4;    // width of center divider between inbound/outbound
export const QUEUE_STEP       = 38;   // px between adjacent queued vehicles
export const ANIM_DURATION_MS = 1600; // vehicle leave-animation duration (ms)

export const ROAD_COLOR: Record<Road, string> = {
    north: '#f87171',
    south: '#60a5fa',
    east:  '#fbbf24',
    west:  '#4ade80',
};

class IntersectionConfig {
    lanes!: Record<Road, LaneEntry[]>;
    lights!: Map<string, LightConfig>;
    timing!: { minGreen: number; maxGreen: number; yellow: number; red: number };
    phases!: Map<string, Array<{ road: string; laneIndex: number }>>;
    activePhaseId!:    string | null;
    activePhaseState!: LightState | null;
    roadHalfNS!: number;
    roadHalfEW!: number;
    INT!: IntersectionBox;

    constructor() {
        this.#init(null);
    }

    reset(simConfig: SimConfig | null): void {
        this.#init(simConfig);
    }

    #init(simConfig: SimConfig | null): void {
        this.lanes  = { north: [], south: [], east: [], west: [] };
        this.lights = new Map();
        this.timing = { minGreen: 1, maxGreen: 5, yellow: 0, red: 0 };
        this.phases = new Map();
        this.activePhaseId    = null;
        this.activePhaseState = null;

        if (simConfig?.timing) {
            Object.assign(this.timing, simConfig.timing);
        }

        if (simConfig?.laneDeclarations?.length) {
            this.#parseDeclarations(simConfig.laneDeclarations);
        } else {
            this.#defaultConfig();
        }

        this.#computeGeometry();
    }

    #parseDeclarations(decls: LaneDeclaration[]): void {
        for (const d of decls) {
            const road = d.road?.toLowerCase() as Road;
            if (!this.lanes[road]) continue;

            while (this.lanes[road].length <= d.lane) {
                this.lanes[road].push({
                    laneIndex: this.lanes[road].length,
                    signals: [],
                    priority: null,
                });
            }
            const laneEntry = this.lanes[road][d.lane];
            laneEntry.priority = d.priority ?? null;

            const byId = new Map<string, { id: string; movements: string[]; type: string; road: string; laneIndex: number }>();
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
                byId.get(tid)!.movements.push(m.movement.toUpperCase());
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
    }

    #defaultConfig(): void {
        for (const road of ['north', 'south', 'east', 'west'] as Road[]) {
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

    #computeGeometry(): void {
        const nNS = Math.max(this.lanes.north.length, this.lanes.south.length, 1);
        const nEW = Math.max(this.lanes.east.length,  this.lanes.west.length,  1);

        this.roadHalfNS = (nNS + 1) * LANE_W + DIVIDER;
        this.roadHalfEW = (nEW + 1) * LANE_W + DIVIDER;

        this.INT = {
            left:   CX - this.roadHalfNS,
            right:  CX + this.roadHalfNS,
            top:    CY - this.roadHalfEW,
            bottom: CY + this.roadHalfEW,
        };
    }

    laneCenter(road: string, laneIndex = 0): number {
        const k = laneIndex ?? 0;
        switch (road.toLowerCase()) {
            case 'north': return CX - DIVIDER / 2 - (k + 0.5) * LANE_W;
            case 'south': return CX + DIVIDER / 2 + (k + 0.5) * LANE_W;
            case 'east':  return CY + DIVIDER / 2 + (k + 0.5) * LANE_W;
            case 'west':  return CY - DIVIDER / 2 - (k + 0.5) * LANE_W;
            default:      return CX;
        }
    }

    outboundCenter(road: string): number {
        switch (road.toLowerCase()) {
            case 'north': return CX + DIVIDER / 2 + LANE_W / 2;
            case 'south': return CX - DIVIDER / 2 - LANE_W / 2;
            case 'east':  return CY - DIVIDER / 2 - LANE_W / 2;
            case 'west':  return CY + DIVIDER / 2 + LANE_W / 2;
            default:      return CX;
        }
    }

    getLaneCount(road: string): number {
        return Math.max(this.lanes[road?.toLowerCase() as Road]?.length ?? 0, 1);
    }

    setAllRed(): void {
        for (const l of this.lights.values()) l.state = 'RED';
    }

    setGreen(id: string): void {
        const l = this.lights.get(id);
        if (l) l.state = 'GREEN';
    }

    setYellow(id: string): void {
        const l = this.lights.get(id);
        if (l) l.state = 'YELLOW';
    }

    setPhases(phases: Array<{ id: string; lanes: Array<{ road: string; laneIndex: number }> }>): void {
        this.phases = new Map(phases.map(p => [p.id, p.lanes]));
        this.activePhaseId    = null;
        this.activePhaseState = null;
    }

    applyPhaseState(activePhaseId: string, phaseState: LightState): void {
        this.activePhaseId    = activePhaseId;
        this.activePhaseState = phaseState;

        this.setAllRed();
        if (phaseState === 'RED' || !activePhaseId) return;

        const lanes = this.phases.get(activePhaseId);
        if (!lanes) return;

        for (const { road, laneIndex } of lanes) {
            const laneEntry = this.lanes[road.toLowerCase() as Road]?.[laneIndex ?? 0];
            if (!laneEntry) continue;
            for (const sig of laneEntry.signals) {
                if (phaseState === 'GREEN')  this.setGreen(sig.id);
                else if (phaseState === 'YELLOW') this.setYellow(sig.id);
            }
        }
    }

    get phaseList(): PhaseEntry[] {
        return [...this.phases.entries()].map(([id, lanes]) => ({
            id,
            lanes,
            active: id === this.activePhaseId,
            state:  (id === this.activePhaseId ? (this.activePhaseState ?? 'RED') : 'RED') as LightState,
        }));
    }

    applyDepartures(vehicles: Array<{ road: string; lane: number }>): void {
        this.setAllRed();
        for (const { road, lane } of vehicles) {
            const laneEntry = this.lanes[road.toLowerCase() as Road]?.[lane ?? 0];
            if (!laneEntry) {
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

    get lightList(): LightConfig[] {
        return [...this.lights.values()];
    }
}

/** Shared singleton used by all renderer/logic modules. Mutate via cfg.reset(). */
export const cfg = new IntersectionConfig();
