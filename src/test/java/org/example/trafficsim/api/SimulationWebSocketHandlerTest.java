package org.example.trafficsim.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the raw WebSocket endpoint {@code /v1/ws/simulation}.
 *
 * Protocol summary:
 * <ol>
 *   <li>First client message – WsInitRequest JSON (e.g. {@code {}} for default config)</li>
 *   <li>Subsequent messages – CommandDTO JSON ({@code addVehicle}, {@code step}, {@code stop})</li>
 *   <li>Server responds only to {@code step} commands with a StepStatus JSON</li>
 *   <li>Server responds to errors with an error frame and closes the session</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SimulationWebSocketHandlerTest {

    @LocalServerPort
    int port;

    private String wsUrl() {
        return "ws://localhost:" + port + "/v1/ws/simulation";
    }


    /**
     * Connects to the WebSocket endpoint and returns a pre-connected session together with
     * a queue that collects every text message sent by the server.
     */
    private record Connection(WebSocketSession session, BlockingQueue<String> messages) {}

    private Connection connect() throws Exception {
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        CountDownLatch connected = new CountDownLatch(1);

        WebSocketSession session = new StandardWebSocketClient()
                .execute(new TextWebSocketHandler() {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession s) {
                        connected.countDown();
                    }

                    @Override
                    protected void handleTextMessage(WebSocketSession s, TextMessage msg) {
                        messages.add(msg.getPayload());
                    }
                }, wsUrl())
                .get(5, TimeUnit.SECONDS);

        assertTrue(connected.await(5, TimeUnit.SECONDS), "WebSocket connection timed out");
        return new Connection(session, messages);
    }

    /**
     * Sends the init payload and drains the server's phases-info response that is
     * sent immediately after successful initialization.  Returns the phases JSON.
     */
    private String sendInit(Connection conn, String initJson) throws Exception {
        conn.session().sendMessage(new TextMessage(initJson));
        String phases = conn.messages().poll(5, TimeUnit.SECONDS);
        assertNotNull(phases, "Expected phases info frame after init");
        assertThat(phases).contains("\"type\":\"phases\"");
        return phases;
    }

    // happy-path tests

    @Test
    @DisplayName("step with default config returns StepStatus containing the departed vehicle")
    void stepDefaultConfig_returnsLeftVehicles() throws Exception {
        var conn = connect();

        // 1. Init with default config (no laneDeclarations → defaults)
        sendInit(conn, "{}");

        // 2. Add a vehicle going STRAIGHT: south → north
        conn.session().sendMessage(new TextMessage(
                "{\"type\":\"addVehicle\",\"vehicleId\":\"v1\",\"startRoad\":\"south\",\"endRoad\":\"north\"}"
        ));

        // 3. v1 must depart within the phase

        conn.session().sendMessage(new TextMessage("{\"type\":\"step\"}"));
        String response = conn.messages().poll(5, TimeUnit.SECONDS);
        assertNotNull(response, "No step response received within 5 seconds");
        assertThat(response).contains("leftVehicles");
        assertThat(response.contains("\"v1\"")).as("v1 should depart within 4 steps").isTrue();

        conn.session().close();
    }

    @Test
    @DisplayName("two vehicles on different roads - each departs in its own phase cycle")
    void twoVehiclesDifferentRoads_bothDepart() throws Exception {
        var conn = connect();

        sendInit(conn, "{}");

        // v1: south->north
        // v2: west->south
        // This mirrors the scenario-01 timing pattern.
        conn.session().sendMessage(new TextMessage(
                "{\"type\":\"addVehicle\",\"vehicleId\":\"v1\",\"startRoad\":\"south\",\"endRoad\":\"north\"}"
        ));
        conn.session().sendMessage(new TextMessage(
                "{\"type\":\"addVehicle\",\"vehicleId\":\"v2\",\"startRoad\":\"west\",\"endRoad\":\"south\"}"
        ));

        boolean v1Departed = false;
        boolean v2Departed = false;
        for (int i = 0; i < 7; i++) {
            conn.session().sendMessage(new TextMessage("{\"type\":\"step\"}"));
            String msg = conn.messages().poll(5, TimeUnit.SECONDS);
            if (msg == null) break;
            if (msg.contains("\"v1\"")) v1Departed = true;
            if (msg.contains("\"v2\"")) v2Departed = true;
            if (v1Departed && v2Departed) break;
        }

        assertThat(v1Departed).as("v1 (south->north) must depart within 7 steps").isTrue();
        assertThat(v2Departed).as("v2 (west->south) must depart within 7 steps").isTrue();

        conn.session().close();
    }

    @Test
    @DisplayName("custom config with new movements format is accepted")
    void customMovementsConfig_accepted() throws Exception {
        var conn = connect();

        // Init with explicit lanes using the new {movement, type, trafficLightId} format
        sendInit(conn, """
                {
                  "config": {
                    "laneDeclarations": [
                      {"road": "north", "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}]},
                      {"road": "south", "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}]},
                      {"road": "east",  "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "RIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "LEFT", "type": "GENERIC", "trafficLightId": "t1"}]},
                      {"road": "west",  "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "RIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "LEFT", "type": "GENERIC", "trafficLightId": "t1"}]}
                    ]
                  }
                }
                """);

        conn.session().sendMessage(new TextMessage(
                "{\"type\":\"addVehicle\",\"vehicleId\":\"vN\",\"startRoad\":\"north\",\"endRoad\":\"south\"}"
        ));
        conn.session().sendMessage(new TextMessage("{\"type\":\"step\"}"));

        String response = conn.messages().poll(5, TimeUnit.SECONDS);
        assertNotNull(response, "No response to step with custom config");
        assertThat(response).contains("leftVehicles");

        conn.session().close();
    }

    @Test
    @DisplayName("PROTECTED left turn config accepted; vehicle departs in its own phase")
    void protectedLeftConfig_vehicleDeparts() throws Exception {
        var conn = connect();

        sendInit(conn, """
                {
                  "config": {
                    "timing": {"minGreen": 1, "maxGreen": 2, "yellow": 0, "red": 1},
                    "laneDeclarations": [
                      {"road": "north", "lane": 1, "movements": [{"movement": "LEFT", "type": "PROTECTED", "trafficLightId": "t1"}]},
                      {"road": "south", "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "RIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "LEFT", "type": "GENERIC", "trafficLightId": "t1"}]},
                      {"road": "east",  "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "RIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "LEFT", "type": "GENERIC", "trafficLightId": "t1"}]},
                      {"road": "west",  "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "RIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "LEFT", "type": "GENERIC", "trafficLightId": "t1"}]}
                    ]
                  }
                }
                """);

        conn.session().sendMessage(new TextMessage(
                "{\"type\":\"addVehicle\",\"vehicleId\":\"vProt\",\"startRoad\":\"north\",\"endRoad\":\"east\",\"lane\":1}"
        ));

        boolean departed = false;
        for (int i = 0; i < 6; i++) {
            conn.session().sendMessage(new TextMessage("{\"type\":\"step\"}"));
            String msg = conn.messages().poll(5, TimeUnit.SECONDS);
            if (msg != null && msg.contains("vProt")) {
                departed = true;
                break;
            }
        }
        assertThat(departed).as("Protected-left vehicle must depart within 6 steps").isTrue();

        conn.session().close();
    }

    // error handling

    @Test
    @DisplayName("unknown command type after init causes error frame and session close")
    void unknownCommandType_receivesErrorFrameAndSessionCloses() throws Exception {
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        CountDownLatch closed = new CountDownLatch(1);
        AtomicReference<CloseStatus> closeStatusRef = new AtomicReference<>();

        new StandardWebSocketClient()
                .execute(new TextWebSocketHandler() {
                    @Override
                    protected void handleTextMessage(WebSocketSession s, TextMessage msg) {
                        messages.add(msg.getPayload());
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
                        closeStatusRef.set(status);
                        closed.countDown();
                    }
                }, wsUrl())
                .thenAccept(session -> {
                    try {
                        session.sendMessage(new TextMessage("{}"));                          // init (phases frame comes back, but we don't wait for it in this test)
                        Thread.sleep(200);                                                    // let phases frame arrive first
                        session.sendMessage(new TextMessage("{\"type\":\"flyVehicle\"}")); // bad command
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        assertTrue(closed.await(5, TimeUnit.SECONDS), "Session should close after error");

        // Drain any phases-info frame that arrived after init, then find the error frame
        String errorMsg = null;
        for (int i = 0; i < 3; i++) {
            String msg = messages.poll(1, TimeUnit.SECONDS);
            if (msg != null && msg.contains("\"error\"")) { errorMsg = msg; break; }
        }
        assertNotNull(errorMsg, "Expected an error frame before the session closed");
        assertThat(errorMsg).contains("error");
    }

    @Test
    @DisplayName("stop command closes the session gracefully")
    void stopCommand_closesSessionNormally() throws Exception {
        CountDownLatch closed = new CountDownLatch(1);
        AtomicReference<CloseStatus> statusRef = new AtomicReference<>();

        new StandardWebSocketClient()
                .execute(new TextWebSocketHandler() {
                    @Override
                    public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
                        statusRef.set(status);
                        closed.countDown();
                    }
                }, wsUrl())
                .thenAccept(session -> {
                    try {
                        session.sendMessage(new TextMessage("{}")); // init
                        session.sendMessage(new TextMessage("{\"type\":\"stop\"}")); // stop
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        assertTrue(closed.await(5, TimeUnit.SECONDS), "Session should close after stop");
        CloseStatus status = statusRef.get();
        assertThat(status).isNotNull();
        assertThat(status.getCode())
                .as("stop should use NORMAL close (1000)")
                .isEqualTo(CloseStatus.NORMAL.getCode());
    }
}
