package org.example.trafficsim.signal;

import org.example.trafficsim.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class Phase {

    private final String id;
    private final PhaseTiming timing;
    private final Map<VehicleSignal, TrafficLightType> signals;
    private final long positionsMask;
    private final int[] allowedMovementMaskByPosition;

    public Phase(String id, PhaseTiming timing,
                 long positionsMask,
                 int[] allowedMovementMaskByPosition,
                 List<LaneSignal> signals
                 ) {
        this.id = id;
        this.timing = timing;
        this.positionsMask = positionsMask;
        this.allowedMovementMaskByPosition = allowedMovementMaskByPosition;
        this.signals = createVehicleSignalsMap(signals);
    }

    public String id()          { return id; }
    public PhaseTiming timing() { return timing; }
    public long positionsMask()  { return positionsMask; }

    public long allowedMask(int positionId) {
        return allowedMovementMaskByPosition[positionId];
    }

    public boolean allowsDeparture(int positionId, Road fromRoad, Road toRoad) {
        long mask = allowedMovementMaskByPosition[positionId];
        if (mask == 0) return false;

        Movement needed = Movement.fromRoads(fromRoad, toRoad);
        return switch (needed) {
            case RIGHT -> (mask & MovementMask.RIGHT) != 0;
            case LEFT -> (mask & MovementMask.LEFT) != 0;
            case STRAIGHT -> (mask & MovementMask.STRAIGHT) != 0;
        };
    }
    // Vehicles don't carry TrafficLightType, so we expand each LaneSignal's bitmask into
    // individual (road, lane, movement) → type entries for runtime yield checks.
    private Map<VehicleSignal, TrafficLightType> createVehicleSignalsMap(List<LaneSignal> laneSignals) {
        Map<VehicleSignal, TrafficLightType> map = new HashMap<>();
        for (LaneSignal ls : laneSignals) {
            for (Movement m : MovementMask.movementsFromMask(ls.movementMask())) {
                VehicleSignal vs = new VehicleSignal(ls.road(), ls.laneIndex(), m);
                map.put(vs, ls.trafficLightType());
            }
        }
        return map;
    }

    public TrafficLightType getTrafficLightMode(VehicleSignal signal) {
        if (!signals.containsKey(signal)) {
            return null;
        }
        return signals.get(signal);
    }
}
