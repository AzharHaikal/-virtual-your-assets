# Authentication API Specification

**Application**: Virtual Your Assets (VYRA)
**Module**: Authentication
**Base Path**: `/api/v1/auth`

Dokumentasi ini mendefinisikan kontrak API untuk modul **Authentication**. Seluruh response menggunakan wrapper `BaseResponse<T>` dan telah disesuaikan dengan **AuthenticationService**, **ErrorConstant**, serta skenario error handling pada backend.

---

## Response Wrapper (BaseResponse)

### Success Response

```json
{
  "responseStatus": "VYRA-XXX-000",
  "responseMessage": "Success message",
  "data": {}
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

## 1. Register

Mendaftarkan member baru dengan status awal **INACTIVE**. Setelah pendaftaran berhasil, sistem akan mengirimkan OTP ke email untuk proses verifikasi.

* **Endpoint**: `POST /register`
* **Bisnis Logic**:
    * Mandatory field (firstName, LastName, email, phoneNumber, pin)
    * Email dan nomor telepon harus unik
    * PIN di-hash menggunakan **BCrypt**
    * Generate OTP 6 digit (hashed)
    * Mencatat activity log `ATTEMPT_REGISTER` dan `SUCCESS_REGISTER`

### Request Body

| Field         | Type   | Constraint                             | Description          |
|---------------|--------|----------------------------------------|----------------------|
| `firstName`   | String | Not Blank, Max 20                      | Nama depan member    |
| `lastName`    | String | Not Blank, Max 20                      | Nama belakang member |
| `email`       | String | Not Blank, Valid Email, Max 30         | Email unik           |
| `phoneNumber` | String | Not Blank, Pattern `62xx`, 10–15 digit | Nomor telepon unik   |
| `pin`         | String | Not Blank, Pattern `\\d{6}`            | PIN 6 digit angka    |

### JSON Body
```json
{
  "firstName": "Azhar",
  "lastName": "Haikal",
  "email": "azhar@mail.com",
  "phoneNumber": "62812345678",
  "pin": "123456"
}
```

### Success Response — `200 OK`

```json
{
  "responseStatus": "VYRA-REG-000",
  "responseMessage": "Registration completed successfully",
  "data": {
    "memberId": "uuid-string",
    "email": "azhar@mail.com",
    "phoneNumber": "62812345678"
  }
}
```

### Error Scenarios

| Condition                     | HTTP Status | Response Status | Response Message                                           |
|-------------------------------|-------------|-----------------|------------------------------------------------------------|
| DTO validation gagal          | 400         | VYRA-ERR400     | Pesan validasi field pertama                               |
| Nomor telepon sudah terdaftar | 400         | VYRA-EXS-001    | Account already exists                                     |
| Email sudah terdaftar         | 400         | VYRA-EXS-002    | Account already exists                                     |
| Internal server error         | 500         | VYRA-ERR500    | We are experiencing a system issue. Please try again later |

---

## 2. Resend OTP

Mengirim ulang OTP jika kode sebelumnya tidak diterima atau sudah kadaluarsa.

* **Endpoint**: `POST /resend-otp`
* **Logic**:

    * Menghapus OTP lama berdasarkan email dan tipe OTP
    * Generate dan kirim OTP baru

### Request Body

```json
{
  "email": "azhar@mail.com",
  "otpType": "REGISTER"
}
```

### Success Response — `200 OK`

```json
{
  "responseStatus": "VYRA-OTP-001",
  "responseMessage": "If the account exists, an OTP has been sent",
  "data": null
}
```

---

## 3. Verify OTP

Memvalidasi kode OTP untuk aktivasi akun atau proses verifikasi lainnya.

* **Endpoint**: `POST /verify-otp`
* **Logic**:

    * Validasi OTP dan expiry time
    * Jika OTP salah, jumlah attempt bertambah
    * Jika attempt mencapai **3 kali**, data pendaftaran member akan dihapus otomatis

### Request Body

```json
{
  "email": "azhar@mail.com",
  "otpCode": "123456",
  "otpType": "REGISTER"
}
```

### Success Response — `200 OK`

```json
{
  "responseStatus": "VYRA-OTP-000",
  "responseMessage": "OTP verified successfully",
  "data": null
}
```

### Error Scenarios

| Condition                     | Response Status | Response Message                                                               |
|-------------------------------|-----------------|--------------------------------------------------------------------------------|
| OTP expired                   | VYRA-OTP-003    | Invalid or expired OTP                                                         |
| OTP salah (< 3x)              | VYRA-OTP-004    | Invalid or expired OTP                                                         |
| OTP salah (3x / max attempts) | VYRA-OTP-005    | Maximum verification attempts reached. Your registration data has been cleared |

---

## 4. Login

Melakukan autentikasi menggunakan **Email** atau **Nomor Telepon** dan **PIN**.

* **Endpoint**: `POST /login`
* **Logic**:

    * Pencarian member berdasarkan identifier
    * Status akun harus **ACTIVE**
    * Validasi PIN
    * Generate Access Token (UUID tanpa dash, 32 karakter)
    * Token berlaku selama **12 jam**

### Request Body

```json
{
  "identifier": "62812345678",
  "pin": "123456"
}
```

### Success Response — `200 OK`

```json
{
  "responseStatus": "VYRA-LGN-000",
  "responseMessage": "Login successful",
  "data": {
    "phoneNumber": "62812345678",
    "email": "azhar@mail.com",
    "token": "32charsuuidwithoutdash"
  }
}
```

### Error Scenarios

| Condition                             | Response Status | Response Message                                   |
|---------------------------------------|-----------------|----------------------------------------------------|
| Member tidak ditemukan                | VYRA-DNF-003    | Invalid account. Please check your account details |
| Akun belum diverifikasi / tidak aktif | VYRA-DNF-004    | Invalid account. Please check your account status  |
| PIN salah                             | VYRA-INV-001    | Invalid PIN. Please try again                      |

---

## Notes

* Semua endpoint bersifat **idempotent** untuk OTP resend
* OTP dan PIN **tidak pernah disimpan dalam bentuk plaintext**
* Error code mengikuti standar `ErrorConstant` pada backend