package com.camunda.assignment.lab3;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class HospitalAppointmentWorker {

    private static final Logger log = LoggerFactory.getLogger(HospitalAppointmentWorker.class);

    @JobWorker(type = "book-appointment", autoComplete = true)
    public Map<String, Object> bookAppointment(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        String patientRef = (String) variables.getOrDefault("patientReference", "PAT-UNKNOWN");
        String confirmationCode = "APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[LAB 3] Confirmed appointment booking for patient {}. Code: {}", patientRef, confirmationCode);

        Map<String, Object> result = new HashMap<>();
        result.put("bookingConfirmationCode", confirmationCode);
        result.put("bookingStatus", "CONFIRMED");
        return result;
    }

    @JobWorker(type = "send-appointment-confirmation", autoComplete = true)
    public void sendConfirmation(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.info("[LAB 3] Sent appointment confirmation to patient {} (Code: {})", 
                 variables.get("patientName"), variables.get("bookingConfirmationCode"));
    }

    @JobWorker(type = "dispatch-emergency-care", autoComplete = true)
    public Map<String, Object> dispatchEmergencyCare(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        String emergencyId = "EMRG-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        log.warn("[LAB 3 CRITICAL] Dispatching immediate emergency clinical care for patient {}. Triage Id: {}", 
                 variables.get("patientName"), emergencyId);

        Map<String, Object> result = new HashMap<>();
        result.put("emergencyDispatchId", emergencyId);
        result.put("emergencyStatus", "DISPATCHED");
        return result;
    }

    @JobWorker(type = "notify-invalid-registration", autoComplete = true)
    public void notifyInvalidRegistration(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.warn("[LAB 3] Registration invalid for patient. Details: {}", variables.get("validationRemarks"));
    }

    @JobWorker(type = "notify-waitlist", autoComplete = true)
    public void notifyWaitlist(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.info("[LAB 3] Patient {} placed on appointment waitlist.", variables.get("patientName"));
    }
}

