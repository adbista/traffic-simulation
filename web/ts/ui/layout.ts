/**
 * Dynamic canvas geometry helpers.
 */
import { cfg, CANVAS_SIZE, CX, CY, QUEUE_STEP, ANIM_DURATION_MS, ROAD_COLOR } from '../app/intersection-config.js';
import type { Point } from '../types.js';

export const W = CANVAS_SIZE;
export const H = CANVAS_SIZE;
export { CX, CY, ANIM_DURATION_MS, ROAD_COLOR };

/** Pixel position of the n-th vehicle queued on a road in a given lane. */
export function queuePos(road: string, laneIndex: number, queueIndex: number): Point {
    const lc  = cfg.laneCenter(road, laneIndex ?? 0);
    const gap = (queueIndex + 1) * QUEUE_STEP;
    switch (road) {
        case 'north': return { x: lc,                y: cfg.INT.top    - gap };
        case 'south': return { x: lc,                y: cfg.INT.bottom + gap };
        case 'east':  return { x: cfg.INT.right + gap, y: lc            };
        case 'west':  return { x: cfg.INT.left  - gap, y: lc            };
        default:      return { x: CX, y: CY };
    }
}

/** Off-screen exit position for a vehicle whose destination is `endRoad`. */
export function exitPos(endRoad: string): Point {
    const ob = cfg.outboundCenter(endRoad);
    switch (endRoad) {
        case 'north': return { x: ob,     y: -40   };
        case 'south': return { x: ob,     y: H + 40 };
        case 'east':  return { x: W + 40, y: ob    };
        case 'west':  return { x: -40,    y: ob    };
        default:      return { x: CX,     y: -40   };
    }
}

/** Linear interpolation between two points. */
export function lerp(a: Point, b: Point, t: number): Point {
    return { x: a.x + (b.x - a.x) * t, y: a.y + (b.y - a.y) * t };
}

/** Smooth ease-in-out curve (cubic). */
export function easeInOut(t: number): number {
    return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
}
