package com.camunda.assignment.lab3;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PatientValidationWorker {

    private static final Logger log = LoggerFactory.getLogger(PatientValidationWorker.class);

    @JobWorker(type = "validate-patient-registration", autoComplete = true)
    public Map<String, Object> validatePatientRegistration(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.info("[LAB 3 WORKER] Validating patient registration for Job Key: {}, Variables: {}", 
                 job.getKey(), variables);

        // Incident simulation
        Object testIncident = variables.get("testIncident");
        if (Boolean.TRUE.equals(testIncident) || "true".equalsIgnoreCase(String.valueOf(testIncident))) {
            log.error("[LAB 3 WORKER] Simulating unhandled worker exception in PatientValidationWorker!");
            throw new RuntimeException("Simulated patient registration validation fault for incident recovery test.");
        }

        String patientRef = (String) variables.get("patientReference");
        String patientName = (String) variables.get("patientName");
        String appointmentPreference = (String) variables.getOrDefault("appointmentPreference", "GENERAL");

        boolean isValid = true;
        StringBuilder errorMsg = new StringBuilder();

        if (patientRef == null || !patientRef.startsWith("PAT-")) {
            isValid = false;
            errorMsg.append("Invalid or missing synthetic patient reference. ");
        }
        if (patientName == null || patientName.trim().isEmpty()) {
            isValid = false;
            errorMsg.append("Patient name is required. ");
        }

        String departmentRequired = (String) variables.getOrDefault("departmentRequired", 
            "SPECIALIST".equalsIgnoreCase(appointmentPreference) ? "Internal Medicine" : "General Practice");

        Map<String, Object> result = new HashMap<>();
        result.put("registrationValid", isValid);
        result.put("departmentRequired", departmentRequired);
        result.put("validationRemarks", isValid ? "Synthetic registration verified." : errorMsg.toString());

        log.info("[LAB 3 WORKER] Registration validation completed: valid={}, department={}", isValid, departmentRequired);
        return result;
    }
}

