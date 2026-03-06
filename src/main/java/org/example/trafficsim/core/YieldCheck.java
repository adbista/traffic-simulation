package org.example.trafficsim.core;

import org.example.trafficsim.model.*;
import org.example.trafficsim.signal.Phase;
import org.example.trafficsim.model.VehicleSignal;


/** Used only in situations when we have green light BUT we are not allowed to make a move. */
public final class YieldCheck { // give way

    private YieldCheck() {}

    /**
     *
     * @param vehicle - vehicle at that position
     * @param currentPhase - active phase - green lights on
     * @param queues - used to peek at opposing vehicles
     */
    public static boolean check(Vehicle vehicle,
                                Phase currentPhase,
                                TrafficQueues queues) {

        Movement movement = Movement.fromRoads(vehicle.startRoad(), vehicle.endRoad());
        // create VehicleSignal for this vehicle
        var trafficLightMode = currentPhase.getTrafficLightMode(new VehicleSignal(vehicle.startRoad(), vehicle.lane(), movement));
        if (trafficLightMode == TrafficLightType.PROTECTED || movement != Movement.LEFT){
            // we are protected from collisions
            return true;
        }

        // Find all positions in this phase that belong to the opposite road.
        Road opposite = Movement.opposite(vehicle.startRoad());
        int[] positionIdsFromOppositeLanes = queues.positionIdsOfRoad(opposite);
        for( int oppositePosId : positionIdsFromOppositeLanes) {
            // this position is not green in this phase, so we can ignore it
            if ((currentPhase.positionsMask() & (1L << oppositePosId)) == 0L) continue;
            // this position is green, but there is no vehicle, so we can ignore it
            if ((queues.nonEmptyPositionsMask() & (1L << oppositePosId)) == 0L) continue;
            Vehicle oppositeHead = queues.peek(oppositePosId);

            Movement oppositeMovement = Movement.fromRoads(oppositeHead.startRoad(), oppositeHead.endRoad());
            if (oppositeMovement == Movement.STRAIGHT || oppositeMovement == Movement.RIGHT) {
                // there is a conflicting vehicle with green light, so we must wait
                return false;
            }


        }

        return true;
    }
}
