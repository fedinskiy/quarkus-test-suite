package io.quarkus.ts.reactive;

import static io.restassured.RestAssured.given;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.bootstrap.RestService;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractReactiveDatabaseIT {
    @Test
    public void getAll() {
        getApp().given()
                .get("/library/books")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testUniEndpoint() {
        Response response = given()
                .when().get("/library/books/1");
        String title = response.body().asString();
        Assertions.assertEquals(HttpStatus.SC_OK, response.statusCode());
        Assertions.assertEquals("Slovník", title);
    }

    @Test
    public void testI18N() {
        Response response = given()
                .when().get("/library/book");
        String title = response.body().asString();
        Assertions.assertEquals(HttpStatus.SC_OK, response.statusCode());
        Assertions.assertEquals("Slovník", title);
    }

    @Test
    public void testFind() {
        String title = given()
                .when().get("/library/books/2")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        Assertions.assertEquals("Thinking fast and slow", title);
    }

    @Test
    public void testNotFound() {
        Response response = given().when().get("/library/books/256");
        Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, response.statusCode());
    }

    @Test
    public void testMultiEndpoint() {
        String result = given()
                .when().get("/library/books")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        Assertions.assertTrue(result.contains("Slovník"));
        Assertions.assertTrue(result.contains("Thinking fast and slow"));
    }

    @Test
    public void testQuery() {
        given()
                .when().get("/library/authors")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testJoinSearch() {
        String result = given()
                .when().get("/library/books/author/Kahneman")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        Assertions.assertTrue(result.contains("Attention and Effort"));
        Assertions.assertTrue(result.contains("Thinking fast and slow"));
    }

    @Test
    public void testLimitedSearch() {
        String result = given()
                .when().get("/hibernate/books/author/4")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        Assertions.assertFalse(result.contains("Attention and Effort"));
        Assertions.assertTrue(result.contains("Thinking fast and slow"));
    }

    @Test
    public void testNamedSearch() {
        Response response = given()
                .when().get("/hibernate/books/starts_with/Thinking");
        String result = response.getBody().asString();
        Assertions.assertTrue(result.contains("Thinking fast and slow"));
    }

    @Test
    @Order(3)
    public void testAuthorCreation() {
        Response post = given()
                .contentType(ContentType.JSON)
                .post("/hibernate/author/create/Plato");
        Assertions.assertEquals(HttpStatus.SC_CREATED, post.statusCode());
        String result = given()
                .when().get("/library/author/7")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        Assertions.assertEquals("Plato", result);
    }

    @Test
    public void testAuthor() {
        Response response = given()
                .when().get("/library/author/2");
        String result = response.then()
                .extract().body().asString();
        Assertions.assertEquals(HttpStatus.SC_OK, response.statusCode());
        Assertions.assertEquals("Vern", result);
    }

    @Test
    @Order(1)
    public void testCreation() {
        Response post = given()
                .contentType(ContentType.JSON)
                .post("/library/author/Wodehouse");
        Assertions.assertEquals(HttpStatus.SC_CREATED, post.statusCode());
        String result = given()
                .when().get("/library/author/5")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        Assertions.assertEquals("Wodehouse", result);
    }

    @Test
    @Order(2)
    public void testTooLongName() {
        Response creation = given().contentType(ContentType.JSON).post("library/author/Subrahmanyakavi");
        Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, creation.statusCode());
        given()
                .when().get("/library/author/6")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testGeneratedId() {
        String author = given()
                .when().get("/library/author/2")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        Assertions.assertEquals("Vern", author);
        Response creation = given().put("library/books/2/Around_the_World_in_Eighty_Days");
        Assertions.assertEquals(HttpStatus.SC_CREATED, creation.statusCode());
        String result = given()
                .when().get("library/books/author/Vern")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        Assertions.assertEquals("[Around_the_World_in_Eighty_Days]", result);
    }

    @Test
    public void deletion() {
        given().delete("library/author/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
        Response response = given()
                .when().get("/library/author/1");
        Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, response.statusCode());
    }

    @Test
    public void dto() {
        Response response = given().when().get("library/dto/4");
        Assertions.assertEquals(HttpStatus.SC_OK, response.statusCode());
        JsonPath jsonPath = response.getBody().jsonPath();
        Assertions.assertEquals("Thinking fast and slow", jsonPath.getString("[0].title"));
        Assertions.assertEquals("Attention and Effort", jsonPath.getString("[1].title"));
    }

    @Test
    public void testSession() {
        String title = given()
                .when().get("/hibernate/books/2")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        Assertions.assertEquals("Thinking fast and slow", title);
    }

    @Test
    public void testTransaction() {
        Response creation = given()
                .contentType(ContentType.JSON)
                .post("hibernate/books/Dick/Ubik");
        Assertions.assertEquals(HttpStatus.SC_CREATED, creation.statusCode());
        Response response = given()
                .when().get("/library/books/author/Dick");
        Assertions.assertEquals(HttpStatus.SC_OK, response.statusCode());
        Assertions.assertEquals("[Ubik]", response.getBody().asString());
    }

    @Test
    public void getAutoconvertedValue() {
        Response response = getApp().given().get("/library/isbn/2");
        Assertions.assertEquals(HttpStatus.SC_OK, response.statusCode());
        Assertions.assertEquals("9780374275631", response.body().asString());
    }

    @Test
    public void getAutoconvertedZeroValue() {
        Response response = getApp().given().get("/library/isbn/3");
        Assertions.assertEquals(HttpStatus.SC_OK, response.statusCode());
        Assertions.assertEquals("0", response.body().asString());
    }

    @Test
    public void setAutoconvertedZeroValue() {
        Response change = getApp().given().put("/library/isbn/1/5170261586");
        Assertions.assertEquals(HttpStatus.SC_OK, change.statusCode());
        Response lookup = getApp().given().get("/library/isbn/1");
        Assertions.assertEquals(HttpStatus.SC_OK, lookup.statusCode());
        Assertions.assertEquals("5170261586", lookup.body().asString());
    }

    protected abstract RestService getApp();
}
