package io.quarkus.ts.http.restclient.reactive.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.ts.http.restclient.reactive.BashUtils;
import io.smallrye.mutiny.Uni;

@Path("/files")
public class FileResource {
    private static final File FILE = Paths.get("server.txt").toAbsolutePath().toFile();
    private static final String BIGGER_THAN_TWO_GIGABYTES = "2050MiB";

    public FileResource() throws IOException, InterruptedException {
        BashUtils.createFile(FILE.getAbsolutePath(), BIGGER_THAN_TWO_GIGABYTES);
    }

    @GET
    @Path("/download")
    public Uni<Response> download() {
        return Uni.createFrom().item(Response.ok(FILE).build());
    }

    @POST
    @Path("/upload")
    public Uni<Response> upload(File body) throws IOException, InterruptedException {
        /*
         * final java.nio.file.Path file = Paths.get(".").toAbsolutePath().getParent().getParent().resolve("client.txt")
         * .toAbsolutePath();
         * Files.copy(body, file);
         */
        String sum = BashUtils.getSum(body.getAbsolutePath());
        return Uni.createFrom().item(Response.ok(sum).build());
    }

    @GET
    @Path("/hash")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> hash() throws IOException, InterruptedException {
        String hashSum = BashUtils.getSum(FILE.getAbsolutePath());
        return Uni.createFrom().item(Response.ok(hashSum).build());
    }
}
