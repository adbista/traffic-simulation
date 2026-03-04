package org.example.trafficsim.control;

public record PolicySelection(
        ScoreByPhase activePhaseScore,
        ScoreByPhase bestPhaseScore
) {}