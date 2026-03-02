package org.example.trafficsim.signal;

import org.example.trafficsim.model.LaneDeclaration;
import org.example.trafficsim.model.LaneSignal;
import org.example.trafficsim.model.Movement;
import org.example.trafficsim.model.Road;

import java.util.*;

public class PhaseSetGenerator implements PhaseFactory {

    private final List<LaneDeclaration> declarations;
    private final PhaseTiming timing;

    public PhaseSetGenerator(List<LaneDeclaration> declarations, PhaseTiming timing) {
        this.declarations = List.copyOf(declarations);
        this.timing = timing;
    }

    public static PhaseSetGenerator defaults() {
        List<LaneDeclaration> decls = new ArrayList<>();
        for (Road r : Road.values()) {
            decls.add(LaneDeclaration.defaultLane(r, 0));
        }
        return new PhaseSetGenerator(decls, new PhaseTiming(1, 5, 1));
    }

    @Override
    public List<Phase> create() {
        List<LaneSignal> signals = expand(declarations);

        List<List<LaneSignal>> maximalSets = new ArrayList<>();
        bronKerbosch(new ArrayList<>(), new ArrayList<>(signals), new ArrayList<>(), maximalSets);

        List<Phase> phases = new ArrayList<>();
        int idx = 0;

        for (List<LaneSignal> set : maximalSets) {
            Set<Road> roads = EnumSet.noneOf(Road.class);
            for (LaneSignal s : set) {
                roads.add(s.road());
            }

            // Intentionally ignore single-road phases in this simulator.
            if (roads.size() < 2) {
                continue;
            }

            phases.add(toPhase("p" + idx, set));
            idx++;
        }

        if (phases.isEmpty()) {
            throw new IllegalStateException("PhaseSetGenerator did not produce any phases — check laneDeclarations");
        }

        return List.copyOf(phases);
    }

    private static List<LaneSignal> expand(List<LaneDeclaration> decls) {
        List<LaneSignal> result = new ArrayList<>();

        for (LaneDeclaration d : decls) {
            List<Movement> sorted = d.movements().stream()
                    .filter(m -> m != Movement.RIGHT)
                    .sorted(Comparator.comparingInt(Enum::ordinal))
                    .toList();

            for (Movement m : sorted) {
                result.add(new LaneSignal(d.road(), d.index(), m));
            }
        }

        result.sort(Comparator
                .comparingInt((LaneSignal s) -> s.road().ordinal())
                .thenComparingInt(LaneSignal::laneIndex)
                .thenComparingInt(s -> s.movement().ordinal()));

        return result;
    }

    private void bronKerbosch(
            List<LaneSignal> r,
            List<LaneSignal> p,
            List<LaneSignal> x,
            List<List<LaneSignal>> results
    ) {
        if (p.isEmpty() && x.isEmpty()) {
            results.add(new ArrayList<>(r));
            return;
        }

        for (int i = 0; i < p.size(); i++) {
            LaneSignal v = p.get(i);
            r.add(v);

            List<LaneSignal> newP = new ArrayList<>();
            for (LaneSignal u : p) {
                if (u != v && !ConflictMatrix.signalsConflict(v, u)) {
                    newP.add(u);
                }
            }

            List<LaneSignal> newX = new ArrayList<>();
            for (LaneSignal u : x) {
                if (!ConflictMatrix.signalsConflict(v, u)) {
                    newX.add(u);
                }
            }

            bronKerbosch(r, newP, newX, results);

            r.remove(r.size() - 1);
            p.remove(i);
            i--;
            x.add(v);
        }
    }

    private Phase toPhase(String id, List<LaneSignal> set) {
        Set<LaneSignal> immutableSignals = Set.copyOf(set);

        Map<Road, Set<Integer>> laneIndices = new EnumMap<>(Road.class);
        Set<Road> greenRoads = EnumSet.noneOf(Road.class);

        for (LaneSignal s : set) {
            greenRoads.add(s.road());
            laneIndices.computeIfAbsent(s.road(), r -> new LinkedHashSet<>()).add(s.laneIndex());
        }

        Map<Road, Set<Integer>> immutableLaneIndices = new EnumMap<>(Road.class);
        for (Map.Entry<Road, Set<Integer>> e : laneIndices.entrySet()) {
            immutableLaneIndices.put(e.getKey(), Set.copyOf(e.getValue()));
        }

        return new Phase(
                id,
                Set.copyOf(greenRoads),
                timing,
                Map.copyOf(immutableLaneIndices),
                immutableSignals
        );
    }
}