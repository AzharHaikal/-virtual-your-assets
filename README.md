# 🌀 Virtual Your Assets (VYRA) — Backend API

**VYRA Backend API** adalah layanan backend berbasis **Spring Boot** untuk mengelola keuangan virtual pribadi — mencatat pemasukan & pengeluaran, mengelola wallet, dan memberikan insight melalui chart transaksi.

---

## 📋 Daftar Isi

- [Fitur Utama](#-fitur-utama)
- [Tech Stack](#-tech-stack)
- [Arsitektur & Struktur Folder](#-arsitektur--struktur-folder)
- [Database Schema](#-database-schema)
- [Keamanan](#-keamanan)
- [API Endpoints](#-api-endpoints)
- [Konfigurasi Environment](#-konfigurasi-environment)
- [Cara Menjalankan Lokal](#-cara-menjalankan-lokal)
- [Testing](#-testing)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Roadmap](#-roadmap)

---

## 🚀 Fitur Utama

### 🔐 Authentication & Identity
- Registrasi member dengan validasi email & nomor telepon unik
- Login via **email atau nomor telepon** + PIN (6 digit)
- Verifikasi akun menggunakan **OTP 6 digit** yang dikirim via email (async)
- Resend OTP dengan penghapusan OTP lama secara otomatis
- **Forgot PIN** — generate OTP lalu reset PIN baru
- **Change PIN** dengan validasi PIN lama
- **Access Token** (expire **5 menit**) + **Refresh Token** (expire **30 hari**)
- Logout dengan invalidasi token di database

### 💰 Wallet
- Pembuatan wallet otomatis saat registrasi
- Saldo wallet dienkripsi di database menggunakan **AES-256-GCM**
- Pencatatan riwayat perubahan saldo per transaksi (`WalletStatementHistory`)

### 📊 Transaksi
- Catat transaksi **INCOME** atau **EXPENSE** dengan kategori lengkap
- Generate **reference number** unik per transaksi
- Status transaksi: `INQUIRY` → `SUCCESS` (atau `FAILED`, `SUSPECT`, `DELETED`)
- Riwayat transaksi top (tanpa pagination)
- Riwayat transaksi dengan **pagination** + filter tanggal & tipe

### 📈 Chart & Analitik
- Data chart income vs expense berdasarkan periode: `1W`, `1M`, `3M`, `1Y`, `ALL`
- Bucket kosong (hari/minggu/bulan tanpa transaksi) diisi otomatis dengan nilai `0`
- Kalkulasi **growth percentage** berdasarkan saldo bulan sebelumnya

### 👤 Member Profile
- Get profil + ringkasan keuangan (balance, totalIncome, totalExpense, growthPercentage)
- Update profil (nama, email, nomor telepon) dengan sinkronisasi otomatis ke wallet & transaksi
- Riwayat aktivitas member (`MemberActivity`) untuk setiap aksi penting

---

## 🛠 Tech Stack

| Komponen | Teknologi | Versi |
|---|---|---|
| Framework | Spring Boot | 3.5.8 |
| Language | Java | 17 |
| Database | PostgreSQL | — |
| ORM | Spring Data JPA + Hibernate | — |
| Security | Spring Security (Stateless) | 6.5.4 |
| Migration | Flyway | — |
| Email | Spring Boot Mail | — |
| Enkripsi Saldo | AES-256-GCM (`CryptoService`) | — |
| Hash PIN | BCryptPasswordEncoder | — |
| Tracing | Micrometer + Brave | — |
| Utility | Apache Commons Lang3 | 3.14.0 |
| Boilerplate | Lombok | 1.18.32 |
| Build Tool | Maven | 3.9.6 |
| Testing | JUnit 5 + Mockito | — |
| CI/CD | GitLab CI | — |
| Container | Docker (eclipse-temurin:17-jdk-alpine) | — |

---

## 🏗 Arsitektur & Struktur Folder

```
src/main/java/com/vyra/be_virtual_your_assets/
│
├── config/
│   ├── SecurityConfig.java          # Spring Security: stateless, whitelist, filter chain
│   └── WebConfig.java               # CORS configuration
│
├── constant/
│   ├── ApiPath.java                 # Semua path endpoint terpusat
│   ├── ErrorConstant.java           # Semua kode & pesan response (VYRA-XXX-000)
│   ├── MemberActivityEvent.java     # Enum event aktivitas member
│   ├── MemberStatus.java            # ACTIVE, INACTIVE, SUSPENDED, DELETED
│   ├── OtpType.java                 # REGISTER, FORGOT_PIN, LOGIN
│   ├── SecurityConstant.java        # Whitelist URL (public endpoints)
│   └── transaction/
│       ├── TransactionCategory.java # SALARY, FOOD_AND_BEVERAGE, dll
│       ├── TransactionStatus.java   # INQUIRY, SUCCESS, FAILED, dll
│       └── TransactionType.java     # INCOME, EXPENSE
│
├── controller/
│   ├── AuthenticationController.java
│   ├── MemberController.java
│   ├── TransactionController.java
│   └── WalletController.java
│
├── crypto/
│   ├── BalanceEncryptConverter.java # JPA AttributeConverter: encrypt/decrypt balance
│   └── CryptoService.java           # AES-256-GCM encrypt/decrypt
│
├── dto/
│   ├── BaseResponse.java            # Wrapper response: {responseStatus, responseMessage, data}
│   ├── auth/                        # DTO login, register, OTP, token, PIN
│   ├── chart/                       # DTO chart data
│   ├── member/                      # DTO get/update member
│   ├── transaction/                 # DTO create/history transaksi
│   └── wallet/                      # DTO create/get/update wallet
│
├── entity/
│   ├── BaseEntity.java              # createdAt, updatedAt, createdBy, modifiedBy
│   ├── Member.java                  # schema: idp
│   ├── MemberActivity.java          # schema: idp
│   ├── MemberOtp.java               # schema: idp
│   ├── MemberToken.java             # schema: idp
│   ├── MemberWallet.java            # schema: wallet
│   ├── Transaction.java             # schema: transaction_finance
│   ├── WalletStatement.java         # schema: wallet (balance dienkripsi)
│   └── WalletStatementHistory.java  # schema: wallet
│
├── exception/
│   ├── BadRequestException.java
│   └── BusinessException.java
│
├── handler/
│   └── GlobalExceptionHandler.java  # Handler: BusinessException, BadRequest, NPE, Validation
│
├── repository/                      # Spring Data JPA repositories
│
├── security/
│   ├── filter/AuthTokenFilter.java  # Validasi Bearer token per request
│   └── model/CustomUserDetails.java # Menyimpan memberId + accessToken
│
├── service/
│   ├── AuthenticationService.java
│   ├── EmailClient.java             # Kirim OTP via SMTP (async)
│   ├── MemberActivityService.java
│   ├── MemberService.java
│   ├── OtpService.java              # @Async wrapper untuk kirim OTP
│   ├── TransactionService.java
│   ├── ValidationService.java       # Semua validasi data terpusat
│   ├── WalletService.java
│   └── WhatsAppClient.java          # Placeholder (belum aktif)
│
└── util/
    ├── OtpUtil.java                 # Generate OTP 6 digit (SecureRandom), expired 5 menit
    ├── TokenUtil.java               # Generate access/refresh token (SecureRandom + Base64)
    └── TransactionUtil.java         # Generate reference number transaksi
```

---

## 🗄 Database Schema

Aplikasi menggunakan **3 schema PostgreSQL** yang terpisah:

### Schema `idp` — Identity Provider
| Tabel | Deskripsi |
|---|---|
| `member` | Data member: nama, email, phoneNumber, PIN (bcrypt), status |
| `member_otp` | OTP aktif: phoneNumber, otpCode (bcrypt), otpType, attempts, expiredAt |
| `member_token` | Session aktif: accessToken, refreshToken, expiry, deviceId, deviceName, ipAddress |
| `member_activity` | Log setiap aksi penting member (audit trail) |

### Schema `wallet` — Wallet
| Tabel | Deskripsi |
|---|---|
| `member_wallet` | Data wallet member: memberId, phoneNumber, status |
| `wallet_statement` | Saldo aktual: totalIncome, totalExpense, **balance (AES-256-GCM encrypted)** |
| `wallet_statement_history` | Riwayat perubahan saldo per transaksi: previousBalance, currentBalance, income/expense |

### Schema `transaction_finance` — Transaksi
| Tabel | Deskripsi |
|---|---|
| `transaction` | Data transaksi: memberId, phoneNumber, email, category, type, amount, status, referenceNumber |

> ⚠️ **Migrasi database** dikelola oleh **Flyway** secara otomatis saat aplikasi start.

---

## 🔒 Keamanan

### Autentikasi Token
- Token berupa **opaque token** (random Base64, bukan JWT) yang disimpan di database
- Setiap request ke endpoint protected wajib menyertakan header:
  ```
  Authorization: Bearer <accessToken>
  ```
- `AuthTokenFilter` memvalidasi token ke database di setiap request
- Access token expire dalam **5 menit**, refresh token dalam **30 hari**

### Enkripsi & Hashing
| Data | Mekanisme |
|---|---|
| PIN member | BCrypt hashing |
| OTP code | BCrypt hashing (disimpan sebagai hash) |
| Saldo wallet (`balance`) | AES-256-GCM encryption via `CryptoService` |

### OTP Security
- OTP 6 digit, generated via `SecureRandom`
- Berlaku selama **5 menit**
- Maksimum **4 kali percobaan** — jika melebihi, akun otomatis **SUSPENDED**

### Public Endpoints (Whitelist)
Endpoint berikut tidak memerlukan autentikasi:
```
POST /api/v1/auth/register
POST /api/v1/auth/resend-otp
POST /api/v1/auth/verify-otp
POST /api/v1/auth/login
POST /api/v1/auth/refresh-token
POST /api/v1/auth/forgot-pin
POST /api/v1/auth/reset-pin
```

---

## 📡 API Endpoints

Semua response menggunakan format:
```json
{
  "responseStatus": "VYRA-XXX-000",
  "responseMessage": "...",
  "data": { ... }
}
```

### 🔐 Authentication — `/api/v1/auth`

| Method | Endpoint | Auth | Deskripsi |
|---|---|---|---|
| `POST` | `/register` | ❌ | Registrasi member baru |
| `POST` | `/resend-otp` | ❌ | Kirim ulang OTP ke email |
| `POST` | `/verify-otp` | ❌ | Verifikasi OTP |
| `POST` | `/login` | ❌ | Login dengan email/phone + PIN |
| `POST` | `/refresh-token` | ❌ | Perbarui access token |
| `POST` | `/forgot-pin` | ❌ | Request OTP untuk reset PIN |
| `POST` | `/reset-pin` | ❌ | Set PIN baru setelah OTP diverifikasi |
| `POST` | `/logout` | ✅ | Invalidasi session |

### 👤 Member — `/api/v1/member`

| Method | Endpoint | Auth | Deskripsi |
|---|---|---|---|
| `GET` | `/get-member` | ✅ | Get profil + ringkasan keuangan |
| `PATCH` | `/update-profile` | ✅ | Update nama/email/nomor telepon |
| `PATCH` | `/change-pin` | ✅ | Ganti PIN dengan validasi PIN lama |

### 💼 Wallet — `/api/v1/wallet`

| Method | Endpoint | Auth | Deskripsi |
|---|---|---|---|
| `POST` | `/create` | ❌ | Buat wallet (dipanggil otomatis saat register) |
| `GET` | `/get-wallet/{phoneNumber}` | ❌ | Get data wallet berdasarkan nomor telepon |

### 💸 Transaction — `/api/v1/transaction`

| Method | Endpoint | Auth | Deskripsi |
|---|---|---|---|
| `GET` | `/get-chart?period={period}` | ✅ | Data chart income/expense (`1W`,`1M`,`3M`,`1Y`,`ALL`) |
| `POST` | `/create` | ✅ | Catat transaksi baru |
| `GET` | `/get-top-history` | ✅ | Riwayat transaksi terbaru (tanpa pagination) |
| `GET` | `/get-history` | ✅ | Riwayat transaksi dengan pagination & filter |

> Dokumentasi lengkap request/response ada di `API_Specification.md`

---

## ⚙️ Konfigurasi Environment

Buat file `application.yml` (atau `application-local.yml`) dan sesuaikan variabel berikut:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/vyra_db
    username: your_db_user
    password: your_db_password
  jpa:
    hibernate:
      ddl-auto: validate        # Flyway yang handle migrasi
    properties:
      hibernate:
        default_schema: idp     # Schema default
  mail:
    host: smtp.gmail.com
    port: 587
    username: your_email@gmail.com
    password: your_app_password
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

crypto:
  secret-key: your_32_char_aes_key   # Wajib 32 karakter untuk AES-256
```

> 🔑 **`crypto.secret-key`** harus tepat **32 karakter** untuk AES-256-GCM. Simpan sebagai secret di environment production, jangan di-commit ke repository.

---

## 🚀 Cara Menjalankan Lokal

### Prasyarat
- Java 17
- Maven 3.9+
- PostgreSQL (running lokal atau Docker)

### 1. Clone Repository
```bash
git clone https://gitlab.com/your-namespace/be-virtual-your-assets.git
cd be-virtual-your-assets
```

### 2. Siapkan Database PostgreSQL
```sql
CREATE DATABASE vyra_db;
CREATE SCHEMA idp;
CREATE SCHEMA wallet;
CREATE SCHEMA transaction_finance;
```

### 3. Konfigurasi `application.yml`
Sesuaikan koneksi database, mail server, dan `crypto.secret-key` seperti di atas.

### 4. Build & Run
```bash
# Build + jalankan semua unit test
mvn clean package

# Jalankan aplikasi
mvn spring-boot:run

# Atau jalankan JAR langsung
java -jar target/be-virtual-your-assets-0.0.1-SNAPSHOT.jar
```

Aplikasi akan berjalan di `http://localhost:8080`.
Flyway akan otomatis menjalankan migrasi database saat startup.

---

## 🧪 Testing

Unit test menggunakan **JUnit 5** + **Mockito**, tanpa bergantung pada database fisik.

```bash
# Jalankan semua test
mvn test

# Laporan tersedia di
target/surefire-reports/
```

### Coverage Test

| File Test | Modul yang Diuji |
|---|---|
| `AuthenticationServiceTest.java` | Register, Login, OTP, Reset PIN, Logout |
| `MemberServiceTest.java` | Get Member, Update Profile, Change PIN |
| `ValidationServiceTest.java` | Semua validasi data |
| `OtpServiceTest.java` | Generate & kirim OTP |
| `EmailClientTest.java` | Email sending |
| `MemberActivityServiceTest.java` | Pencatatan aktivitas |
| `AuthenticationControllerTest.java` | Controller layer auth |
| `MemberControllerTest.java` | Controller layer member |
| `WalletControllerTest.java` | Controller layer wallet |

---

## 🔄 CI/CD Pipeline

Pipeline GitLab CI terdiri dari **4 stage**:

```
preparation → check-branch → unit-test → code-build
```

| Stage | Job | Image | Deskripsi |
|---|---|---|---|
| `preparation` | `prep-work` | `maven:3.9.6-eclipse-temurin-17` | Validasi Maven tersedia |
| `check-branch` | `branch-check` | `alpine:latest` | Log nama branch aktif |
| `unit-test` | `run-tests` | `maven:3.9.6-eclipse-temurin-17` | Jalankan `mvn test`, simpan Surefire Reports sebagai artifact |
| `code-build` | `build-jar` | `maven:3.9.6-eclipse-temurin-17` | Build JAR (`-DskipTests`), simpan sebagai artifact |

**Rules:** Pipeline hanya berjalan pada branch `main` atau saat `merge_request_event`.

**Cache:** Dependency Maven di-cache di `.m2/repository` untuk mempercepat build.

---

## 🗺 Roadmap

### 🐳 Containerization (Docker)
- Base image: `eclipse-temurin:17-jdk-alpine` (ringan & production-ready)
- Image push otomatis ke Docker Hub / GitLab Container Registry

### ☸️ Kubernetes Deployment
- Deploy otomatis ke cluster K8s via `app-deployment.yaml`
- Environment secret management melalui GitLab CI Variables

### 📲 WhatsApp Notification
- `WhatsAppClient.java` sudah disiapkan sebagai placeholder
- OTP juga dikirim via WhatsApp (saat ini hanya email yang aktif)

### 🔍 Static Code Analysis
- Integrasi SonarQube untuk deteksi bugs, vulnerabilities, dan code smells

---

## 📄 Lisensi

Project ini menggunakan lisensi internal / proprietary.

---

> ✨ **VYRA** — Virtual Your Assets | Backend API dengan fokus pada keamanan, kualitas kode, dan skalabilitas.