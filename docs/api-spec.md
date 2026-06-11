# API Specification — Virtual Your Assets (VYRA)

Base URL: `https://<host>/api/v1`

All responses follow the standard wrapper:

```json
{
  "responseStatus": "VYRA-XXX-000",
  "responseMessage": "Human-readable message",
  "data": { ... }
}
```

Authentication uses **Bearer Token** in the `Authorization` header (except endpoints marked as *public*).

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Member](#2-member)
3. [Wallet](#3-wallet)
4. [Transaction](#4-transaction)
5. [Enums](#5-enums)
6. [Error Codes](#6-error-codes)

---

## 1. Authentication

Base path: `/api/v1/auth`

---

### 1.1 Register

`POST /api/v1/auth/register` · *Public*

Register a new member account.

**Request Body**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `firstName` | string | ✅ | Max 20 chars, letters only |
| `lastName` | string | ✅ | Max 20 chars, letters only |
| `email` | string | ✅ | Valid email, max 30 chars |
| `phoneNumber` | string | ✅ | Must start with `628`, 10–15 digits |
| `pin` | string | ✅ | Exactly 6 digits |

**Request Example**

```json
{
  "firstName": "Budi",
  "lastName": "Santoso",
  "email": "budi@example.com",
  "phoneNumber": "628123456789",
  "pin": "123456"
}
```

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-REG-000",
  "responseMessage": "Your registration was successful",
  "data": {
    "memberId": "uuid-string",
    "fullName": "Budi Santoso",
    "email": "budi@example.com",
    "phoneNumber": "628123456789"
  }
}
```

---

### 1.2 Resend OTP

`POST /api/v1/auth/resend-otp` · *Public*

Resend an OTP code to the registered email.

**Request Body**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `email` | string | ✅ | Valid email, max 30 chars |
| `otpType` | `OtpType` | ✅ | See [Enums](#5-enums) |

**Request Example**

```json
{
  "email": "budi@example.com",
  "otpType": "REGISTER"
}
```

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-OTP-001",
  "responseMessage": "If the account exists, a new OTP has been sent",
  "data": null
}
```

---

### 1.3 Verify OTP

`POST /api/v1/auth/verify-otp` · *Public*

Verify an OTP code sent to the member's email.

**Request Body**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `email` | string | ✅ | Valid email, max 30 chars |
| `otpCode` | string | ✅ | OTP code from email |
| `otpType` | `OtpType` | ✅ | See [Enums](#5-enums) |

**Request Example**

```json
{
  "email": "budi@example.com",
  "otpCode": "849201",
  "otpType": "REGISTER"
}
```

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-OTP-000",
  "responseMessage": "OTP verification successful",
  "data": null
}
```

---

### 1.4 Login

`POST /api/v1/auth/login` · *Public*

Authenticate a member using email **or** phone number (not both) and PIN.

**Request Body**

| Field | Type | Required | Validation |
|-------|------|---|------------|
| `email` | string | One of `email`/`phoneNumber` | Valid email, max 30 chars |
| `phoneNumber` | string | One of `email`/`phoneNumber` | Must start with `62`, 10–15 digits |
| `pin` | string | ✅ | Exactly 6 digits |
| `deviceId` | string | ❌ (Coming soon) | Device identifier |
| `deviceName` | string | ❌ (Coming soon) | Device name |
| `ipAddress` | string | ❌ (Coming soon) | Client IP address |

> **Note:** Exactly one of `email` or `phoneNumber` must be provided.

**Request Example**

```json
{
  "email": "budi@example.com",
  "pin": "123456",
  "deviceId": "device-abc",
  "deviceName": "Pixel 8",
  "ipAddress": "180.0.0.1"
}
```

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-LGN-000",
  "responseMessage": "You've successfully logged in",
  "data": {
    "memberId": "uuid-string",
    "phoneNumber": "628123456789",
    "email": "budi@example.com",
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "accessTokenExpiredAt": "2026-06-10T14:00:00",
    "refreshTokenExpiredAt": "2026-06-17T12:00:00"
  }
}
```

---

### 1.5 Refresh Token

`POST /api/v1/auth/refresh-token` · *Public*

Exchange a valid refresh token for a new access token.

**Request Body**

| Field | Type | Required |
|-------|------|----------|
| `refreshToken` | string | ✅ |

**Request Example**

```json
{
  "refreshToken": "eyJ..."
}
```

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-TKN-005",
  "responseMessage": "Access token refreshed successfully",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "accessTokenExpiredAt": "2026-06-10T16:00:00",
    "refreshTokenExpiredAt": "2026-06-17T12:00:00"
  }
}
```

---

### 1.6 Forgot PIN

`POST /api/v1/auth/forgot-pin` · *Public*

Initiate the PIN reset flow by sending an OTP to the member's email.

**Request Body**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `email` | string | ✅ | Valid email, max 30 chars |

**Request Example**

```json
{
  "email": "budi@example.com"
}
```

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-FP-001",
  "responseMessage": "An OTP has been sent",
  "data": null
}
```

---

### 1.7 Reset PIN

`POST /api/v1/auth/reset-pin` · *Public*

Reset the member's PIN after OTP verification.

**Request Body**

| Field | Type | Required |
|-------|------|----------|
| `phoneNumber` | string | ❌ |
| `email` | string | ❌ |
| `newPin` | string | ✅ |

**Request Example**

```json
{
  "email": "budi@example.com",
  "newPin": "654321"
}
```

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-FP-002",
  "responseMessage": "Your PIN has been successfully updated",
  "data": null
}
```

---

### 1.8 Logout

`POST /api/v1/auth/logout` · 🔒 *Requires Auth*

Invalidate the current session and access token.

**Request Body** — None

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-LGT-000",
  "responseMessage": "You've successfully logged out",
  "data": null
}
```

---

## 2. Member

Base path: `/api/v1/member` · 🔒 All endpoints require authentication.

---

### 2.1 Get Member

`GET /api/v1/member/get-member`

Retrieve the authenticated member's profile and financial summary.

**Request Body** — None

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-GMS-000",
  "responseMessage": "Get member detail successful",
  "data": {
    "firstName": "Budi",
    "lastName": "Santoso",
    "fullName": "Budi Santoso",
    "email": "budi@example.com",
    "phoneNumber": "628123456789",
    "totalIncome": 5000000.00,
    "totalExpense": 2000000.00,
    "balance": 3000000.00,
    "growthPercentage": 15.50
  }
}
```

---

### 2.2 Update Profile

`PATCH /api/v1/member/update-profile`

Update the authenticated member's profile information. All fields are optional.

**Request Body**

| Field | Type | Validation |
|-------|------|------------|
| `firstName` | string | Max 20 chars, letters only |
| `lastName` | string | Max 20 chars, letters only |
| `email` | string | Valid email, max 30 chars |
| `phoneNumber` | string | Must start with `62`, 10–15 digits |

**Request Example**

```json
{
  "firstName": "Budi",
  "email": "budi.new@example.com"
}
```

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-UPS-000",
  "responseMessage": "Update member profile successful",
  "data": {
    "firstName": "Budi",
    "lastName": "Santoso",
    "email": "budi.new@example.com",
    "phoneNumber": "628123456789"
  }
}
```

---

### 2.3 Change PIN

`PATCH /api/v1/member/change-pin`

Change the authenticated member's PIN.

**Request Body**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `oldPin` | string | ✅ | Exactly 6 digits |
| `newPin` | string | ✅ | Exactly 6 digits |

**Request Example**

```json
{
  "oldPin": "123456",
  "newPin": "654321"
}
```

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-CPS-000",
  "responseMessage": "Change pin successful",
  "data": null
}
```

---

## 3. Wallet

Base path: `/api/v1/wallet`

---

### 3.1 Create Wallet

`POST /api/v1/wallet/create` · *Public*

Create a new wallet linked to a member account.

**Request Body**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `phoneNumber` | string | ✅ | Must start with `62`, 10–15 digits |
| `memberId` | string | ✅ | Valid member UUID |

**Request Example**

```json
{
  "phoneNumber": "628123456789",
  "memberId": "uuid-string"
}
```

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-WLT-000",
  "responseMessage": "Create wallet was successful",
  "data": {
    "memberId": "uuid-string",
    "phoneNumber": "628123456789",
    "balance": 0.00
  }
}
```

---

### 3.2 Get Member Wallet

`GET /api/v1/wallet/get-wallet/{phoneNumber}` · *Public*

Retrieve wallet details for a member by phone number.

**Path Parameter**

| Parameter | Type | Description |
|-----------|------|-------------|
| `phoneNumber` | string | Member's phone number |

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-GWS-000",
  "responseMessage": "Get member detail successful",
  "data": {
    "phoneNumber": "628123456789",
    "totalIncome": 5000000.00,
    "totalExpense": 2000000.00,
    "balance": 3000000.00
  }
}
```

---

## 4. Transaction

Base path: `/api/v1/transaction` · 🔒 All endpoints require authentication.

---

### 4.1 Get Chart

`GET /api/v1/transaction/get-chart`

Retrieve income and expense chart data for the authenticated member, grouped by period.

**Query Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `period` | string | ✅ | Period granularity (e.g. `WEEKLY`, `MONTHLY`) |

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-GCS-000",
  "responseMessage": "Get chart successful",
  "data": {
    "chart": [
      {
        "label": "Jan",
        "income": 3000000.00,
        "expense": 1500000.00
      },
      {
        "label": "Feb",
        "income": 3500000.00,
        "expense": 1200000.00
      }
    ]
  }
}
```

---

### 4.2 Create Transaction

`POST /api/v1/transaction/create`

Record a new income or expense transaction for the authenticated member.

**Request Body**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `category` | `TransactionCategory` | ❌ | See [Enums](#5-enums) |
| `type` | `TransactionType` | ❌ | `INCOME` or `EXPENSE` |
| `amount` | decimal | ✅ | Must be greater than 0 |
| `transactionDate` | datetime | ❌ | ISO 8601 format |
| `transactionDesc` | string | ❌ | Transaction description |

**Request Example**

```json
{
  "category": "SALARY",
  "type": "INCOME",
  "amount": 5000000.00,
  "transactionDate": "2026-06-10T09:00:00",
  "transactionDesc": "Monthly salary"
}
```

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-CTS-000",
  "responseMessage": "Create transaction successful",
  "data": {
    "transactionId": "uuid-string",
    "phoneNumber": "628123456789",
    "email": "budi@example.com",
    "category": "SALARY",
    "transactionType": "INCOME",
    "amount": 5000000.00,
    "referenceNumber": "TXN-20260610-001",
    "transactionDesc": "Monthly salary",
    "balance": 8000000.00
  }
}
```

---

### 4.3 Get Top Transaction History

`GET /api/v1/transaction/get-top-history`

Retrieve the most recent transactions for the authenticated member (limited list, no pagination).

**Query Parameters** — None

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-GTT-000",
  "responseMessage": "Top transaction history retrieved successfully",
  "data": [
    {
      "transactionId": "uuid-string",
      "referenceNumber": "TXN-20260610-001",
      "type": "INCOME",
      "category": "SALARY",
      "status": "SUCCESS",
      "amount": 5000000.00,
      "transactionDesc": "Monthly salary",
      "transactionDate": "2026-06-10T09:00:00"
    }
  ]
}
```

---

### 4.4 Get Transaction History

`GET /api/v1/transaction/get-history`

Retrieve paginated transaction history for the authenticated member with date range and optional type filter.

**Query Parameters**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `startDate` | string | ✅ | — | Start date (ISO 8601) |
| `endDate` | string | ✅ | — | End date (ISO 8601) |
| `type` | `TransactionType` | ❌ | — | Filter by `INCOME` or `EXPENSE` |
| `page` | integer | ❌ | `0` | Page index (0-based) |
| `size` | integer | ❌ | `10` | Number of items per page |

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-GTH-000",
  "responseMessage": "Transaction history retrieved successfully",
  "data": {
    "content": [
      {
        "transactionId": "uuid-string",
        "referenceNumber": "TXN-20260610-001",
        "type": "INCOME",
        "category": "SALARY",
        "status": "SUCCESS",
        "amount": 5000000.00,
        "transactionDesc": "Monthly salary",
        "transactionDate": "2026-06-10T09:00:00"
      }
    ],
    "totalElements": 25,
    "totalPages": 3,
    "size": 10,
    "number": 0
  }
}
```

---

## 5. Enums

### OtpType

| Value | Description |
|-------|-------------|
| `REGISTER` | OTP for account registration |
| `FORGOT_PIN` | OTP for PIN reset flow |
| `LOGIN` | OTP for login verification |

### TransactionType

| Value | Description |
|-------|-------------|
| `INCOME` | Money coming in |
| `EXPENSE` | Money going out |

### TransactionCategory

**Income Categories**

| Value | Description |
|-------|-------------|
| `SALARY` | Employment salary |
| `PASSIVE_INCOME` | Passive income sources |
| `INVESTMENT` | Investment returns |
| `OTHERS_INCOME` | Other income |

**Expense Categories**

| Value | Description |
|-------|-------------|
| `FOOD_AND_BEVERAGE` | Food & drinks |
| `TRANSPORTATION` | Transportation costs |
| `UTILITIES_AND_BILLS` | Bills and utilities |
| `ENTERTAINMENT` | Entertainment spending |
| `OTHERS_EXPENSE` | Other expenses |

### TransactionStatus

| Value | Description |
|-------|-------------|
| `INQUIRY` | Transaction under review |
| `SUSPECT` | Flagged as suspicious |
| `FAILED` | Transaction failed |
| `DELETED` | Transaction deleted |
| `SUCCESS` | Transaction completed |

---

## 6. Error Codes

### Success Codes

| Code | Message |
|------|---------|
| `VYRA-REG-000` | Your registration was successful |
| `VYRA-WLT-000` | Create wallet was successful |
| `VYRA-LGN-000` | You've successfully logged in |
| `VYRA-LGT-000` | You've successfully logged out |
| `VYRA-FP-001` | An OTP has been sent |
| `VYRA-FP-002` | Your PIN has been successfully updated |
| `VYRA-TKN-001` | Your session has been created |
| `VYRA-TKN-005` | Access token refreshed successfully |
| `VYRA-OTP-000` | OTP verification successful |
| `VYRA-OTP-001` | If the account exists, a new OTP has been sent |
| `VYRA-GMS-000` | Get member detail successful |
| `VYRA-UPS-000` | Update member profile successful |
| `VYRA-GWS-000` | Get member wallet successful |
| `VYRA-GCS-000` | Get chart successful |
| `VYRA-CTS-000` | Create transaction successful |
| `VYRA-GTT-000` | Top transaction history retrieved successfully |
| `VYRA-GTH-000` | Transaction history retrieved successfully |
| `VYRA-CPS-000` | Change pin successful |

### Error Codes

| Code | Message | Description |
|------|---------|-------------|
| `VYRA-REG-001` | Your registration was failed | Registration failure |
| `VYRA-EML-001` | Failed when sent otp via email | Email sending failure |
| `VYRA-TKN-002` | Your session has expired. Please log in again | Access token expired |
| `VYRA-TKN-003` | Invalid refresh token | Refresh token invalid |
| `VYRA-TKN-004` | Your refresh session has expired. Please log in again | Refresh token expired |
| `VYRA-INV-001` | Invalid PIN. Please try again | PIN format invalid |
| `VYRA-INV-002` | Wrong PIN. Please input with correct PIN | PIN mismatch |
| `VYRA-DNF-001` | Invalid account. Please check your account details | Phone number not found |
| `VYRA-DNF-002` | Invalid account. Please check your account details | Email not found |
| `VYRA-DNF-003` | Invalid account. Please check your account details | Member not found |
| `VYRA-DNF-004` | Invalid account. Please check your account status | Member not active |
| `VYRA-DNF-005` | Invalid account. Please check your account status | Member suspended |
| `VYRA-EXS-001` | Your account already exists. Please use a different account | Phone number already exists |
| `VYRA-EXS-002` | Your account already exists. Please use a different account | Email already exists |
| `VYRA-OTP-002` | Invalid or expired OTP | OTP not found |
| `VYRA-OTP-003` | The OTP has expired. Please request a new one | OTP expired |
| `VYRA-OTP-004` | Invalid OTP. Please enter the OTP correctly. Remaining attempts - | Wrong OTP code |
| `VYRA-OTP-005` | You've reached the maximum attempts. Your account has been suspended | Max OTP attempts reached |
| `VYRA-ERR400` | Something went wrong with your request | Bad request / validation error |
| `VYRA-ERR404` | The requested data could not be found | Data not found |
| `VYRA-ERR500` | We are experiencing a system issue. Please try again later | Internal server error |