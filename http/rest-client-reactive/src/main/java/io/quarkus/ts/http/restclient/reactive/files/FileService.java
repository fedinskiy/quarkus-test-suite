package io.quarkus.ts.http.restclient.reactive.files;

import java.io.IOException;
import java.nio.file.Paths;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.ts.http.restclient.reactive.BashUtils;
import io.smallrye.mutiny.Uni;

@Path("/client-wrapper")
public class FileService {
    private static final String BIGGER_THAN_TWO_GIGABYTES = "2049MiB";
    private static final java.nio.file.Path FILE = Paths.get("upload.txt").toAbsolutePath();
    final FileClient client;

    @Inject
    public FileService(@RestClient FileClient client) throws IOException, InterruptedException {
        BashUtils.createFile(FILE.toString(), BIGGER_THAN_TWO_GIGABYTES);
        this.client = client;
    }

    @GET
    @Path("/client-hash")
    public Uni<Response> calculateHash() throws IOException, InterruptedException {
        String hashSum = BashUtils.getSum(FILE.toString());
        return Uni.createFrom().item(Response.ok(hashSum).build());
    }

    @GET
    @Path("/hash")
    public Uni<Response> hash() {
        return client.hash().map(hash -> Response.ok(hash).build());
    }

    @GET
    @Path("/download")
    public Uni<String> download() {
        return client.download().map(file -> {
            try {
                String path = file.getAbsolutePath();
                return BashUtils.getSum(path);
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @GET
    @Path("/download-multipart")
    public String downloadMultipart() {
        FileWrapper wrapper = client.downloadMultipart();
        try {
            String path = wrapper.file.getAbsolutePath();
            return BashUtils.getSum(path);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @POST
    @Path("/multipart")
    public Uni<String> uploadMultipart() {
        FileWrapper wrapper = new FileWrapper();
        wrapper.file = FILE.toFile();
        return client.sendMultipart(wrapper);
    }

    @POST
    @Path("/upload-file")
    public Uni<String> upload() {
        return client.sendFile(FILE.toFile());
    }
}
