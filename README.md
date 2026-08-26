# LearntriX — EdTech Platform

An enterprise-grade learning platform: course catalogue and learning (LMS), live classes and recordings, coding practice, career/placement tools, and role-based portals for students, teachers, mentors, placement officers and admins.

| Layer    | Tech                                                                     | Port |
| -------- | ------------------------------------------------------------------------ | ---- |
| Frontend | React 19, TanStack Start + Router, TypeScript, Tailwind CSS 4, shadcn/ui | 8080 |
| Backend  | Spring Boot 3.4, Spring Security (JWT), JPA/Hibernate, Flyway            | 8081 |
| Database | PostgreSQL 16 (Docker)                                                   | 5432 |

> Redis and Kafka appear in `backend/docker-compose.yml` for future features — only PostgreSQL is required to run the app today.

## Prerequisites

- **Node.js 20+** and npm
- **JDK 21+** and **Maven 3.9+**
- **Docker** (for PostgreSQL)

## Running the app

### 1. Start PostgreSQL

```bash
cd backend
docker compose up -d postgres
```

This starts Postgres on `localhost:5432` with database `edtech_dev`, user `edtech`, password `edtech`. Flyway creates the schema and seeds demo data automatically when the backend first starts.

(Alternative: use any local Postgres — create a database named `learntrix` and the backend's dev defaults `postgres`/`postgres` apply, no env vars needed.)

### 2. Start the backend

From `backend/`:

**bash / Git Bash**

```bash
export DB_URL='jdbc:postgresql://localhost:5432/edtech_dev'
export DB_USERNAME=edtech
export DB_PASSWORD=edtech
mvn spring-boot:run
```

**PowerShell**

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/edtech_dev'
$env:DB_USERNAME = 'edtech'
$env:DB_PASSWORD = 'edtech'
mvn spring-boot:run
```

> **Java version note:** the pom targets Java 25. On JDK 21–24, add `-Djava.version=21` (or your JDK's version) to the Maven command:
> `mvn spring-boot:run -Djava.version=21`

Verify it's up:

- Health: <http://localhost:8081/actuator/health> → `{"status":"UP"}`
- API docs (Swagger): <http://localhost:8081/swagger-ui.html>

### 3. Start the frontend

From the repo root:

```bash
npm install
npm run dev
```

Open <http://localhost:8080>.

The committed `.env` already points the frontend at the local backend (`VITE_API_URL=http://localhost:8081/api`, `VITE_USE_MOCKS=false`). To run the UI standalone on mock fixtures — no backend or database needed — set `VITE_USE_MOCKS=true` and restart the dev server.

## Demo accounts

Seeded by the Flyway migrations. Password for all accounts: **`password`**

| Role              | Email                   |
| ----------------- | ----------------------- |
| Student           | student@learntrix.com   |
| Teacher           | teacher@learntrix.com   |
| Admin             | admin@learntrix.com     |
| Super admin       | super@learntrix.com     |
| Mentor            | mentor@learntrix.com    |
| Placement officer | placement@learntrix.com |

## Project layout

```
├── src/                  # Frontend (TanStack Start)
│   ├── routes/           # File-based routes
│   ├── components/       # UI components (shadcn/ui based)
│   ├── services/         # API service layer (swaps between real API and mocks)
│   ├── mock/             # Mock fixtures used when VITE_USE_MOCKS=true
│   └── types/            # Shared TypeScript contracts for the API
├── backend/              # Spring Boot API
│   ├── src/main/java/com/learntrix/edtech/
│   └── src/main/resources/db/migration/   # Flyway migrations + seed data
└── docs/                 # Design and integration notes
```

## Useful commands

| Command                          | What it does                    |
| -------------------------------- | ------------------------------- |
| `npm run dev`                    | Frontend dev server (port 8080) |
| `npm run build`                  | Production frontend build       |
| `npm run lint`                   | ESLint                          |
| `npx tsc --noEmit`               | Typecheck the frontend          |
| `mvn spring-boot:run` (backend/) | Run the API (port 8081)         |
| `mvn test` (backend/)            | Backend tests                   |
