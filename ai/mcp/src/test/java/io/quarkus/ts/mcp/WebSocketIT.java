package io.quarkus.ts.mcp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Dependency;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.ts.mcp.app.AdvancedServer;
import io.quarkus.ts.mcp.app.FileServer;
import io.quarkus.ts.mcp.app.MCPClient;
import io.quarkus.ts.mcp.app.MyResources;
import io.restassured.response.Response;

@QuarkusScenario
public class WebSocketIT extends BasicMCPIT {
    @QuarkusApplication(boms = { @Dependency(artifactId = "quarkus-mcp-server-bom") }, dependencies = {
            @Dependency(groupId = "io.quarkiverse.mcp", artifactId = "quarkus-mcp-server-websocket")
    }, classes = { FileServer.class, MyResources.class, AdvancedServer.class })
    static final RestService server = new RestService()
            .withProperty("quarkus.mcp.server.traffic-logging.enabled", "true")
            .withProperty("quarkus.mcp.server.traffic-logging.text-limit", "1000")
            .withProperty("working.folder", () -> Path.of("target").toAbsolutePath().toString());

    @QuarkusApplication(boms = {
            @Dependency(artifactId = "quarkus-langchain4j-bom") }, dependencies = {
                    @Dependency(artifactId = "quarkus-rest"),
                    @Dependency(groupId = "io.quarkiverse.langchain4j", artifactId = "quarkus-langchain4j-mcp"),
            }, classes = { MCPClient.class })
    static final RestService client = new RestService()
            .withProperty("quarkus.langchain4j.mcp.filesystem.transport-type", "websocket")
            .withProperty("quarkus.langchain4j.mcp.filesystem.url",
                    () -> getUrl(server));

    private static final Logger LOG = Logger.getLogger(WebSocketIT.class);

    private static String getUrl(RestService service) {
        return service.getURI(Protocol.WS).withPath("/mcp/ws").toString();
    }

    @Override
    public RestService client() {
        return client;
    }

    @Test
    void iconCheck() throws ExecutionException, InterruptedException, IOException {
        Session session = new Session(getUrl(server));
        session.sendRequest("initialize", Session.initParamas());
        JsonNode initialization = session.getResponse();
        Assertions.assertEquals("mcp", initialization
                .get("result")
                .get("serverInfo")
                .get("name").asText());

        session.sendRequest("tools/list", null);
        JsonNode tools = session.getResponse();
        Assertions.assertEquals("image/svg", tools
                .get("result")
                .get("tools")
                .get(0)
                .get("icons")
                .get(0)
                .get("mimeType").asText());
    }

    @Test
    void resourceNotifications() throws ExecutionException, InterruptedException, IOException {
        Response resources = client().given().get("/mcp/resources/");
        Assertions.assertEquals(200, resources.statusCode());
        Assertions.assertEquals("[file:///hello, file:///number, file:///separate]", resources.body().asString());

        Session session = new Session(getUrl(server));
        session.sendRequest("initialize", Session.initParamas());
        JsonNode initialization = session.getResponse();
        Assertions.assertEquals("mcp", initialization
                .get("result")
                .get("serverInfo")
                .get("name").asText());

        Response before = client().given().get("/mcp/resources/readFile/number");
        Assertions.assertEquals(200, before.statusCode());
        Assertions.assertEquals("1", before.body().asString());

        session.sendRequest("resources/subscribe", """
                {"uri": "file:///number"}
                """);

        JsonNode subscription = session.getResponse();
        Assertions.assertEquals("", subscription.get("result").asText());
        session.clearHistory();

        Response update = client().given().put("/mcp/meta/resources/number");
        Assertions.assertEquals(204, update.statusCode());

        JsonNode notification = session.getResponse();
        Assertions.assertEquals("notifications/resources/updated",
                notification.get("method").asText(),
                "No update in " + notification.asText());
        Assertions.assertEquals("file:///number", notification.get("params").get("uri").asText());

        Response after = client().given().get("/mcp/resources/readFile/number");
        Assertions.assertEquals(200, after.statusCode());
        Assertions.assertEquals("2", after.body().asString());
    }

    @Test
    void promptNotifications() throws ExecutionException, InterruptedException, IOException {
        Session session = new Session(getUrl(server));
        session.sendRequest("initialize", Session.initParamas());
        JsonNode initialization = session.getResponse();
        Assertions.assertEquals("mcp", initialization
                .get("result")
                .get("serverInfo")
                .get("name").asText());

        session.sendRequest("prompts/list", null);
        JsonNode before = session.getResponse();
        JsonNode promptsBefore = before.get("result").get("prompts");
        Assertions.assertEquals(1, promptsBefore.size());
        Assertions.assertEquals("meta", promptsBefore.get(0).get("name").asText());

        session.sendRequest("notifications/initialized", null);
        session.clearHistory();

        // another client asks to create a new prompt
        Response create = client().given().post("/mcp/meta/prompt/about-greeter");
        Assertions.assertEquals(200, create.statusCode());

        // a notification about update has arrived
        JsonNode notification = session.getResponse();
        Assertions.assertEquals("notifications/prompts/list_changed",
                notification.get("method").asText());

        session.sendRequest("prompts/list", null);
        JsonNode list = session.getResponse();
        JsonNode promptsAfter = list.get("result").get("prompts");
        Assertions.assertEquals(2, promptsAfter.size());
        Assertions.assertEquals("meta", promptsAfter.get(0).get("name").asText());
        Assertions.assertEquals("about-greeter", promptsAfter.get(1).get("name").asText());

        session.clearHistory();
        Response delete = client().given().delete("/mcp/meta/prompt/about-greeter");
        Assertions.assertEquals(200, delete.statusCode());

        JsonNode deleteNotification = session.getResponse();
        Assertions.assertEquals("notifications/prompts/list_changed",
                deleteNotification.get("method").asText());

        session.sendRequest("prompts/list", null);
        JsonNode last = session.getResponse();
        JsonNode finalPrompts = last.get("result").get("prompts");
        Assertions.assertEquals(1, finalPrompts.size());
        Assertions.assertEquals("meta", finalPrompts.get(0).get("name").asText());
    }

    @Test
    void sampling() throws ExecutionException, InterruptedException, IOException {
        Session session = new Session(getUrl(server));
        session.sendRequest("initialize", Session.initParamas());
        JsonNode initialization = session.getResponse();
        Assertions.assertEquals("mcp", initialization
                .get("result")
                .get("serverInfo")
                .get("name").asText());

        session.sendRequest("tools/call", """
                {
                 "name":"unsampled",
                 "arguments": {
                  "question": "What is the airspeed velocity of unladen swallow?"
                  }
                }
                """);
        JsonNode first = session.getResponse();
        session.sendRequest("tools/call", """
                {
                 "name":"sampled",
                 "arguments": {
                  "question": "What is the airspeed velocity of unladen swallow?"
                  }
                }
                """);

        JsonNode second = session.getResponse();
        System.out.println(second.toPrettyString());
    }

    class WebSocketListener implements WebSocket.Listener {
        private final List<String> answers;
        private StringBuilder current;

        WebSocketListener(List<String> answers) {
            this.answers = answers;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (current == null) {
                current = new StringBuilder();
            }
            current.append(data);
            if (last) {
                answers.add(current.toString());
                current = null;
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }
    }

    class Session {
        private final WebSocket webSocket;
        private final ObjectMapper mapper = new ObjectMapper();
        private final List<String> answers = Collections.synchronizedList(new ArrayList<>());

        int id = 0;

        Session(String url) throws ExecutionException, InterruptedException {
            this.webSocket = HttpClient.newHttpClient().newWebSocketBuilder()
                    .buildAsync(URI.create(url), new WebSocketListener(answers)).get();
        }

        public static String initParamas() {
            return """
                     {
                      "protocolVersion" : "2025-11-25",
                      "clientInfo" : {
                        "name" : "custom",
                        "version" : "1.0"
                      },
                      "capabilities": {
                        "sampling": {
                          "tools": {}
                        }
                      }
                    }

                    """;
        }

        public void clearHistory() {
            answers.clear();
        }

        public void sendRequest(String method, String params) {
            this.clearHistory();
            String request = this.generateRequest(method, params);
            webSocket.sendText(request, true);
        }

        public JsonNode getResponse() throws InterruptedException, JsonProcessingException {
            int repeats = -1;
            int lastSize = 0;
            while (answers.isEmpty() || answers.size() != lastSize) {
                if (++repeats > 10) {
                    LOG.warn("We have waited for: " + repeats + " seconds and it is too much!");
                    break;
                }
                lastSize = answers.size();
                Thread.sleep(1000);
            }
            return mapper.readTree(String.join("", answers));
        }

        String generateRequest(String method, String parameters) {
            String params;
            if (parameters == null) {
                params = "";
            } else {
                params = ",\n\"params\":" + parameters;
            }
            return """
                    {
                      "jsonrpc": "2.0",
                      "id": %d,
                      "method": "%s"%s
                    }
                    """.formatted(id++, method, params);
        }
    }
}
