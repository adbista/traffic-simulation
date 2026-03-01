package org.example.trafficsim.core;

import org.example.trafficsim.control.SignalController;
import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.Vehicle;
import org.example.trafficsim.signal.ActivePhase;

import java.util.*;

// Runs the simulation step by step.
// Each call to step() does three things:
//   1. Releases vehicles on green roads (one per lane, highest priority first).
//   2. Asks the signal controller whether to switch the phase.
//   3. Advances the signal state machine (GREEN -> YELLOW -> ALL_RED -> GREEN).
//
// Departure is always lane-level and uses greenLaneIndices from the phase:
//   - if the phase declares specific lanes (signal-based via PhaseSetGenerator or manually):
//       only the indicated lanes are green; one vehicle per lane departs.
//   - if the phase declares no lanes (manually constructed Phase without greenLaneIndices):
//       defaults to lane 0 for each green road.
public class SimulationEngine {
    private final TrafficQueues queues;
    private final ActivePhase activePhase;
    private final SignalController controller;

    private long stepNo = 0;
    private long vehicleSeq = 0;

    public SimulationEngine(TrafficQueues queues, ActivePhase activePhase, SignalController controller) {
        this.queues = queues;
        this.activePhase = activePhase;
        this.controller = controller;
    }

    public void addVehicle(String id, Road start, Road end, int lane) {
        queues.addVehicle(new Vehicle(id, start, end, stepNo, vehicleSeq++, lane));
    }

    public StepResult step() {
        stepNo++;

        List<Vehicle> departing = new ArrayList<>();

        Set<Road> greens = activePhase.isGreen()
                ? activePhase.current().greenRoads()
                : Set.of();

        // Release vehicles and simultaneously build greenLaneMap
        Map<Road, Set<Integer>> greenLaneMap = new EnumMap<>(Road.class);
        for (Road r : greens) {
            Set<Integer> greenLanes = activePhase.current().greenLanesFor(r);
            Set<Integer> toProcess = greenLanes.isEmpty() ? Set.of(0) : greenLanes;
            greenLaneMap.put(r, toProcess);
            for (int laneIdx : toProcess) {
                Vehicle v = queues.pollSpecificLane(r, laneIdx);
                if (v != null) departing.add(v);
            }
        }

        departing.sort(Comparator.comparingLong(Vehicle::seqNum));

        queues.updateWaitCounters(greenLaneMap);

        CrossroadState snap = queues.snapshot();
        controller.maybeRequestSwitch(snap, activePhase);
        activePhase.tick(controller::resolve);

        List<String> leftVehicles = new ArrayList<>(departing.size());
        departing.forEach(v -> leftVehicles.add(v.id()));
        return new StepResult(leftVehicles);
    }
}

