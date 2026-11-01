package com.camunda.assignment.lab1;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class LoanValidationWorker {

    private static final Logger log = LoggerFactory.getLogger(LoanValidationWorker.class);

    @JobWorker(type = "validate-applicant", autoComplete = true)
    public Map<String, Object> validateApplicant(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.info("[LAB 1 WORKER] Executing 'validate-applicant' for Job Key: {}, Variables: {}", 
                 job.getKey(), variables);

        // Test Scenario 5: Incident Simulation
        Object testIncident = variables.get("testIncident");
        if (Boolean.TRUE.equals(testIncident) || "true".equalsIgnoreCase(String.valueOf(testIncident))) {
            log.error("[LAB 1 WORKER] INTENTIONAL FAILURE: Simulating unhandled worker exception for incident recovery test!");
            throw new RuntimeException("Simulated unhandled exception in LoanValidationWorker for incident recovery verification.");
        }

        String applicantName = (String) variables.get("applicantName");
        String applicantEmail = (String) variables.get("applicantEmail");
        Number requestedAmount = (Number) variables.get("requestedAmount");
        Number monthlyIncome = (Number) variables.get("monthlyIncome");

        boolean isValid = true;
        StringBuilder errorMsg = new StringBuilder();

        if (applicantName == null || applicantName.trim().isEmpty()) {
            isValid = false;
            errorMsg.append("Applicant name is required. ");
        }
        if (applicantEmail == null || !applicantEmail.contains("@")) {
            isValid = false;
            errorMsg.append("Valid applicant email is required. ");
        }
        if (requestedAmount == null || requestedAmount.doubleValue() <= 0) {
            isValid = false;
            errorMsg.append("Requested amount must be greater than zero. ");
        }
        if (monthlyIncome == null || monthlyIncome.doubleValue() <= 0) {
            isValid = false;
            errorMsg.append("Monthly income must be greater than zero. ");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isValid", isValid);
        result.put("validationMessage", isValid ? "Applicant data validated successfully." : errorMsg.toString());

        log.info("[LAB 1 WORKER] Validation Completed. Result: {}", result);
        return result;
    }
}

