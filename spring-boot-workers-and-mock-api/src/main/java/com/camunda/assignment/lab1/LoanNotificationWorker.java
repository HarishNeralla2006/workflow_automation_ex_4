package com.camunda.assignment.lab1;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class LoanNotificationWorker {

    private static final Logger log = LoggerFactory.getLogger(LoanNotificationWorker.class);

    @JobWorker(type = "disburse-loan", autoComplete = true)
    public Map<String, Object> disburseLoan(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        String applicantName = (String) variables.getOrDefault("applicantName", "Applicant");
        Object requestedAmount = variables.get("requestedAmount");

        String disbursementRef = "DISB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[LAB 1] Loan Disbursed successfully for {}: Amount ${}, Ref: {}", 
                 applicantName, requestedAmount, disbursementRef);

        Map<String, Object> result = new HashMap<>();
        result.put("disbursementReference", disbursementRef);
        result.put("disbursementStatus", "COMPLETED");
        return result;
    }

    @JobWorker(type = "notify-rejection", autoComplete = true)
    public void notifyRejection(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        String applicantName = (String) variables.getOrDefault("applicantName", "Applicant");
        Object reason = variables.getOrDefault("decisionReason", "Eligibility requirements not met");

        log.info("[LAB 1] Sent Rejection Notice to {}. Reason: {}", applicantName, reason);
    }

    @JobWorker(type = "notify-applicant", autoComplete = true)
    public void notifyApplicant(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.warn("[LAB 1] Application invalid. Notifying applicant: {}", variables.get("validationMessage"));
    }
}

