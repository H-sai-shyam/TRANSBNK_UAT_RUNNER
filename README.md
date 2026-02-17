# TransBNK UAT API Runner

## Base URL
http://localhost:8081

## Usage
Each API can be triggered using a POST call.

### Identity & Validation
curl.exe -X POST http://localhost:8081/run/aadhaar-validation  
curl.exe -X POST http://localhost:8081/run/bank-account-validation  
curl.exe -X POST http://localhost:8081/run/vpa-validation  

### Docuflow
curl.exe -X POST http://localhost:8081/run/docuflow-create  
curl.exe -X POST http://localhost:8081/run/docuflow-status  
curl.exe -X POST http://localhost:8081/run/docuflow-resend  
curl.exe -X POST http://localhost:8081/run/docuflow-cancel  

### NACH
curl.exe -X POST http://localhost:8081/run/nach-mandate-create  
curl.exe -X POST http://localhost:8081/run/nach-status  

### UPI
curl.exe -X POST http://localhost:8081/run/upi-validate-vpa  
curl.exe -X POST http://localhost:8081/run/upi-mandate-create  

### Payout
curl.exe -X POST http://localhost:8081/run/payout-create  

## Output
All responses are saved under:
responses/
