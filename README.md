# Traffic Simulation

Adaptive traffic-light simulator for a four-way intersection. Written in Java 17 / Spring Boot. Accepts commands either from a JSON file (CLI) or via REST / WebSocket.

---

## Algorithm

### Intersection model

Four approach roads: NORTH, WEST, SOUTH, EAST. Each road has one or more lanes; each lane declares one or more movements (STRAIGHT, LEFT, RIGHT) with a signal type:

- **GENERIC** — light permits the movement; a left-turning vehicle still yields to oncoming straight/right traffic at runtime.
- **PROTECTED** — geometric safety is guaranteed by the phase schedule; no runtime yield needed.

### Traffic light IDs and signal grouping

Each movement in a lane declaration carries a `trafficLightId`. Movements on the **same lane** that share an ID are merged into one `LaneSignal` (their movement bits are OR-ed into a bitmask). Movements with **different IDs** produce independent signals that may end up in different phases.

Same ID → one signal:
```json
{ "road": "north", "lane": 0, "movements": [
    { "movement": "LEFT",  "type": "GENERIC", "trafficLightId": "t1" },
    { "movement": "RIGHT", "type": "GENERIC", "trafficLightId": "t1" }
]}
```
Result: `LaneSignal(NORTH, lane=0, mask=LEFT|RIGHT, GENERIC)`.

Different IDs → two independent signals (can be in separate phases):
```json
{ "road": "north", "lane": 0, "movements": [
    { "movement": "LEFT",  "type": "GENERIC",   "trafficLightId": "t1" },
    { "movement": "RIGHT", "type": "PROTECTED", "trafficLightId": "t2" }
]}
```
Do not reuse the same ID for movements with different signal types.

### Lane weights

Each lane can have a numeric `weight` (default `1.0`) that scales its contribution in the scoring function. A higher weight (e.g. `2.0` for a bus lane) makes the controller prefer that lane when choosing the next phase. It does not affect conflict detection.

### Phase generation — DSATUR graph coloring

Two signals can share a phase only if they do not geometrically conflict. This is modeled as graph coloring: each `LaneSignal` is a node; an edge means the two signals cannot be green simultaneously. Each color maps to one phase.

The system uses **DSATUR** (Degree of Saturation):

1. Build the conflict graph.
2. Select the next uncolored node by:
   - **Highest saturation** — the number of *distinct colors already assigned to its neighbors*. A highly saturated node has fewer legal colors left and must be placed first to avoid dead-ends.
   - **Tie-break: highest degree** — among equally saturated nodes, pick the one with the most neighbors (uncolored or not). Dense nodes are more constrained; placing them early keeps future choices feasible.
3. Assign the lowest-numbered color not used by any neighbor.
4. Repeat until all nodes are colored. Each color group becomes one phase.

Complexity: O(V³) where V = number of signals, paid once at startup.

Conflict rules:
- **Same road**: never conflict.
- **Perpendicular roads**: always conflict (crossing paths).
- **Opposite roads, >= 1 PROTECTED**: STRAIGHT vs STRAIGHT is safe; PROTECTED LEFT vs any oncoming signal conflicts.
- **Opposite roads, both GENERIC**: no static conflict; left-turner yields at runtime.

### Phase scoring

After each departure step the controller scores every phase to decide what to activate next:

    score(P) = Σ  lane_weight[i] × (10·EMA[i] + 1·queue[i] + 0.01·min(age[i], 200))

The sum runs over all positions (road + lane) that belong to phase P.

**Components:**

- **EMA[i]** (`β = 0.10`) — exponential moving average of vehicle arrivals:  
  `EMA_t = (1 − β)·EMA_(t−1) + β·arrivals_t`  
  Tracks the *trend* of incoming traffic rather than noisy per-step counts. Weight **10** makes it the dominant signal: a lane with steady throughput will always score high even when its queue is temporarily empty.

- **queue[i]** — number of vehicles currently waiting at lane i. Captures *immediate* congestion. Weight **1** keeps it secondary to the trend, so a transient burst does not override a consistently busy lane.

- **age[i]** — number of steps since lane i last received green, capped at 200. Provides *anti-starvation*: a lane that has been waiting a long time gradually accumulates score so it cannot be ignored forever. The cap prevents a very stale lane from dominating the score once it reaches the ceiling.

- **lane_weight[i]** — per-lane multiplier (default `1.0`). Lets the operator permanently boost a lane (e.g. `2.0` for a bus lane) without touching the other weights.

Lanes with zero queue and zero EMA contribute nothing, so the controller never awards green time to an empty phase.

### Phase controller logic

Each step (only while current phase is GREEN):

1. **Minimum green** — no switch before `minGreenSteps` have elapsed.
2. **Maximum green** — force a switch after `maxGreenSteps` to prevent starvation.
3. **Gap-out** — if no vehicle is queued in the current phase, immediately switch to the highest-scoring phase.
4. **Hysteresis switch** — switch when another phase's score exceeds the current by > 10 %. The threshold prevents rapid oscillation between nearly equal phases.

Transition: GREEN → YELLOW (skipped if `yellowSteps = 0`) → all-RED clearance → GREEN (new phase).

### Safety

- **Static (DSATUR)**: conflicting signals are never in the same phase; checked once at startup.
- **Runtime (YieldCheck)**: GENERIC left-turners yield to any opposing STRAIGHT/RIGHT vehicle that also holds green that step. PROTECTED left-turns skip this check.

### Complexity

| Component                  | Complexity                                    |
| -------------------------- | --------------------------------------------- |
| Phase generation (DSATUR)  | O(V^3), V = number of signals — startup only   |
| Phase scoring per step     | O(P × L) worst case                           |
| Vehicle departure per step | O(A + Y·O + D log D)                          |
| EMA update per step        | O(N), N = total lanes                         |

P = phases, L = lanes, A = active positions, Y = yield-checked vehicles, O = opposite-road positions, D = departing vehicles.

Runtime scoring uses 64-bit position bitmasks to iterate only active positions, avoiding full lane scans.

---

## Running

### CLI (JSON file)

```bash
./gradlew run --args="input.json output.json"
```

### Spring Boot (local server)

```bash
./gradlew bootRun --args="--server.port=8080"
```

### Input format

The `config` block is optional. Without it the simulation uses one GENERIC lane (STRAIGHT / LEFT / RIGHT) per road and timing `minGreen=1, maxGreen=5, yellow=0, red=1`.

Full example (also scenario `05-multilane-grouped-signals`):

```json
{
  "config": {
    "timing": { "minGreen": 2, "maxGreen": 10, "yellow": 1, "red": 1 },
    "laneDeclarations": [
      {
        "road": "north", "lane": 0, "weight": 1.5,
        "movements": [
          { "movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "n0-main" },
          { "movement": "RIGHT",    "type": "GENERIC", "trafficLightId": "n0-main" }
        ]
      },
      {
        "road": "north", "lane": 1,
        "movements": [
          { "movement": "LEFT", "type": "PROTECTED", "trafficLightId": "n1-arrow" }
        ]
      },
      {
        "road": "south", "lane": 0,
        "movements": [
          { "movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "s0-main" },
          { "movement": "RIGHT",    "type": "GENERIC", "trafficLightId": "s0-main" },
          { "movement": "LEFT",     "type": "GENERIC", "trafficLightId": "s0-main" }
        ]
      },
      {
        "road": "east", "lane": 0,
        "movements": [
          { "movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "e0" },
          { "movement": "RIGHT",    "type": "GENERIC", "trafficLightId": "e0" },
          { "movement": "LEFT",     "type": "GENERIC", "trafficLightId": "e0" }
        ]
      },
      {
        "road": "west", "lane": 0,
        "movements": [
          { "movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "w0" },
          { "movement": "RIGHT",    "type": "GENERIC", "trafficLightId": "w0" },
          { "movement": "LEFT",     "type": "GENERIC", "trafficLightId": "w0" }
        ]
      }
    ]
  },
  "commands": [
    { "type": "addVehicle", "vehicleId": "v1", "startRoad": "north", "endRoad": "south", "lane": 0 },
    { "type": "addVehicle", "vehicleId": "v2", "startRoad": "south", "endRoad": "north", "lane": 0 },
    { "type": "addVehicle", "vehicleId": "v3", "startRoad": "north", "endRoad": "east",  "lane": 1 },
    { "type": "step" },
    { "type": "step" },
    { "type": "step" }
  ]
}
```

Key points of this config:
- **NORTH lane 0** (`n0-main`): STRAIGHT + RIGHT share one signal (merged bitmask), GENERIC.
- **NORTH lane 1** (`n1-arrow`): independent PROTECTED LEFT arrow — different ID, so DSATUR places it in its own phase.
- **SOUTH lane 0** (`s0-main`): all three movements merged into one GENERIC signal; LEFT yields at runtime.
- **NORTH lane 0 weight 1.5**: scores 50 % higher than default lanes when the controller picks the next phase.

### Output format

```json
{
  "stepStatuses": [
    { "leftVehicles": ["v1", "v2"] },
    { "leftVehicles": [] }
  ]
}
```

Each entry corresponds to one `step` command.

---

## Running via Docker

```bash
docker build -t trafficsim:latest .
docker run --rm -p 8080:8080 --name trafficsim trafficsim:latest
```

Available at `http://localhost:8080` / `ws://localhost:8080/v1/ws/simulation`.

## Web client

**Recommended**:
You can run the spring boot app using 
`./gradlew bootRun --args="--server.port=8080"` and then open `http://localhost:8080` to access the web client.

**Alternative** (serving static files with Python):
```bash
# 1. Start the backend (locally or in Docker)
# 2. Serve the frontend
python -m http.server 5500 --directory web
# 3. Open http://localhost:5500
```



## E2E tests (Docker Compose)

```bash
docker build -t trafficsim:latest .
docker compose -f docker-compose.e2e.yml up --abort-on-container-exit --exit-code-from e2e
docker compose -f docker-compose.e2e.yml down --remove-orphans
```

The test (`e2e/run.js`) waits for `/health`, connects via WebSocket, sends `addVehicle` × 2 + `step` × 2, and asserts both vehicles departed in the first step.

## GitHub Actions — `workflow.yml`

Triggers: push/PR to `main`/`master`, manual dispatch.

- **build**: `./gradlew clean build`, publishes `app-jar`, `test-reports`, `web-client`.
- **e2e** (after build): builds Docker image, runs `docker-compose.e2e.yml`, dumps logs on failure.
