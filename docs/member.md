# Member API Specification

**Application**: Virtual Your Assets (VYRA)
**Module**: Member
**Base Path**: `/api/v1/member`

Dokumentasi ini mendefinisikan kontrak API untuk manajemen akun **Member**, khususnya alur **pemulihan akses (Forgot PIN / Reset PIN)**. Seluruh response menggunakan wrapper `BaseResponse<T>` dan mengikuti standar `ErrorConstant`.

---

## Response Wrapper (BaseResponse)

### Success Response

```json
{
  "responseStatus": "VYRA-XXX-000",
  "responseMessage": "Success message",
  "data": null
}
```

### Error Response

```json
{
  "responseStatus": "VYRA-ERR-XXX",
  "responseMessage": "Error message",
  "data": null
}
```

---

## 1. Forgot Password (Request OTP)

Digunakan ketika member lupa PIN. Sistem akan memvalidasi nomor telepon dan mengirimkan kode OTP untuk proses verifikasi.

* **Endpoint**: `POST /forgot-password`
* **Logic**:

    * Mencari member berdasarkan nomor telepon
    * Generate OTP baru dengan tipe `FORGOT_PASSWORD`
    * Mencatat audit log:

        * `ATTEMPT_GENERATE_FORGOT_PASSWORD`
        * `SUCCESS_GENERATE_FORGOT_PASSWORD`
    * Mengirimkan OTP (saat ini melalui system log, rencana pengembangan via SMS / WhatsApp Gateway)

### Request Body

| Field         | Type   | Constraint | Description             |
| ------------- | ------ | ---------- | ----------------------- |
| `phoneNumber` | String | Not Blank  | Nomor telepon terdaftar |

```json
{
  "phoneNumber": "62812345678"
}
```

### Success Response — `200 OK`

```json
{
  "responseStatus": "VYRA-FP-001",
  "responseMessage": "Success generate, an OTP has been sent",
  "data": null
}
```

### Error Scenarios

| Condition                | Response Status | Response Message                                   |
| ------------------------ | --------------- | -------------------------------------------------- |
| Member tidak ditemukan   | VYRA-DNF-003    | Invalid account. Please check your account details |
| Request body tidak valid | VYRA-ERR400     | Invalid request                                    |

---

## 2. Reset Password (Update PIN)

Digunakan untuk memperbarui PIN member setelah proses verifikasi OTP berhasil dilakukan.

* **Endpoint**: `POST /reset-password`
* **Logic**:

    * Validasi keberadaan member berdasarkan nomor telepon
    * Enkripsi PIN baru menggunakan **BCrypt**
    * Update field `updatedAt`
    * Mencatat audit log:

        * `ATTEMPT_RESET_PASSWORD`
        * `SUCCESS_RESET_PASSWORD`

### Request Body

| Field         | Type   | Constraint      | Description              |
| ------------- | ------ | --------------- | ------------------------ |
| `phoneNumber` | String | Not Blank       | Nomor telepon member     |
| `newPin`      | String | Pattern `\d{6}` | PIN baru (6 digit angka) |

```json
{
  "phoneNumber": "62812345678",
  "newPin": "654321"
}
```

### Success Response — `200 OK`

```json
{
  "responseStatus": "VYRA-FP-002",
  "responseMessage": "PIN has been reset successfully",
  "data": null
}
```

### Error Scenarios

| Condition                | Response Status | Response Message                                   |
| ------------------------ | --------------- | -------------------------------------------------- |
| Member tidak ditemukan   | VYRA-DNF-003    | Invalid account. Please check your account details |
| Request body tidak valid | VYRA-ERR400     | Invalid request                                    |
| Internal server error    | VYRA-ERR500     | An unexpected error occurred                       |

---

## Error Codes Reference

| Code         | Message                                            | Description                       |
| ------------ | -------------------------------------------------- | --------------------------------- |
| VYRA-FP-001  | Success generate, an OTP has been sent             | Berhasil membuat request lupa PIN |
| VYRA-FP-002  | PIN has been reset successfully                    | Berhasil memperbarui PIN          |
| VYRA-DNF-003 | Invalid account. Please check your account details | Member tidak ditemukan            |
| VYRA-ERR400  | Invalid request                                    | Validasi body request gagal       |
| VYRA-ERR500  | An unexpected error occurred                       | Kesalahan sistem internal         |
