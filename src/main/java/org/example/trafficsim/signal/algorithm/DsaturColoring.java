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
 * uncolored-neighbor degree.  It then assigns the smallest color not used by
 * any neighbor.
 *
 * complexity: O(V^3) in this straightforward implementation, 
 *
 * @return int array of length {@code signals.size()}: {@code colors[i]} is
 *         the 0-based phase index assigned to {@code signals.get(i)}.
 */
public class DsaturColoring {

    public int[] color(List<LaneSignal> signals, BiPredicate<LaneSignal, LaneSignal> conflicts) {
        int n = signals.size();
        int[] colors = new int[n];
        Arrays.fill(colors, -1);

        // saturation[i] = number of distinct neighbor colors already assigned
        int[] saturation = new int[n];
        // degree[i]     = number of still-uncolored conflicting neighbors
        int[] degree = new int[n];

        // Pre-compute adjacency matrix
        boolean[][] adj = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (conflicts.test(signals.get(i), signals.get(j))) {
                    adj[i][j] = adj[j][i] = true;
                    degree[i]++;
                    degree[j]++;
                }
            }
        }

        for (int iter = 0; iter < n; iter++) {
            // Pick the uncolored node with highest saturation (degree as tiebreak)
            int chosen = -1;
            for (int i = 0; i < n; i++) {
                if (colors[i] != -1) continue;
                if (chosen == -1
                        || saturation[i] > saturation[chosen]
                        || (saturation[i] == saturation[chosen] && degree[i] > degree[chosen])) {
                    chosen = i;
                }
            }

            // Assign the smallest color not used by any neighbor
            Set<Integer> usedByNeighbors = new HashSet<>();
            for (int j = 0; j < n; j++) {
                if (adj[chosen][j] && colors[j] != -1) {
                    usedByNeighbors.add(colors[j]);
                }
            }
            int c = 0;
            while (usedByNeighbors.contains(c)) c++;
            colors[chosen] = c;

            // Update saturation of uncolored neighbors:
            // if color c is new in their neighborhood, increment their saturation
            for (int j = 0; j < n; j++) {
                if (!adj[chosen][j] || colors[j] != -1) continue;

                // Check whether color c was already seen by j from another neighbor
                boolean alreadySeen = false;
                for (int k = 0; k < n; k++) {
                    if (k != chosen && adj[j][k] && colors[k] == c) {
                        alreadySeen = true;
                        break;
                    }
                }
                if (!alreadySeen) {
                    saturation[j]++;
                }
                degree[j]--;
            }
        }

        return colors;
    }
}
