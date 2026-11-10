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
@RequestMapping("/api")
public class CreditAssessmentRestController {

    private static final Logger log = LoggerFactory.getLogger(CreditAssessmentRestController.class);

    @PostMapping(value = {"/credit-assessment", "/credit-check"})
    public ResponseEntity<Map<String, Object>> assessCredit(@RequestBody Map<String, Object> request) {
        log.info("[LAB 1 REST API] Received Credit Assessment / Check Request: {}", request);

        // Check if error simulation is requested (Test Scenario 4)
        Object simulateErrorObj = request.get("simulateApiError");
        if (Boolean.TRUE.equals(simulateErrorObj) || "true".equalsIgnoreCase(String.valueOf(simulateErrorObj))) {
            log.error("[LAB 1 REST API] Simulating Credit Assessment Bureau Timeout/500 Internal Error!");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Credit Bureau Service Unavailable / Timeout");
        }

        // Handle customerName / applicantName
        String customerName = "Applicant";
        if (request.containsKey("customerName") && request.get("customerName") != null) {
            customerName = String.valueOf(request.get("customerName"));
        } else if (request.containsKey("applicantName") && request.get("applicantName") != null) {
            customerName = String.valueOf(request.get("applicantName"));
        }

        // Monthly income
        Number monthlyIncomeNum = (Number) request.getOrDefault("monthlyIncome", 3000);
        double monthlyIncome = monthlyIncomeNum != null ? monthlyIncomeNum.doubleValue() : 3000.0;

        // Existing EMI
        Number existingEmiNum = (Number) request.getOrDefault("existingEmi", 0);
        double existingEmi = existingEmiNum != null ? existingEmiNum.doubleValue() : 0.0;

        // Loan / Requested Amount
        Number loanAmountNum = null;
        if (request.containsKey("loanAmount") && request.get("loanAmount") != null) {
            loanAmountNum = (Number) request.get("loanAmount");
        } else if (request.containsKey("requestedAmount") && request.get("requestedAmount") != null) {
            loanAmountNum = (Number) request.get("requestedAmount");
        }
        double loanAmount = loanAmountNum != null ? loanAmountNum.doubleValue() : 10000.0;

        // Tenure in months
        Number tenureMonthsNum = (Number) request.getOrDefault("tenureMonths", 36);
        int tenureMonths = tenureMonthsNum != null ? tenureMonthsNum.intValue() : 36;

        // Calculate illustrative EMI (12% per annum = 1% per month)
        double monthlyRate = 0.12 / 12.0;
        double emi = 0.0;
        if (loanAmount > 0 && tenureMonths > 0) {
            emi = loanAmount * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths)
                    / (Math.pow(1 + monthlyRate, tenureMonths) - 1);
        }
        double totalMonthlyEmi = existingEmi + emi;
        double emiRatio = monthlyIncome > 0 ? (totalMonthlyEmi / monthlyIncome) * 100.0 : 100.0;

        // Determine synthetic credit score / demoScore
        int creditScore;
        if (request.containsKey("creditScore") && request.get("creditScore") != null) {
            creditScore = ((Number) request.get("creditScore")).intValue();
        } else if (request.containsKey("demoScore") && request.get("demoScore") != null) {
            creditScore = ((Number) request.get("demoScore")).intValue();
        } else {
            // Apply illustrative tutorial formula
            int score = 900;
            if (emiRatio > 50) score -= 250;
            else if (emiRatio > 40) score -= 150;
            else if (emiRatio > 30) score -= 75;
            if (monthlyIncome < 25000) score -= 100;
            creditScore = Math.max(300, Math.min(900, score));
        }

        // Determine credit tier
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

        // Determine decision according to tutorial logic
        String decision;
        boolean eligible;
        if (creditScore >= 700 && emiRatio <= 40) {
            decision = "ELIGIBLE";
            eligible = true;
        } else if (creditScore >= 600 && emiRatio <= 50) {
            decision = "MANUAL_REVIEW";
            eligible = false;
        } else {
            decision = "NOT_ELIGIBLE";
            eligible = false;
        }

        String message = switch (decision) {
            case "ELIGIBLE" -> "Meets the demonstration eligibility rules";
            case "MANUAL_REVIEW" -> "Requires manual review";
            default -> "Does not meet the demonstration eligibility rules";
        };

        Map<String, Object> response = new HashMap<>();
        response.put("customerName", customerName);
        response.put("applicantName", customerName);
        response.put("demoScore", creditScore);
        response.put("creditScore", creditScore);
        response.put("creditTier", creditTier);
        response.put("estimatedEmi", Math.round(emi * 100.0) / 100.0);
        response.put("totalMonthlyEmi", Math.round(totalMonthlyEmi * 100.0) / 100.0);
        response.put("emiRatio", Math.round(emiRatio * 100.0) / 100.0);
        response.put("eligible", eligible);
        response.put("decision", decision);
        response.put("message", message);
        response.put("status", "SUCCESS");
        response.put("assessmentTimestamp", java.time.Instant.now().toString());

        log.info("[LAB 1 REST API] Returning Credit Assessment / Check: {}", response);
        return ResponseEntity.ok(response);
    }
}
