package io.quarkus.ts.http.restclient.reactive.proxy;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;

@Path("/proxied")
public class ProxyResource {

    @Inject
    @RestClient
    ProxyClient client;

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Uni<String> getRoot() {
        return client.getSite();
    }

    @GET
    @Path("/banned")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> getBanned() {
        return client.getText();
    }

}
