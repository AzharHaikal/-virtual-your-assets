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

### Dates & time zones

- Every date-time in a response is a **UTC instant** in ISO-8601 with a trailing `Z`, e.g. `"2026-10-22T16:30:00Z"`. Clients convert it to local time for display.
- Every date-time sent by a client should include its offset, e.g. `"2026-10-22T23:30:00+07:00"`. A value without offset is read in the member's time zone (compatibility with older app versions).
- Each member has an IANA `timezone` (default `Asia/Jakarta`). It decides the member's "today" and the **local date** of each transaction, fixed when the transaction is saved: changing the time zone later never moves past transactions to another day.
- Date filters (`yyyy-MM-dd`) on member endpoints refer to those local dates, both ends inclusive.

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Member](#2-member)
3. [Wallet](#3-wallet)
4. [Transaction](#4-transaction)
4a. [Mood](#4a-mood)
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
| `timezone` | string | ❌ | IANA region id of the device, e.g. `Asia/Jakarta` (default). Invalid → `400 VYRA-INV-003` |

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

Exchange a valid refresh token for a new access token. The refresh token is rotated. Refused (and the token deleted) when the member is no longer `ACTIVE` (suspended, deleted, not verified) with `VYRA-DNF-004`.

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

**Precondition:** a `FORGOT_PIN` OTP for this member must have been successfully verified with `POST /auth/verify-otp` (`otpType: FORGOT_PIN`) within the last 5 minutes. The verification is single-use: a successful reset consumes it and revokes every active session of the member. Without it the call fails with `VYRA-OTP-002` (no verified OTP) or `VYRA-OTP-003` (reset window expired). Only `ACTIVE` members can reset a PIN (`VYRA-DNF-004`).

**Request Body**

| Field | Type | Required |
|-------|------|----------|
| `phoneNumber` | string | One of `email`/`phoneNumber` | Must start with `62`, 10–15 digits |
| `email` | string | One of `email`/`phoneNumber` | Valid email, max 30 chars |
| `newPin` | string | ✅ | Exactly 6 digits |

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
    "timezone": "Asia/Jakarta",
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

**Changing the e-mail address or phone number requires the current PIN** (`pin`), because OTPs are delivered to those contact details. A missing or wrong PIN is rejected with `VYRA-INV-002` and nothing is changed. Name-only updates do not need a PIN. Re-sending the member's current e-mail / phone is not treated as a change.

**Contact changes are confirmed with an OTP.** Names are saved immediately. A new e-mail / phone is only recorded as a *pending change*; the response is `VYRA-UPS-001` with `contactChangePending: true` and `verificationSentTo` (masked e-mail), and a 6-digit OTP is e-mailed — to the **new** address when the e-mail changes, otherwise to the current address (OTPs are delivered by e-mail only). The member keeps the old e-mail / phone until `POST /member/update-profile/verify` succeeds. See 2.3a / 2.3b below.

**Request Body**

| Field | Type | Validation |
|-------|------|------------|
| `firstName` | string | Max 20 chars, letters only |
| `lastName` | string | Max 20 chars, letters only |
| `email` | string | Valid email, max 30 chars |
| `phoneNumber` | string | Must start with `62`, 10–15 digits |
| `pin` | string | Exactly 6 digits; **required** when `email` or `phoneNumber` changes |
| `timezone` | string | IANA region id, e.g. `Asia/Makassar`; no PIN needed. Only new transactions use the new zone. Invalid → `400 VYRA-INV-003` |

**Request Example**

```json
{
  "firstName": "Budi",
  "email": "budi.new@example.com",
  "pin": "123456"
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

### 2.2a Verify contact change

`POST /api/v1/member/update-profile/verify` · 🔒 *Requires Auth*

Confirms the pending e-mail / phone change with the OTP. On success the change is applied, wallet and transaction records are synchronised and **every other session of the member is ended** (the calling session stays signed in).

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `otpCode` | string | ✅ | Exactly 6 digits |

Response `VYRA-UPS-000` with `firstName`, `lastName`, `email`, `phoneNumber`. Errors: `VYRA-OTP-002` (nothing pending / no OTP), `VYRA-OTP-003` (expired), `VYRA-OTP-004` (wrong code, remaining attempts), `VYRA-OTP-005` (4th wrong code, verification locked 10 minutes), `VYRA-LCK-002` (429, locked), `VYRA-EXS-003/004` (address / number registered by someone else in the meantime).

### 2.2b Resend contact-change OTP

`POST /api/v1/member/update-profile/resend-otp` · 🔒 *Requires Auth* · no body

Sends a new OTP for the pending change (at most one per 60 seconds: `VYRA-RTL-002`, HTTP 429). Response `VYRA-UPS-001` with `verificationSentTo`.

### 2.3 Change PIN

`PATCH /api/v1/member/change-pin`

Change the authenticated member's PIN. On success every other session of the member (other devices, stolen tokens) is ended; the session making this call stays signed in. Wrong old PINs count towards the PIN lock-out (HTTP 429 `VYRA-LCK-001`).

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

`POST /api/v1/wallet/create` · 🔒 *Requires Auth*

Create a wallet linked to the **authenticated** member. `memberId` and `phoneNumber` must belong to the caller, otherwise the call is answered like an unknown account (`VYRA-DNF-003`). A member can only have one wallet; a second call fails with `VYRA-WLT-001`. The wallet is normally created automatically at registration. The body is JSON.

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

`GET /api/v1/wallet/get-wallet/{phoneNumber}` · 🔒 *Requires Auth*

Retrieve the wallet of the **authenticated** member. `phoneNumber` must be the caller's own number; any other number is answered like an unknown account (`VYRA-DNF-003`).

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

**Header (recommended):** `Idempotency-Key: <unique id per form submission>` (max 100 chars). Sending the same request again with the same key (e.g. a retry after a timeout) returns the first response and creates **no** duplicate. The same key with a different body → `409 VYRA-IDM-002`; the same key while the first request is still running → `409 VYRA-IDM-003`. Keys expire after 24 hours. Without the header the request is processed normally.

**Request Body**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `categoryId` | string | ❌ | Category id from [4.7](#47-finance-categories) (built-in or own); must match `type`. Preferred over `category` |
| `category` | `TransactionCategory` | ❌ | Legacy built-in category (older apps). Neither given → "Other" of the type |
| `type` | `TransactionType` | ✅ | `INCOME` or `EXPENSE` |
| `amount` | decimal | ✅ | Must be greater than 0 |
| `transactionDate` | string | ❌ | ISO-8601 with offset (`2026-06-10T09:00:00+07:00`). Without offset it is read in the member's time zone; empty = now. Invalid → `400 VYRA-INV-004` |
| `transactionDesc` | string | ❌ | Transaction description |
| `paymentMethod` | string | ❌ | `CASH`, `TRANSFER`, `E_WALLET`, `CARD`, `OTHER` |

**Request Example**

```json
{
  "category": "SALARY",
  "type": "INCOME",
  "amount": 5000000.00,
  "transactionDate": "2026-06-10T09:00:00+07:00",
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
| `startDate` | string | ✅ | — | First local date (`yyyy-MM-dd`, member's time zone) |
| `endDate` | string | ✅ | — | Last local date (`yyyy-MM-dd`, inclusive) |
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
        "transactionDate": "2026-06-10T02:00:00Z",
        "localDate": "2026-06-10"
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

### 4.5 Update Transaction

`PATCH /api/v1/transactions/{transactionId}`

Edits one of the member's own transactions. Every field is optional (`null` keeps the current value). The wallet is corrected by the difference and the change is added to the wallet history as an `UPDATE` entry (the original entry is never rewritten). Only `SUCCESS` transactions can be edited.

**Request Body**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `type` | `TransactionType` | ❌ | `INCOME` or `EXPENSE` |
| `categoryId` | string | ❌ | Category id (built-in or own), must match the (new) type |
| `category` | `TransactionCategory` | ❌ | Legacy built-in category |
| `amount` | decimal | ❌ | Greater than 0; the sign follows `type` |
| `transactionDate` | string | ❌ | Same rules as on create; the local date is recomputed |
| `transactionDesc` | string | ❌ | Max 255 chars |

**Request Example**

```json
{ "amount": 75000, "transactionDesc": "Makan siang kantor" }
```

**Response `200 OK`**

```json
{
  "responseStatus": "VYRA-UTS-000",
  "responseMessage": "Transaction updated successfully",
  "data": {
    "transaction": {
      "transactionId": "uuid-string",
      "referenceNumber": "VYRA20261022...",
      "type": "EXPENSE",
      "category": "FOOD_AND_BEVERAGE",
      "status": "SUCCESS",
      "amount": -75000.00,
      "transactionDesc": "Makan siang kantor",
      "transactionDate": "2026-10-22T05:00:00Z",
      "localDate": "2026-10-22"
    },
    "balance": 725000.00,
    "totalIncome": 1000000.00,
    "totalExpense": -275000.00
  }
}
```

**Errors:** `404 VYRA-TRX-404` (not found, deleted, or another member's), `400 VYRA-TRX-001` (not a `SUCCESS` transaction), `400` validation errors.

---

### 4.6 Delete Transaction

`DELETE /api/v1/transactions/{transactionId}`

Soft-deletes one of the member's own transactions (`status = DELETED`, `deletedAt` set) and removes it from the wallet with a `DELETE` history entry. Deleted transactions disappear from history, charts and summaries. **Idempotent:** deleting an already deleted transaction returns `200` and changes nothing.

**Response `200 OK`** — same shape as 4.5 with `responseStatus: "VYRA-DTS-000"` and `transaction.status: "DELETED"`.

**Errors:** `404 VYRA-TRX-404` (not found or another member's).

---

### 4.8 List Transactions (search, filter, sort)

`GET /api/v1/transactions`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `type` | `TransactionType` | — | `INCOME` / `EXPENSE` |
| `categoryId` | string | — | One category |
| `from`, `to` | date (`yyyy-MM-dd`) | — | Member's local dates, inclusive |
| `q` | string | — | Searches description and reference number (case-insensitive) |
| `sort` | string | `date,desc` | `date` or `amount` (absolute value), `,asc` / `,desc` |
| `page`, `size` | int | `0`, `20` | `size` max 100 |

Response `VYRA-TRX-LST-000`, `data` = `{ content: [history item…], pageNumber, pageSize, totalElements, totalPages, last }`. Items as in 4.4 plus `categoryId/Name/Icon/Color` and `paymentMethod`. Deleted transactions are never listed. Errors: `400 VYRA-INV-005` (from after to), `400 VYRA-INV-006` (sort).

---

### 4.9 Finance Summary

`GET /api/v1/finance/summary?from=2026-10-01&to=2026-10-31`

`from` / `to` optional (default: the current month in the member's time zone). Only `SUCCESS` transactions count; all amounts are positive.

```json
{
  "responseStatus": "VYRA-FIN-SUM-000",
  "data": {
    "from": "2026-10-01", "to": "2026-10-31",
    "totalIncome": 5000000.00, "totalExpense": 400000.00, "net": 4600000.00,
    "previousFrom": "2026-09-01", "previousTo": "2026-09-30",
    "previousIncome": 4800000.00, "previousExpense": 380000.00,
    "expenseByCategory": [
      { "categoryId": "default-FOOD_AND_BEVERAGE", "code": "FOOD_AND_BEVERAGE", "name": "Food & Beverage",
        "icon": "restaurant", "color": "#EF4444", "amount": 300000.00, "percentage": 75.0 }
    ],
    "incomeByCategory": [ ... ]
  }
}
```

The previous period is the previous calendar month when the range is a whole month, otherwise the same number of days just before `from`. Categories are sorted biggest first.

---

### 4.7 Finance Categories

Built-in categories (shared, `custom: false`, id `default-<CODE>`) plus the member's own (`custom: true`). Errors for another member's category look like a missing one.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/finance/categories?type=INCOME\|EXPENSE` | Built-in first, then own; `type` optional |
| `POST` | `/api/v1/finance/categories` | Create own category → `201` |
| `PATCH` | `/api/v1/finance/categories/{categoryId}` | Rename / change icon or color (own only; the type cannot change) |
| `DELETE` | `/api/v1/finance/categories/{categoryId}` | Soft delete (own only); transactions keep showing its name |

**Create body:** `name` (≤ 50, unique per type), `type`, `icon` (key such as `local_cafe`, pattern `^[a-z][a-z0-9_]{0,39}$`), `color` (`#RRGGBB`). Max 50 own categories.

**Category object**

```json
{ "id": "default-FOOD_AND_BEVERAGE", "code": "FOOD_AND_BEVERAGE", "name": "Food & Beverage",
  "type": "EXPENSE", "icon": "restaurant", "color": "#EF4444", "custom": false }
```

Transactions in history responses now also carry `categoryId`, `categoryName`, `categoryIcon` and `categoryColor`; `category` keeps the legacy code (`OTHERS_*` for a custom category).

**Errors:** `404 VYRA-CAT-404`, `409 VYRA-CAT-002` (name exists), `400 VYRA-CAT-003` (limit), on transactions `400 VYRA-CAT-001` (type mismatch) / `400 VYRA-CAT-404` (unknown or deleted category).

---

## 4a. Mood

One check-in per member and **local day** (the member's time zone). Only the member can read it — there is no admin endpoint. The journal (`note`) is encrypted at rest and never logged.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/mood-entries?from=yyyy-MM-dd&to=yyyy-MM-dd` | Entries of the range, newest first (default: the last 30 days up to today) |
| `GET` | `/api/v1/mood-entries/today` | Today's entry; `data: null` when there is none yet |
| `PUT` | `/api/v1/mood-entries/{localDate}` | Create or replace the entry of that day |
| `DELETE` | `/api/v1/mood-entries/{localDate}` | Soft delete of that day's entry |
| `GET` | `/api/v1/mood-tags` | Built-in tags first, then own |
| `POST` | `/api/v1/mood-tags` | Create own tag (`name` ≤ 40, unique per member, max 30) → `201` |
| `DELETE` | `/api/v1/mood-tags/{tagId}` | Soft delete of an own tag; past entries keep showing it |

**PUT body**

```json
{ "moodScore": 4, "note": "Hari ini produktif", "tagIds": ["mood-tag-GOOD_SLEEP", "mood-tag-WORK"] }
```

`moodScore` 1 (very bad) … 5 (very good), required. `note` ≤ 2000 characters, optional. `tagIds` at most 10. Saving the same day twice updates that day's entry (also under concurrent requests). A day after today in the member's zone is rejected.

**Entry object**

```json
{ "id": "c3c8…", "localDate": "2026-10-23", "moodScore": 4, "note": "Hari ini produktif",
  "tags": [ { "id": "mood-tag-WORK", "code": "WORK", "name": "Work", "custom": false } ],
  "checkedInAt": "2026-10-23T02:41:50Z" }
```

Built-in tag codes: `GOOD_SLEEP`, `FRIENDS`, `FAMILY`, `WORK`, `EXERCISE`, `GOOD_FOOD`, `HOBBY`, `ACHIEVEMENT`, `STRESS`, `TIRED`, `SICK`, `MONEY` (id `mood-tag-<CODE>`). Clients translate built-in names by `code`.

**Errors:** `400 VYRA-MOD-001` (future day), `404 VYRA-MOD-404` (no entry that day), `400 VYRA-MTG-404` / `404 VYRA-MTG-404` (unknown tag on save / delete), `409 VYRA-MTG-002` (name exists), `400 VYRA-MTG-003` (limit).

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

> **Lock-out and rate limiting (HTTP 429).** `429` responses use the normal envelope and carry a `Retry-After` header (seconds). Brute-force-prone endpoints (`/auth/login`, `/auth/verify-otp`, `/auth/resend-otp`, `/auth/forgot-pin`, `/auth/reset-pin`, `/auth/register`, `/auth/refresh-token`, `/admin/auth/login`, `/admin/auth/refresh-token`) are limited per client address. After 5 consecutive wrong PINs (login, change PIN, e-mail/phone change) a member is locked for 15 minutes; after 4 wrong OTPs OTP verification is locked for 10 minutes; admins are locked for 15 minutes after 5 wrong passwords. A new OTP e-mail of the same type can be requested once per 60 seconds.

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
| `VYRA-INV-003` | Invalid time zone. Use a region id such as Asia/Jakarta | `timezone` is not an IANA region id |
| `VYRA-INV-004` | Invalid date. Use ISO-8601, e.g. 2026-10-22T08:30:00+07:00 | `transactionDate` cannot be parsed |
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
| `VYRA-OTP-005` | You've reached the maximum attempts. Verification is temporarily locked. Please try again in 10 minutes. | Max OTP attempts reached (HTTP 400) |
| `VYRA-LCK-001` | Too many failed attempts. Your account is temporarily locked. Please try again in N minutes. | HTTP 429 – PIN / admin password lock-out |
| `VYRA-LCK-002` | Too many invalid codes. Verification is temporarily locked. Please try again in N minutes. | HTTP 429 – OTP lock |
| `VYRA-RTL-001` | Too many requests. Please try again in N seconds. | HTTP 429 – per-address rate limit |
| `VYRA-UPS-001` | A verification code has been sent. Enter it to confirm the change… | Contact change staged (HTTP 200) |
| `VYRA-ERR503` | The system is busy right now. Please try again in a moment | HTTP 503 – database lock / statement timeout |
| `VYRA-RTL-002` | A code was sent recently. Please wait before requesting another one. Try again in N seconds. | HTTP 429 – OTP e-mail cool-down |
| `VYRA-ERR400` | Something went wrong with your request | Bad request / validation error |
| `VYRA-ERR404` | The requested data could not be found | Data not found |
| `VYRA-ERR500` | We are experiencing a system issue. Please try again later | Internal server error |