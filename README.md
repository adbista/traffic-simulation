# Traffic Simulation

This project simulates adaptive traffic lights at a four-way intersection. The system
adjusts signal cycles based on real-time vehicle queues and arrival rates. It is written in Java 17 with Spring Boot and can be driven either by a JSON command file (CLI) or through a REST / WebSocket API.

---

## Algorithm

### Intersection model

The intersection has four approach roads: NORTH, WEST, SOUTH, EAST. Each road may have one or more lanes. Each lane carries a set of movements (STRAIGHT, LEFT, RIGHT) and each
movement is tagged with a signal type:

- GENERIC: the light permits the movement but a left-turning vehicle must still yield to
  oncoming straight/right traffic at runtime (unprotected left).
- PROTECTED: the light guarantees geometric safety; no runtime yield check is needed.

### Traffic light IDs and signal grouping

Every movement declaration in a lane carries a `trafficLightId` string. This ID is the way you tell the system which movements share a single physical traffic light on that lane. All movements on the same lane that share the same `trafficLightId` are
merged into one signal group. Their movement bits are OR-ed together into a single bitmask and they get exactly one `LaneSignal` entry. The `type` (GENERIC or PROTECTED) is taken from the first declaration in that group.

Example: same ID, movements merged into one signal

```json
{ "road": "north", "lane": 0, "movements": [
    { "movement": "LEFT",  "type": "GENERIC", "trafficLightId": "t1" },
    { "movement": "RIGHT", "type": "GENERIC", "trafficLightId": "t1" }
]}
```

Result: one LaneSignal(NORTH, lane=0, mask=LEFT|RIGHT, GENERIC). Both movements are
controlled by the same physical light. When that light is green, both LEFT and RIGHT are
permissible on that lane (subject to runtime yield for the LEFT). Please do not use same id when you have different signal types.

Example: different IDs, two independent signals

```json
{ "road": "north", "lane": 0, "movements": [
    { "movement": "LEFT",  "type": "GENERIC",   "trafficLightId": "t1" },
    { "movement": "RIGHT", "type": "PROTECTED", "trafficLightId": "t2" }
]}
```

Result: two separate LaneSignals on the same lane. The DSATUR algorithm treats them as
independent nodes in the conflict graph. They may end up in different phases (but not for sure) , meaning LEFT and RIGHT on the same physical lane can be green at different times. This is the way to model an intersection where a dedicated arrow controls one direction independently from the general signal on the same lane.

### Lane priorities

Each position (road + lane index) can be assigned a numeric weight. This weight is a
multiplier applied to the scoring function for that position:

    score(P) += lane_weight[i] * (flow_weight * EMA[i] + queue_weight * queue[i] + ageW * age[i])

The default weight for every lane is 1.0. Setting a higher value (for example 2.0 for a bus lane) makes the controller prefer that lane more strongly when deciding which phase to activate next. The weight does not affect conflict detection or the light cycle mechanics; it only influences how urgently the system wants to serve that lane relative to others.

Weights are configured per lane in the `laneDeclarations` block using the `weight` field. If omitted the default of 1.0 is used.

### Automatic phase generation with DSATUR graph coloring

Before the simulation starts, the system automatically builds a conflict-free set of phases from the lane configuration. The key insight is that two signals can share a phase only if they do not geometrically conflict with each other.

This is modeled as a graph coloring problem. Each lane signal is a node. An edge between two nodes means they cannot be active simultaneously. Each color produced by the algorithm is one traffic phase.

The system uses DSATUR (Degree of Saturation), a greedy heuristic that gives near-optimal
colorings on sparse conflict graphs:

1. Build the conflict graph for all lane signals.
2. Iteratively pick the uncolored node with the highest saturation (number of distinct colors
   already seen among its neighbors). Break ties by the number of remaining uncolored
   neighbors.
3. Assign the smallest color not used by any neighbor.
4. Each resulting color group becomes one phase; signals in the same phase all get green at the same time.

DSATUR complexity is O(V^3) where V is the number of lane signals. 

Conflict rules (ConflictMovements):
- Same road: never conflict (different lanes on the same road can run together).
- Perpendicular roads: always conflict (crossing paths).
- Opposite roads with at least one PROTECTED signal: geometric check applies. STRAIGHT vs
  STRAIGHT is fine; PROTECTED LEFT vs any oncoming movement is a conflict.
- Opposite roads, both GENERIC: no static conflict; a left-turning vehicle must yield at
  runtime instead.

It can be adjusted as we want to support more complex scenarios in the future.

### Phase scoring

Every simulation step, after departures are processed, the PhaseController evaluates each phase with a scoring function:

    score(P) = sum over position i in P of:
        lane_weight[i] * (flow_weight * EMA[i] + queue_weight * queue[i] + age_weight * min(age[i], age_cap))

Default weights: flow_weight = 10.0, queue_weight = 1.0, age_weight = 0.01, age_cap = 200.

- EMA[i]: exponential moving average of vehicle arrivals at lane i. Tracks the underlying
  traffic trend rather than noisy per-step counts.
- queue[i]: number of vehicles waiting at lane i, providing a direct measure of current
  congestion.
- age[i]: number of steps since lane i last had a green light, capped at age_cap. This is
  the anti-starvation component; it ensures no lane waits forever, but the cap prevents very old lanes from dominating the score indefinitely.

Positions with an empty queue and no incoming flow contribute zero to the score, so the
system never awards green time to a lane that has nothing to serve.

Lane weights allow the operator to give priority to certain lanes (for example a bus lane or a high-volume arterial).

### EMA flow tracking

Vehicle arrival rate is tracked using an exponential moving average (EMA):

    EMA_t = (1 - beta) * EMA_(t-1) + beta * arrivals_t

- arrivals_t: number of new vehicles that arrived at the lane during step t.
- EMA_(t-1): the previous EMA value for that lane.

Default beta = 0.10, which smooths over roughly ten steps. A higher beta reacts faster but is more sensitive to temporary spikes. A lower beta gives a more stable estimate but adapts more slowly to changes in traffic demand.

### Phase controller logic

The PhaseController runs once per step and decides whether to switch phases. The decision
process is:

1. Do nothing if the current phase is not GREEN. No decisions during YELLOW or all-RED.
2. Respect minimum green time (minGreenSteps). The current phase must have been GREEN for at least this many steps before any switch is even considered.
3. Force a round-robin switch at maximum green time (maxGreenSteps). This prevents a single direction from holding the green indefinitely when traffic is continuous.
4. Gap-out. If no vehicles are waiting on any lane of the current phase, immediately jump to the highest-scoring phase. This avoids wasting time and cycling through empty phases one by one, which would be especially wasteful in light-traffic conditions.
5. Score-based switch with hysteresis. If another phase scores strictly higher than the
   current one by more than a configurable threshold (default 10%), the switch is triggered. The hysteresis window prevents rapid oscillation when two phases have nearly equal scores.

When a switch is triggered, the signal transitions through the full cycle:
GREEN -> YELLOW (if yellowSteps > 0) -> all-RED clearance -> GREEN (new phase).
by default yellow step is disabled since we don't want the whole step to be consumed by the yellow phase. It is not needed anyway because frontend can simulate the yellow light visually without blocking the backend step.

### Safety guarantees

Safety is enforced at two independent layers:

Phase level (static): DSATUR ensures that no two signals in the same phase geometrically
conflict. This is checked once at startup and never needs to be re-evaluated at runtime.

Runtime yield (YieldCheck): For GENERIC left-turn movements, the system checks at each step whether an opposing vehicle with a straight or right-turn intent is already queued and also holds green. If so, the left-turning vehicle must wait in place even though its phase is green. PROTECTED left turns bypass this check entirely because the phase schedule already guarantees no opposing green.

### Complexity

| Component                        | Complexity                       |
|----------------------------------|----------------------------------|
| Phase generation (DSATUR)        | O(V^3), V = signal count         |
| Phase scoring per step           | O(P * L), P = phases, L = lanes  |
| Vehicle departure per step       | O(L), L = active positions       |
| EMA update per step              | O(N), N = total positions        |


The DSATUR O(V^3) cost applies only to startup. During the simulation, the scoring loop uses bitmask operations (position bitmasks stored as long) to iterate only over the positions active in a given phase, avoiding per-lane array scans over all positions.

---

## Running the simulation (CLI)

The main entry point reads a JSON input file and writes a JSON output file. Build with Gradle
and run:

```bash
./gradlew run --args="input.json output.json"
```


### Input format

The `config` section is optional. When omitted, the simulation defaults to one lane per road
with GENERIC STRAIGHT/LEFT/RIGHT movements and timing (minGreen=1, maxGreen=5, yellow=0, red=1).

Full example with multi-lane roads, a protected left-turn arrow, mixed signal groups, and
custom lane weights (this is also scenario `05-multilane-grouped-signals` in `src/test/resources/scenarios/`):

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

What this config sets up:

- NORTH lane 0: one physical light (`n0-main`) covers both STRAIGHT and RIGHT together Because they share the same `trafficLightId`, they are one signal group -- one LaneSignal with a merged bitmask. That signal is GENERIC.
- NORTH lane 1: a separate protected left-arrow (`n1-arrow`). Because it has a different ID, DSATUR treats it as an independent node. It will end up in its own phase, separate from the NORTH lane 0 signal, so the arrow can be green while the main light is red and vice versa.
- SOUTH lane 0: one signal (`s0-main`) covering all three movements -- merged mask
  STRAIGHT|RIGHT|LEFT, all GENERIC. A vehicle going LEFT on this lane must still yield at
  runtime to oncoming traffic.
- EAST and WEST: single standard lights covering all movements.
- NORTH lane 0 has weight 1.5 so it scores 50% higher than default lanes when the
  controller is deciding which phase to activate next.

### Output format

```json
{
  "stepStatuses": [
    { "leftVehicles": ["v1", "v2"] },
    { "leftVehicles": [] }
  ]
}
```

Each entry in `stepStatuses` corresponds to one `step` command.

---

## Running via Docker

### 1) Build the image

```bash
docker build -t trafficsim:latest .
```

### 2) Start the container

```bash
docker run --rm -p 8080:8080 --name trafficsim trafficsim:latest
```

The application is then available at:
- http://localhost:8080
- WebSocket: ws://localhost:8080/v1/ws/simulation

## Web client

The frontend is in the `web/` directory.

Quick start:
1. Start the backend (locally or in Docker).
2. In a separate terminal, serve the frontend:
   ```bash
   python -m http.server 5500 --directory web
   ```
3. Open: http://localhost:5500

Details: `web/README.md`.

## E2E tests (Docker Compose)

End-to-end tests run the application in a separate container on port 9090 and send a full
WebSocket scenario (addVehicle -> step -> assertion).

```bash
# 1. Build the application image
docker build -t trafficsim:latest .

# 2. Run the E2E suite
docker compose -f docker-compose.e2e.yml up --abort-on-container-exit --exit-code-from e2e

# 3. Clean up
docker compose -f docker-compose.e2e.yml down --remove-orphans
```

What the test (`e2e/run.js`) checks:
1. Waits for `GET /health` to return `{"status":"UP"}` (retries up to 30 s).
2. Connects via WebSocket.
3. Sends init `{}`, two `addVehicle` commands, two `step` commands, then `stop`.
4. Asserts that the first `StepStatus.leftVehicles` contains both vehicles.

## GitHub Actions -- `workflow.yml`

File: `.github/workflows/workflow.yml`

The workflow runs on:
- `push` to `main`/`master`
- `pull_request` to `main`/`master`
- manually (`workflow_dispatch`)

### Job 1 -- `build`
1. Sets up JDK 17, runs `./gradlew clean build`.
2. Publishes artifacts: `app-jar`, `test-reports`, `web-client`.

### Job 2 -- `e2e` (depends on `build`)
1. Builds the Docker image.
2. Runs `docker-compose.e2e.yml` with the test container.
3. On failure, dumps logs from both containers.
