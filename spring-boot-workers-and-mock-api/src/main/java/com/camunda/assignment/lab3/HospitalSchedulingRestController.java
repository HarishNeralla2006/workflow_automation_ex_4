package com.camunda.assignment.lab3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class HospitalSchedulingRestController {

    private static final Logger log = LoggerFactory.getLogger(HospitalSchedulingRestController.class);

    @PostMapping("/check-slots")
    public ResponseEntity<Map<String, Object>> checkSlots(@RequestBody Map<String, Object> request) {
        log.info("[LAB 3 REST API] Received Slot Query Request: {}", request);

        // Scenario 5: Scheduling API Failure / Incident
        Object simulateError = request.get("simulateApiError");
        if (Boolean.TRUE.equals(simulateError) || "true".equalsIgnoreCase(String.valueOf(simulateError))) {
            log.error("[LAB 3 REST API] Simulating Hospital Scheduling System Database Lock / 500 Outage!");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Hospital Scheduling Core API Unresponsive");
        }

        String patientRef = (String) request.getOrDefault("patientReference", "PAT-UNKNOWN");
        String department = (String) request.getOrDefault("departmentRequired", "General Medicine");
        String urgency = (String) request.getOrDefault("triageCategory", "Routine");

        // Scenario 4: No slots available
        Object simulateNoSlots = request.get("simulateNoSlots");
        boolean noSlots = Boolean.TRUE.equals(simulateNoSlots) || "true".equalsIgnoreCase(String.valueOf(simulateNoSlots));

        Map<String, Object> response = new HashMap<>();
        response.put("patientReference", patientRef);
        response.put("department", department);

        if (noSlots) {
            log.warn("[LAB 3 REST API] No available slots in department {} for patient {}", department, patientRef);
            response.put("hasAvailableSlots", false);
            response.put("assignedSlot", null);
            response.put("doctorName", null);
            response.put("message", "No suitable clinic appointments available in requested window.");
        } else {
            String slotTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String doctor = "Urgent Assessment".equals(urgency) ? "Dr. Marcus Vance (Emergency Attending)" : "Dr. Sarah Jenkins (Senior Consultant)";
            
            log.info("[LAB 3 REST API] Slot available: {} with {}", slotTime, doctor);
            response.put("hasAvailableSlots", true);
            response.put("assignedSlot", slotTime);
            response.put("doctorName", doctor);
            response.put("clinicRoom", "Clinic Wing 3, Suite B");
            response.put("message", "Slot provisionally reserved pending workflow confirmation.");
        }

        return ResponseEntity.ok(response);
    }
}

