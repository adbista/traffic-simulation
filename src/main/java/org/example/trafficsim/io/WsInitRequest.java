package org.example.trafficsim.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * First message sent over a WebSocket simulation session.
 *
 * Contains only the optional {@link SimConfig} block that determines lane layout and timing.
 * If {@code config} is null (or the field is absent from JSON), the simulation starts with
 * default settings: one GENERAL lane per road, standard NS/EW phases.
 *
 * Example — default config:
 * <pre>{@code {} }</pre>
 *
 * Example — custom lanes:
 * <pre>{@code
 * {
 *   "config": {
 *     "laneDeclarations": [
 *       { "road": "north", "lane": 0, "movements": ["STRAIGHT"] },
 *       { "road": "north", "lane": 1, "movements": ["LEFT"] }
 *     ]
 *   }
 * }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WsInitRequest {
    /** Optional lane / phase configuration. {@code null} → defaults. */
    public SimConfig config;
}
