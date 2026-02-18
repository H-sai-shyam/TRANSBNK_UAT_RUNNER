# TransBNK UAT API Runner

## Base URL
http://localhost:8081

## Usage
Each API can be triggered using a POST call.

### Identity & Validation
curl -X POST http://localhost:8081/run/aadhaar-validation  
curl -X POST http://localhost:8081/run/bank-account-validation  
curl -X POST http://localhost:8081/run/vpa-validation  

### Docuflow
curl -X POST http://localhost:8081/run/docuflow-create  
curl -X POST http://localhost:8081/run/docuflow-status  
curl -X POST http://localhost:8081/run/docuflow-resend  
curl -X POST http://localhost:8081/run/docuflow-cancel  

### NACH
curl -X POST http://localhost:8081/run/nach-mandate-create  
curl -X POST http://localhost:8081/run/nach-status  

### UPI
curl -X POST http://localhost:8081/run/upi-validate-vpa  
curl -X POST http://localhost:8081/run/upi-mandate-create  

### Payout
curl -X POST http://localhost:8081/run/payout-create  

## Output
All responses are saved under:
responses/
