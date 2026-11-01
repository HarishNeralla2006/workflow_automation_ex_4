# LAB 1: End-to-End Loan Approval Automation

## Overview
This directory contains the complete Camunda 8 implementation for **Lab 1: End-to-End Loan Approval Automation**, including:
1. **BPMN Model**: `loan_approval.bpmn` (Executable Zeebe workflow)
2. **DMN Model**: `loan_approval_decision.dmn` (Camunda 8 decision table)
3. **Test Scenarios**: Preconfigured JSON test payloads in `test-payloads/`

---

## 1. Process Architecture

```
[Start: Loan Application Received]
              │
              ▼
[Service Task: Validate Applicant Information] (Worker: validate-applicant)
              │
              ▼
    <Gateway: Is Valid?> ─────────── (No: isValid = false) ──► [Notify Failure] ──► [End: Rejected]
              │ (Yes: isValid = true)
              ▼
[Service Task: Assess Credit via REST API] (Connector: io.camunda:http-json:1)
              │
              ▼
[Business Rule Task: Evaluate Loan Decision Rules] (DMN: loan-approval-decision)
              │
              ▼
    <Gateway: Decision Route>
      ├── (Approved) ──────────────────────────┐
      │                                        ▼
      ├── (Manual Review) ──► [User Task: Manual Review] ──► <Gateway: Outcome?> ──► (Approved) ──► [Disburse Loan] ──► [End: Approved]
      │                                                                          └──► (Rejected) ──► [Send Rejection] ──► [End: Rejected]
      └── (Rejected) ─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Process Variables Dictionary

| Variable Name | Type | Direction / Source | Description |
|---|---|---|---|
| `applicantName` | String | Input | Full name of the applicant |
| `applicantEmail` | String | Input | Contact email address |
| `monthlyIncome` | Number | Input | Gross monthly income in USD |
| `requestedAmount` | Number | Input | Desired loan principal in USD |
| `employmentStatus` | String | Input | Employment state (`EMPLOYED`, `SELF_EMPLOYED`, etc.) |
| `isValid` | Boolean | Output (Worker) | Whether input format and threshold checks passed |
| `validationMessage` | String | Output (Worker) | Details of validation or reasons for invalid status |
| `creditScore` | Number | Output (REST API) | Credit bureau synthetic score (300 to 850) |
| `creditTier` | String | Output (REST API) | Tier rating (`EXCELLENT`, `GOOD`, `FAIR`, `POOR`) |
| `decision` | String | Output (DMN) | Decision outcome: `"Approved"`, `"Rejected"`, or `"Manual Review"` |
| `decisionReason` | String | Output (DMN) | Educational justification string from DMN rule table |
| `manualReviewOutcome` | String | User Task Input | Officer resolution: `"APPROVED"` or `"REJECTED"` |
| `reviewNotes` | String | User Task Input | Officer justification recorded in Tasklist |
| `disbursementReference`| String | Output (Worker) | Unique disbursement transaction reference |

---

## 3. DMN Decision Table Specification (`loan_approval_decision.dmn`)

- **Hit Policy**: `FIRST`
- **Decision ID**: `loan-approval-decision`
- **Inputs**:
  1. `creditScore` (number)
  2. `requestedAmount` (number)
  3. `monthlyIncome` (number)
- **Outputs**:
  1. `decision` (string: "Approved", "Rejected", "Manual Review")
  2. `reason` (string)

### Rule Matrix:
| Rule # | Credit Score | Requested Amount | Monthly Income | Decision | Reason |
|:---:|:---:|:---:|:---:|:---:|---|
| **1** | `< 580` | `-` | `-` | `"Rejected"` | Credit score below minimum eligible threshold of 580 |
| **2** | `>= 700` | `<= monthlyIncome * 10` | `-` | `"Approved"` | Excellent credit score and loan amount within 10x monthly income |
| **3** | `>= 650` | `<= monthlyIncome * 5` | `-` | `"Approved"` | Good credit score and loan amount within 5x monthly income |
| **4** | `[580..649]` | `-` | `-` | `"Manual Review"` | Fair credit score requires risk officer review and assessment |
| **5** | `>= 650` | `> monthlyIncome * 10`| `-` | `"Manual Review"` | Requested loan amount exceeds 10x monthly income - risk review required |
| **6** | `-` | `-` | `-` | `"Manual Review"` | Borderline criteria require manual loan officer determination |

---

## 4. REST Connector Configuration

- **Connector Template**: Camunda HTTP Outbound Connector (`io.camunda:http-json:1`)
- **HTTP Method**: `POST`
- **URL**: `http://localhost:8080/api/credit-assessment`
- **Headers**:
  - `Content-Type`: `application/json`
- **Request Body (FEEL)**:
  ```feel
  ={
    applicantName: applicantName,
    monthlyIncome: monthlyIncome,
    creditScore: creditScore,
    simulateApiError: simulateApiError
  }
  ```
- **Result Variable**: `creditResponse`
- **Output Mappings**:
  - `creditScore` = `creditResponse.creditScore`
  - `creditTier` = `creditResponse.creditTier`

---

## 5. Minimum Test Scenarios & Payloads

| Scenario | Payload File | Input Characteristics | Expected Route & Outcome |
|---|---|---|---|
| **1. Approved** | `test-payloads/1_approved.json` | Credit: 750, Income: \$6,000, Loan: \$25,000 | Validation -> Credit API (750) -> DMN Rule 2 -> Auto-Approved -> Disbursed |
| **2. Rejected** | `test-payloads/2_rejected.json` | Credit: 520, Income: \$3,200, Loan: \$18,000 | Validation -> Credit API (520) -> DMN Rule 1 -> Auto-Rejected -> Notice Sent |
| **3. Manual Review**| `test-payloads/3_manual_review.json` | Credit: 625, Income: \$4,500, Loan: \$30,000 | Validation -> Credit API (625) -> DMN Rule 4 -> Tasklist User Task Created |
| **4. API Error / Incident** | `test-payloads/4_credit_api_failure.json`| `simulateApiError: true` | Credit API returns 500 -> Incident in Operate -> Fix/Retry |
| **5. Worker Failure & Recovery** | `test-payloads/5_worker_failure_recovery.json` | `testIncident: true` | Worker throws exception -> Incident in Operate -> Edit variable / Retry |

---

## 6. How to Deploy and Run in Camunda 8

1. **Deploy Models**:
   - Open Camunda 8 Desktop Modeler or Camunda Web Modeler.
   - Open `loan_approval_decision.dmn` and click **Deploy**.
   - Open `loan_approval.bpmn` and click **Deploy**.
2. **Start Services & Workers**:
   - Run the mock REST API and Spring Boot worker (see root README).
3. **Start Process Instance**:
   - In Modeler or Operate, click **Start Instance** and paste the JSON from `test-payloads/1_approved.json`.
4. **Complete User Tasks in Tasklist**:
   - For Scenario 3, navigate to **Camunda Tasklist** (`http://localhost:8081` or SaaS console).
   - Claim task **Manual Loan Review**, set `manualReviewOutcome` to `"APPROVED"` or `"REJECTED"`, and click **Complete Task**.
5. **Inspect and Resolve Incidents in Operate**:
   - For Scenarios 4 and 5, open **Camunda Operate** (`http://localhost:8081/operate`).
   - Find the incident under active instances, inspect stack trace, resolve root cause, and click **Retry**.

