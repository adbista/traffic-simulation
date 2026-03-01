package org.example.trafficsim.model;


// Uzywany przez PhaseSetGenerator do budowania faz na poziomie ruchow
// Dzieki temu mozna w jednej fazie mieć razem np.:
//  { LaneSignal(NORTH, 0, STRAIGHT), LaneSignal(SOUTH, 0, PERMISSIVE_LEFT) }
public record LaneSignal(Road road, int laneIndex, Movement movement) {

    public Road destinationRoad() {
        return movement.toRoad(road);
    }
}
