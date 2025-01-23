package io.quarkus.ts.messaging.kafka;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestStreamElementType;

import io.smallrye.mutiny.Multi;

@Path("/stock-price")
public class StockPriceEndpoint {
    private static final Logger LOG = Logger.getLogger(StockPriceEndpoint.class);
    @Inject
    @Channel("source-stock-price")
    @OnOverflow(value = OnOverflow.Strategy.DROP)
    Emitter<StockPrice> stockPriceEmitter;

    @Inject
    @Channel("price-stream")
    Multi<String> stockPrices;

    @Inject
    @Channel("price-stream-batch")
    Multi<List<String>> stockPricesBatch;

    @GET
    @Path("/stream")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> stream() {
        LOG.warn("stream");
        return stockPrices;
    }


    @POST
    public Response addStockPrice(StockPriceDto stockPrice) {
        LOG.warn("add price " + stockPrice.getValue());
        stockPriceEmitter.send(stockPrice.toAvro());
        return Response.accepted().build();
    }
}
