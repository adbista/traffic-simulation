import {
    W, H, CX, CY,
    INT, INT_HALF, ROAD_HALF, LANE_OFFSET,
    INBOUND, OUTBOUND,
} from './layout.js';

const COLOR = {
    ground:        '#1e293b',
    road:          '#374151',
    intersection:  '#4b5563',
    markingDash:   'rgba(255,255,255,0.55)',
    markingEdge:   'rgba(255,255,255,0.25)',
    stopLine:      'rgba(255,255,255,0.9)',
    arrowFill:     'rgba(255,255,255,0.22)',
    lightHousing:  '#111827',
    lightRed:      '#ef4444',
    lightRedDim:   '#3f0f0f',
    lightGreen:    '#22c55e',
    lightGreenDim: '#052e16',
};

const LIGHT_R = 6; // signal-circle radius

/**
 * Single Responsibility: renders the static intersection geometry.
 * Call draw(phase) whenever the traffic-light phase changes.
 * phase: 'NS' | 'EW' | null
 */
export class IntersectionCanvas {
    /** @param {HTMLCanvasElement} canvas */
    constructor(canvas) {
        this._ctx = canvas.getContext('2d');
    }

    draw(phase) {
        const ctx = this._ctx;
        ctx.clearRect(0, 0, W, H);
        this.#drawGround(ctx);
        this.#drawRoads(ctx);
        this.#drawIntersectionBox(ctx);
        this.#drawCenterLines(ctx);
        this.#drawEdgeLines(ctx);
        this.#drawStopLines(ctx);
        this.#drawRoadArrows(ctx);
        this.#drawZebraCrossings(ctx);
        this.#drawTrafficLights(ctx, phase);
    }

    // ── ground ───────────────────────────────────────────────────────────────
    #drawGround(ctx) {
        ctx.fillStyle = COLOR.ground;
        ctx.fillRect(0, 0, W, H);
    }

    // ── road surfaces ────────────────────────────────────────────────────────
    #drawRoads(ctx) {
        ctx.fillStyle = COLOR.road;
        // north road
        ctx.fillRect(CX - ROAD_HALF, 0, ROAD_HALF * 2, INT.top);
        // south road
        ctx.fillRect(CX - ROAD_HALF, INT.bottom, ROAD_HALF * 2, H - INT.bottom);
        // west road
        ctx.fillRect(0, CY - ROAD_HALF, INT.left, ROAD_HALF * 2);
        // east road
        ctx.fillRect(INT.right, CY - ROAD_HALF, W - INT.right, ROAD_HALF * 2);
    }

    #drawIntersectionBox(ctx) {
        ctx.fillStyle = COLOR.intersection;
        ctx.fillRect(INT.left, INT.top, INT_HALF * 2, INT_HALF * 2);
    }

    // ── centre dashed dividing lines ─────────────────────────────────────────
    #drawCenterLines(ctx) {
        ctx.save();
        ctx.strokeStyle = COLOR.markingDash;
        ctx.lineWidth = 2;
        ctx.setLineDash([14, 10]);

        // north–south centre
        ctx.beginPath();
        ctx.moveTo(CX, 0);
        ctx.lineTo(CX, INT.top);
        ctx.moveTo(CX, INT.bottom);
        ctx.lineTo(CX, H);
        ctx.stroke();

        // east–west centre
        ctx.beginPath();
        ctx.moveTo(0, CY);
        ctx.lineTo(INT.left, CY);
        ctx.moveTo(INT.right, CY);
        ctx.lineTo(W, CY);
        ctx.stroke();

        ctx.restore();
    }

    // ── road edge lines ───────────────────────────────────────────────────────
    #drawEdgeLines(ctx) {
        ctx.save();
        ctx.strokeStyle = COLOR.markingEdge;
        ctx.lineWidth = 2;
        ctx.setLineDash([]);

        const offsets = [-ROAD_HALF, ROAD_HALF];
        for (const o of offsets) {
            // north & south edges (vertical)
            ctx.beginPath();
            ctx.moveTo(CX + o, 0);       ctx.lineTo(CX + o, INT.top);
            ctx.moveTo(CX + o, INT.bottom); ctx.lineTo(CX + o, H);
            ctx.stroke();

            // west & east edges (horizontal)
            ctx.beginPath();
            ctx.moveTo(0, CY + o);       ctx.lineTo(INT.left, CY + o);
            ctx.moveTo(INT.right, CY + o); ctx.lineTo(W, CY + o);
            ctx.stroke();
        }
        ctx.restore();
    }

    // ── stop lines ────────────────────────────────────────────────────────────
    #drawStopLines(ctx) {
        ctx.save();
        ctx.strokeStyle = COLOR.stopLine;
        ctx.lineWidth = 3;
        ctx.setLineDash([]);

        // top of intersection (south end of north road)
        ctx.beginPath();
        ctx.moveTo(CX - ROAD_HALF + 4, INT.top);
        ctx.lineTo(CX, INT.top); // only inbound half
        ctx.stroke();

        // bottom of intersection (north end of south road)
        ctx.beginPath();
        ctx.moveTo(CX, INT.bottom);
        ctx.lineTo(CX + ROAD_HALF - 4, INT.bottom);
        ctx.stroke();

        // left of intersection (east end of west road)
        ctx.beginPath();
        ctx.moveTo(INT.left, CY);
        ctx.lineTo(INT.left, CY + ROAD_HALF - 4);
        ctx.stroke();

        // right of intersection (west end of east road)
        ctx.beginPath();
        ctx.moveTo(INT.right, CY - ROAD_HALF + 4);
        ctx.lineTo(INT.right, CY);
        ctx.stroke();

        ctx.restore();
    }

    // ── road direction arrows ─────────────────────────────────────────────────
    #drawRoadArrows(ctx) {
        // north road – inbound arrow pointing south, mid-road
        this.#arrowStraight(ctx, INBOUND.north, INT.top - 80, 'south');
        // south road – inbound arrow pointing north, mid-road
        this.#arrowStraight(ctx, INBOUND.south, INT.bottom + 80, 'north');
        // east road – inbound arrow pointing west, mid-road
        this.#arrowStraight(ctx, INT.right + 80, INBOUND.east, 'west');
        // west road – inbound arrow pointing east, mid-road
        this.#arrowStraight(ctx, INT.left - 80, INBOUND.west, 'east');
    }

    /** Draw a car-lane directional arrow. */
    #arrowStraight(ctx, x, y, direction) {
        const len = 22;
        const headW = 9;
        ctx.save();
        ctx.translate(x, y);
        switch (direction) {
            case 'south': ctx.rotate(0); break;
            case 'north': ctx.rotate(Math.PI); break;
            case 'east':  ctx.rotate(Math.PI / 2); break;
            case 'west':  ctx.rotate(-Math.PI / 2); break;
        }
        ctx.fillStyle = COLOR.arrowFill;
        ctx.beginPath();
        // shaft
        ctx.rect(-2, -len / 2, 4, len - 6);
        ctx.fill();
        // arrowhead (down = south in default rotation)
        ctx.beginPath();
        ctx.moveTo(0, len / 2);
        ctx.lineTo(-headW / 2, len / 2 - 9);
        ctx.lineTo(headW / 2, len / 2 - 9);
        ctx.closePath();
        ctx.fill();
        ctx.restore();
    }

    // ── zebra crossings ────────────────────────────────────────────────────────
    #drawZebraCrossings(ctx) {
        ctx.save();
        ctx.fillStyle = 'rgba(255,255,255,0.12)';
        const strW = 5, gap = 9, count = 5;

        // north crossing (horizontal stripes across roads at INT.top)
        for (let i = 0; i < count; i++) {
            const yOffset = 4 + i * (strW + gap);
            ctx.fillRect(CX - ROAD_HALF + 4, INT.top - yOffset - strW, ROAD_HALF * 2 - 8, strW);
        }
        // south crossing
        for (let i = 0; i < count; i++) {
            const yOffset = 4 + i * (strW + gap);
            ctx.fillRect(CX - ROAD_HALF + 4, INT.bottom + yOffset, ROAD_HALF * 2 - 8, strW);
        }
        // west crossing (vertical stripes)
        for (let i = 0; i < count; i++) {
            const xOffset = 4 + i * (strW + gap);
            ctx.fillRect(INT.left - xOffset - strW, CY - ROAD_HALF + 4, strW, ROAD_HALF * 2 - 8);
        }
        // east crossing
        for (let i = 0; i < count; i++) {
            const xOffset = 4 + i * (strW + gap);
            ctx.fillRect(INT.right + xOffset, CY - ROAD_HALF + 4, strW, ROAD_HALF * 2 - 8);
        }
        ctx.restore();
    }

    // ── traffic lights ────────────────────────────────────────────────────────
    #drawTrafficLights(ctx, phase) {
        const NS_green = phase === 'NS';
        const EW_green = phase === 'EW';

        // Positions: right side of inbound lane at intersection entry
        const lights = [
            // road,    x,                       y,                      dir
            ['north', INT.left  + 14,      INT.top   - 16,      NS_green ],
            ['south', INT.right - 14,      INT.bottom + 16,     NS_green ],
            ['west',  INT.left  - 16,      INT.top   + 14,      EW_green ],
            ['east',  INT.right + 16,      INT.bottom - 14,     EW_green ],
        ];

        for (const [, x, y, green] of lights) {
            this.#drawSignalHead(ctx, x, y, green);
        }
    }

    /** Draw a two-lamp vertical signal head. */
    #drawSignalHead(ctx, x, y, green) {
        const hw = LIGHT_R * 1.8;
        const hh = LIGHT_R * 4.8;

        // housing
        ctx.save();
        ctx.fillStyle = COLOR.lightHousing;
        ctx.strokeStyle = 'rgba(255,255,255,0.15)';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.roundRect(x - hw / 2, y - hh / 2, hw, hh, 3);
        ctx.fill();
        ctx.stroke();

        // red lamp (top)
        const redLit  = !green;
        ctx.fillStyle = redLit ? COLOR.lightRed : COLOR.lightRedDim;
        if (redLit) { ctx.shadowColor = COLOR.lightRed; ctx.shadowBlur = 8; }
        ctx.beginPath();
        ctx.arc(x, y - LIGHT_R * 1.4, LIGHT_R, 0, Math.PI * 2);
        ctx.fill();
        ctx.shadowBlur = 0;

        // green lamp (bottom)
        const greenLit = green;
        ctx.fillStyle = greenLit ? COLOR.lightGreen : COLOR.lightGreenDim;
        if (greenLit) { ctx.shadowColor = COLOR.lightGreen; ctx.shadowBlur = 8; }
        ctx.beginPath();
        ctx.arc(x, y + LIGHT_R * 1.4, LIGHT_R, 0, Math.PI * 2);
        ctx.fill();
        ctx.shadowBlur = 0;

        ctx.restore();
    }
}
