package io.quarkus.ts.http.restclient.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;

@QuarkusScenario
public class ClientBookResourceIT {

    @QuarkusApplication
    static RestService app = new RestService().withProperties("modern.properties");

    @Test
    public void shouldGetBookFromRestClientJson() {
        Response response = app.given().with().pathParam("id", "123")
                .get("/client/{id}/book/json");
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals("Title in Json: 123", response.jsonPath().getString("title"));
    }

    @Tag("QUARKUS-1568")
    @Test
    public void supportPathParamFromBeanParam() {
        Response response = app.given().with().pathParam("id", "123")
                .get("/client/{id}/book/jsonByBeanParam");
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals("Title in Json: 123", response.jsonPath().getString("title"));
    }

    @Test
    public void byPath() {
        Response response = app.given().get("/books/John/Apocalypse");
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals("Apocalypse", response.jsonPath().getString("title"));
        assertEquals("John", response.jsonPath().getString("author"));
    }

    @Test
    public void byQuery() {
        Response response = app.given().get("/books/query?id=Apocalypse&author=John");
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals("Apocalypse", response.jsonPath().getString("title"));
        assertEquals("John", response.jsonPath().getString("author"));
    }

    @Test
    public void byMapQuery() {
        Response response = app.given()
                .when()
                .queryParam("param", "{\"id\":\"Hagakure\",\"author\":\"Tsuramoto\"}")
                .get("/books/map");
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals("Hagakure", response.jsonPath().getString("title"));
        assertEquals("Tsuramoto", response.jsonPath().getString("author"));
    }
}
