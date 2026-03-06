// ── Shared domain types ───────────────────────────────────────────────────────

export type Road = 'north' | 'south' | 'east' | 'west';
export type LightState = 'RED' | 'YELLOW' | 'GREEN';
export type VehicleAnimState = 'waiting' | 'leaving';

export interface SignalEntry {
    id: string;
    movements: string[];
    type: string;
}

export interface LaneEntry {
    laneIndex: number;
    signals: SignalEntry[];
    priority: string | null;
}

export interface LightConfig {
    id: string;
    road: string;
    laneIndex: number;
    movements: string[];
    type: string;
    state: LightState;
}

export interface TimingConfig {
    minGreen: number;
    maxGreen: number;
    yellow: number;
    red: number;
}

export interface MovementDecl {
    movement: string;
    type?: string;
    trafficLightId?: string;
}

export interface LaneDeclaration {
    road: string;
    lane: number;
    movements?: MovementDecl[];
    priority?: string;
}

export interface SimConfig {
    timing?: Partial<TimingConfig>;
    laneDeclarations?: LaneDeclaration[];
}

export interface PhaseInfo {
    id: string;
    lanes: Array<{ road: string; laneIndex: number }>;
}

export interface PhaseEntry extends PhaseInfo {
    active: boolean;
    state: LightState;
}

export interface IntersectionBox {
    left: number;
    right: number;
    top: number;
    bottom: number;
}

export interface Vehicle {
    id: string;
    startRoad: string;
    endRoad: string;
    lane: number;
    state: VehicleAnimState;
    queueIndex: number;
    color: string;
    animStartTime: number | null;
}

export interface StepEntry {
    step: number;
    leftVehicles: string[];
    departures: Array<{ road: string; lane: number }>;
}

export interface Point {
    x: number;
    y: number;
}

export interface AddVehicleParams {
    vehicleId: string;
    startRoad: string;
    endRoad: string;
    lane: string;
}

export interface AddVehicleCommand {
    type: 'addVehicle';
    vehicleId: string;
    startRoad: string;
    endRoad: string;
    lane?: number;
}

export interface WebSocketHandlers {
    onOpen?: () => void;
    onClose?: () => void;
    onMessage?: (data: string) => void;
    onError?: (event: Event) => void;
}
