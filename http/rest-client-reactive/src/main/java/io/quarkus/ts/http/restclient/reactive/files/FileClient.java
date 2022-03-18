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
import org.jboss.resteasy.reactive.MultipartForm;

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

    @GET
    @Produces(MediaType.MULTIPART_FORM_DATA)
    @javax.ws.rs.Path("/download-multipart")
    FileWrapper downloadMultipart();

    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    @javax.ws.rs.Path("/upload")
    Uni<String> sendFile(File data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @javax.ws.rs.Path("/upload-multipart")
    Uni<String> sendMultipart(@MultipartForm FileWrapper data);

}
