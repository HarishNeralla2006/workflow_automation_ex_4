# LAB 3: End-to-End Hospital Appointment and Patient Triage Management

## Overview
This directory contains the Camunda 8 implementation for **Lab 3: End-to-End Hospital Appointment and Patient Triage Management**, featuring:
1. **BPMN Model**: `patient_triage.bpmn` (Executable workflow with patient validation, clinical triage DMN, scheduling REST integration, urgent assessment, and staff waitlist routing)
2. **DMN Model**: `patient_triage_decision.dmn` (Camunda 8 decision table for clinical triage classification)
3. **Test Scenarios**: Preconfigured test JSON payloads in `test-payloads/`

---

## 1. Process Architecture

```
[Start: Patient Appointment Request Received]
              │
              ▼
[Service Task: Validate Patient Registration] (Worker: validate-patient-registration)
              │
              ▼
    <Gateway: Registration Valid?> ── (No: registrationValid = false) ──► [Notify Error] ──► [End: Rejected]
              │ (Yes: registrationValid = true)
              ▼
[Business Rule Task: Evaluate Clinical Triage Rules] (DMN: patient-triage-decision)
              │
              ▼
    <Gateway: Triage Assessment?>
      ├── (Urgent Assessment) ──► [User Task: Immediate Clinical Assessment] ──► [Dispatch Emergency] ──► [End: Urgent Care]
      │
      └── (Routine or Priority Review)
              │
              ▼
       [Service Task: Query Scheduling API for Available Slots] (Connector: io.camunda:http-json:1)
              │
              ▼
       <Gateway: Slots Available?>
         ├── (Yes: hasAvailableSlots = true) ──┐
         │                                     ▼
         │                              <Gateway: Booking Join>
         │                                     │
         │                                     ▼
         │                              [Service Task: Confirm & Book Appointment] (Worker: book-appointment)
         │                                     │
         │                                     ▼
         │                              [Service Task: Send Appointment Confirmation] (Worker: send-confirmation)
         │                                     │
         │                                     ▼
         │                              [End: Appointment Confirmed & Scheduled]
         │
         └── (No: hasAvailableSlots = false)
                 │
                 ▼
          [User Task: Authorized Staff Review (Waitlist & Allocation)] (Candidate Group: hospital-scheduling-staff)
                 │
                 ▼
          <Gateway: Staff Action?>
            ├── (OVERRIDE_BOOKING) ────────────┘
            └── (PLACE_ON_WAITLIST) ──────────► [Service Task: Send Waitlist Notice] ──► [End: Patient Waitlisted]
```

---

## 2. Process Variables Dictionary

| Variable Name | Type | Direction / Source | Description |
|---|---|---|---|
| `patientReference` | String | Input | Synthetic patient identifier (e.g. `PAT-90112`) |
| `patientName` | String | Input | Full synthetic patient name |
| `symptomsSummary` | String | Input | Free-text clinical summary of presenting complaint |
| `appointmentPreference`| String | Input | Requested visit type (`GENERAL`, `SPECIALIST`, `URGENT`) |
| `hasCriticalSymptoms` | Boolean | Input | Clinical red flag indicators (chest pain, dyspnea, acute distress) |
| `painLevel` | Number | Input | Subjective numeric pain score (1 to 10) |
| `durationDays` | Number | Input | Duration of current acute symptoms in days |
| `registrationValid` | Boolean | Output (Worker) | Identity and record format validity check result |
| `departmentRequired` | String | Output (Worker) | Assigned target clinic department |
| `triageCategory` | String | Output (DMN) | Classified urgency: `"Urgent Assessment"`, `"Priority Review"`, `"Routine"` |
| `triageNotes` | String | Output (DMN) | Educational clinical rationale for triage assignment |
| `hasAvailableSlots` | Boolean | Output (REST API) | Whether clinic scheduling API has open appointment slots |
| `assignedSlot` | String | Output (REST API) | ISO 8601 appointment date/time slot |
| `doctorName` | String | Output (REST API) | Attending physician name |
| `emergencyAction` | String | User Task Input | Action taken in urgent assessment (`ADMIT_EMERGENCY`, `EXPEDITE`) |
| `staffAction` | String | User Task Input | Authorized staff decision (`OVERRIDE_BOOKING`, `PLACE_ON_WAITLIST`) |
| `bookingConfirmationCode`| String | Output (Worker)| Unique hospital booking appointment reference |

---

## 3. DMN Decision Table Specification (`patient_triage_decision.dmn`)

- **Hit Policy**: `FIRST`
- **Decision ID**: `patient-triage-decision`
- **Inputs**:
  1. `hasCriticalSymptoms` (boolean)
  2. `painLevel` (number, 1-10)
  3. `durationDays` (number)
- **Outputs**:
  1. `triageCategory` (string: "Urgent Assessment", "Priority Review", "Routine")
  2. `triageNotes` (string)

### Rule Matrix:
| Rule # | Has Critical Symptoms | Pain Level | Symptom Duration | Triage Category | Triage Clinical Notes |
|:---:|:---:|:---:|:---:|:---:|---|
| **T1** | `true` | `-` | `-` | `"Urgent Assessment"` | Critical red-flag symptoms: immediate clinical assessment mandated |
| **T2** | `-` | `>= 8` | `-` | `"Urgent Assessment"` | Severe acute pain (>= 8): fast-track emergency evaluation required |
| **T3** | `-` | `[5..7]` | `-` | `"Priority Review"` | Moderate pain (5-7): expedited specialist or review within 24h |
| **T4** | `-` | `-` | `> 7` | `"Priority Review"` | Persistent symptoms > 7 days warrant priority physician evaluation |
| **T5** | `-` | `< 5` | `<= 7` | `"Routine"` | Mild, short duration symptoms suitable for standard scheduling |
| **T6** | `-` | `-` | `-` | `"Routine"` | Standard routine outpatient triage category applied |

---

## 4. REST Connector Configuration

- **Connector Template**: Camunda HTTP Outbound Connector (`io.camunda:http-json:1`)
- **HTTP Method**: `POST`
- **URL**: `http://localhost:8080/api/appointments/check-slots`
- **Headers**:
  - `Content-Type`: `application/json`
- **Request Body (FEEL)**:
  ```feel
  ={
    patientReference: patientReference,
    departmentRequired: departmentRequired,
    triageCategory: triageCategory,
    appointmentPreference: appointmentPreference,
    simulateNoSlots: simulateNoSlots,
    simulateApiError: simulateApiError
  }
  ```
- **Result Variable**: `schedulingResponse`
- **Output Mappings**:
  - `hasAvailableSlots` = `schedulingResponse.hasAvailableSlots`
  - `assignedSlot` = `schedulingResponse.assignedSlot`
  - `doctorName` = `schedulingResponse.doctorName`

---

## 5. Minimum Test Scenarios & Payloads

| Scenario | Payload File | Input Characteristics | Expected Route & Outcome |
|---|---|---|---|
| **1. Routine Available** | `test-payloads/1_routine_available.json` | Pain: 2, Duration: 4 days, Critical: false | DMN (Routine) -> Slots Found -> Auto Confirmed & Scheduled |
| **2. Priority Staff Review**| `test-payloads/2_priority_review.json` | Pain: 6, Duration: 16 days, Specialist | DMN (Priority Review) -> Slots Found -> Assigned to Orthopedics |
| **3. Urgent Assessment** | `test-payloads/3_urgent_assessment.json` | Critical: true, Pain: 9, Chest tightness | DMN (Urgent Assessment) -> Immediate Human Triage -> Emergency Dispatched |
| **4. No Slots Available** | `test-payloads/4_no_slots.json` | Routine checkup, `simulateNoSlots: true`| No slots found -> User Task (Staff Review) -> Waitlist or Override |
| **5. Scheduling API Error**| `test-payloads/5_api_failure.json` | `simulateApiError: true` | Scheduling API returns 500 -> Incident in Operate -> Fixed & Retried |

