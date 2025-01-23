package io.quarkus.ts.messaging.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.SseEventSource;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.restassured.http.ContentType;

abstract class BaseKafkaAvroGroupIdIT {
    private static final int TIMEOUT_SEC = 10;
    private static final int EVENTS_AMOUNT = 5;

    private boolean completed;
    private Random rand = new Random();

    @Test
    public void testSane() {
        String endpointA = getEndpoint(getAppA()) + "/stock-price/stream";
        AppResults resultsA = new AppResults(EVENTS_AMOUNT, endpointA);
        String endpointB = getEndpoint(getAppB()) + "/stock-price/stream";
        AppResults resultsB = new AppResults(EVENTS_AMOUNT + EVENTS_AMOUNT, endpointB);

        GivenSomeStockPrices(getAppA(), EVENTS_AMOUNT);
        assertTrue(resultsA.readFromTheEndpoint(), "Not all expected kafka events has been consumed.");
        GivenSomeStockPrices(getAppB(), EVENTS_AMOUNT);
        // Application B should have a different auto-generated group ID so,
        // double the number of events
        assertTrue(resultsB.readFromTheEndpoint(), "Not all expected kafka events has been consumed.");
    }

    protected abstract RestService getAppA();

    protected abstract RestService getAppB();

    private void GivenSomeStockPrices(RestService app, int amount) {
        IntStream.range(0, amount).forEach(i -> app.given()
                .contentType(ContentType.JSON)
                .body(randomStockPrice())
                .post("/stock-price")
                .then()
                .statusCode(202));
    }

    private String getEndpoint(RestService app) {
        return app.getURI(Protocol.HTTP).toString();
    }

    private StockPriceDto randomStockPrice() {
        StockPriceDto stockPriceDto = new StockPriceDto();
        stockPriceDto.setId(UUID.randomUUID().toString());
        stockPriceDto.setValue(rand.nextInt(256));
        return stockPriceDto;
    }

    class AppResults {
        private final int expectedAmount;
        private final AtomicInteger totalAmountReceived = new AtomicInteger(0);
        final CountDownLatch latch;
        private SseEventSource source;

        AppResults(int expectedAmount, String endpoint) {
            this.expectedAmount = expectedAmount;
            this.latch = new CountDownLatch(this.expectedAmount);
            final Client client = ClientBuilder.newClient();
            final WebTarget target = client.target(endpoint);
            source = SseEventSource.target(target).build();
            source.register(inboundSseEvent -> {
                System.out.println("Getting data from " + endpoint);
                System.out.println(inboundSseEvent.readData());
                final var data = inboundSseEvent.readData(String.class, MediaType.APPLICATION_JSON_TYPE);
                System.out.println(data.toString());
                totalAmountReceived.incrementAndGet();
                latch.countDown();
            });
            //            source.open();
        }

        public boolean readFromTheEndpoint() {
            try {
                source.open();
                latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
                source.close();
            } finally {
                int received = totalAmountReceived.get();
                assertEquals(expectedAmount, received, "You should not process more msg than the expected ones");
                return expectedAmount == received;
            }
        }
    }
}
