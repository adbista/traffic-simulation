import {
    W, H, CX, CY,
    queuePos, exitPos, lerp, easeInOut,
    ANIM_DURATION_MS, ROAD_COLOR,
} from './layout.js';

const VEHICLE_RADIUS = 9;
const LABEL_FONT     = 'bold 8px monospace';

/**
 * Animates and renders vehicle dots on the vehicle canvas layer.
 * Vehicles are placed in their assigned lane using lane-aware queuePos/exitPos.
 */
export class VehicleLayer {
    constructor(canvas, { onVehicleGone } = {}) {
        this._ctx = canvas.getContext('2d');
        this._onVehicleGone = onVehicleGone ?? (() => {});
        canvas.width  = W;
        canvas.height = H;
    }

    draw(vehicles, timestamp) {
        const ctx = this._ctx;
        ctx.clearRect(0, 0, W, H);
        for (const v of vehicles) {
            if      (v.state === 'waiting') this.#drawWaiting(ctx, v);
            else if (v.state === 'leaving') this.#animateLeaving(ctx, v, timestamp);
        }
    }

    #drawWaiting(ctx, v) {
        const pos = queuePos(v.startRoad, v.lane ?? 0, v.queueIndex);
        this.#drawVehicle(ctx, pos.x, pos.y, v, 1);
    }

    #animateLeaving(ctx, v, timestamp) {
        if (!v.animStartTime) v.animStartTime = timestamp;
        const elapsed  = timestamp - v.animStartTime;
        const raw      = Math.min(elapsed / ANIM_DURATION_MS, 1);
        const progress = easeInOut(raw);

        const startPos = queuePos(v.startRoad, v.lane ?? 0, 0);
        const center   = { x: CX, y: CY };
        const end      = exitPos(v.endRoad);

        let pos;
        if (progress < 0.4) {
            pos = lerp(startPos, center, progress / 0.4);
        } else {
            pos = lerp(center, end, (progress - 0.4) / 0.6);
        }

        const alpha = progress > 0.75 ? 1 - (progress - 0.75) / 0.25 : 1;
        this.#drawVehicle(ctx, pos.x, pos.y, v, alpha);

        if (raw >= 1) this._onVehicleGone(v.id);
    }

    #drawVehicle(ctx, x, y, v, alpha) {
        if (alpha <= 0) return;
        ctx.save();
        ctx.globalAlpha = alpha;

        // glow
        ctx.shadowColor = v.color;
        ctx.shadowBlur  = 14;

        // body
        ctx.fillStyle = v.color;
        ctx.beginPath();
        ctx.arc(x, y, VEHICLE_RADIUS, 0, Math.PI * 2);
        ctx.fill();
        ctx.shadowBlur = 0;

        // headlight dot
        ctx.fillStyle = 'rgba(255,255,255,0.88)';
        ctx.beginPath();
        ctx.arc(x, y, 3, 0, Math.PI * 2);
        ctx.fill();

        // id label
        ctx.fillStyle   = '#f8fafc';
        ctx.font        = LABEL_FONT;
        ctx.textAlign   = 'center';
        ctx.textBaseline = 'bottom';
        ctx.shadowColor = 'rgba(0,0,0,0.8)';
        ctx.shadowBlur  = 4;
        ctx.fillText(v.id, x, y - VEHICLE_RADIUS - 2);
        ctx.shadowBlur  = 0;

        ctx.restore();
    }
}
