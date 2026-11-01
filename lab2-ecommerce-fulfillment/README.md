# LAB 2: End-to-End E-Commerce Order Processing and Fulfilment

## Overview
This directory contains the Camunda 8 implementation for **Lab 2: End-to-End E-Commerce Order Processing and Fulfilment**, featuring:
1. **BPMN Model**: `order_fulfillment.bpmn` (Executable workflow with inventory checking, priority DMN, REST payment gateway, and high-value verification)
2. **DMN Model**: `order_priority.dmn` (Camunda 8 decision table for SLA and priority classification)
3. **Test Scenarios**: Preconfigured test JSON payloads in `test-payloads/`

---

## 1. Process Architecture

```
[Start: Order Placed]
          │
          ▼
[Service Task: Validate Order & Check Inventory] (Worker: validate-and-check-inventory)
          │
          ▼
    <Gateway: Stock Available?> ── (No: stockAvailable = false) ──► [Notify Stock Out] ──► [End: Canceled]
          │ (Yes: stockAvailable = true)
          ▼
[Business Rule Task: Determine Order Priority] (DMN: order-priority-decision)
          │
          ▼
[Service Task: Process Payment via REST] (Connector: io.camunda:http-json:1)
          │
          ▼
    <Gateway: Payment Successful?> ── (No: paymentStatus != "SUCCESS") ──► [Notify Decline] ──► [End: Declined]
          │ (Yes: paymentStatus = "SUCCESS")
          ▼
    <Gateway: Requires High-Value Verification?>
      ├── (Yes: orderTotal >= 1000 or priority = "HIGH")
      │          │
      │          ▼
      │   [User Task: Manual Order Verification] (Candidate Group: sales-ops)
      │          │
      │          ▼
      │    <Gateway: Verification Approved?> ── (No) ──► [Cancel & Refund] ──► [End: Canceled]
      │          │ (Yes)
      │          ▼
      └──► <Gateway: Fulfill Join>
                 │
                 ▼
          [Service Task: Pack and Ship Order] (Worker: fulfill-order)
                 │
                 ▼
          [End: Order Successfully Fulfilled]
```

---

## 2. Process Variables Dictionary

| Variable Name | Type | Direction / Source | Description |
|---|---|---|---|
| `orderId` | String | Input | Unique sales order identifier (e.g. `ORD-1001`) |
| `customerReference` | String | Input | Unique customer identification code |
| `productId` | String | Input | SKU or product identifier |
| `quantity` | Number | Input | Number of items ordered |
| `unitPrice` | Number | Input | Unit price per item in USD |
| `deliveryPreference`| String | Input | Shipping speed (`STANDARD`, `EXPRESS`, `SAME_DAY`) |
| `customerCategory` | String | Input | Customer loyalty tier (`PLATINUM`, `GOLD`, `REGULAR`) |
| `paymentToken` | String | Input | Simulated token / authorization code for payment |
| `orderTotal` | Number | Output (Worker) | Calculated total amount: `quantity * unitPrice` |
| `stockAvailable` | Boolean| Output (Worker) | Whether requested quantity is in inventory |
| `stockQuantity` | Number | Output (Worker) | Available stock in warehouse |
| `priority` | String | Output (DMN) | Priority level: `"HIGH"`, `"MEDIUM"`, `"LOW"` |
| `slaHours` | Number | Output (DMN) | Fulfillment SLA turnaround window in hours |
| `paymentStatus` | String | Output (REST API) | `"SUCCESS"`, `"DECLINED"`, or `"FAILED"` |
| `transactionId` | String | Output (REST API) | Merchant gateway transaction reference ID |
| `orderVerified` | Boolean| User Task Input | Operator approval decision for high-value orders |
| `trackingNumber` | String | Output (Worker) | Carrier tracking code for fulfilled shipment |

---

## 3. DMN Decision Table Specification (`order_priority.dmn`)

- **Hit Policy**: `FIRST`
- **Decision ID**: `order-priority-decision`
- **Inputs**:
  1. `orderTotal` (number)
  2. `customerCategory` (string: "PLATINUM", "GOLD", "REGULAR")
  3. `deliveryPreference` (string: "SAME_DAY", "EXPRESS", "STANDARD")
- **Outputs**:
  1. `priority` (string: "HIGH", "MEDIUM", "LOW")
  2. `slaHours` (number)

### Rule Matrix:
| Rule # | Order Total | Customer Category | Delivery Preference | Priority | SLA (Hours) | Rule Description |
|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **P1** | `-` | `"PLATINUM"` | `-` | `"HIGH"` | 4 | VIP platinum tier customers get highest priority |
| **P2** | `>= 1000` | `-` | `-` | `"HIGH"` | 6 | High-value order priority |
| **P3** | `-` | `-` | `"SAME_DAY"` | `"HIGH"` | 2 | Same-day delivery urgency |
| **P4** | `-` | `"GOLD"` | `-` | `"MEDIUM"` | 12 | Gold tier customer expedited SLA |
| **P5** | `-` | `-` | `"EXPRESS"` | `"MEDIUM"` | 12 | Express delivery speed priority |
| **P6** | `< 200` | `"REGULAR"` | `"STANDARD"` | `"LOW"` | 48 | Standard low-value delivery |
| **P7** | `-` | `-` | `-` | `"MEDIUM"` | 24 | Default standard fulfillment priority |

---

## 4. REST Connector Configuration

- **Connector Template**: Camunda HTTP Outbound Connector (`io.camunda:http-json:1`)
- **HTTP Method**: `POST`
- **URL**: `http://localhost:8080/api/payment/charge`
- **Headers**:
  - `Content-Type`: `application/json`
- **Request Body (FEEL)**:
  ```feel
  ={
    orderId: orderId,
    amount: orderTotal,
    paymentToken: paymentToken,
    testIncident: testIncident,
    simulatePaymentDecline: simulatePaymentDecline
  }
  ```
- **Result Variable**: `paymentResponse`
- **Output Mappings**:
  - `paymentStatus` = `paymentResponse.paymentStatus`
  - `transactionId` = `paymentResponse.transactionId`

---

## 5. Minimum Test Scenarios & Payloads

| Scenario | Payload File | Input Characteristics | Expected Route & Outcome |
|---|---|---|---|
| **1. Stock Available & Success** | `test-payloads/1_success.json` | 1 bag (\$85), Regular, Standard | Stock OK -> DMN (LOW) -> Payment OK (\$85) -> No review needed -> Shipped |
| **2. Insufficient Stock** | `test-payloads/2_out_of_stock.json` | 50 items, StockOut=true | Inventory Worker sets stockAvailable=false -> Notify Out of Stock -> Canceled |
| **3. Payment Declined** | `test-payloads/3_payment_declined.json` | Token: `tok_declined_card_0000` | Stock OK -> DMN (MEDIUM) -> Payment DECLINED -> Notify Decline -> Canceled |
| **4. High-Value Manual Review** | `test-payloads/4_high_value_review.json` | Total \$3,200, Platinum | Stock OK -> DMN (HIGH) -> Payment OK -> User Task Created -> Approved -> Shipped |
| **5. Service Outage & Recovery** | `test-payloads/5_service_failure.json` | `testIncident: true` | Gateway throws 503 outage -> Incident in Operate -> Fixed & Retried |

