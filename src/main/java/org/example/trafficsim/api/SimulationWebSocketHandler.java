package org.example.trafficsim.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trafficsim.cli.InputReader;
import org.example.trafficsim.cli.SimulationEngineBuilder;
import org.example.trafficsim.cli.StreamingCommandDispatcher;
import org.example.trafficsim.core.SimulationEngine;
import org.example.trafficsim.io.CommandDTO;
import org.example.trafficsim.io.OutputFile;
import org.example.trafficsim.io.WsInitRequest;
import org.example.trafficsim.signal.PhaseSetGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for the simulation streaming API.
 *
 * <h3>Session lifecycle</h3>
 * <ol>
 *   <li><b>Connect</b> — session state is allocated (uninitialized).</li>
 *   <li><b>First text frame</b> — parsed as {@link WsInitRequest}; the engine and
 *       {@link StreamingCommandDispatcher} are built. Any JSON/config error closes the
 *       session with an error frame.</li>
 *   <li><b>Subsequent frames</b> — parsed as {@link CommandDTO} and dispatched:
 *       <ul>
 *         <li>{@code addVehicle} — adds a vehicle to the queue.</li>
 *         <li>{@code step} — advances the simulation; server emits a {@code StepStatus} frame.</li>
 *         <li>{@code stop} — server closes the session gracefully.</li>
 *       </ul>
 *   </li>
 *   <li><b>Disconnect / error</b> — session state is cleaned up.</li>
 * </ol>
 *
 * <h3>Error frames</h3>
 * On any error the server sends {@code {"error":"<message>"}} and closes the session.
 */
@Component
public class SimulationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SimulationWebSocketHandler.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Per-session mutable state; keyed by WebSocketSession id. */
    private final ConcurrentHashMap<String, SessionState> sessions = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Spring WebSocket lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), new SessionState());
        log.debug("WS session opened: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        SessionState state = sessions.get(session.getId());
        if (state == null) return;

        try {
            if (!state.initialized) {
                initializeSession(session, state, message.getPayload());
            } else {
                dispatchCommand(session, state, message.getPayload());
            }
        } catch (Exception e) {
            sendErrorAndClose(session, e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WS transport error on session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.debug("WS session closed: {} status={}", session.getId(), status);
    }

    // -------------------------------------------------------------------------
    // Initialization (first frame)
    // -------------------------------------------------------------------------

    private void initializeSession(WebSocketSession session,
                                   SessionState state,
                                   String payload) throws IOException {
        WsInitRequest initRequest = MAPPER.readValue(payload, WsInitRequest.class);

        InputReader.ParsedConfig cfg = InputReader.parseConfig(initRequest.config);
        PhaseSetGenerator generator  = new PhaseSetGenerator(cfg.laneDeclarations(), cfg.phaseTiming());
        SimulationEngine engine      = new SimulationEngineBuilder().phaseFactory(generator).build();

        state.dispatcher = new StreamingCommandDispatcher(
                engine,
                status -> sendStatus(session, status),
                ()     -> closeGracefully(session)
        );
        state.initialized = true;
        log.debug("WS session {} initialized", session.getId());
    }

    // -------------------------------------------------------------------------
    // Command dispatching (subsequent frames)
    // -------------------------------------------------------------------------

    private void dispatchCommand(WebSocketSession session,
                                 SessionState state,
                                 String payload) throws IOException {
        CommandDTO cmd = MAPPER.readValue(payload, CommandDTO.class);
        state.dispatcher.dispatch(cmd);
    }

    // -------------------------------------------------------------------------
    // Sending helpers
    // -------------------------------------------------------------------------

    private void sendStatus(WebSocketSession session, OutputFile.StepStatus status) {
        try {
            String json = MAPPER.writeValueAsString(status);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.warn("Failed to send step status on session {}: {}", session.getId(), e.getMessage());
        }
    }

    private void sendErrorAndClose(WebSocketSession session, String message) {
        try {
            String json = MAPPER.writeValueAsString(new ErrorFrame(message));
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                    session.close(CloseStatus.BAD_DATA);
                }
            }
        } catch (IOException ex) {
            log.warn("Failed to send error frame on session {}: {}", session.getId(), ex.getMessage());
        } finally {
            sessions.remove(session.getId());
        }
    }

    private void closeGracefully(WebSocketSession session) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.close(CloseStatus.NORMAL);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to close session {} gracefully: {}", session.getId(), e.getMessage());
        } finally {
            sessions.remove(session.getId());
        }
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    private static class SessionState {
        volatile boolean initialized = false;
        StreamingCommandDispatcher dispatcher;
    }

    /** JSON shape of error frames sent to the client: {@code {"error":"..."}}. */
    record ErrorFrame(String error) {}
}
