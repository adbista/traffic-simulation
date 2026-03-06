package org.example.trafficsim.model;

//Signal light cycle: GREEN → YELLOW → RED → GREEN (new phase).
// at one time, every light will be RED. This is needed for synchronisation.
public enum PhaseState {
    GREEN,
    YELLOW,
    RED
}