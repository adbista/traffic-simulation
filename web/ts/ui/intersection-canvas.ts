/**
 * IntersectionCanvas — renders the complete static intersection scene using
 * the shared `cfg` singleton from intersection-config.ts.
 */
import { cfg, CX, CY, LANE_W, DIVIDER, CANVAS_SIZE } from '../app/intersection-config.js';
import type { LightConfig } from '../types.js';

const W = CANVAS_SIZE;
const H = CANVAS_SIZE;

const CLR = {
    ground:           '#0f172a',
    road:             '#1e293b',
    intersection:     '#293548',
    divider:          'rgba(255,255,255,0.85)',
    laneDash:         'rgba(255,255,255,0.3)',
    edgeLine:         'rgba(255,255,255,0.18)',
    stopLine:         'rgba(255,255,255,0.95)',
    zebra:            'rgba(255,255,255,0.10)',
    arrowFill:        'rgba(255,255,255,0.22)',
    housing:          '#0c1424',
    housingBorder:    'rgba(255,255,255,0.25)',
    lightRed:         '#ef4444',
    lightRedDim:      '#3f0707',
    lightYellow:      '#eab308',
    lightYellowDim:   '#2c1e00',
    lightGreen:       '#22c55e',
    lightGreenDim:    '#031a0c',
    protectedHousing: '#1a0c2e',
} as const;

const ROAD_DIR: Record<string, number> = {
    north: 0,
    south: Math.PI,
    east:  Math.PI / 2,
    west: -Math.PI / 2,
};

export class IntersectionCanvas {
    #ctx:    CanvasRenderingContext2D;
    #canvas: HTMLCanvasElement;

    constructor(canvas: HTMLCanvasElement) {
        this.#ctx    = canvas.getContext('2d')!;
        this.#canvas = canvas;
        canvas.width  = W;
        canvas.height = H;
    }

    draw(): void {
        const ctx = this.#ctx;
        const INT = cfg.INT;
        const nsH = cfg.roadHalfNS;
        const ewH = cfg.roadHalfEW;
        ctx.clearRect(0, 0, W, H);
        this.#ground(ctx);
        this.#roads(ctx, INT.left, INT.right, INT.top, INT.bottom, nsH, ewH);
        this.#intersectionBox(ctx, INT.left, INT.right, INT.top, INT.bottom);
        this.#centerDividers(ctx, INT.left, INT.right, INT.top, INT.bottom);
        this.#laneDividers(ctx, INT.left, INT.right, INT.top, INT.bottom);
        this.#edgeLines(ctx, INT.left, INT.right, INT.top, INT.bottom, nsH, ewH);
        this.#stopLines(ctx, INT.left, INT.right, INT.top, INT.bottom);
        this.#zebraCrossings(ctx, INT.left, INT.right, INT.top, INT.bottom, nsH, ewH);
        this.#movementArrows(ctx, INT.left, INT.right, INT.top, INT.bottom);
        this.#trafficLights(ctx, INT.left, INT.right, INT.top, INT.bottom);
        this.#roadLabels(ctx);
    }

    #ground(ctx: CanvasRenderingContext2D): void {
        ctx.fillStyle = CLR.ground;
        ctx.fillRect(0, 0, W, H);
    }

    #roads(ctx: CanvasRenderingContext2D, il: number, ir: number, it: number, ib: number, nsH: number, ewH: number): void {
        ctx.fillStyle = CLR.road;
        ctx.fillRect(CX - nsH, 0,        nsH * 2, it);
        ctx.fillRect(CX - nsH, ib,        nsH * 2, H - ib);
        ctx.fillRect(0,        CY - ewH,  il,      ewH * 2);
        ctx.fillRect(ir,       CY - ewH,  W - ir,  ewH * 2);
    }

    #intersectionBox(ctx: CanvasRenderingContext2D, il: number, ir: number, it: number, ib: number): void {
        const grd = ctx.createLinearGradient(il, it, ir, ib);
        grd.addColorStop(0, '#293548');
        grd.addColorStop(1, '#243040');
        ctx.fillStyle = grd;
        ctx.fillRect(il, it, ir - il, ib - it);
    }

    #centerDividers(ctx: CanvasRenderingContext2D, il: number, ir: number, it: number, ib: number): void {
        ctx.save();
        ctx.strokeStyle = CLR.divider;
        ctx.lineWidth = 2;
        ctx.setLineDash([]);
        ctx.beginPath();
        ctx.moveTo(CX, 0);  ctx.lineTo(CX, it);
        ctx.moveTo(CX, ib); ctx.lineTo(CX, H);
        ctx.moveTo(0, CY);  ctx.lineTo(il, CY);
        ctx.moveTo(ir, CY); ctx.lineTo(W, CY);
        ctx.stroke();
        ctx.restore();
    }

    #laneDividers(ctx: CanvasRenderingContext2D, il: number, ir: number, it: number, ib: number): void {
        ctx.save();
        ctx.strokeStyle = CLR.laneDash;
        ctx.lineWidth = 1.5;
        ctx.setLineDash([13, 9]);
        for (const road of ['north', 'south'] as const) {
            const n = cfg.getLaneCount(road);
            for (let k = 1; k < n; k++) {
                const lc0 = cfg.laneCenter(road, k - 1);
                const lc1 = cfg.laneCenter(road, k);
                const x   = (lc0 + lc1) / 2;
                ctx.beginPath();
                if (road === 'north') { ctx.moveTo(x, 0); ctx.lineTo(x, it); }
                else                  { ctx.moveTo(x, ib); ctx.lineTo(x, H); }
                ctx.stroke();
            }
        }
        for (const road of ['east', 'west'] as const) {
            const n = cfg.getLaneCount(road);
            for (let k = 1; k < n; k++) {
                const lc0 = cfg.laneCenter(road, k - 1);
                const lc1 = cfg.laneCenter(road, k);
                const y   = (lc0 + lc1) / 2;
                ctx.beginPath();
                if (road === 'east') { ctx.moveTo(ir, y); ctx.lineTo(W, y); }
                else                 { ctx.moveTo(0, y);  ctx.lineTo(il, y); }
                ctx.stroke();
            }
        }
        ctx.restore();
    }

    #edgeLines(ctx: CanvasRenderingContext2D, il: number, ir: number, it: number, ib: number, nsH: number, ewH: number): void {
        ctx.save();
        ctx.strokeStyle = CLR.edgeLine;
        ctx.lineWidth = 2;
        ctx.setLineDash([]);
        for (const dx of [-nsH, nsH]) {
            ctx.beginPath();
            ctx.moveTo(CX + dx, 0);  ctx.lineTo(CX + dx, it);
            ctx.moveTo(CX + dx, ib); ctx.lineTo(CX + dx, H);
            ctx.stroke();
        }
        for (const dy of [-ewH, ewH]) {
            ctx.beginPath();
            ctx.moveTo(0,  CY + dy); ctx.lineTo(il, CY + dy);
            ctx.moveTo(ir, CY + dy); ctx.lineTo(W,  CY + dy);
            ctx.stroke();
        }
        ctx.restore();
    }

    #stopLines(ctx: CanvasRenderingContext2D, il: number, ir: number, it: number, ib: number): void {
        ctx.save();
        ctx.strokeStyle = CLR.stopLine;
        ctx.lineWidth   = 3;
        ctx.setLineDash([]);
        for (const road of ['north', 'south', 'east', 'west'] as const) {
            const n = cfg.getLaneCount(road);
            for (let k = 0; k < n; k++) {
                const lc   = cfg.laneCenter(road, k);
                const half = LANE_W / 2 - 2;
                ctx.beginPath();
                if      (road === 'north') { ctx.moveTo(lc - half, it); ctx.lineTo(lc + half, it); }
                else if (road === 'south') { ctx.moveTo(lc - half, ib); ctx.lineTo(lc + half, ib); }
                else if (road === 'east')  { ctx.moveTo(ir, lc - half); ctx.lineTo(ir, lc + half); }
                else                       { ctx.moveTo(il, lc - half); ctx.lineTo(il, lc + half); }
                ctx.stroke();
            }
        }
        ctx.restore();
    }

    #zebraCrossings(ctx: CanvasRenderingContext2D, il: number, ir: number, it: number, ib: number, nsH: number, ewH: number): void {
        ctx.save();
        ctx.fillStyle = CLR.zebra;
        const sw = 5, sg = 9, cnt = 4;
        for (let i = 0; i < cnt; i++) {
            const off = 6 + i * (sw + sg);
            ctx.fillRect(CX - nsH + 3,  it - off - sw, nsH * 2 - 6, sw);
            ctx.fillRect(CX - nsH + 3,  ib + off,      nsH * 2 - 6, sw);
            ctx.fillRect(il - off - sw, CY - ewH + 3,  sw, ewH * 2 - 6);
            ctx.fillRect(ir + off,      CY - ewH + 3,  sw, ewH * 2 - 6);
        }
        ctx.restore();
    }

    #movementArrows(ctx: CanvasRenderingContext2D, il: number, ir: number, it: number, ib: number): void {
        const ARROW_DIST = 70;
        for (const road of ['north', 'south', 'east', 'west'] as const) {
            const n = cfg.getLaneCount(road);
            for (let k = 0; k < n; k++) {
                const laneEntry = cfg.lanes[road]?.[k];
                if (!laneEntry) continue;
                const movements = new Set<string>();
                for (const sig of laneEntry.signals) {
                    for (const m of sig.movements) movements.add(m);
                }
                const lc = cfg.laneCenter(road, k);
                let ax = 0, ay = 0;
                switch (road) {
                    case 'north': ax = lc; ay = it - ARROW_DIST; break;
                    case 'south': ax = lc; ay = ib + ARROW_DIST; break;
                    case 'east':  ax = ir + ARROW_DIST; ay = lc; break;
                    case 'west':  ax = il - ARROW_DIST; ay = lc; break;
                }
                ctx.save();
                ctx.translate(ax, ay);
                ctx.rotate(ROAD_DIR[road]);
                this.#drawArrowSet(ctx, movements);
                ctx.restore();
            }
        }
    }

    #drawArrowSet(ctx: CanvasRenderingContext2D, movements: Set<string>): void {
        const labels: string[] = [];
        if (movements.has('LEFT'))     labels.push('LEFT');
        if (movements.has('STRAIGHT')) labels.push('STR');
        if (movements.has('RIGHT'))    labels.push('RIGHT');

        const angle = ctx.getTransform
            ? Math.atan2(ctx.getTransform().b, ctx.getTransform().a)
            : 0;
        ctx.rotate(-angle);

        ctx.font         = 'bold 8px monospace';
        ctx.textAlign    = 'center';
        ctx.textBaseline = 'middle';

        const lineH  = 10;
        const startY = -((labels.length - 1) * lineH) / 2;

        for (let i = 0; i < labels.length; i++) {
            ctx.fillStyle = CLR.arrowFill;
            ctx.fillText(labels[i], 0, startY + i * lineH);
        }
    }

    #trafficLights(ctx: CanvasRenderingContext2D, il: number, ir: number, it: number, ib: number): void {
        for (const road of ['north', 'south', 'east', 'west'] as const) {
            const n = cfg.getLaneCount(road);
            for (let k = 0; k < n; k++) {
                const laneEntry = cfg.lanes[road]?.[k];
                if (!laneEntry) continue;
                const sigs  = laneEntry.signals;
                const total = sigs.length;
                const lc    = cfg.laneCenter(road, k);
                for (let s = 0; s < total; s++) {
                    const sig   = sigs[s];
                    const light = cfg.lights.get(sig.id);
                    if (!light) continue;
                    const offset = total === 1 ? 0 : (s - (total - 1) / 2) * 9;
                    let hx = 0, hy = 0;
                    switch (road) {
                        case 'north': hx = lc + offset;    hy = it - 16;       break;
                        case 'south': hx = lc + offset;    hy = ib + 16;       break;
                        case 'east':  hx = ir + 16;        hy = lc + offset;   break;
                        case 'west':  hx = il - 16;        hy = lc + offset;   break;
                    }
                    this.#drawSignalHead(ctx, hx, hy, road, light);
                }
            }
        }
    }

    #drawSignalHead(ctx: CanvasRenderingContext2D, hx: number, hy: number, road: string, light: LightConfig): void {
        const state = light.state;
        const SZ = 10;
        const color = state === 'GREEN'  ? '#22c55e'
                    : state === 'YELLOW' ? '#eab308'
                    :                      '#ef4444';

        ctx.save();
        if (state === 'GREEN') { ctx.shadowColor = '#22c55e'; ctx.shadowBlur = 10; }
        if (state === 'RED')   { ctx.shadowColor = '#ef4444'; ctx.shadowBlur = 6;  }
        ctx.fillStyle = color;
        ctx.fillRect(hx - SZ / 2, hy - SZ / 2, SZ, SZ);
        ctx.shadowBlur = 0;

        ctx.font         = '7px monospace';
        ctx.fillStyle    = 'rgba(255,255,255,0.75)';
        ctx.textBaseline = 'middle';
        switch (road) {
            case 'north': ctx.textAlign = 'center'; ctx.fillText(light.id, hx, hy - SZ / 2 - 5); break;
            case 'south': ctx.textAlign = 'center'; ctx.fillText(light.id, hx, hy + SZ / 2 + 5); break;
            case 'east':  ctx.textAlign = 'left';   ctx.fillText(light.id, hx + SZ / 2 + 3, hy); break;
            case 'west':  ctx.textAlign = 'right';  ctx.fillText(light.id, hx - SZ / 2 - 3, hy); break;
        }
        ctx.restore();
    }

    #roadLabels(ctx: CanvasRenderingContext2D): void {
        ctx.save();
        ctx.font         = 'bold 11px monospace';
        ctx.fillStyle    = 'rgba(255,255,255,0.35)';
        ctx.textAlign    = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('N', CX, 14);
        ctx.fillText('S', CX, H - 14);
        ctx.fillText('W', 14, CY);
        ctx.fillText('E', W - 14, CY);
        ctx.restore();
    }
}
