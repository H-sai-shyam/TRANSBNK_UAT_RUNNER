# TransBNK UAT API Runner

## Base URL
http://localhost:8081

## Usage
Each API can be triggered using a POST call.

### Identity & Validation
curl -X POST http://localhost:8081/api/aadhaar-validation
curl -X POST http://localhost:8081/api/bank-account-validation
curl -X POST http://localhost:8081/api/vpa-validation

### Docuflow
curl -X POST http://localhost:8081/api/docuflow-create
curl -X POST http://localhost:8081/api/docuflow-status
curl -X POST http://localhost:8081/api/docuflow-resend
curl -X POST http://localhost:8081/api/docuflow-cancel

### NACH
curl -X POST http://localhost:8081/api/nach-mandate-create
curl -X POST http://localhost:8081/api/nach-status

### UPI
curl -X POST http://localhost:8081/api/upi-validate-vpa
curl -X POST http://localhost:8081/api/upi-mandate-create

### Payout
curl -X POST http://localhost:8081/api/payout-create

## Output
All responses are saved under:
responses/
