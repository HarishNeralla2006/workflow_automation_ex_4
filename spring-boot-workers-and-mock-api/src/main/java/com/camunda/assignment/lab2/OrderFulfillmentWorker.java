package com.camunda.assignment.lab2;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderFulfillmentWorker {

    private static final Logger log = LoggerFactory.getLogger(OrderFulfillmentWorker.class);

    @JobWorker(type = "fulfill-order", autoComplete = true)
    public Map<String, Object> fulfillOrder(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        String orderId = (String) variables.getOrDefault("orderId", "ORD-UNKNOWN");
        String trackingNumber = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[LAB 2] Order {} packed and dispatched. Tracking Number: {}", orderId, trackingNumber);

        Map<String, Object> result = new HashMap<>();
        result.put("trackingNumber", trackingNumber);
        result.put("shippingStatus", "SHIPPED");
        return result;
    }

    @JobWorker(type = "notify-out-of-stock", autoComplete = true)
    public void notifyOutOfStock(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.warn("[LAB 2] Notifying customer: Product {} is out of stock for order {}", 
                 variables.get("productId"), variables.get("orderId"));
    }

    @JobWorker(type = "notify-payment-failed", autoComplete = true)
    public void notifyPaymentFailed(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.warn("[LAB 2] Notifying customer: Payment failed for order {}", variables.get("orderId"));
    }

    @JobWorker(type = "cancel-order", autoComplete = true)
    public void cancelOrder(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.warn("[LAB 2] Order {} canceled following failed manual verification.", variables.get("orderId"));
    }
}

