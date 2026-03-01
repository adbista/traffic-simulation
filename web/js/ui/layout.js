/**
 * Shared canvas geometry constants and helpers.
 * Single source of truth for layout — all renderers import from here.
 *
 * Coordinate convention (looking from above):
 *   North → traffic approaches driving south  (inbound: x < CX)
 *   South → traffic approaches driving north  (inbound: x > CX)
 *   East  → traffic approaches driving west   (inbound: y > CY)
 *   West  → traffic approaches driving east   (inbound: y < CY)
 */

export const W = 600;
export const H = 600;
export const CX = W / 2;              // 300
export const CY = H / 2;              // 300
export const INT_HALF = 75;           // intersection box half-size
export const ROAD_HALF = 44;          // road half-width → full width 88px
export const LANE_OFFSET = 22;        // offset of lane centre from road centre
export const QUEUE_STEP = 34;         // pixels between queued vehicles
export const ANIM_DURATION_MS = 1500; // vehicle leave animation duration

export const INT = {
    left:   CX - INT_HALF, // 225
    right:  CX + INT_HALF, // 375
    top:    CY - INT_HALF, // 225
    bottom: CY + INT_HALF, // 375
};

/** Inbound lane centre coordinate (the axis on which vehicles travel toward intersection). */
export const INBOUND = {
    north: CX - LANE_OFFSET, // x = 278  (driving south, on right side of north road)
    south: CX + LANE_OFFSET, // x = 322  (driving north, on right side of south road)
    east:  CY + LANE_OFFSET, // y = 322  (driving west,  on right side of east road)
    west:  CY - LANE_OFFSET, // y = 278  (driving east,  on right side of west road)
};

/** Outbound lane centre coordinate (vehicles exit the intersection on this axis). */
export const OUTBOUND = {
    north: CX + LANE_OFFSET, // x = 322
    south: CX - LANE_OFFSET, // x = 278
    east:  CY - LANE_OFFSET, // y = 278
    west:  CY + LANE_OFFSET, // y = 322
};

/** Pixel position of the n-th vehicle queued on a given approach road.
 *  Index 0 = closest to intersection. */
export function queuePos(road, index) {
    const gap = (index + 1) * QUEUE_STEP;
    switch (road) {
        case 'north': return { x: INBOUND.north, y: INT.top    - gap };
        case 'south': return { x: INBOUND.south, y: INT.bottom + gap };
        case 'east':  return { x: INT.right + gap, y: INBOUND.east };
        case 'west':  return { x: INT.left  - gap, y: INBOUND.west };
        default:      return { x: CX, y: CY };
    }
}

/** Off-screen exit position for a vehicle whose destination is `endRoad`. */
export function exitPos(endRoad) {
    switch (endRoad) {
        case 'north': return { x: OUTBOUND.north, y: -30 };
        case 'south': return { x: OUTBOUND.south, y: H  + 30 };
        case 'east':  return { x: W + 30,          y: OUTBOUND.east };
        case 'west':  return { x: -30,              y: OUTBOUND.west };
        default:      return { x: CX,               y: -30 };
    }
}

/** Linear interpolation between two {x, y} points. */
export function lerp(a, b, t) {
    return { x: a.x + (b.x - a.x) * t, y: a.y + (b.y - a.y) * t };
}

/** Smooth ease-in-out curve (cubic). */
export function easeInOut(t) {
    return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
}

/** Vehicle colour per approach road. */
export const ROAD_COLOR = {
    north: '#f87171', // red-400
    south: '#60a5fa', // blue-400
    east:  '#fbbf24', // amber-400
    west:  '#4ade80', // green-400
};
