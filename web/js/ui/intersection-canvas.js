/**
 * IntersectionCanvas — renders the complete static intersection scene using
 * the shared `cfg` singleton from intersection-config.js.
 *
 * Call draw() any time the config or signal states change.
 */
import { cfg, CX, CY, LANE_W, DIVIDER, CANVAS_SIZE } from '../app/intersection-config.js';

const W = CANVAS_SIZE;
const H = CANVAS_SIZE;

const CLR = {
    ground:        '#0f172a',
    road:          '#1e293b',
    intersection:  '#293548',
    divider:       'rgba(255,255,255,0.85)',
    laneDash:      'rgba(255,255,255,0.3)',
    edgeLine:      'rgba(255,255,255,0.18)',
    stopLine:      'rgba(255,255,255,0.95)',
    zebra:         'rgba(255,255,255,0.10)',
    arrowFill:     'rgba(255,255,255,0.22)',
    housing:       '#0c1424',
    housingBorder: 'rgba(255,255,255,0.25)',
    lightRed:      '#ef4444',
    lightRedDim:   '#3f0707',
    lightYellow:   '#eab308',
    lightYellowDim:'#2c1e00',
    lightGreen:    '#22c55e',
    lightGreenDim: '#031a0c',
    protectedHousing: '#1a0c2e',
};

const ROAD_DIR = {
    north: 0,
    south: Math.PI,
    east:  Math.PI / 2,
    west: -Math.PI / 2,
};

export class IntersectionCanvas {
    constructor(canvas) {
        this._ctx    = canvas.getContext('2d');
        this._canvas = canvas;
        canvas.width  = W;
        canvas.height = H;
    }

    draw() {
        const ctx = this._ctx;
        const INT = cfg.INT;
        const nsH = cfg.roadHalfNS;
        const ewH = cfg.roadHalfEW;
        ctx.clearRect(0, 0, W, H);
        this.#ground(ctx);
        this.#roads(ctx, INT, nsH, ewH);
        this.#intersectionBox(ctx, INT);
        this.#centerDividers(ctx, INT);
        this.#laneDividers(ctx, INT, nsH, ewH);
        this.#edgeLines(ctx, INT, nsH, ewH);
        this.#stopLines(ctx, INT);
        this.#zebraCrossings(ctx, INT, nsH, ewH);
        this.#movementArrows(ctx, INT);
        this.#trafficLights(ctx, INT);
        this.#roadLabels(ctx, INT, nsH, ewH);
    }

    #ground(ctx) {
        ctx.fillStyle = CLR.ground;
        ctx.fillRect(0, 0, W, H);
    }

    #roads(ctx, INT, nsH, ewH) {
        ctx.fillStyle = CLR.road;
        ctx.fillRect(CX - nsH, 0,           nsH * 2, INT.top);
        ctx.fillRect(CX - nsH, INT.bottom,   nsH * 2, H - INT.bottom);
        ctx.fillRect(0,        CY - ewH,     INT.left, ewH * 2);
        ctx.fillRect(INT.right, CY - ewH,    W - INT.right, ewH * 2);
    }

    #intersectionBox(ctx, INT) {
        const grd = ctx.createLinearGradient(INT.left, INT.top, INT.right, INT.bottom);
        grd.addColorStop(0, '#293548');
        grd.addColorStop(1, '#243040');
        ctx.fillStyle = grd;
        ctx.fillRect(INT.left, INT.top, INT.right - INT.left, INT.bottom - INT.top);
    }

    #centerDividers(ctx, INT) {
        ctx.save();
        ctx.strokeStyle = CLR.divider;
        ctx.lineWidth = 2;
        ctx.setLineDash([]);
        ctx.beginPath();
        ctx.moveTo(CX, 0);         ctx.lineTo(CX, INT.top);
        ctx.moveTo(CX, INT.bottom); ctx.lineTo(CX, H);
        ctx.moveTo(0, CY);          ctx.lineTo(INT.left, CY);
        ctx.moveTo(INT.right, CY);  ctx.lineTo(W, CY);
        ctx.stroke();
        ctx.restore();
    }

    #laneDividers(ctx, INT, nsH, ewH) {
        ctx.save();
        ctx.strokeStyle = CLR.laneDash;
        ctx.lineWidth = 1.5;
        ctx.setLineDash([13, 9]);
        for (const road of ['north', 'south']) {
            const n = cfg.getLaneCount(road);
            for (let k = 1; k < n; k++) {
                const lc0 = cfg.laneCenter(road, k - 1);
                const lc1 = cfg.laneCenter(road, k);
                const x   = (lc0 + lc1) / 2;
                ctx.beginPath();
                if (road === 'north') { ctx.moveTo(x, 0); ctx.lineTo(x, INT.top); }
                else                  { ctx.moveTo(x, INT.bottom); ctx.lineTo(x, H); }
                ctx.stroke();
            }
        }
        for (const road of ['east', 'west']) {
            const n = cfg.getLaneCount(road);
            for (let k = 1; k < n; k++) {
                const lc0 = cfg.laneCenter(road, k - 1);
                const lc1 = cfg.laneCenter(road, k);
                const y   = (lc0 + lc1) / 2;
                ctx.beginPath();
                if (road === 'east') { ctx.moveTo(INT.right, y); ctx.lineTo(W, y); }
                else                 { ctx.moveTo(0, y); ctx.lineTo(INT.left, y); }
                ctx.stroke();
            }
        }
        ctx.restore();
    }

    #edgeLines(ctx, INT, nsH, ewH) {
        ctx.save();
        ctx.strokeStyle = CLR.edgeLine;
        ctx.lineWidth = 2;
        ctx.setLineDash([]);
        for (const dx of [-nsH, nsH]) {
            ctx.beginPath();
            ctx.moveTo(CX + dx, 0);          ctx.lineTo(CX + dx, INT.top);
            ctx.moveTo(CX + dx, INT.bottom);  ctx.lineTo(CX + dx, H);
            ctx.stroke();
        }
        for (const dy of [-ewH, ewH]) {
            ctx.beginPath();
            ctx.moveTo(0,         CY + dy); ctx.lineTo(INT.left,  CY + dy);
            ctx.moveTo(INT.right, CY + dy); ctx.lineTo(W,         CY + dy);
            ctx.stroke();
        }
        ctx.restore();
    }

    #stopLines(ctx, INT) {
        ctx.save();
        ctx.strokeStyle = CLR.stopLine;
        ctx.lineWidth   = 3;
        ctx.setLineDash([]);
        for (const road of ['north', 'south', 'east', 'west']) {
            const n = cfg.getLaneCount(road);
            for (let k = 0; k < n; k++) {
                const lc   = cfg.laneCenter(road, k);
                const half = LANE_W / 2 - 2;
                ctx.beginPath();
                if      (road === 'north') { ctx.moveTo(lc - half, INT.top);    ctx.lineTo(lc + half, INT.top); }
                else if (road === 'south') { ctx.moveTo(lc - half, INT.bottom); ctx.lineTo(lc + half, INT.bottom); }
                else if (road === 'east')  { ctx.moveTo(INT.right, lc - half);  ctx.lineTo(INT.right, lc + half); }
                else                       { ctx.moveTo(INT.left,  lc - half);  ctx.lineTo(INT.left,  lc + half); }
                ctx.stroke();
            }
        }
        ctx.restore();
    }

    #zebraCrossings(ctx, INT, nsH, ewH) {
        ctx.save();
        ctx.fillStyle = CLR.zebra;
        const sw = 5, sg = 9, cnt = 4;
        for (let i = 0; i < cnt; i++) {
            const off = 6 + i * (sw + sg);
            ctx.fillRect(CX - nsH + 3,  INT.top - off - sw,   nsH * 2 - 6, sw);
            ctx.fillRect(CX - nsH + 3,  INT.bottom + off,     nsH * 2 - 6, sw);
            ctx.fillRect(INT.left - off - sw, CY - ewH + 3,   sw, ewH * 2 - 6);
            ctx.fillRect(INT.right + off,     CY - ewH + 3,   sw, ewH * 2 - 6);
        }
        ctx.restore();
    }

    #movementArrows(ctx, INT) {
        const ARROW_DIST = 70;
        for (const road of ['north', 'south', 'east', 'west']) {
            const n = cfg.getLaneCount(road);
            for (let k = 0; k < n; k++) {
                const laneEntry = cfg.lanes[road]?.[k];
                if (!laneEntry) continue;
                const movements = new Set();
                for (const sig of laneEntry.signals) {
                    for (const m of sig.movements) movements.add(m);
                }
                const lc = cfg.laneCenter(road, k);
                let ax, ay;
                switch (road) {
                    case 'north': ax = lc; ay = INT.top    - ARROW_DIST; break;
                    case 'south': ax = lc; ay = INT.bottom + ARROW_DIST; break;
                    case 'east':  ax = INT.right + ARROW_DIST; ay = lc;  break;
                    case 'west':  ax = INT.left  - ARROW_DIST; ay = lc;  break;
                }
                ctx.save();
                ctx.translate(ax, ay);
                ctx.rotate(ROAD_DIR[road]);
                this.#drawArrowSet(ctx, movements);
                ctx.restore();
            }
        }
    }

    /**
     * Draw text labels (STRAIGHT / LEFT / RIGHT) in local rotated space.
     * The ctx is already translated to the lane centre and rotated by ROAD_DIR.
     * We undo the rotation before drawing so text is always horizontal on screen.
     */
    #drawArrowSet(ctx, movements) {
        // Collect labels in display order
        const labels = [];
        if (movements.has('LEFT'))     labels.push('LEFT');
        if (movements.has('STRAIGHT')) labels.push('STR');
        if (movements.has('RIGHT'))    labels.push('RIGHT');

        // Undo the ROAD_DIR rotation so text reads horizontally
        // (ctx already has translate+rotate applied by the caller)
        const angle = ctx.getTransform
            ? Math.atan2(ctx.getTransform().b, ctx.getTransform().a)
            : 0;
        ctx.rotate(-angle);

        ctx.font         = 'bold 8px monospace';
        ctx.textAlign    = 'center';
        ctx.textBaseline = 'middle';

        const lineH = 10;
        const startY = -((labels.length - 1) * lineH) / 2;

        for (let i = 0; i < labels.length; i++) {
            ctx.fillStyle = CLR.arrowFill;
            ctx.fillText(labels[i], 0, startY + i * lineH);
        }
    }

    #trafficLights(ctx, INT) {
        for (const road of ['north', 'south', 'east', 'west']) {
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
                    let hx, hy;
                    switch (road) {
                        case 'north': hx = lc + offset; hy = INT.top    - 16; break;
                        case 'south': hx = lc + offset; hy = INT.bottom + 16; break;
                        case 'east':  hx = INT.right + 16; hy = lc + offset;  break;
                        case 'west':  hx = INT.left  - 16; hy = lc + offset;  break;
                    }
                    this.#drawSignalHead(ctx, hx, hy, road, light);
                }
            }
        }
    }

    #drawSignalHead(ctx, hx, hy, road, light) {
        const state = light.state;  // 'RED' | 'YELLOW' | 'GREEN'

        const SZ = 10;   // square side length (px)
        const color = state === 'GREEN'  ? '#22c55e'
                    : state === 'YELLOW' ? '#eab308'
                    :                      '#ef4444';

        ctx.save();
        if (state === 'GREEN')  { ctx.shadowColor = '#22c55e'; ctx.shadowBlur = 10; }
        if (state === 'RED')    { ctx.shadowColor = '#ef4444'; ctx.shadowBlur = 6; }
        ctx.fillStyle = color;
        ctx.fillRect(hx - SZ / 2, hy - SZ / 2, SZ, SZ);
        ctx.shadowBlur = 0;

        // Draw ID label offset away from the intersection edge
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

    #roadLabels(ctx, INT, nsH, ewH) {
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
