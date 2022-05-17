package io.quarkus.ts.http.restclient.reactive;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;

@QuarkusScenario
public class ProxyIT {
    @QuarkusApplication
    static RestService proxyApp = new RestService().withProperties("proxy.properties");

    @Test
    void getThrough() {
        Response proxied = proxyApp.given().with().get("/proxied/");
        System.out.println(proxied.statusCode());
        //        System.out.println(proxied.body().asString());
        Assertions.assertTrue(proxied.body().asString().contains("Gravatar URLs"));
    }

    @Test
    void banned() {
        Response banned = proxyApp.given().with().get("/proxied/banned");
        Assertions.assertEquals(HttpStatus.SC_FORBIDDEN, banned.statusCode());
        Assertions.assertEquals("Reading is prohibited by corporate policy!",
                banned.body().asString());
    }
}
