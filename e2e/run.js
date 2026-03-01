/**
 * E2E test for traffic simulation via WebSocket.
 *
 * Flow:
 *   1. Poll GET /health until server is UP (max 30 s).
 *   2. Connect WebSocket.
 *   3. Send WsInitRequest ({}).
 *   4. Add two vehicles: v1 (south → north), v2 (north → south).
 *   5. Advance two steps.
 *   6. Assert that the first StepStatus.leftVehicles contains both v1 and v2.
 *   7. Send stop and close cleanly.
 *
 * Exit 0 on pass, 1 on failure.
 */

import WebSocket from "ws";

const WS_URL = process.env.WS_URL ?? "ws://localhost:8080/v1/ws/simulation";
const HEALTH_URL = process.env.HEALTH_URL ?? "http://localhost:8080/health";

const MAX_WAIT_MS = 30000;
const RETRY_INTERVAL = 2000;

function log(tag, msg) {
    console.log(`[${new Date().toISOString()}] [${tag}] ${msg}`);
}

async function waitForHealth() {
    const deadline = Date.now() + MAX_WAIT_MS;
    while (Date.now() < deadline) {
        try {
            const res  = await fetch(HEALTH_URL);
            if (res.ok) {
                const body = await res.json();
                log("HEALTH", `Server ready: ${JSON.stringify(body)}`);
                return;
            }
            log("HEALTH", `HTTP ${res.status}, retrying…`);
        } catch {
            log("HEALTH", `Not reachable yet, retrying in ${RETRY_INTERVAL}ms…`);
        }
        await new Promise((r) => setTimeout(r, RETRY_INTERVAL));
    }
    throw new Error(`Server did not become healthy within ${MAX_WAIT_MS / 1000}s`);
}

async function runScenario() {
    return new Promise((resolve, reject) => {
        const ws = new WebSocket(WS_URL);
        const stepResponses = [];

        ws.on("open", () => {
            log("WS", `Connected to ${WS_URL}`);

            ws.send(JSON.stringify({}));

            ws.send(JSON.stringify({ type: "addVehicle", vehicleId: "v1", startRoad: "south", endRoad: "north" }));
            ws.send(JSON.stringify({ type: "addVehicle", vehicleId: "v2", startRoad: "north", endRoad: "south" }));

            ws.send(JSON.stringify({ type: "step" }));
            ws.send(JSON.stringify({ type: "step" }));

            ws.send(JSON.stringify({ type: "stop" }));
        });

        ws.on("message", (raw) => {
            let msg;
            try {
                msg = JSON.parse(raw.toString());
            } catch {
                return reject(new Error(`Non-JSON message received: ${raw}`));
            }

            if (msg.error) {
                ws.terminate();
                return reject(new Error(`Server error frame: ${msg.error}`));
            }

            if (Array.isArray(msg.leftVehicles)) {
                stepResponses.push(msg.leftVehicles);
                log("WS", `StepStatus #${stepResponses.length}: leftVehicles=${JSON.stringify(msg.leftVehicles)}`);
            }
        });

        ws.on("close", () => {
            log("WS", "Connection closed");

            if (stepResponses.length < 1) {
                return reject(new Error("No StepStatus frames received"));
            }

            const step1 = stepResponses[0];
            const hasV1 = step1.includes("v1");
            const hasV2 = step1.includes("v2");

            if (hasV1 && hasV2) {
                log("ASSERT", `PASS — step 1 leftVehicles=${JSON.stringify(step1)}`);
                resolve();
            } else {
                reject(
                    new Error(
                        `FAIL — step 1 leftVehicles=${JSON.stringify(step1)}, expected ["v1","v2"]`
                    )
                );
            }
        });

        ws.on("error", (err) => reject(new Error(`WS transport error: ${err.message}`)));
    });
}

async function main() {
    log("E2E", `Health URL: ${HEALTH_URL}`);
    log("E2E", `WebSocket URL: ${WS_URL}`);

    await waitForHealth();
    await runScenario();

    log("E2E", "All assertions passed ✓");
    process.exit(0);
}

main().catch((err) => {
    console.error(`[E2E] FAILED: ${err.message}`);
    process.exit(1);
});
