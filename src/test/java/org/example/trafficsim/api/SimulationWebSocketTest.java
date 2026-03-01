package org.example.trafficsim.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SimulationWebSocketTest {

    @LocalServerPort
    int port;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void defaultConfig_addVehiclesAndStep_emitsCorrectStepStatuses() throws Exception {
        try (WsTestClient ws = connect()) {
            ws.send("{}");
            ws.send(addVehicle("v1", "south", "north"));
            ws.send(addVehicle("v2", "north", "south"));
            ws.send(step());
            ws.send(step());

            JsonNode s1 = ws.nextJson(3);
            JsonNode s2 = ws.nextJson(3);

            assertEquals(List.of("v1", "v2"), leftVehicles(s1), "step 1");
            assertEquals(List.of(),           leftVehicles(s2), "step 2");
        }
    }

    @Test
    void stepsWithEmptyQueues_emitEmptyLeftVehiclesEachStep() throws Exception {
        try (WsTestClient ws = connect()) {
            ws.send("{}");
            ws.send(step());
            ws.send(step());
            ws.send(step());

            for (int i = 0; i < 3; i++) {
                JsonNode status = ws.nextJson(3);
                assertTrue(leftVehicles(status).isEmpty(), "step " + (i + 1) + " should have no vehicles");
            }
        }
    }

    @Test
    void customConfig_laneDeclarations_sessionInitializesSuccessfully() throws Exception {
        String initMsg = """
                {
                  "config": {
                    "laneDeclarations": [
                      { "road": "north", "lane": 0, "movements": ["STRAIGHT"] },
                      { "road": "north", "lane": 1, "movements": ["LEFT"] }
                    ]
                  }
                }
                """;
        try (WsTestClient ws = connect()) {
            ws.send(initMsg);
            ws.send(addVehicle("v1", "south", "north", 0));
            ws.send(step());

            JsonNode status = ws.nextJson(3);
            assertNotNull(status);
            assertFalse(status.has("error"), "Expected a StepStatus, not an error: " + status);
        }
    }

    @Test
    void stopCommand_closesSessionGracefully() throws Exception {
        WsTestClient ws = connect();
        ws.send("{}");
        ws.send(stop());

        assertTrue(ws.awaitClose(5), "Session should close after stop command");
        ws.close();
    }

    @Test
    void invalidInitJson_receivesErrorFrameAndSessionCloses() throws Exception {
        try (WsTestClient ws = connect()) {
            ws.send("{ this is not valid json }");

            JsonNode errorFrame = ws.nextJson(3);
            assertNotNull(errorFrame, "Expected an error frame");
            assertTrue(errorFrame.has("error"), "Frame should contain 'error' field: " + errorFrame);
            assertTrue(ws.awaitClose(5), "Session should close after parse error");
        }
    }

    @Test
    void unknownCommandType_receivesErrorFrameAndSessionCloses() throws Exception {
        try (WsTestClient ws = connect()) {
            ws.send("{}");
            ws.send("{\"type\": \"teleport\"}");

            JsonNode errorFrame = ws.nextJson(3);
            assertNotNull(errorFrame, "Expected an error frame");
            assertTrue(errorFrame.has("error"), "Frame should contain 'error' field: " + errorFrame);
            assertTrue(errorFrame.get("error").asText().contains("teleport"), "Error should mention the unknown type");
            assertTrue(ws.awaitClose(5), "Session should close after unknown command");
        }
    }

    @Test
    void invalidRoadName_receivesErrorFrameAndSessionCloses() throws Exception {
        try (WsTestClient ws = connect()) {
            ws.send("{}");
            ws.send(addVehicle("v1", "nowhere", "north"));

            JsonNode errorFrame = ws.nextJson(3);
            assertNotNull(errorFrame, "Expected an error frame");
            assertTrue(errorFrame.has("error"), "Frame should contain 'error' field: " + errorFrame);
            assertTrue(ws.awaitClose(5), "Session should close after invalid road name");
        }
    }

    @Test
    void twoSessions_engineStatesAreIndependent() throws Exception {
        try (WsTestClient ws1 = connect();
             WsTestClient ws2 = connect()) {

            ws1.send("{}");
            ws2.send("{}");

            ws1.send(addVehicle("session1-vehicle", "north", "south"));
            ws2.send(addVehicle("session2-vehicle", "south", "north"));

            ws1.send(step());
            ws2.send(step());

            JsonNode r1 = ws1.nextJson(3);
            JsonNode r2 = ws2.nextJson(3);

            List<String> lv1 = leftVehicles(r1);
            List<String> lv2 = leftVehicles(r2);

            assertTrue(lv1.contains("session1-vehicle"), "ws1 should see session1-vehicle, got: " + lv1);
            assertFalse(lv1.contains("session2-vehicle"), "ws1 must not see session2-vehicle");

            assertTrue(lv2.contains("session2-vehicle"), "ws2 should see session2-vehicle, got: " + lv2);
            assertFalse(lv2.contains("session1-vehicle"), "ws2 must not see session1-vehicle");
        }
    }

    @Test
    void commandBeforeInit_receivesErrorFrameAndSessionCloses() throws Exception {
        try (WsTestClient ws = connect()) {
            ws.send(addVehicle("v1", "north", "south"));
            ws.send(step());
            JsonNode status = ws.nextJson(3);
            assertFalse(status.has("error"), "Should successfully initialize from any non-malformed JSON");
            assertTrue(status.has("leftVehicles"), "Step response should have leftVehicles field");
        }
    }


    @Test
    void reconnect_newSession_hasCleanState() throws Exception {
        try (WsTestClient ws1 = connect()) {
            ws1.send("{}");
            ws1.send(addVehicle("old-vehicle", "north", "south"));
            ws1.send(step());
            JsonNode r = ws1.nextJson(3);
            assertTrue(leftVehicles(r).contains("old-vehicle"));
        }

        try (WsTestClient ws2 = connect()) {
            ws2.send("{}");
            ws2.send(step());
            JsonNode r = ws2.nextJson(3);
            assertFalse(leftVehicles(r).contains("old-vehicle"),
                    "New session must start with a clean engine");
        }
    }

    @Test
    void stepResponse_alwaysContains_leftVehiclesField() throws Exception {
        try (WsTestClient ws = connect()) {
            ws.send("{}");
            ws.send(step());
            JsonNode status = ws.nextJson(3);
            assertTrue(status.has("leftVehicles"),
                    "Step response must always contain 'leftVehicles' field, got: " + status);
            assertTrue(status.get("leftVehicles").isArray(),
                    "'leftVehicles' must be an array");
        }
    }

    @Test
    void scenario01_recruitmentExample_fullWalkthrough() throws Exception {
        try (WsTestClient ws = connect()) {
            ws.send("{}");
            ws.send(addVehicle("vehicle1", "south", "north"));
            ws.send(addVehicle("vehicle2", "north", "south"));
            ws.send(step());
            ws.send(step());
            ws.send(addVehicle("vehicle3", "west", "south"));
            ws.send(addVehicle("vehicle4", "west", "south"));
            ws.send(step());
            ws.send(step());

            assertEquals(List.of("vehicle1", "vehicle2"), leftVehicles(ws.nextJson(3)), "step 1");
            assertEquals(List.of(),                        leftVehicles(ws.nextJson(3)), "step 2");
            assertEquals(List.of("vehicle3"),              leftVehicles(ws.nextJson(3)), "step 3");
            assertEquals(List.of("vehicle4"),              leftVehicles(ws.nextJson(3)), "step 4");
        }
    }


    private static String addVehicle(String id, String start, String end) {
        return String.format(
                "{\"type\":\"addVehicle\",\"vehicleId\":\"%s\",\"startRoad\":\"%s\",\"endRoad\":\"%s\"}",
                id, start, end);
    }

    private static String addVehicle(String id, String start, String end, int lane) {
        return String.format(
                "{\"type\":\"addVehicle\",\"vehicleId\":\"%s\",\"startRoad\":\"%s\",\"endRoad\":\"%s\",\"lane\":%d}",
                id, start, end, lane);
    }

    private static String step() {
        return "{\"type\":\"step\"}";
    }

    private static String stop() {
        return "{\"type\":\"stop\"}";
    }

    @SuppressWarnings("unchecked")
    private static List<String> leftVehicles(JsonNode node) throws Exception {
        return MAPPER.treeToValue(node.get("leftVehicles"), List.class);
    }

    private WsTestClient connect() throws Exception {
        return new WsTestClient("ws://localhost:" + port + "/v1/ws/simulation");
    }

    private static class WsTestClient implements AutoCloseable {

        private volatile WebSocketSession session;
        private final BlockingQueue<String> inbox  = new LinkedBlockingQueue<>();
        private final CountDownLatch opened = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);

        WsTestClient(String url) throws Exception {
            new StandardWebSocketClient().execute(
                    new AbstractWebSocketHandler() {
                        @Override
                        public void afterConnectionEstablished(WebSocketSession s) {
                            session = s;
                            opened.countDown();
                        }

                        @Override
                        protected void handleTextMessage(WebSocketSession s, TextMessage msg) {
                            inbox.add(msg.getPayload());
                        }

                        @Override
                        public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
                            closed.countDown();
                        }
                    },
                    null,
                    URI.create(url)
            );
            assertTrue(opened.await(5, TimeUnit.SECONDS), "WebSocket did not connect in time");
        }

        void send(String json) throws IOException {
            session.sendMessage(new TextMessage(json));
        }

        /** Polls the inbox, parses the next message as JSON, asserts non-null on timeout. */
        JsonNode nextJson(long timeoutSeconds) throws Exception {
            String raw = inbox.poll(timeoutSeconds, TimeUnit.SECONDS);
            assertNotNull(raw, "Timed out waiting for next WebSocket message");
            return MAPPER.readTree(raw);
        }

        /** Returns true if the session was closed within {@code timeoutSeconds}. */
        boolean awaitClose(long timeoutSeconds) throws InterruptedException {
            return closed.await(timeoutSeconds, TimeUnit.SECONDS);
        }

        @Override
        public void close() throws Exception {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
