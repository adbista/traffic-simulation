import {
    W, H, CX, CY,
    queuePos, exitPos, lerp, easeInOut,
    ANIM_DURATION_MS,
} from './layout.js';

const VEHICLE_RADIUS = 9;
const LABEL_FONT = 'bold 8px monospace';

/**
 * Single Responsibility: animates and renders vehicle dots on the vehicle canvas layer.
 * Reads vehicle states from the store (passed in draw()), never mutates them.
 * Signals when a vehicle animation completes via onVehicleGone callback.
 */
export class VehicleLayer {
    /**
     * @param {HTMLCanvasElement} canvas  — transparent overlay canvas
     * @param {{ onVehicleGone: (id: string) => void }} opts
     */
    constructor(canvas, { onVehicleGone } = {}) {
        this._ctx = canvas.getContext('2d');
        this._onVehicleGone = onVehicleGone ?? (() => {});
    }

    /**
     * Render all vehicles for the current animation frame.
     * @param {VehicleState[]} vehicles - from VehicleStore.getAll()
     * @param {number} timestamp        - DOMHighResTimeStamp from requestAnimationFrame
     */
    draw(vehicles, timestamp) {
        const ctx = this._ctx;
        ctx.clearRect(0, 0, W, H);

        for (const v of vehicles) {
            if (v.state === 'waiting') {
                const pos = queuePos(v.startRoad, v.queueIndex);
                this.#drawVehicle(ctx, pos.x, pos.y, v, 1);
            } else if (v.state === 'leaving') {
                this.#animateLeaving(ctx, v, timestamp);
            }
        }
    }

    // ── private ───────────────────────────────────────────────────────────────

    #animateLeaving(ctx, v, timestamp) {
        if (!v.animStartTime) v.animStartTime = timestamp;

        const elapsed  = timestamp - v.animStartTime;
        const raw      = Math.min(elapsed / ANIM_DURATION_MS, 1);
        const progress = easeInOut(raw);

        const startPos = queuePos(v.startRoad, 0);
        const center   = { x: CX, y: CY };
        const end      = exitPos(v.endRoad);

        let pos;
        if (progress < 0.45) {
            // approach: queue → intersection centre
            pos = lerp(startPos, center, progress / 0.45);
        } else {
            // exit: intersection centre → off-screen
            pos = lerp(center, end, (progress - 0.45) / 0.55);
        }

        const alpha = progress > 0.8 ? 1 - (progress - 0.8) / 0.2 : 1;
        this.#drawVehicle(ctx, pos.x, pos.y, v, alpha);

        if (raw >= 1) {
            this._onVehicleGone(v.id);
        }
    }

    #drawVehicle(ctx, x, y, v, alpha) {
        if (alpha <= 0) return;
        ctx.save();
        ctx.globalAlpha = alpha;

        // glow
        ctx.shadowColor = v.color;
        ctx.shadowBlur  = 12;

        // body
        ctx.fillStyle = v.color;
        ctx.beginPath();
        ctx.arc(x, y, VEHICLE_RADIUS, 0, Math.PI * 2);
        ctx.fill();

        ctx.shadowBlur = 0;

        // white dot (headlight)
        ctx.fillStyle = 'rgba(255,255,255,0.85)';
        ctx.beginPath();
        ctx.arc(x, y, 3, 0, Math.PI * 2);
        ctx.fill();

        // id label above vehicle
        ctx.fillStyle = '#f8fafc';
        ctx.font = LABEL_FONT;
        ctx.textAlign  = 'center';
        ctx.textBaseline = 'bottom';
        ctx.fillText(v.id, x, y - VEHICLE_RADIUS - 2);

        ctx.restore();
    }
}
