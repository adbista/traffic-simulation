var __classPrivateFieldSet = (this && this.__classPrivateFieldSet) || function (receiver, state, value, kind, f) {
    if (kind === "m") throw new TypeError("Private method is not writable");
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a setter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot write private member to an object whose class did not declare it");
    return (kind === "a" ? f.call(receiver, value) : f ? f.value = value : state.set(receiver, value)), value;
};
var __classPrivateFieldGet = (this && this.__classPrivateFieldGet) || function (receiver, state, kind, f) {
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a getter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot read private member from an object whose class did not declare it");
    return kind === "m" ? f : kind === "a" ? f.call(receiver) : f ? f.value : state.get(receiver);
};
var _VehicleLayer_instances, _VehicleLayer_ctx, _VehicleLayer_onVehicleGone, _VehicleLayer_drawWaiting, _VehicleLayer_animateLeaving, _VehicleLayer_drawVehicle;
import { W, H, CX, CY, queuePos, exitPos, lerp, easeInOut, ANIM_DURATION_MS, } from './layout.js';
const VEHICLE_RADIUS = 9;
const LABEL_FONT = 'bold 8px monospace';
export class VehicleLayer {
    constructor(canvas, { onVehicleGone } = {}) {
        _VehicleLayer_instances.add(this);
        _VehicleLayer_ctx.set(this, void 0);
        _VehicleLayer_onVehicleGone.set(this, void 0);
        __classPrivateFieldSet(this, _VehicleLayer_ctx, canvas.getContext('2d'), "f");
        __classPrivateFieldSet(this, _VehicleLayer_onVehicleGone, onVehicleGone ?? (() => { }), "f");
        canvas.width = W;
        canvas.height = H;
    }
    draw(vehicles, timestamp) {
        const ctx = __classPrivateFieldGet(this, _VehicleLayer_ctx, "f");
        ctx.clearRect(0, 0, W, H);
        for (const v of vehicles) {
            if (v.state === 'waiting')
                __classPrivateFieldGet(this, _VehicleLayer_instances, "m", _VehicleLayer_drawWaiting).call(this, ctx, v);
            else if (v.state === 'leaving')
                __classPrivateFieldGet(this, _VehicleLayer_instances, "m", _VehicleLayer_animateLeaving).call(this, ctx, v, timestamp);
        }
    }
}
_VehicleLayer_ctx = new WeakMap(), _VehicleLayer_onVehicleGone = new WeakMap(), _VehicleLayer_instances = new WeakSet(), _VehicleLayer_drawWaiting = function _VehicleLayer_drawWaiting(ctx, v) {
    const pos = queuePos(v.startRoad, v.lane ?? 0, v.queueIndex);
    __classPrivateFieldGet(this, _VehicleLayer_instances, "m", _VehicleLayer_drawVehicle).call(this, ctx, pos.x, pos.y, v, 1);
}, _VehicleLayer_animateLeaving = function _VehicleLayer_animateLeaving(ctx, v, timestamp) {
    if (!v.animStartTime)
        v.animStartTime = timestamp;
    const elapsed = timestamp - v.animStartTime;
    const raw = Math.min(elapsed / ANIM_DURATION_MS, 1);
    const progress = easeInOut(raw);
    const startPos = queuePos(v.startRoad, v.lane ?? 0, 0);
    const center = { x: CX, y: CY };
    const end = exitPos(v.endRoad);
    const pos = progress < 0.4
        ? lerp(startPos, center, progress / 0.4)
        : lerp(center, end, (progress - 0.4) / 0.6);
    const alpha = progress > 0.75 ? 1 - (progress - 0.75) / 0.25 : 1;
    __classPrivateFieldGet(this, _VehicleLayer_instances, "m", _VehicleLayer_drawVehicle).call(this, ctx, pos.x, pos.y, v, alpha);
    if (raw >= 1)
        __classPrivateFieldGet(this, _VehicleLayer_onVehicleGone, "f").call(this, v.id);
}, _VehicleLayer_drawVehicle = function _VehicleLayer_drawVehicle(ctx, x, y, v, alpha) {
    if (alpha <= 0)
        return;
    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.shadowColor = v.color;
    ctx.shadowBlur = 14;
    ctx.fillStyle = v.color;
    ctx.beginPath();
    ctx.arc(x, y, VEHICLE_RADIUS, 0, Math.PI * 2);
    ctx.fill();
    ctx.shadowBlur = 0;
    ctx.fillStyle = 'rgba(255,255,255,0.88)';
    ctx.beginPath();
    ctx.arc(x, y, 3, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#f8fafc';
    ctx.font = LABEL_FONT;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'bottom';
    ctx.shadowColor = 'rgba(0,0,0,0.8)';
    ctx.shadowBlur = 4;
    ctx.fillText(v.id, x, y - VEHICLE_RADIUS - 2);
    ctx.shadowBlur = 0;
    ctx.restore();
};
