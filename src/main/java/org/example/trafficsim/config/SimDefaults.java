package org.example.trafficsim.config;

import org.example.trafficsim.model.Movement;
import org.example.trafficsim.model.PhaseTiming;

import java.util.Set;


public final class SimDefaults {

    private SimDefaults() {}

    public static final PhaseTiming DEFAULT_TIMING =
            new PhaseTiming(1, 5, 0, 1);


    public static final Set<Movement> DEFAULT_MOVEMENTS =
            Set.of(
                    Movement.STRAIGHT,
                    Movement.LEFT,
                    Movement.RIGHT
            );
}