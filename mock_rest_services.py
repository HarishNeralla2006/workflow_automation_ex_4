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
        # LAB 1: Credit Assessment API
        # -------------------------------------------------------------
        if self.path.startswith('/api/credit-assessment'):
            if req_data.get('simulateApiError') is True:
                print(" -> Simulating Credit Assessment Bureau Outage (500 Internal Error)")
                self._send_json(500, {"error": "Credit Bureau Service Unavailable / Timeout", "status": 500})
                return

            monthly_income = float(req_data.get('monthlyIncome', 3000))
            if 'creditScore' in req_data and req_data['creditScore'] is not None:
                credit_score = int(req_data['creditScore'])
            elif monthly_income >= 5000:
                credit_score = 740
            elif monthly_income >= 3500:
                credit_score = 630
            else:
                credit_score = 540

            tier = "EXCELLENT" if credit_score >= 700 else "GOOD" if credit_score >= 650 else "FAIR" if credit_score >= 580 else "POOR"
            resp = {
                "applicantName": req_data.get('applicantName', 'Applicant'),
                "creditScore": credit_score,
                "creditTier": tier,
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

