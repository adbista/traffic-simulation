package org.example.trafficsim.core;

import org.example.trafficsim.control.PhaseController;
import org.example.trafficsim.metrics.FlowEmaTracker;
import org.example.trafficsim.metrics.GreenStepTracker;
import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.Vehicle;
import org.example.trafficsim.signal.ActivePhase;
import org.example.trafficsim.signal.Phase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SimulationEngine {
    private final TrafficQueues queues;
    private final ActivePhase activePhase;
    private final PhaseController controller;
    private final FlowEmaTracker flowTracker;
    private final GreenStepTracker greenTracker;
    private int stepNo = 0;
    private int vehicleSeq = 0;

    public SimulationEngine(TrafficQueues queues, ActivePhase activePhase, PhaseController controller,
                            FlowEmaTracker flowTracker, GreenStepTracker greenTracker) {
        this.queues = queues;
        this.activePhase = activePhase;
        this.controller = controller;
        this.flowTracker = flowTracker;
        this.greenTracker = greenTracker;
    }

    public void addVehicle(String id, Road start, Road end, int lane) {
        int posId = queues.addVehicle(new Vehicle(id, start, end, stepNo, vehicleSeq++, lane));
        flowTracker.onArrival(posId);
    }

    public StepResult step() {
        stepNo++;
        flowTracker.advanceStep();

        List<Vehicle> departing = new ArrayList<>();
        Phase currentPhase = activePhase.current();

        if (activePhase.isGreen()) {
            // mask for positionIds that are related to this phase
            long greenMask = currentPhase.positionsMask();
            long tmp = greenMask;

            while (tmp != 0) {
                // find next '1' indicating this positionId is in this phase
                int posId = Long.numberOfTrailingZeros(tmp);
                tmp &= tmp - 1;

                greenTracker.markGreenNow(posId, stepNo);
                // check if there is a vehicle on this line
                if ((queues.nonEmptyPositionsMask() & (1L << posId)) == 0L) continue;
                Vehicle head = queues.peek(posId);

                Road fromRoad = queues.roadOf(posId);
                // allowsDeparture comes from situation that on the same line we can have possible movements like LEFT, RIGHT.
                // One car wants to go LEFT which is allowed in this phase
                // but second wants to go RIGHT, which is prohibited - red light
                if (currentPhase.allowsDeparture(posId, fromRoad, head.endRoad())
                        && YieldCheck.check(head, currentPhase, queues)) {
                    departing.add(queues.poll(posId));
                }

            }
        }
        // Sort by arrival sequence so that leftVehicles is in deterministic order.
        // Without this, vehicles would appear in posId-iteration order (e.g. NORTH before
        // SOUTH), not arrival order. Scenario tests use exact list equality, so this is
        // required for 01-recruitment-scenario (expected: [vehicle1, vehicle2] = arrival
        // order, not [vehicle2, vehicle1] = posId order). Traffic correctness is unaffected;
        // the ordering only matters for reproducible output.
        departing.sort(Comparator.comparingLong(Vehicle::seqNum));
        List<String> leftVehicles = new ArrayList<>(departing.size());
        for (Vehicle v : departing) {
            leftVehicles.add(v.id());
        }

        controller.managePhaseSwitch(queues, stepNo, activePhase);

        activePhase.manageLightState();

        return new StepResult(leftVehicles);
    }
}
