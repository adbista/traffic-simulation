package org.example.trafficsim.core;

import org.example.trafficsim.control.SignalController;
import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.Vehicle;
import org.example.trafficsim.signal.ActivePhase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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

        for (Road r : greens) {
            Set<Integer> greenLanes = activePhase.current().greenLanesFor(r);
            for (int laneIdx : greenLanes) {
                queues.markGreenNow(r, laneIdx, stepNo);

                Vehicle v = queues.pollSpecificLane(r, laneIdx);
                if (v != null) {
                    departing.add(v);
                }
            }
        }

        departing.sort(Comparator.comparingLong(Vehicle::seqNum));

        controller.maybeRequestSwitch(queues, stepNo, activePhase);
        activePhase.tick(controller::resolve);

        List<String> leftVehicles = new ArrayList<>(departing.size());
        for (Vehicle v : departing) {
            leftVehicles.add(v.id());
        }

        return new StepResult(leftVehicles);
    }
}