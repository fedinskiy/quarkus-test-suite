package io.quarkus.ts.vertx;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;

@Path("/hello")
public class HelloResource {
    private final Vertx vertx;

    @Inject
    public HelloResource(Vertx vertx) {
        this.vertx = vertx;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Hello> getMessage(@QueryParam("name") @DefaultValue("World") String name) {
        return vertx.fileSystem()
                .readFile("message.template")
                .map(Buffer::toString)
                .map(string -> String.format(string, name).trim())
                .map(Hello::new);
    }

    @GET
    @Path("bus")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Hello> getFromBus(@QueryParam("name") @DefaultValue("World") String name) {
        return vertx.eventBus().<String> request("greetings", name)
                .onItem()
                .transform(response -> new Hello(response.body()));
    }
}
