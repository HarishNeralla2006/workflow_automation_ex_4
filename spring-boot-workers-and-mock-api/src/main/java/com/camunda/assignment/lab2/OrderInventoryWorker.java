package com.camunda.assignment.lab2;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderInventoryWorker {

    private static final Logger log = LoggerFactory.getLogger(OrderInventoryWorker.class);

    @JobWorker(type = "validate-and-check-inventory", autoComplete = true)
    public Map<String, Object> validateAndCheckInventory(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.info("[LAB 2 WORKER] Validating order and inventory for Job Key: {}, Variables: {}", 
                 job.getKey(), variables);

        // Incident simulation
        Object testIncident = variables.get("testIncident");
        if (Boolean.TRUE.equals(testIncident) || "true".equalsIgnoreCase(String.valueOf(testIncident))) {
            log.error("[LAB 2 WORKER] Simulating unhandled worker exception in OrderInventoryWorker!");
            throw new RuntimeException("Simulated inventory service crash for incident investigation.");
        }

        String orderId = (String) variables.getOrDefault("orderId", "ORD-DEFAULT");
        String productId = (String) variables.getOrDefault("productId", "PROD-GENERIC");
        Number quantityNum = (Number) variables.getOrDefault("quantity", 1);
        int quantity = quantityNum != null ? quantityNum.intValue() : 1;

        Number unitPriceNum = (Number) variables.getOrDefault("unitPrice", 100.0);
        double unitPrice = unitPriceNum != null ? unitPriceNum.doubleValue() : 100.0;

        // Calculate order total
        double orderTotal = quantity * unitPrice;

        // Check stock availability
        Object simulateStockOut = variables.get("simulateStockOut");
        boolean stockAvailable = true;
        if (Boolean.TRUE.equals(simulateStockOut) || "true".equalsIgnoreCase(String.valueOf(simulateStockOut))) {
            stockAvailable = false;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderTotal", orderTotal);
        result.put("stockAvailable", stockAvailable);
        result.put("stockQuantity", stockAvailable ? (quantity + 25) : 0);
        result.put("inventoryLocation", "WAREHOUSE-EAST-1");

        log.info("[LAB 2 WORKER] Inventory check result: orderTotal={}, stockAvailable={}", orderTotal, stockAvailable);
        return result;
    }
}

