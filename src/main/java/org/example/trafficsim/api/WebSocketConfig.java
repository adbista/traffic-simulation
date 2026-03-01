package org.example.trafficsim.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers WebSocket endpoints.
 *
 * Endpoint: {@code /v1/ws/simulation}
 *
 * Protocol (no STOMP, plain text frames):
 * <ol>
 *   <li>Client connects.</li>
 *   <li>Client sends one {@code WsInitRequest} JSON frame (may be {@code {}}) to configure
 *       the simulation (lane layout, phase timing). The engine is built from this message.</li>
 *   <li>Client sends {@code CommandDTO} JSON frames ({@code addVehicle}, {@code step}, {@code stop}).</li>
 *   <li>Server replies with a {@code StepStatus} JSON frame for every {@code step} command.</li>
 *   <li>A {@code stop} command (or transport close) terminates the session.</li>
 * </ol>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SimulationWebSocketHandler handler;

    public WebSocketConfig(SimulationWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(handler, "/v1/ws/simulation")
                .setAllowedOrigins("*");
    }
}
