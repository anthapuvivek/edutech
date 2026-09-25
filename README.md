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

Backend configuration lives in `backend/.env` (gitignored — copy `backend/.env.example`
if you don't have one). **Spring Boot does not read `.env` files on its own**, so start
the backend through the launcher scripts, which load it into the process environment:

**PowerShell**

```powershell
cd backend
.
un.ps1
```

**bash / Git Bash**

```bash
cd backend
./run.sh
```

Running `mvn spring-boot:run` directly still works, but only if you export the variables
yourself — otherwise the DB password and SMTP credentials never reach the app and
activation emails silently go nowhere.

> **Java version note:** the pom targets Java 25. On JDK 21–24, add `-Djava.version=21` (or your JDK's version) to the Maven command:
> `mvn spring-boot:run -Djava.version=21`

Verify it's up:

- Health: <http://localhost:8081/actuator/health> → `{"status":"UP"}`
- API docs (Swagger): <http://localhost:8081/swagger-ui.html>

### 2b. Configure outbound email (activation links)

When an admin onboards a student or trainer, the backend creates the account, issues a
one-time activation token, and emails the recipient a link to set their password. That
email only goes out if SMTP is configured in `backend/.env`:

```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your.address@gmail.com
MAIL_PASSWORD=your-16-char-app-password
MAIL_FROM=your.address@gmail.com
APP_CLIENT_URL=http://localhost:8080
```

For Gmail, `MAIL_PASSWORD` must be a 16-character **App Password** from
<https://myaccount.google.com/apppasswords> (2-Step Verification must be enabled on the
account). A normal Gmail password is rejected with `535-5.7.8 Username and Password not
accepted`.

`APP_CLIENT_URL` is the origin baked into the activation link, so it must match where the
frontend actually runs — otherwise recipients get a dead link.

Check and test the configuration as an admin:

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/admin/mail/status
```

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"to":"you@example.com"}' http://localhost:8081/api/admin/mail/test
```

If SMTP is unset or the server rejects the message, onboarding still creates the account
but reports `emailStatus: NOT_CONFIGURED` / `FAILED` and returns the `activationUrl` so an
admin can pass the link on by hand. The admin UI shows the failure and copies that link to
the clipboard — it never claims an email was delivered when it was not.

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
| `.
un.ps1` / `./run.sh` (backend/) | Run the API with `.env` loaded (port 8081) |
| `mvn spring-boot:run` (backend/) | Run the API without `.env` (port 8081) |
| `mvn test` (backend/)            | Backend tests                   |
