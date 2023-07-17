package com.polarbookshop.dispatcherservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@FunctionalSpringBootTest
public class DispatchingFunctionsIntegrationTests {
    @Autowired
    private FunctionCatalog catalog;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void packOrder() {
        Function<OrderAcceptedMessage, Long> pack = catalog.lookup(Function.class, "pack");
        long orderId = 121;
        assertThat(pack.apply(new OrderAcceptedMessage(orderId))).isEqualTo(orderId);
    }

    @Test
    void labelOrder() {
        Function<Flux<Long>, Flux<OrderDispatchedMessage>> label = catalog.lookup(Function.class, "label");
        long orderId = 121;
        StepVerifier.create(label.apply(Flux.just(orderId)))
                .expectNextMatches(dispatchedOrder -> dispatchedOrder.equals(new OrderDispatchedMessage(orderId)))
                .verifyComplete();
    }

    @Test
    @Disabled
    void packAndLabelOrder() {
        Function<OrderAcceptedMessage, Flux<OrderDispatchedMessage>> packAndLabel =
                catalog.lookup(Function.class, "pack|label");
        long orderId = 121;

        StepVerifier.create(packAndLabel.apply(new OrderAcceptedMessage(orderId)))
                .expectNextMatches(dispatchedOrder ->
                        dispatchedOrder.equals(new OrderDispatchedMessage(orderId)))
                .verifyComplete();
    }

    @Test
    void packAndLabelOrder2() {
        Function<OrderAcceptedMessage, Flux<Message<byte[]>>>
                packAndLabel = catalog.lookup(Function.class, "pack|label");
        long orderId = 121;

        StepVerifier.create(packAndLabel.apply(new OrderAcceptedMessage(orderId)))
                .expectNextMatches(dispatchedOrder -> {
                    try {
                        OrderDispatchedMessage orderDispatchedMessage = objectMapper.readValue(dispatchedOrder.getPayload(), OrderDispatchedMessage.class);
                        return orderDispatchedMessage.equals(new OrderDispatchedMessage(orderId));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }


}
