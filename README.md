# 🌀 Virtual Your Asset (VYRA) – Backend API

**VYRA Backend API** adalah layanan backend berbasis **Spring Boot** yang dirancang untuk mengelola aset virtual dengan standar kualitas kode yang tinggi melalui **CI/CD pipeline otomatis**.

---

## 🚀 Fitur Utama

### 🔐 Authentication Module
- Registrasi pengguna
- Login
- Verifikasi OTP

### 🛡 Security
- Implementasi **JWT (JSON Web Token)** untuk otorisasi API

### 🗄 Database
- **PostgreSQL** untuk manajemen data persisten

### 🌍 Internationalization
- Mendukung pesan error multibahasa (i18n)

---

## 🛠 Tech Stack

| Komponen        | Teknologi                     |
|-----------------|-------------------------------|
| Framework       | Spring Boot 3.4.x             |
| Language        | Java 17                       |
| Database        | PostgreSQL                    |
| Testing         | JUnit 5, Mockito              |
| Build Tool      | Maven                         |
| CI/CD           | GitLab CI                     |

---

## 🏗 CI/CD Pipeline & Quality Gate

Proyek ini mengimplementasikan **Continuous Integration (CI)** untuk memastikan kualitas dan stabilitas kode.

### 1️⃣ Testing – JUnit & Mockito
- Menggunakan **JUnit 5** dan **Mockito** untuk unit testing
- **Mockito Extension**:
    - Mocking layer Service & Repository
    - Tidak bergantung pada database fisik saat CI
- **Surefire Reports**:
    - Laporan testing otomatis
    - Disimpan sebagai artifact pipeline untuk audit kualitas

### 2️⃣ Static Code Analysis – SonarQube
Kode dipindai secara berkala untuk mendeteksi:
- 🐞 Bugs & Vulnerabilities
- 🧹 Code Smells
- ✅ Validasi test case
  > *Add at least one assertion*

### 3️⃣ GitLab CI Configuration
Pipeline terdiri dari **3 tahap utama**:

1. **Preparation**
    - Menyiapkan environment Maven
    - Cache dependency untuk mempercepat build

2. **Check-Branch**
    - Validasi workflow Git
    - Hanya mengizinkan `main` & Merge Request

3. **Code-Build**
    - Menjalankan unit test
    - Build aplikasi menjadi file executable `.jar`

---

## 📦 Containerization & Deployment (Future Roadmap)

### 🐳 Docker
Fondasi containerization telah disiapkan.

- **Base Image**:
  ```dockerfile 
  eclipse-temurin:17-jdk-alpine
  
- **Image ringan, aman, dan siap production**

## 🚧 TODO – Continuous Deployment (CD)

### Rencana pengembangan selanjutnya:
- **Automated Docker Image Push**
  Push image ke Docker Hub / GitLab Container Registry

- **Kubernetes Deployment**
   - Deploy otomatis ke cluster K8s menggunakan:

app-deployment.yaml


Environment Secret Management
Integrasi GitLab CI Variables untuk:

Database credentials

Mail server credentials

🛠 Cara Menjalankan Secara Lokal
1️⃣ Clone Repository
git clone https://gitlab.com/username/virtual-your-assets.git

2️⃣ Konfigurasi Database

Sesuaikan application.yml dengan PostgreSQL lokal Anda:

spring:
datasource:
url: jdbc:postgresql://localhost:5432/vyra
username: your_user
password: your_password

3️⃣ Build & Run Test
mvn clean package

4️⃣ Jalankan Aplikasi
mvn spring-boot:run

📂 Struktur Folder
├── .gitlab-ci.yml          # Konfigurasi CI/CD Pipeline
├── src
│   ├── main/java           # Source code utama
│   └── test/java           # Unit & Integration Tests
├── target/                 # Output build (.jar)
└── pom.xml                 # Maven dependencies

🤝 Kontribusi

Kontribusi sangat terbuka 🚀
Silakan:

Buat Issue untuk bug atau enhancement

Ajukan Merge Request untuk fitur baru

⚠️ Pastikan pipeline tetap hijau (✅) sebelum merge

📄 License

Project ini menggunakan lisensi internal / proprietary
(Sesuaikan jika ingin menggunakan MIT / Apache 2.0)

✨ VYRA – Virtual Your Assets
Backend API dengan fokus pada kualitas, keamanan, dan scalability