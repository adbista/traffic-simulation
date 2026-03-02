package org.example.trafficsim.control;

import org.example.trafficsim.core.TrafficQueues;
import org.example.trafficsim.signal.Phase;

import java.util.List;

// Interfejs strategii wyboru nastepnej fazy sygnalizatora.
// Mozna podmienic implementacje zeby zmienic algorytm przelaczania.
public interface PhaseSelectionPolicy {
    // Wybiera najlepsza faze na podstawie aktualnego stanu kolejek.
    // currentPhaseId - id fazy ktora jest teraz aktywna
    // zwraca Selection z wybranym id fazy i jej wynikiem
    Selection select(TrafficQueues queues, long stepNo, List<Phase> phases, String currentPhaseId);

    // Wynik selekcji: ktora faza wygrala i z jakim wynikiem
    record Selection(String phaseId, double score) {}
}
