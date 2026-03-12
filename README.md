# Cloud Processing Service

An enterprise-style backend system for async file compression built with **Java 21 + Spring Boot 3.2**.

Files are uploaded directly to S3 via presigned URLs, compressed asynchronously, and made available for download — all with full job-status tracking.

---

## Architecture

```
Client  ──►  REST API (Spring MVC)
                │
                ├──► PostgreSQL  (users, file metadata, job records)
                ├──► S3          (original files + compressed output)
                └──► Job executor (virtual-thread pool, async compression)
```

**Key design decisions**

| Decision | Rationale |
|---|---|
| Java 21 virtual threads | Eliminates thread-per-request bottleneck for I/O-heavy workloads |
| S3 presigned uploads | Client uploads directly to object storage; API never touches file bytes |
| Flyway migrations | Schema changes are versioned, reviewable, and reproducible |
| Stateless JWT auth | Fits containerised/load-balanced deployment without sticky sessions |

---

## Package Layout

```
com.cloudprocessing
├── auth          # Registration, login, JWT filter
├── file          # File metadata CRUD, presigned URL generation
├── job           # Compression job lifecycle (queued → processing → done)
├── storage       # S3 abstraction layer
├── compression   # Compression algorithm implementations
├── config        # Spring beans: security, async, S3, properties
└── common        # ApiResponse, AppException, GlobalExceptionHandler
```

---

## Getting Started

### Prerequisites

- Docker + Docker Compose
- Java 21 (for local runs without Docker)

### Run with Docker Compose (recommended)

```bash
docker compose up --build
```

This starts:
- **app** on `http://localhost:8080`
- **PostgreSQL** on `localhost:5432`
- **LocalStack** (S3 emulator) on `localhost:4566`

LocalStack automatically creates the `cloud-processing-files` bucket on startup.

### Run locally (without Docker)

1. Start just the infrastructure:
   ```bash
   docker compose up postgres localstack
   ```

2. Set `JAVA_HOME` to Java 21:
   ```bat
   set JAVA_HOME=C:\Program Files\Java\jdk-21
   ```

3. Run the app:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Health check

```bash
curl http://localhost:8080/actuator/health
```

Expected: `{"status":"UP"}`

---

## API Endpoints

> Full OpenAPI spec coming in Week 2.

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Create account |
| `POST` | `/api/v1/auth/login` | Get JWT |
| `POST` | `/api/v1/files` | Create file record + get presigned upload URL |
| `GET` | `/api/v1/files/{id}` | Get file metadata |
| `POST` | `/api/v1/jobs` | Submit compression job |
| `GET` | `/api/v1/jobs/{id}` | Poll job status |
| `GET` | `/api/v1/jobs/{id}/download` | Get presigned download URL |

---

## Running Tests

```bash
./mvnw test
```

Integration tests use **Testcontainers** (real Postgres in Docker) and mock the AWS S3 beans, so no cloud credentials are needed.

---

## Build Milestones

| # | Checkpoint |
|---|---|
| 1 | Create a file record and upload the original file to S3 with a presigned URL |
| 2 | Create a compression job, process it in the background, and see status updates in the database |
| 3 | Sign in, upload a file, watch the job complete, and download the compressed result from a deployed system |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (virtual threads) |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (jjwt) |
| Persistence | Spring Data JPA + PostgreSQL 16 |
| Migrations | Flyway |
| Object Storage | AWS S3 (SDK v2) / LocalStack for local dev |
| Testing | JUnit 5 + Testcontainers |
| Packaging | Docker (multi-stage build) |
