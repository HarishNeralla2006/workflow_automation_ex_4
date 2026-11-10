#!/usr/bin/env python3
"""
Zero-dependency Mock REST Services for Camunda 8 Labs 1 to 3
Runs on http://localhost:8080 to test Camunda HTTP Connectors immediately.
"""
from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import uuid
import datetime

class CamundaMockHandler(BaseHTTPRequestHandler):

    def _send_json(self, status_code, data):
        self.send_response(status_code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
        self.wfile.write(json.dumps(data, indent=2).encode('utf-8'))

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length)
        try:
            req_data = json.loads(body.decode('utf-8')) if body else {}
        except Exception:
            req_data = {}

        print(f"\n[REQUEST RECEIVED] POST {self.path}")
        print(f"Payload: {json.dumps(req_data)}")

        # -------------------------------------------------------------
        # LAB 1: Credit Assessment & Credit Check API
        # -------------------------------------------------------------
        if self.path.startswith('/api/credit-assessment') or self.path.startswith('/api/credit-check'):
            if req_data.get('simulateApiError') is True:
                print(" -> Simulating Credit Assessment Bureau Outage (500 Internal Error)")
                self._send_json(500, {"error": "Credit Bureau Service Unavailable / Timeout", "status": 500})
                return

            customer_name = req_data.get('customerName') or req_data.get('applicantName', 'Applicant')
            monthly_income = float(req_data.get('monthlyIncome', 3000))
            existing_emi = float(req_data.get('existingEmi', 0))
            loan_amount = float(req_data.get('loanAmount') or req_data.get('requestedAmount', 10000))
            tenure_months = int(req_data.get('tenureMonths', 36))

            # Illustrative annual interest rate: 12%
            monthly_rate = 0.12 / 12.0
            if loan_amount > 0 and tenure_months > 0:
                emi = loan_amount * monthly_rate * ((1 + monthly_rate) ** tenure_months) / (((1 + monthly_rate) ** tenure_months) - 1)
            else:
                emi = 0.0
            total_emi = existing_emi + emi
            emi_ratio = (total_emi / monthly_income) * 100.0 if monthly_income > 0 else 100.0

            if 'creditScore' in req_data and req_data['creditScore'] is not None:
                credit_score = int(req_data['creditScore'])
            elif 'demoScore' in req_data and req_data['demoScore'] is not None:
                credit_score = int(req_data['demoScore'])
            else:
                score = 900
                if emi_ratio > 50:
                    score -= 250
                elif emi_ratio > 40:
                    score -= 150
                elif emi_ratio > 30:
                    score -= 75
                if monthly_income < 25000:
                    score -= 100
                credit_score = max(300, min(900, score))

            tier = "EXCELLENT" if credit_score >= 700 else "GOOD" if credit_score >= 650 else "FAIR" if credit_score >= 580 else "POOR"
            
            if credit_score >= 700 and emi_ratio <= 40:
                decision = "ELIGIBLE"
                eligible = True
            elif credit_score >= 600 and emi_ratio <= 50:
                decision = "MANUAL_REVIEW"
                eligible = False
            else:
                decision = "NOT_ELIGIBLE"
                eligible = False

            resp = {
                "customerName": customer_name,
                "applicantName": customer_name,
                "demoScore": credit_score,
                "creditScore": credit_score,
                "creditTier": tier,
                "estimatedEmi": round(emi, 2),
                "totalMonthlyEmi": round(total_emi, 2),
                "emiRatio": round(emi_ratio, 2),
                "eligible": eligible,
                "decision": decision,
                "message": "Meets demonstration eligibility rules" if eligible else "Requires manual review" if decision == "MANUAL_REVIEW" else "Does not meet demonstration eligibility rules",
                "status": "SUCCESS",
                "assessmentTimestamp": datetime.datetime.now(datetime.timezone.utc).isoformat()
            }
            print(f" -> Credit Response: {resp}")
            self._send_json(200, resp)
            return

        # -------------------------------------------------------------
        # LAB 2: Payment Gateway API
        # -------------------------------------------------------------
        elif self.path.startswith('/api/payment/charge'):
            token = str(req_data.get('paymentToken', ''))
            if req_data.get('testIncident') is True or 'fail' in token or 'error' in token:
                print(" -> Simulating Payment Gateway Service Outage (503 Service Unavailable)")
                self._send_json(503, {"error": "Payment Gateway Network Timeout", "status": 503})
                return

            is_decline = req_data.get('simulatePaymentDecline') is True or 'declined' in token
            order_id = req_data.get('orderId', 'ORD-UNKNOWN')
            amount = float(req_data.get('amount', 0.0))

            if is_decline:
                resp = {
                    "orderId": order_id,
                    "amount": amount,
                    "paymentStatus": "DECLINED",
                    "declineReason": "Insufficient funds or invalid payment token",
                    "transactionId": None
                }
            else:
                resp = {
                    "orderId": order_id,
                    "amount": amount,
                    "paymentStatus": "SUCCESS",
                    "transactionId": f"TXN-{uuid.uuid4().hex[:10].upper()}",
                    "chargedAt": datetime.datetime.now(datetime.timezone.utc).isoformat()
                }
            print(f" -> Payment Response: {resp}")
            self._send_json(200, resp)
            return

        # -------------------------------------------------------------
        # LAB 3: Hospital Scheduling API
        # -------------------------------------------------------------
        elif self.path.startswith('/api/appointments/check-slots'):
            if req_data.get('simulateApiError') is True:
                print(" -> Simulating Hospital Scheduling Core API Outage (500 Error)")
                self._send_json(500, {"error": "Hospital Scheduling Database Lock / Timeout", "status": 500})
                return

            patient_ref = req_data.get('patientReference', 'PAT-UNKNOWN')
            department = req_data.get('departmentRequired', 'General Practice')
            urgency = req_data.get('triageCategory', 'Routine')
            no_slots = req_data.get('simulateNoSlots') is True

            if no_slots:
                resp = {
                    "patientReference": patient_ref,
                    "department": department,
                    "hasAvailableSlots": False,
                    "assignedSlot": None,
                    "doctorName": None,
                    "message": "No suitable clinic appointments available in requested window."
                }
            else:
                resp = {
                    "patientReference": patient_ref,
                    "department": department,
                    "hasAvailableSlots": True,
                    "assignedSlot": "2026-11-05T10:30:00Z",
                    "doctorName": "Dr. Marcus Vance" if urgency == "Urgent Assessment" else "Dr. Sarah Jenkins",
                    "clinicRoom": "Clinic Wing 3, Suite B",
                    "message": "Slot provisionally reserved pending workflow confirmation."
                }
            print(f" -> Scheduling Response: {resp}")
            self._send_json(200, resp)
            return

        else:
            self._send_json(404, {"error": f"Endpoint {self.path} not found"})

    def do_GET(self):
        if self.path == '/' or self.path == '/health':
            self._send_json(200, {
                "status": "UP",
                "message": "Camunda 8 Mock REST Services are running on port 8080",
                "endpoints": [
                    "POST /api/credit-assessment",
                    "POST /api/payment/charge",
                    "POST /api/appointments/check-slots"
                ]
            })
        else:
            self._send_json(404, {"error": f"Endpoint {self.path} not found"})

def run(port=8080):
    server_address = ('', port)
    httpd = HTTPServer(server_address, CamundaMockHandler)
    print(f"============================================================")
    print(f" Camunda 8 Mock REST Services running on port {port}")
    print(f" Endpoints:")
    print(f"  - Lab 1: POST http://localhost:{port}/api/credit-assessment")
    print(f"  - Lab 2: POST http://localhost:{port}/api/payment/charge")
    print(f"  - Lab 3: POST http://localhost:{port}/api/appointments/check-slots")
    print(f" Press Ctrl+C to terminate.")
    print(f"============================================================")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down mock server.")
        httpd.server_close()

if __name__ == '__main__':
    run()

