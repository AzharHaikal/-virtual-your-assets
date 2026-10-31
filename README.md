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

> ⚠️ **Migrasi database** dikelola sepenuhnya oleh **Flyway** (`src/main/resources/db/migration`) saat aplikasi start. Hibernate berjalan dengan `ddl-auto: validate`: aplikasi **gagal start** bila entity tidak cocok dengan skema.

### Aturan Migrasi
- Setiap perubahan skema = file baru `V{n}__deskripsi_singkat.sql`. File yang sudah ter-apply **tidak boleh diedit** (checksum Flyway akan gagal).
- Tulis DDL biasa (tanpa pola kondisional `to_regclass` seperti V2–V4). `V5__baseline_schema.sql` membuat semua tabel V1 pada database baru.
- Index & constraint didefinisikan di migrasi. `@Table(indexes = ...)` di entity hanya dokumentasi.
- Menambah nilai enum yang disimpan sebagai string (mis. `OtpType`, `TransactionStatus`) wajib disertai migrasi yang membuat ulang check constraint kolom tersebut.
- Job CI `migration-check` membangun skema dari nol dan menjalankan `SchemaMigrationIT`.

### Zona Waktu
Semua perhitungan waktu lewat `TimeService` agar member di zona berbeda tidak saling konflik.

| Aturan | Implementasi |
|---|---|
| Semua momen disimpan sebagai UTC | Kolom `timestamptz`; JVM, sesi JDBC & Hibernate dipaksa UTC (`main()`, `-Duser.timezone=UTC`, `hibernate.jdbc.time_zone`). Setiap `LocalDateTime` di kode = UTC |
| Response JSON jelas zonanya | `LocalDateTime` dikirim sebagai `2026-10-22T16:30:00Z` (`TimeConfig`) |
| Zona per member | `member.timezone` (IANA, default `Asia/Jakarta`), dikirim saat register / ubah profil |
| Hari milik member | `transaction.local_date` dihitung dari zona member **saat ditulis** dan tidak dihitung ulang bila zona diganti. Chart, filter history, growth % dan laporan harian memakai `local_date` |
| Input dari app | `transactionDate` ISO-8601 dengan offset; tanpa offset dibaca di zona member (app versi lama) |
| Filter tanggal admin (audit, dashboard "hari ini") | Hari di `vyra.time.default-zone` (env `VYRA_DEFAULT_TIMEZONE`, default `Asia/Jakarta`) |

Migrasi `V7__timezone_support.sql` mengonversi data lama (database development yang dibuat sebelum V7): kolom audit dianggap ditulis di `VYRA_LEGACY_SERVER_TIMEZONE` (default `Asia/Jakarta`, zona laptop development), `transaction_date` di `VYRA_LEGACY_CLIENT_TIMEZONE` (default `Asia/Jakarta`, jam HP). Database baru tidak terpengaruh. Jika suatu database lama ditulis oleh server UTC, set `VYRA_LEGACY_SERVER_TIMEZONE=UTC` sebelum V7 pertama kali jalan.

---

## 🔒 Keamanan

### Autentikasi Token
- Token berupa **opaque token** (random Base64, bukan JWT) yang disimpan di database
- Setiap request ke endpoint protected wajib menyertakan header:
  ```
  Authorization: Bearer <accessToken>
  ```
- `AuthTokenFilter` memvalidasi token ke database di setiap request **dan memastikan member masih `ACTIVE`** (member yang di-suspend/dihapus langsung ditolak, `401`)
- Refresh token juga ditolak (dan dihapus) untuk member/admin yang tidak `ACTIVE`
- Sesi dicabut otomatis: admin suspend/hapus member → semua token member dihapus; reset PIN → semua sesi dihapus; ganti PIN → semua sesi **lain** dihapus (sesi yang sedang dipakai tetap)
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
- Maksimum **4 kali percobaan** per OTP — jika melebihi, OTP dihapus dan verifikasi/kirim ulang OTP **dikunci 10 menit** (akun tidak lagi di-SUSPEND)
- OTP e-mail dengan tipe yang sama hanya bisa diminta **1x per 60 detik** per member
- OTP `FORGOT_PIN` harus diverifikasi (`verify-otp`) sebelum `/auth/reset-pin`; verifikasi berlaku 5 menit, sekali pakai, dan semua sesi member dicabut setelah reset

### Lock-out & Rate Limiting
| Proteksi | Aturan | Respons |
|---|---|---|
| Lock PIN member | 5 PIN salah berturut-turut (login, change PIN, ubah email/telepon) → kunci 15 menit | HTTP 429 `VYRA-LCK-001` |
| Lock OTP member | 4 OTP salah → kunci 10 menit | HTTP 400 `VYRA-OTP-005`, lalu 429 `VYRA-LCK-002` |
| Lock login admin | 5 password salah berturut-turut → kunci 15 menit (audit `LOGIN_LOCKED`) | HTTP 429 `VYRA-LCK-001` |
| Cool-down OTP | 1 e-mail OTP per tipe per 60 detik | HTTP 429 `VYRA-RTL-002` |
| Rate limit per IP | login 10/mnt, verify-otp 10/mnt, resend-otp + forgot-pin 5/mnt, reset-pin 5/mnt, register 10/10 mnt, refresh 30/mnt, admin login 10/mnt | HTTP 429 `VYRA-RTL-001` + `Retry-After` |

- Lock otomatis berakhir; admin yang meng-aktifkan kembali member/admin (atau reset password admin) langsung membuka kunci.
- Semua batas dapat diatur lewat env: `VYRA_PIN_MAX_ATTEMPTS`, `VYRA_PIN_LOCK_MINUTES`, `VYRA_OTP_LOCK_MINUTES`, `VYRA_OTP_RESEND_COOLDOWN_SECONDS`, `VYRA_ADMIN_MAX_ATTEMPTS`, `VYRA_ADMIN_LOCK_MINUTES`, `VYRA_RATE_LIMIT_ENABLED`.
- Rate limit disimpan di **PostgreSQL** (`idp.rate_limit_bucket`, satu upsert atomik per request) sehingga **dibagi antar instance**; bila database gagal dipakai, request dihitung secara lokal (in-memory). `VYRA_RATE_LIMIT_STORE=memory` memaksa hitung lokal. Lock PIN/OTP/admin juga tersimpan di database.
- Alamat klien diambil dari `X-Forwarded-For` (`FORWARD_HEADERS_STRATEGY=native`, default) — pastikan backend berada di belakang reverse proxy tepercaya.
- Timeout database: `DB_LOCK_TIMEOUT_MS` (default 5000) dan `DB_STATEMENT_TIMEOUT_MS` (default 30000) dipasang di setiap koneksi; request yang menunggu baris terkunci terlalu lama dijawab `503 VYRA-ERR503` (bukan menggantung).
- Ubah e-mail / nomor telepon: PIN **dan** OTP. OTP dikirim ke e-mail baru (atau e-mail saat ini bila hanya telepon yang berubah); perubahan baru berlaku setelah `POST /member/update-profile/verify`, dan sesi lain dihentikan.

### Akses Data Keuangan Member (Admin Portal)
Data keuangan **per member** (daftar transaksi member, saldo, total income/expense member, laporan transaksi per baris, recent transactions) hanya bisa dilihat oleh **SUPER_ADMIN**, dan hanya selama setting `privacy.member_transaction_detail.enabled` bernilai `true` (default).

| Role | Detail transaksi & saldo member | Data agregat (dashboard summary, chart, distribusi kategori, laporan income/expense per hari) |
|---|---|---|
| SUPER_ADMIN | ✅ bila setting aktif · `403 VYRA-ADM-AUT-002` bila nonaktif | ✅ |
| ADMINISTRATOR / OPERATOR / VIEWER | ❌ `403 VYRA-ADM-AUT-001` · saldo di daftar/detail member & member report bernilai `null` | ✅ |

- Permission: `MEMBER_TRANSACTION_VIEW` (hanya SUPER_ADMIN). Saat setting nonaktif, permission ini tidak dikirim di `/admin/me` & login, sehingga portal menyembunyikan menunya.
- Setting dikelola lewat `GET /api/v1/admin/settings` (`SETTINGS_VIEW`) dan `PUT /api/v1/admin/settings/{configKey}` body `{"value":"true|false"}` (`SETTINGS_MANAGE`, hanya SUPER_ADMIN).
- Audit: `VIEW_MEMBER_TRANSACTIONS` (member yang dilihat / periode laporan) dan `UPDATE_SYSTEM_CONFIG`.

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
| `PATCH` | `/update-profile` | ✅ | Update nama/email/nomor telepon (ubah email/telepon wajib kirim `pin`) |
| `PATCH` | `/change-pin` | ✅ | Ganti PIN dengan validasi PIN lama |

### 💼 Wallet — `/api/v1/wallet`

| Method | Endpoint | Auth | Deskripsi |
|---|---|---|---|
| `POST` | `/create` | ✅ | Buat wallet milik member yang login (otomatis saat register; satu wallet per member) |
| `GET` | `/get-wallet/{phoneNumber}` | ✅ | Get wallet milik sendiri (nomor telepon harus milik member yang login) |

### 💸 Transaction — `/api/v1/transaction`

| Method | Endpoint | Auth | Deskripsi |
|---|---|---|---|
| `GET` | `/get-chart?period={period}` | ✅ | Data chart income/expense (`1W`,`1M`,`3M`,`1Y`,`ALL`) |
| `POST` | `/create` | ✅ | Catat transaksi baru |
| `GET` | `/get-top-history` | ✅ | Riwayat transaksi terbaru (tanpa pagination) |
| `GET` | `/get-history` | ✅ | Riwayat transaksi dengan pagination & filter |

> Dokumentasi lengkap request/response ada di [`docs/api-spec.md`](docs/api-spec.md).
> OpenAPI/Swagger UI tersedia di `/swagger-ui.html` (spec: `/v3/api-docs`) bila `VYRA_API_DOCS_ENABLED=true` — aktifkan hanya di lokal/staging. Saat nonaktif path tersebut mengembalikan 404.

### 💾 Backup & Restore
Backup harian (`scripts/db-backup.sh`, retensi ≥ 7 hari) dan uji restore bulanan (`scripts/db-restore-check.sh`). Prosedur lengkap: [`docs/backup-restore.md`](docs/backup-restore.md).

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
```
Schema dan tabel dibuat otomatis oleh Flyway saat aplikasi pertama kali start.

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

### Real-database tests
`WalletConcurrencyIT` fires parallel transactions at one wallet and checks that no balance update is lost (the wallet row is locked with `SELECT ... FOR UPDATE`). `SecurityHardeningIT` checks the cross-instance rate limiter, the database lock timeout and the OTP-verified e-mail / phone change (run it with `DB_LOCK_TIMEOUT_MS=1500`). Both need PostgreSQL, so they are skipped unless enabled. Use a **scratch database** — the application schema is created there and test rows are written:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/vyra_scratch DATABASE_USERNAME=postgres DATABASE_PASSWORD=... \
MAIL_USERNAME=x MAIL_PASSWORD=x CRYPTO_SECRET_KEY=0123456789abcdef0123456789abcdef \
DB_LOCK_TIMEOUT_MS=1500 \
mvn test -Dtest='WalletConcurrencyIT,SecurityHardeningIT' -Dvyra.it.db=true
```

`SchemaMigrationIT` builds the schema from the Flyway migrations alone and starts the application with `ddl-auto=validate`. Run it against an **empty** database (CI job `migration-check`):

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/vyra_empty DATABASE_USERNAME=postgres DATABASE_PASSWORD=... \
MAIL_USERNAME=x MAIL_PASSWORD=x CRYPTO_SECRET_KEY=0123456789abcdef0123456789abcdef \
mvn test -Dtest=SchemaMigrationIT -Dvyra.it.db=true
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

## 🌿 Branch & Rilis

- `dev`, `sit`, `uat`, `prod` diproteksi di GitLab: **tidak ada yang bisa push langsung** (termasuk maintainer), force push dan hapus branch tidak bisa. Perubahan hanya lewat merge request; pipeline harus hijau dan semua thread harus selesai. Default branch: `dev`.
- Alur: `feature/<nama>` dari `dev` → MR ke `dev` → MR promosi `dev → sit` → `sit → uat` → `uat → prod`. Tidak boleh melompati environment dan tidak ada merge balik dari `sit`/`uat`/`prod` ke `dev`: job `merge-flow-check` (`scripts/ci/check-merge-flow.sh`) menolaknya.
- Nama branch: `feature/`, `fix/`, `hotfix/`, `chore/`, `docs/`, `refactor/`, `test/`, `ci/`.
- Darurat: `hotfix/*` → `prod` hanya dengan label `emergency-release` dan di-merge maintainer; setelah itu MR hotfix yang sama ke `dev`.
- Sekali per clone: `sh scripts/setup-git-hooks.sh` (atau `git config core.hooksPath .githooks`). Hook pre-push menolak push ke branch environment — pengaman lokal saja; GitLab yang menegakkan aturannya.
