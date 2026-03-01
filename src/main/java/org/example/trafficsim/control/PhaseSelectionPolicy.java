package org.example.trafficsim.control;

import org.example.trafficsim.core.CrossroadState;
import org.example.trafficsim.signal.Phase;

import java.util.List;

// Interfejs strategii wyboru nastepnej fazy sygnalizatora.
// Mozna podmienic implementacje zeby zmienic algorytm przelaczania.
public interface PhaseSelectionPolicy {
    // Wybiera najlepsza faze na podstawie aktualnego snapshotu kolejek.
    // currentPhaseId - id fazy ktora jest teraz aktywna
    // zwraca Selection z wybranym id fazy i jej wynikiem
    Selection select(CrossroadState snapshot, List<Phase> phases, String currentPhaseId);

    // Wynik selekcji: ktora faza wygrala i z jakim wynikiem
    record Selection(String phaseId, double score) {}
}
