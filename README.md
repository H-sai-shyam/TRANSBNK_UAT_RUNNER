# TransBNK UAT API Runner

## Base URL
http://localhost:8081

## Wrapper Flow (Protective Layer)
1) Generate a wrapper token (stored in `master_transactions` in your UAT DB).
2) Call any TransBNK API with `Authorization: Bearer <token>`.
3) The API hits TrustHub, stores full request/response logs in the DB, and returns a **customized (trimmed)** JSON response for the frontend (currently implemented for `bank-account-validation` only).

### 1) Generate Token
**PowerShell (Windows)**

> Important: `transaction_timestamp` must be within the last 15 minutes, otherwise validation will fail as `expired`.

```powershell
curl.exe --% -X POST "http://localhost:8081/api/v1/token/generate" -H "Content-Type: application/json" -d "{\"transaction_userid\":\"317161\",\"transaction_merchantid\":\"446442\",\"client_Id\":\"5e06f31d-d298-11f0-96ff-4201c0a81e02\",\"transaction_timestamp\":\"2026-02-27 09:10:00\",\"processor\":\"TRANSBANK\"}"
```

Validate token:

```powershell
curl.exe --% -X POST "http://localhost:8081/api/v1/token/validate" -H "Content-Type: application/json" -d "{\"token\":\"<token-from-generate>\",\"clientId\":\"5e06f31d-d298-11f0-96ff-4201c0a81e02\",\"processor\":\"TRANSBANK\"}"
```

**Bash / Linux / macOS**

```bash
curl -X POST "http://localhost:8081/api/v1/token/generate" -H "Content-Type: application/json" -d '{"transaction_userid":"317161","transaction_merchantid":"446442","client_Id":"5e06f31d-d298-11f0-96ff-4201c0a81e02","transaction_timestamp":"2026-02-27 09:10:00","processor":"TRANSBANK"}'
```

### 2) Call TransBNK APIs (Token Required)
All endpoints under `/api/**` require `Authorization: Bearer <token>` (except `/api/v1/token/**`).

Example (Bank Account Validation):
```bash
curl -X POST http://localhost:8081/api/bank-account-validation -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"requestId\":\"REQ-0001\",\"custName\":\"Test User\",\"custIfsc\":\"ICIC0000000\",\"custAcctNo\":\"000000000000\",\"trackingRefNo\":\"TRACK-0001\",\"txnType\":\"IMPS\"}"
```

### Available APIs
All of these are called as `POST /api/{apiName}` (token required):

**Identity & Validation**
- `aadhaar-validation`
- `bank-account-validation`
- `vpa-validation`

**Docuflow**
- `docuflow-create`
- `docuflow-status`
- `docuflow-resend`
- `docuflow-cancel`

**NACH**
- `nach-mandate-create`
- `nach-status`

**UPI**
- `upi-validate-vpa`
- `upi-mandate-create`

**Payout**
- `payout-create`

## DB Tables Required
- `master_transactions` (wrapper tokens)
- `bank_validation_audit` (wrapper audit)
- TransBNK logging table(s) like `bank_account_validation_log` (already used by the module)

## Notes
- Copy `src/main/resources/application.example.yaml` to `src/main/resources/application.yaml` for local runs (it is git-ignored).
- `bank-account-validation` logs are inserted into `bank_account_validation_log`.
