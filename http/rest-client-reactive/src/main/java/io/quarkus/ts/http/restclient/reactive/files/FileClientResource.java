package io.quarkus.ts.http.restclient.reactive.files;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.runtime.ShutdownEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

@Path("/file-client")
public class FileClientResource {
    private static final String BIGGER_THAN_TWO_GIGABYTES = OsUtils.SIZE_2049MiB;

    private final java.nio.file.Path file;
    private final List<java.nio.file.Path> deathRow = new LinkedList<>();
    private final FileClient client;
    private final OsUtils utils;

    @Inject
    public FileClientResource(@RestClient FileClient client,
            @ConfigProperty(name = "client.filepath") Optional<String> folder) {
        utils = OsUtils.get();
        file = folder
                .map(existing -> java.nio.file.Path.of(existing).resolve("upload.txt").toAbsolutePath())
                .orElseGet(() -> {
                    try {
                        return Files.createTempFile("upload", ".txt");
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
        utils.createFile(file.toString(), BIGGER_THAN_TWO_GIGABYTES);
        deathRow.add(file);
        this.client = client;
    }

    @GET
    @Path("/client-hash")
    @Blocking
    public Uni<String> calculateHash() {
        return utils.getSum(file.toString());
    }

    @GET
    @Path("/hash")
    public Uni<String> hash() {
        return client.hash();
    }

    @GET
    @Path("/download")
    public Uni<String> download() {
        return client.download()
                .map(file -> {
                    java.nio.file.Path path = file.toPath().toAbsolutePath();
                    deathRow.add(path);
                    return path.toString();
                })
                .onItem()
                .transformToUni(utils::getSum);
    }

    @GET
    @Path("/download-multipart")
    public Uni<String> downloadMultipart() {
        return client.downloadMultipart()
                .map(wrapper -> wrapper.file.toPath())
                .map(java.nio.file.Path::toAbsolutePath)
                .invoke(deathRow::add)
                .map(java.nio.file.Path::toString)
                .flatMap(utils::getSum);
    }

    @GET
    @Path("/download-broken-multipart")
    public Uni<String> downloadMultipartResponse() {
        return client.brokenMultipart()
                .map(wrapper -> wrapper.file.getAbsolutePath())
                .flatMap(utils::getSum);
    }

    @POST
    @Path("/multipart")
    public Uni<String> uploadMultipart() {
        FileWrapper wrapper = new FileWrapper();
        wrapper.file = file.toFile();
        wrapper.name = file.toString();
        return client.sendMultipart(wrapper);
    }

    @POST
    @Path("/upload-file")
    public Uni<String> upload() {
        return client.sendFile(file.toFile());
    }

    void onStop(@Observes ShutdownEvent ev) throws IOException {
        for (java.nio.file.Path path : deathRow) {
            Files.delete(path);
        }
    }
}
