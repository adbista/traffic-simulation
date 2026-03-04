package org.example.trafficsim.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trafficsim.io.InputReader;
import org.example.trafficsim.core.SimulationEngineBuilder;
import org.example.trafficsim.command.StreamingCommandDispatcher;
import org.example.trafficsim.config.IntersectionConfig;
import org.example.trafficsim.core.SimulationEngine;
import org.example.trafficsim.io.CommandDTO;
import org.example.trafficsim.io.OutputFile;
import org.example.trafficsim.io.WsInitRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class SimulationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SimulationWebSocketHandler.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ConcurrentHashMap<String, SessionState> sessions = new ConcurrentHashMap<>();


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

    private void initializeSession(WebSocketSession session,
                                   SessionState state,
                                   String payload) throws IOException {
        WsInitRequest initRequest = MAPPER.readValue(payload, WsInitRequest.class);

       IntersectionConfig cfg = InputReader.parseConfig(initRequest.config);
        SimulationEngine engine = new SimulationEngineBuilder(cfg).build();

       state.dispatcher = new StreamingCommandDispatcher(
               engine,
               status -> sendStatus(session, status),
               () -> closeGracefully(session)
       );
       state.initialized = true;
       log.debug("WS session {} initialized", session.getId());
    }

    private void dispatchCommand(WebSocketSession session,
                                 SessionState state,
                                 String payload) throws IOException {
        CommandDTO cmd = MAPPER.readValue(payload, CommandDTO.class);
        state.dispatcher.dispatch(cmd);
    }

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

    private static class SessionState {
        volatile boolean initialized = false;
        StreamingCommandDispatcher dispatcher;
    }

    record ErrorFrame(String error) {}
}
