package io.quarkus.ts.http.restclient.reactive.files;

import java.io.File;
import java.nio.file.Path;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@RegisterRestClient
@javax.ws.rs.Path("/files")
@RegisterClientHeaders
public interface FileClient {

    @GET
    @javax.ws.rs.Path("/hash")
    @Produces(MediaType.TEXT_PLAIN)
    Uni<String> hash();

    @GET
    @javax.ws.rs.Path("/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Uni<File> download();

    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    @javax.ws.rs.Path("/upload")
    Uni<String> sendPath(Path data); //fixme this sends file with file path as content. Need to create an issue

    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    @javax.ws.rs.Path("/upload")
    Uni<String> sendFile(File data);

}
