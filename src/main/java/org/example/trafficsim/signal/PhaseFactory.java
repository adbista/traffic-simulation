package org.example.trafficsim.signal;

import java.util.List;

// Interfejs fabryk tworzacych liste faz sygnalizatora.
// Implementacje: StandardPhaseFactory (2-fazowy), TurnPhaseFactory (4-fazowy ze skretami).
public interface PhaseFactory {
    List<Phase> create();
}
