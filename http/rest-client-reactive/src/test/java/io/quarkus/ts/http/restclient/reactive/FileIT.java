package io.quarkus.ts.http.restclient.reactive;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusScenario
public class FileIT {

    private static final String BIGGER_THAN_TWO_GIGABYTES = OsUtils.SIZE_2049MiB;
    private final Path downloaded;
    private final Path uploaded;
    private final OsUtils utils = OsUtils.get();

    @QuarkusApplication
    static RestService app = new RestService().withProperties("modern.properties");

    public FileIT() throws IOException {
        downloaded = Files.createTempFile("downloaded", "txt").toAbsolutePath();
        Files.delete(downloaded);
        uploaded = Files.createTempFile("uploaded", "txt").toAbsolutePath();
    }

    @Test
    @Disabled
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "https://github.com/quarkusio/quarkus/issues/24763")
    public void wrapperTest() {
        Response original = app.given().get("/files/hash");
        Response wrapped = app.given().get("/client-wrapper/hash");
        assertEquals(HttpStatus.SC_OK, original.statusCode());
        assertEquals(HttpStatus.SC_OK, wrapped.statusCode());
        assertNotNull(original.body().asString());
        assertEquals(original.body().asString(), wrapped.body().asString());
    }

    @Test
    @Disabled
    public void downloadManually() throws IOException {
        Response hashSum = app.given().get("/files/hash");
        assertEquals(HttpStatus.SC_OK, hashSum.statusCode());
        String serverSum = hashSum.body().asString();

        Response download = app.given().get("/files/download");
        assertEquals(HttpStatus.SC_OK, download.statusCode());
        InputStream stream = download.body().asInputStream();
        Files.copy(stream, downloaded);
        String clientSum = utils.getSum(downloaded.toString());
        assertEquals(serverSum, clientSum);
    }

    @Test
    @Disabled("https://github.com/quarkusio/quarkus/issues/24402")
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "https://github.com/quarkusio/quarkus/issues/24763")
    public void downloadThroughClient() {
        Response hashSum = app.given().get("/files/hash");
        assertEquals(HttpStatus.SC_OK, hashSum.statusCode());
        String serverSum = hashSum.body().asString();

        Response download = app.given().get("/client-wrapper/download");
        assertEquals(HttpStatus.SC_OK, download.statusCode());
        String clientSum = download.body().asString();

        assertEquals(serverSum, clientSum);
    }

    @Test
    @Disabled("https://github.com/quarkusio/quarkus/issues/24415")
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "https://github.com/quarkusio/quarkus/issues/24763")
    public void downloadMultipart() {
        Response hashSum = app.given().get("/files/hash");
        assertEquals(HttpStatus.SC_OK, hashSum.statusCode());
        String serverSum = hashSum.body().asString();

        Response download = app.given().get("/client-wrapper/download-multipart");
        assertEquals(HttpStatus.SC_OK, download.statusCode());
        String clientSum = download.body().asString();

        assertEquals(serverSum, clientSum);
    }

    @Test
    @Disabled("https://github.com/rest-assured/rest-assured/issues/1480")
    public void uploadRestStream() throws IOException {
        utils.createFile(uploaded.toString(), BIGGER_THAN_TWO_GIGABYTES);
        String hashsum = utils.getSum(uploaded.toString());
        try (InputStream stream = new FileInputStream(uploaded.toFile())) {
            Response response = app.given()
                    .body(stream)
                    .post("/files/upload/");
            assertEquals(HttpStatus.SC_OK, response.statusCode());
            assertEquals(hashsum, response.body().asString());
        }
    }

    @Test
    @Disabled("https://github.com/rest-assured/rest-assured/issues/1566")
    public void uploadRestFile() {
        utils.createFile(uploaded.toString(), BIGGER_THAN_TWO_GIGABYTES);
        String hashsum = utils.getSum(uploaded.toString());
        Response response = app.given()
                .body(uploaded.toFile())
                .post("/files/upload/");
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals(hashsum, response.body().asString());
    }

    @Test
    @Disabled("https://github.com/quarkusio/quarkus/issues/24405")
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "https://github.com/quarkusio/quarkus/issues/24763")
    public void uploadFile() {
        Response hashSum = app.given().get("/client-wrapper/client-hash");
        assertEquals(HttpStatus.SC_OK, hashSum.statusCode());
        String before = hashSum.body().asString();

        Response upload = app.given().post("/client-wrapper/upload-file");
        assertEquals(HttpStatus.SC_OK, upload.statusCode());
        String after = upload.body().asString();

        assertEquals(before, after);
    }

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "https://github.com/quarkusio/quarkus/issues/24763")
    public void uploadMultipart() {
        Response hashSum = app.given().get("/client-wrapper/client-hash");
        assertEquals(HttpStatus.SC_OK, hashSum.statusCode());
        String before = hashSum.body().asString();

        Response upload = app.given().post("/client-wrapper/multipart");
        assertEquals(HttpStatus.SC_OK, upload.statusCode());
        String after = upload.body().asString();

        assertEquals(before, after);
    }
}
