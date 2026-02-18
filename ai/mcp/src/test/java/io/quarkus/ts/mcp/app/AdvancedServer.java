package io.quarkus.ts.mcp.app;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.mcp.server.Sampling;
import io.quarkiverse.mcp.server.SamplingMessage;
import io.quarkiverse.mcp.server.Tool;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
@Startup
public class AdvancedServer {
    @Tool(description = "Ask the AI asynchronously")
    Uni<String> sampled(String question, Sampling sampling) {
        if (!sampling.isSupported()) {
            return Uni.createFrom().item("Sampling not supported");
        }

        return sampling.requestBuilder()
                .setMaxTokens(100)
                .addMessage(SamplingMessage.withUserRole(question))
                .build()
                .send()
                .map(response -> response.content().asText().text());
    }

    @Tool(description = "Ask the AI asynchronously")
    Uni<String> unsampled(String question) {
        return Uni.createFrom().item("Answer to %s is 42".formatted(question));
    }
}
