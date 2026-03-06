package org.example.trafficsim.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.example.trafficsim.io.LaneConfigDTO;
import org.example.trafficsim.io.TimingConfigDTO;

import java.util.List;

/**
 * Optional configuration block at the start of an input file.
 *
 * Example:
 *
 * {
 *   "config": {
 *     "timing": { "minGreen": 1, "maxGreen": 5, "yellow": 0, "red": 1 },
 *     "laneDeclarations": [
 *       { "road": "north", "lane": 0, "movements": [{"STRAIGHT": "GENERIC"}, {"RIGHT": "GENERIC"}] }
 *     ]
 *   }
 * }
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimConfig {

    /** Optional timing for all phases. If absent, defaults are used. */
    public TimingConfigDTO timing;

    /** Explicit lane declarations. If absent, one default lane per road is used
     * with GENERIC traffic light type with LEFT,RIGHT,STRAIGHT movements*/
    public List<LaneConfigDTO> laneDeclarations;
}
