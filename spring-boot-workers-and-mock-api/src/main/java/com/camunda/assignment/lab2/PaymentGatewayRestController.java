package com.camunda.assignment.lab2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
public class PaymentGatewayRestController {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayRestController.class);

    @PostMapping("/charge")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> request) {
        log.info("[LAB 2 REST API] Processing Payment Request: {}", request);

        // Check if service incident simulation is requested (Test Scenario 5)
        Object testIncidentObj = request.get("testIncident");
        String paymentToken = (String) request.getOrDefault("paymentToken", "");
        if (Boolean.TRUE.equals(testIncidentObj) || paymentToken.contains("fail") || paymentToken.contains("error")) {
            log.error("[LAB 2 REST API] Simulating Gateway 503 Service Outage for Incident Recovery!");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment Gateway Outage - Connection Timeout");
        }

        String orderId = (String) request.getOrDefault("orderId", "ORD-UNKNOWN");
        Number amountNum = (Number) request.getOrDefault("amount", 0.0);
        double amount = amountNum != null ? amountNum.doubleValue() : 0.0;

        // Check if simulated payment decline is requested (Test Scenario 3)
        Object simulateDeclineObj = request.get("simulatePaymentDecline");
        boolean isDecline = Boolean.TRUE.equals(simulateDeclineObj) || 
                            "true".equalsIgnoreCase(String.valueOf(simulateDeclineObj)) ||
                            paymentToken.contains("declined");

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("amount", amount);

        if (isDecline) {
            log.warn("[LAB 2 REST API] Payment DECLINED for Order {}", orderId);
            response.put("paymentStatus", "DECLINED");
            response.put("declineReason", "Insufficient funds or invalid payment token");
            response.put("transactionId", null);
        } else {
            String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
            log.info("[LAB 2 REST API] Payment SUCCEEDED for Order {}: Transaction {}", orderId, txnId);
            response.put("paymentStatus", "SUCCESS");
            response.put("transactionId", txnId);
            response.put("chargedAt", java.time.Instant.now().toString());
        }

        return ResponseEntity.ok(response);
    }
}

