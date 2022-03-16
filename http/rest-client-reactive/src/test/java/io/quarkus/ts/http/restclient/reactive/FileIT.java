package io.quarkus.ts.http.restclient.reactive;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusScenario
public class FileIT {

    private static final Path DOWNLOADED = Paths.get("target", "FileIT", "downloaded.txt").toAbsolutePath();
    private static final Path UPLOADED = Paths.get("target", "FileIT", "uploaded.txt").toAbsolutePath();
    private static final String BIGGER_THAN_TWO_GIGABYTES = "2MiB";

    @QuarkusApplication
    static RestService app = new RestService().withProperties("modern.properties");

    @Test
    public void wrapperTest() {
        Response original = app.given().get("/files/hash");
        Response wrapped = app.given().get("/client-wrapper/hash");
        assertEquals(HttpStatus.SC_OK, original.statusCode());
        assertEquals(HttpStatus.SC_OK, wrapped.statusCode());
        assertNotNull(original.body().asString());
        assertEquals(original.body().asString(), wrapped.body().asString());
    }

    @Test
    public void downloadManually() throws IOException, InterruptedException {
        Response hashSum = app.given().get("/files/hash");
        assertEquals(HttpStatus.SC_OK, hashSum.statusCode());
        String serverSum = hashSum.body().asString();

        Response download = app.given().get("/files/download");
        assertEquals(HttpStatus.SC_OK, download.statusCode());
        InputStream stream = download.body().asInputStream();
        Files.copy(stream, DOWNLOADED);
        String clientSum = BashUtils.getSum(DOWNLOADED.toString());
        assertEquals(serverSum, clientSum);
    }

    @Test
    public void download() {
        Response hashSum = app.given().get("/files/hash");
        assertEquals(HttpStatus.SC_OK, hashSum.statusCode());
        String serverSum = hashSum.body().asString();

        Response download = app.given().get("/client-wrapper/download"); //FIXME file is not downloaded completely, should create an issue
        assertEquals(HttpStatus.SC_OK, download.statusCode());
        String clientSum = download.body().asString();

        assertEquals(serverSum, clientSum);
    }

    @Test
    //FIXME create issue
    public void uploadRest() throws IOException, InterruptedException {
        BashUtils.createFile(UPLOADED.toString(), BIGGER_THAN_TWO_GIGABYTES);
        String hashsum = BashUtils.getSum(UPLOADED.toString());
        try (InputStream stream = new FileInputStream(UPLOADED.toFile())) {
            Response response = app.given()
                    .body(stream)
                    .post("/files/upload/");
            assertEquals(HttpStatus.SC_OK, response.statusCode());
            assertEquals(hashsum, response.body().asString());
        }
    }

    @Test
    public void uploadPath() {
        Response hashSum = app.given().get("/client-wrapper/client-hash");
        assertEquals(HttpStatus.SC_OK, hashSum.statusCode());
        String before = hashSum.body().asString();

        Response upload = app.given().post("/client-wrapper/upload-path");
        assertEquals(HttpStatus.SC_OK, upload.statusCode());
        String after = upload.body().asString();

        assertEquals(before, after);
    }

    @Test
    public void uploadFile() {
        Response hashSum = app.given().get("/client-wrapper/client-hash");
        assertEquals(HttpStatus.SC_OK, hashSum.statusCode());
        String before = hashSum.body().asString();

        Response upload = app.given().post("/client-wrapper/upload-file");
        assertEquals(HttpStatus.SC_OK, upload.statusCode());
        String after = upload.body().asString();

        assertEquals(before, after);
    }
}
