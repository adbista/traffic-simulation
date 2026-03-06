package org.example.trafficsim.model;

import org.example.trafficsim.model.Movement;
import org.example.trafficsim.model.Road;

// needed for mapping between LineSignal and this VehicleSignal.
// since Vehicle doesn't know the type of the traffic lights, this mapping will be needed
public record VehicleSignal(Road road, int laneIndex, Movement movement) { }
