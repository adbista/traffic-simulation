package org.example.trafficsim.signal.algorithm;

import org.example.trafficsim.model.LaneSignal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * DSATUR heuristic graph coloring for a conflict graph of {@link LaneSignal}s.
 *
 * Each color corresponds to a traffic phase; signals that share a color can
 * be active simultaneously (no conflicts between them).
 *
 * At every step DSATUR picks the uncolored node with the highest saturation
 * (number of distinct colors already used by its neighbors), breaking ties by
 * the number of uncolored conflicting neighbors (degree). It then assigns the
 * smallest color not used by any neighbor.
 *
 * Complexity: O(V^2)
 *
 */
public class DsaturColoring {

    public int[] color(List<LaneSignal> signals, BiPredicate<LaneSignal, LaneSignal> conflicts) {
        int n = signals.size();

        // Result array -1 means not yet colored
        int[] colors = new int[n];
        Arrays.fill(colors, -1);

        // saturation[i] = number of distinct colors already assigned to neighbors of i
        int[] saturation = new int[n];

        // degree[i] = number of still-uncolored conflicting neighbors of i
        int[] degree = new int[n];


        Set<Integer>[] neighbors = new HashSet[n];
        for (int i = 0; i < n; i++) {
            neighbors[i] = new HashSet<>();
        }

        // neighborColors[i] = set of colors already present among the colored neighbors of i
        Set<Integer>[] neighborColors = new HashSet[n];
        for (int i = 0; i < n; i++) {
            neighborColors[i] = new HashSet<>();
        }

        // Build the conflict graph Check every pair (i, j) exactly once
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (conflicts.test(signals.get(i), signals.get(j))) {
                    neighbors[i].add(j);
                    neighbors[j].add(i);
                    degree[i]++;
                    degree[j]++;
                }
            }
        }

        //  Main coloring loop, one node colored per iteration
        for (int iter = 0; iter < n; iter++) {

            // Step 1: pick the uncolored node with the highest saturation
            // Ties are broken by degree
            int chosen = -1;
            for (int i = 0; i < n; i++) {
                if (colors[i] != -1) continue;
                if (chosen == -1
                        || saturation[i] > saturation[chosen]
                        || (saturation[i] == saturation[chosen]
                        && degree[i] > degree[chosen])) {
                    chosen = i;
                }
            }

            // Step 2: assign the smallest color not used by any neighbor
            int c = 0;
            while (neighborColors[chosen].contains(c)) c++;
            colors[chosen] = c;

            // Step 3: propagate the new color to uncolored neighbors
            for (int j : neighbors[chosen]) {
                if (colors[j] != -1) continue; // skip already-colored neighbors
                // saturation increases.
                if (neighborColors[j].add(c)) {
                    saturation[j]++;
                }

                // chosen is now colored, so j has one fewer uncolored neighbor
                degree[j]--;
            }
        }

        return colors;
    }
}