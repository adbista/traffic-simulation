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

        Phase currentPhase = activePhase.current();

        List<Vehicle> departing = new ArrayList<>();

        if (activePhase.isGreen()) {
            long greenMask = currentPhase.positionsMask();
            long tmp = greenMask;

            while (tmp != 0) {
                int posId = Long.numberOfTrailingZeros(tmp);
                tmp &= tmp - 1;

                greenTracker.markGreenNow(posId, stepNo);

                if ((queues.nonEmptyPositionsMask() & (1L << posId)) == 0L) {
                    continue;
                }

                Vehicle head = queues.peek(posId);
                Road fromRoad = queues.roadOf(posId);

                boolean allowed = currentPhase.allowsDeparture(posId, fromRoad, head.endRoad());
                boolean yieldOk = allowed && YieldCheck.check(head, currentPhase, queues);

                if (yieldOk) {
                    Vehicle v = queues.poll(posId);
                    departing.add(v);
                }
            }
        } 

        departing.sort(Comparator.comparingLong(Vehicle::seqNum));
        List<String> leftVehicles = new ArrayList<>(departing.size());
        for (Vehicle v : departing) leftVehicles.add(v.id());

        // decision first, then FSM tick
        controller.managePhaseSwitch(queues, stepNo, activePhase);
        activePhase.manageLightState();

        return new StepResult(leftVehicles, activePhase.current().id(), activePhase.currentState());
    }

    public List<Phase> getPhases() {
        return controller.getPhases();
    }

}