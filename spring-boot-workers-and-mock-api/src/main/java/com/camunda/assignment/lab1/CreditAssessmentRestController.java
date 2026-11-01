package com.camunda.assignment.lab1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/credit-assessment")
public class CreditAssessmentRestController {

    private static final Logger log = LoggerFactory.getLogger(CreditAssessmentRestController.class);

    @PostMapping
    public ResponseEntity<Map<String, Object>> assessCredit(@RequestBody Map<String, Object> request) {
        log.info("[LAB 1 REST API] Received Credit Assessment Request: {}", request);

        // Check if error simulation is requested (Test Scenario 4)
        Object simulateErrorObj = request.get("simulateApiError");
        if (Boolean.TRUE.equals(simulateErrorObj) || "true".equalsIgnoreCase(String.valueOf(simulateErrorObj))) {
            log.error("[LAB 1 REST API] Simulating Credit Assessment Bureau Timeout/500 Internal Error!");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Credit Bureau Service Unavailable / Timeout");
        }

        String applicantName = (String) request.getOrDefault("applicantName", "Unknown Applicant");
        Number monthlyIncomeNum = (Number) request.getOrDefault("monthlyIncome", 3000);
        double monthlyIncome = monthlyIncomeNum != null ? monthlyIncomeNum.doubleValue() : 3000.0;

        // Determine synthetic credit score based on input or predetermined test score
        int creditScore = 710;
        if (request.containsKey("creditScore") && request.get("creditScore") != null) {
            creditScore = ((Number) request.get("creditScore")).intValue();
        } else {
            if (monthlyIncome >= 5000) {
                creditScore = 740;
            } else if (monthlyIncome >= 3500) {
                creditScore = 630;
            } else {
                creditScore = 540;
            }
        }

        String creditTier;
        if (creditScore >= 700) {
            creditTier = "EXCELLENT";
        } else if (creditScore >= 650) {
            creditTier = "GOOD";
        } else if (creditScore >= 580) {
            creditTier = "FAIR";
        } else {
            creditTier = "POOR";
        }

        Map<String, Object> response = new HashMap<>();
        response.put("applicantName", applicantName);
        response.put("creditScore", creditScore);
        response.put("creditTier", creditTier);
        response.put("status", "SUCCESS");
        response.put("assessmentTimestamp", java.time.Instant.now().toString());

        log.info("[LAB 1 REST API] Returning Credit Assessment: {}", response);
        return ResponseEntity.ok(response);
    }
}

