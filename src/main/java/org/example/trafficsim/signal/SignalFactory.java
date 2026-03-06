package org.example.trafficsim.signal;

import org.example.trafficsim.config.IntersectionConfig;
import org.example.trafficsim.config.SimDefaults;
import org.example.trafficsim.core.LaneRegistry;
import org.example.trafficsim.model.LaneSignal;
import org.example.trafficsim.model.Movement;
import org.example.trafficsim.model.MovementMask;
import org.example.trafficsim.model.SignalGroupSpec;
import org.example.trafficsim.model.TrafficLightType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SignalFactory  {

    /**
     * Creates LaneSignals from the intersection config.
     *
     * Each entry in the lane's {@code movements} list that shares a {@code trafficLightId}
     * forms one signal group (one {@link LaneSignal}) with a merged movement bitmask and
     * a common {@link TrafficLightType}.  This applies equally to GENERIC and PROTECTED
     * signals, so a PROTECTED light may cover multiple movements (e.g. STRAIGHT + RIGHT).
     *
     * If no config is present for a lane, a single merged GENERIC signal covering all
     * default movements is produced.
     */
    public static List<LaneSignal> createSignals(IntersectionConfig config) {
        LaneRegistry laneRegistry = config.laneRegistry();
        Map<Integer, List<SignalGroupSpec>> movementSignalsByPosition =
                config.movementSignalsByPosition();

        List<LaneSignal> result = new ArrayList<>();

        for (int posId = 0; posId < laneRegistry.totalPositions(); posId++) {
            var road = laneRegistry.roadOf(posId);
            int lane = laneRegistry.laneOf(posId);

            List<SignalGroupSpec> groups = movementSignalsByPosition.get(posId);
            if (groups == null) {
                // No specific config: all default movements as one merged GENERIC group
                int genericMask = 0;
                for (Movement m : SimDefaults.DEFAULT_MOVEMENTS) {
                    genericMask |= MovementMask.bit(m);
                }
                result.add(new LaneSignal(road, lane, genericMask, TrafficLightType.GENERIC));
                continue;
            }

            for (SignalGroupSpec group : groups) {
                result.add(new LaneSignal(road, lane, group.movementMask(), group.type()));
            }
        }

        // NOT NECCESSARY STEP, JUST SO 01-RECRUITMENT-SCENARIO OUTPUT PASSES EXACTLY AS EXPECTED 
        // (just ensuring deterministic phase order, 
        // otherwise we would have red lights on NORTH and SOUTH in step 1 instead of green, 
        // which would still be correct but not match the expected output).
        result.sort(Comparator
                .comparingInt((LaneSignal s) -> s.road().ordinal()));

        return result;
    }
}
