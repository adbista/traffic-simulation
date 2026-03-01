package org.example.trafficsim.signal;

import org.example.trafficsim.model.LaneDeclaration;
import org.example.trafficsim.model.Movement;
import org.example.trafficsim.model.Road;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// Testy jednostkowe generatora faz na podstawie deklaracji pasow.
class PhaseSetGeneratorTest {

    private static final PhaseTiming TIMING = new PhaseTiming(1, 5, 1);
    private static final PhaseTiming TURN_T = new PhaseTiming(1, 2, 1);

    // Pomocnik: zbiera greenRoads wszystkich faz jako zbior stringow
    private static Set<Set<Road>> phaseRoadSets(List<Phase> phases) {
        return phases.stream()
                .map(Phase::greenRoads)
                .collect(Collectors.toSet());
    }

    // --- Domyslny generator (brak deklaracji) ---

    @Test
    void defaultsProducesNsAndEwPhases() {
        // Brak deklaracji — 1 pas GENERAL (STRAIGHT+PERMISSIVE_LEFT+RIGHT) per droga — fazy {N,S} i {E,W}
        List<Phase> phases = PhaseSetGenerator.defaults().create();
        assertEquals(2, phases.size());

        Set<Set<Road>> roadSets = phaseRoadSets(phases);
        assertTrue(roadSets.contains(Set.of(Road.NORTH, Road.SOUTH)));
        assertTrue(roadSets.contains(Set.of(Road.EAST,  Road.WEST)));
    }

    @Test
    void defaultsPhaseHasExplicitGreenLaneIndices() {
        List<Phase> phases = PhaseSetGenerator.defaults().create();
        // Każda faza powinna mieć greenLaneIndices z lane 0 per road
        Phase ns = phases.stream()
                .filter(p -> p.greenRoads().contains(Road.NORTH))
                .findFirst().orElseThrow();
        assertEquals(Set.of(0), ns.greenLanesFor(Road.NORTH));
        assertEquals(Set.of(0), ns.greenLanesFor(Road.SOUTH));
    }

    // --- NS z 2 pasami (STRAIGHT+RIGHT / LEFT) ---

    @Test
    void nsTurnDeclarationsProduceThreePhases() {
        // Typowy 2-pasowy uklad NS z lewoskretami chronionymi:
        //   N0=[S,R], N1=[L], S0=[S,R], S1=[L], E0=[S], W0=[S] (domyslne)
        List<LaneDeclaration> decls = List.of(
                new LaneDeclaration(Road.NORTH, 0, Set.of(Movement.STRAIGHT, Movement.RIGHT)),
                new LaneDeclaration(Road.NORTH, 1, Set.of(Movement.LEFT)),
                new LaneDeclaration(Road.SOUTH, 0, Set.of(Movement.STRAIGHT, Movement.RIGHT)),
                new LaneDeclaration(Road.SOUTH, 1, Set.of(Movement.LEFT)),
                LaneDeclaration.defaultLane(Road.EAST,  0),
                LaneDeclaration.defaultLane(Road.WEST,  0)
        );

        List<Phase> phases = new PhaseSetGenerator(decls, TURN_T).create();

        // Oczekiwane fazy: {N0,S0}, {N1,S1}, {E0,W0}
        assertEquals(3, phases.size(), "powinny byc 3 fazy: NS-straight, NS-left, EW");

        Set<Set<Road>> roadSets = phaseRoadSets(phases);
        assertTrue(roadSets.contains(Set.of(Road.NORTH, Road.SOUTH)),
                "brakuje fazy NS (straight)");
        assertTrue(roadSets.contains(Set.of(Road.EAST, Road.WEST)),
                "brakuje fazy EW");
        // Faza NS-left rowniez ma NORTH i SOUTH w greenRoads
        long nsCount = phases.stream()
                .filter(p -> p.greenRoads().equals(Set.of(Road.NORTH, Road.SOUTH)))
                .count();
        assertEquals(2, nsCount, "powinny byc 2 fazy NS: prosto i w lewo");
    }

    @Test
    void nsTurnPhaseLanesAreCorrect() {
        List<LaneDeclaration> decls = List.of(
                new LaneDeclaration(Road.NORTH, 0, Set.of(Movement.STRAIGHT, Movement.RIGHT)),
                new LaneDeclaration(Road.NORTH, 1, Set.of(Movement.LEFT)),
                new LaneDeclaration(Road.SOUTH, 0, Set.of(Movement.STRAIGHT, Movement.RIGHT)),
                new LaneDeclaration(Road.SOUTH, 1, Set.of(Movement.LEFT)),
                LaneDeclaration.defaultLane(Road.EAST, 0),
                LaneDeclaration.defaultLane(Road.WEST, 0)
        );

        List<Phase> phases = new PhaseSetGenerator(decls, TURN_T).create();

        // Faza z lane 0 na N i S: greenLanesFor(N)={0}, greenLanesFor(S)={0}
        Phase nsStraight = phases.stream()
                .filter(p -> p.greenRoads().contains(Road.NORTH) && p.greenLanesFor(Road.NORTH).contains(0))
                .findFirst().orElseThrow();
        assertEquals(Set.of(0), nsStraight.greenLanesFor(Road.NORTH));
        assertEquals(Set.of(0), nsStraight.greenLanesFor(Road.SOUTH));

        // Faza z lane 1 na N i S
        Phase nsLeft = phases.stream()
                .filter(p -> p.greenRoads().contains(Road.NORTH) && p.greenLanesFor(Road.NORTH).contains(1))
                .findFirst().orElseThrow();
        assertEquals(Set.of(1), nsLeft.greenLanesFor(Road.NORTH));
        assertEquals(Set.of(1), nsLeft.greenLanesFor(Road.SOUTH));
    }

    // --- EW z 2 pasami (STRAIGHT+RIGHT / LEFT) ---

    @Test
    void ewTurnDeclarationsProduceThreePhases() {
        List<LaneDeclaration> decls = List.of(
                LaneDeclaration.defaultLane(Road.NORTH, 0),
                LaneDeclaration.defaultLane(Road.SOUTH, 0),
                new LaneDeclaration(Road.EAST,  0, Set.of(Movement.STRAIGHT, Movement.RIGHT)),
                new LaneDeclaration(Road.EAST,  1, Set.of(Movement.LEFT)),
                new LaneDeclaration(Road.WEST,  0, Set.of(Movement.STRAIGHT, Movement.RIGHT)),
                new LaneDeclaration(Road.WEST,  1, Set.of(Movement.LEFT))
        );

        List<Phase> phases = new PhaseSetGenerator(decls, TURN_T).create();

        assertEquals(3, phases.size(), "powinny byc 3 fazy: NS, EW-straight, EW-left");

        Set<Set<Road>> roadSets = phaseRoadSets(phases);
        assertTrue(roadSets.contains(Set.of(Road.NORTH, Road.SOUTH)));
        assertTrue(roadSets.contains(Set.of(Road.EAST, Road.WEST)));
        long ewCount = phases.stream()
                .filter(p -> p.greenRoads().equals(Set.of(Road.EAST, Road.WEST)))
                .count();
        assertEquals(2, ewCount, "powinny byc 2 fazy EW: prosto i lewo");
    }

    // --- Jeden pas wieloruchowy ---

    @Test
    void northTwoStraightLanesProducesCorrectNsPhase() {
        // NORTH ma 2 pasy GENERAL, SOUTH ma 1 — oba pasy NORTH powinny trafic do fazy NS
        List<LaneDeclaration> decls = List.of(
                LaneDeclaration.defaultLane(Road.NORTH, 0),
                LaneDeclaration.defaultLane(Road.NORTH, 1),
                LaneDeclaration.defaultLane(Road.SOUTH, 0),
                LaneDeclaration.defaultLane(Road.EAST,  0),
                LaneDeclaration.defaultLane(Road.WEST,  0)
        );

        List<Phase> phases = new PhaseSetGenerator(decls, TIMING).create();

        // NS faza powinna zawierac lane 0 i 1 dla NORTH
        Phase ns = phases.stream()
                .filter(p -> p.greenRoads().contains(Road.NORTH))
                .findFirst().orElseThrow();
        assertEquals(Set.of(0, 1), ns.greenLanesFor(Road.NORTH), "oba pasy NORTH powinny byc w NS");
        assertEquals(Set.of(0),    ns.greenLanesFor(Road.SOUTH),  "SOUTH ma 1 pas");
    }
}
