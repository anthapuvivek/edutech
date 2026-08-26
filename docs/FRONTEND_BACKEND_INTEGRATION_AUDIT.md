# Frontend ↔ Backend Integration Audit

**Scope:** `src/` (React 19 + TanStack Start) against `backend/` (Spring Boot, `com.learntrix.edtech`)
**Date:** 2026-08-25
**Method:** Every `apiRequest(...)` call site in `src/services/*.ts` was extracted and matched against every
`@RequestMapping` / `@*Mapping` in `backend/src/main/java/.../controller/`.

---

## 0. Headline

The architecture is sound. Every service already has a real-HTTP branch, every controller already wraps
responses in the same `ApiResponse<T>` envelope, and `api-client.ts` already unwraps it. Wiring is **not**
the problem.

The problems are:

1. **`.env` already has `VITE_USE_MOCKS=false`.** The app is _already_ in live mode, so every gap below is
   currently a runtime failure, not a future task.
2. **52 of 122 frontend API paths have no backend route at all** (~43%).
3. **Six cross-cutting defects** break authentication and authorization even on the routes that _do_ match.

Fix the six defects in §2 first — they are small, and until they land, even the 70 matched endpoints
misbehave for anyone who is not a plain `STUDENT` or `TEACHER`.

---

## 1. Endpoint coverage

### 1.1 Matched and working (~70 paths)

| Area                                        | Frontend service                      | Backend controller                                                                   |
| :------------------------------------------ | :------------------------------------ | :----------------------------------------------------------------------------------- |
| Login / register / logout                   | `auth.service.ts`                     | `AuthController`                                                                     |
| Public catalogue                            | `course.service.ts`                   | `CourseController`, `PublicContentController`                                        |
| Student profile / stats / activity          | `student.service.ts`                  | `StudentController`                                                                  |
| Student enrollments                         | `student.service.ts`                  | `CourseController`                                                                   |
| Student live classes                        | `student.service.ts`                  | `LiveClassController`                                                                |
| Recordings (teacher + student + admin)      | `recording.service.ts`                | `ClassRecordingController`, `StudentRecordingController`, `AdminRecordingController` |
| Career (student-facing)                     | `career.service.ts`, `job.service.ts` | `JobController`                                                                      |
| Mentor console (read)                       | `mentor.service.ts`                   | `MentorController`                                                                   |
| Placement console                           | `placement.service.ts`                | `PlacementController`                                                                |
| Teacher console                             | `teacher.service.ts`                  | `TeacherController`                                                                  |
| Admin students / batches / trainers / stats | `admin.service.ts`                    | `AdminController`                                                                    |

### 1.2 Frontend calls with NO backend route (52)

Grouped by what it would take to build them.

**A. Auth completion — 3 routes, blocks 3 existing pages**

| Path                    | Method | Frontend caller      | Page                         |
| :---------------------- | :----- | :------------------- | :--------------------------- |
| `/auth/forgot-password` | POST   | `auth.service.ts:65` | `routes/forgot-password.tsx` |
| `/auth/reset-password`  | POST   | `auth.service.ts:71` | `routes/reset-password.tsx`  |
| `/auth/verify-email`    | POST   | `auth.service.ts:77` | `routes/verify-email.tsx`    |

No `password_reset_tokens` table exists either. Three shipped pages 404 today.

**B. Public funnel — 1 route, blocks lead capture**

| Path         | Method | Frontend caller         | Page                                 |
| :----------- | :----- | :---------------------- | :----------------------------------- |
| `/enquiries` | POST   | `enquiry.service.ts:13` | `components/courses/EnquiryForm.tsx` |

This is the marketing entry point. No `enquiries` table.

**C. Support desk — 9 routes, module is 100% dead**

`/support/overview`, `/support/tickets`, `/support/tickets/mine`, `/support/tickets/:id`,
`/support/tickets/:id/messages`, `/support/tickets/:id/status`, `/support/tickets/:id/assign`,
`/support/tickets/:id/escalate`, `/support/agents`

No controller, no entity, no `support_tickets` table. Powers `routes/admin.support.tsx`.

**D. CRM / leads — 8 routes, module is 100% dead**

`/admin/crm/overview`, `/admin/crm/leads`, `/admin/crm/leads/:id`, `/admin/crm/leads/:id/stage`,
`/admin/crm/leads/:id/assign`, `/admin/crm/leads/:id/notes`, `/admin/crm/leads/:id/communications`,
`/admin/crm/leads/:id/follow-ups`

No `leads` table. Powers `routes/admin.crm.index.tsx`, `routes/admin.crm.$leadId.tsx`.
The `counsellor` role lands on `/admin/crm` — that role currently has nothing that works.

**E. Coupons & checkout — 6 routes**

`/admin/coupons`, `/admin/coupons/overview`, `/admin/coupons/:id`, `/admin/coupons/:id/duplicate`,
`/checkout/coupons/validate`

No `coupons` table. `/checkout/coupons/validate` is the **price authority** — the code comment in
`types/ops.ts` says "the frontend never decides the final price", but with no endpoint the frontend
currently _is_ the only thing computing it (from `mock/ops.ts`).

**F. Admin career operations — 8 routes**

`/admin/career/jobs`, `/admin/career/companies`, `/admin/career/applications`, `/admin/career/referrals`,
`/admin/career/referrals/:id`, `/admin/career/eligibility-rules`, `/admin/career/eligibility-rules/:id`

The `jobs`, `companies`, `job_applications` tables exist (V16) and `JobController` reads them — but only
under `/api/career/**` and only for `hasRole('STUDENT')`. The admin-side CRUD has no route.
`referrals` and `eligibility_rules` have no tables at all.

**G. RBAC — 4 routes**

`/rbac/roles`, `/rbac/roles/:id`, `/rbac/staff`, `/rbac/staff/:id/status`

The `roles`, `permissions`, `role_permissions` tables exist and are seeded (V2, V4). No controller exposes
them. See §2.5 — this is why frontend RBAC is running off a mock table.

**H. Remaining admin gaps — 8 routes**

| Path                                            | Table exists?                              |
| :---------------------------------------------- | :----------------------------------------- |
| `/admin/articles` (GET, POST)                   | no                                         |
| `/admin/attendance` (GET, PATCH)                | no                                         |
| `/admin/audit-logs`                             | partial — only `login_audit_logs`          |
| `/admin/events`                                 | partial — `live_classes` could back it     |
| `/admin/staff`                                  | yes (`users` + `roles`)                    |
| `/admin/trainers/:id/approval` (PATCH)          | yes (GET `/admin/trainers` already exists) |
| `/admin/communication/whatsapp` (GET, PUT)      | no                                         |
| `/admin/communication/notifications` (POST)     | `notifications` exists                     |
| `/admin/mentor-assignments` (GET, POST, DELETE) | no                                         |

**I. Student gaps — 4 routes**

| Path                                  | Note                                                   |
| :------------------------------------ | :----------------------------------------------------- |
| `/student/leaderboard`                | derivable from `student_profiles.points` — cheap win   |
| `/student/mentor`                     | `mentoring_sessions` exists; needs an assignment link  |
| `/student/placement-drives`           | `placement_drives` exists; needs a student-scoped read |
| `/student/placement-drives/:id/apply` | needs write                                            |

**J. Partial — 1 route**

`POST /mentor/action-items` — `mentor.service.ts:116` posts to it; `MentorController` only has the `GET`.

### 1.3 Backend routes the frontend never calls (9)

| Endpoint                                            | Why it matters                                                            |
| :-------------------------------------------------- | :------------------------------------------------------------------------ |
| `GET /api/auth/profile`                             | **Should be wired now.** See §2.3.                                        |
| `POST /api/courses/{id}/enroll`                     | Enrollment works server-side but no UI calls it — students cannot enroll. |
| `POST /api/admin/batches/{batchId}/students`        | Batch assignment UI missing.                                              |
| `GET /api/teacher/batches`                          | `TeacherBatchResponse` built and unused.                                  |
| `GET`/`POST /api/teacher/live-classes`              | Teachers cannot schedule classes from the UI.                             |
| `GET /api/student/notifications`, `POST /{id}/read` | `NotificationController` fully unused.                                    |
| `GET /api/admin/recordings/statistics`              | Global stats unused.                                                      |
| `POST`/`PUT /api/recordings/upload-local`           | Reached only via a hardcoded mock URL. See §5.                            |

---

## 2. Cross-cutting defects

These affect endpoints that _do_ match. Fix these before building any missing route.

### 2.1 `SUPER_ADMIN` is locked out of the entire admin console — critical

`AuthController.mapToUserResponse` (~line 156) collapses `SUPER_ADMIN` into the role string `"admin"`, so the
UI routes that user to `/admin/dashboard`.

But `AuthController.buildLoginResponse` (~line 134) stamps the JWT with the raw role name:

```java
.map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))   // -> "ROLE_SUPER_ADMIN"
```

and `AdminController` requires:

```java
@PreAuthorize("hasRole('ADMIN')")                              // -> needs "ROLE_ADMIN"
```

A `SUPER_ADMIN` therefore lands on a dashboard where **every single request 403s**.

**Fix:** either grant `SUPER_ADMIN` an implicit `ROLE_ADMIN` authority when building the token, or change
every admin guard to `hasAnyRole('ADMIN','SUPER_ADMIN')`. Prefer the first — one change, one place:

```java
// AuthController.buildLoginResponse
List<GrantedAuthority> authorities = user.getRoles().stream()
        .flatMap(r -> "SUPER_ADMIN".equals(r.getName())
                ? Stream.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                            new SimpleGrantedAuthority("ROLE_ADMIN"))
                : Stream.of(new SimpleGrantedAuthority("ROLE_" + r.getName())))
        .collect(Collectors.toList());
```

### 2.2 `remember me` issues a token that dies in 15 minutes — critical

`AuthController.buildLoginResponse`:

```java
String token = jwtProvider.generateToken(auth, user.getId().toString());   // always 900_000 ms
long expiration = remember ? (30L * 24 * 3600 * 1000) : accessTokenExpirationMs;
Instant expiresAt = Instant.now().plusMillis(expiration);                  // claims 30 days
```

`JwtProvider.generateToken` ignores the `remember` flag entirely — it always uses the injected
`accessTokenExpiration` (900 000 ms = 15 min).

The frontend then trusts `expiresAt`: `authService.readSession()` keeps the session alive for 30 days while
the token has been dead for 29 days and 23¾ hours. The user sees a logged-in shell where everything 403s.

**Fix:** pass the TTL into `generateToken(auth, userId, ttlMillis)` so the JWT and `expiresAt` agree.

### 2.3 The session is never validated against the backend

`useAuth` (`hooks/useAuth.tsx:30`) hydrates entirely from `localStorage["learntrix.session"]`.
`GET /api/auth/profile` exists and is never called. Combined with 2.2 this means a revoked, expired, or
tampered token still renders a fully "logged-in" UI.

**Fix:** on mount, if a stored session exists, call `/auth/profile`; on failure clear the session and redirect
to `/login`. This also gives you the natural place to hang a refresh call once 2.4 is done.

### 2.4 No token refresh exists

`refresh_tokens` table shipped in **V5**. `app.jwt.refresh-token-expiration` is configured in all three
profiles. `JWT_REFRESH_EXPIRATION` is in `.env.example`.

But: `LoginResponse` has no `refreshToken` field, there is no `POST /api/auth/refresh`, and
`RefreshTokenRepository` is never used by a controller.

Result: a 15-minute hard session ceiling for every user, with no recovery.

**Fix:** add `refreshToken` to `LoginResponse`, persist it, add `POST /api/auth/refresh`, and have
`api-client.ts` retry once on 401.

### 2.5 Frontend RBAC runs off a hardcoded mock table

`permissions.service.ts:41`:

```ts
export function permissionsFor(role: PlatformRole): Permission[] {
  return mockRoleDefinitions.find((r) => r.role === role)?.permissions ?? [];
}
```

No `env.useMocks` check. `can()` → `permissionsFor()` → `mock/ops.ts`, always. And `guardService` in
`lib/authz.ts` wraps whole service objects so `assertPermission` throws **before the fetch is issued**.

So with `useMocks=false`, every privileged call is still gated by a mock table that the backend has never
seen — while `/rbac/roles` (which would supply the real table) is unimplemented.

This is also the source of the mismatches in 2.6.

**Fix:** implement `GET /rbac/roles`, load it once into a query-cached store at session start, and have
`permissionsFor` read that (falling back to the mock only when `useMocks`).

### 2.6 Frontend and backend disagree about who may call what

`mock/ops.ts` grants `admin` the full placement, mentor-assign and teaching permission set. The backend's
class-level guards are narrower:

| Controller                         | Backend guard                  | Frontend believes it can be called by                                                                                         |
| :--------------------------------- | :----------------------------- | :---------------------------------------------------------------------------------------------------------------------------- |
| `PlacementController`              | `hasRole('PLACEMENT_OFFICER')` | admin, super_admin, placement_officer                                                                                         |
| `MentorController` (`/mentor/**`)  | `hasRole('MENTOR')`            | admin has `mentor.assign`                                                                                                     |
| `TeacherController`                | `hasRole('TEACHER')`           | admin has `teaching.manage`; mentor has `mentor.students`, and `/teacher/students` is registered for it in `routePermissions` |
| `JobController` (`/api/career/**`) | `hasRole('STUDENT')`           | student only — this one agrees                                                                                                |

An admin opening `/admin/career/jobs` or a mentor opening `/teacher/students` passes the client guard and
then gets a wall of 403s.

**Fix:** widen the backend guards to `hasAnyRole(...)` to match `mock/ops.ts`, and make `mock/ops.ts` the
seed for the real `role_permissions` rows so the two can't drift again.

---

## 3. Transport-layer fixes (`src/services/api-client.ts`)

### 3.1 Backend error messages are thrown away — high impact, 5-line fix

```ts
// api-client.ts:66 — current
throw new ApiError(response.status, `Request failed with status ${response.status}`, details);
```

The backend sends `{ success: false, error: { code, message, fieldErrors } }`. A wrong password produces
"Invalid email or password." server-side and **"Request failed with status 400"** on screen. Every form in
the app shows a status code instead of the real reason, including validation `fieldErrors`.

**Fix:**

```ts
const message =
  (details as { error?: { message?: string } })?.error?.message ??
  `Request failed with status ${response.status}`;
throw new ApiError(response.status, message, details);
```

### 3.2 No 401/403 handling

Nothing anywhere clears the session on an auth failure. Add a single interceptor point in `apiRequest`:
on 401 (and on 403 with code `AUTH_TOKEN_INVALID`), call `authService.persist(null)` and redirect to `/login`.

### 3.3 The backend returns 403 where it should return 401

`SecurityConfig` disables both `formLogin` and `httpBasic` and registers no `AuthenticationEntryPoint`, so
Spring Security falls back to `Http403ForbiddenEntryPoint`. Unauthenticated requests get **403 with an empty
body**, indistinguishable from "authenticated but not permitted".

**Fix:** register an entry point that writes `401` plus an `ApiErrorResponse` body. This is a prerequisite
for 3.2 to work correctly.

### 3.4 `@JsonInclude(NON_NULL)` breaks the envelope unwrap for null payloads

`api-client.ts:74` unwraps only when `"data" in json`. `ApiResponse` is annotated `@JsonInclude(NON_NULL)`,
so `ApiResponse.success(null)` serializes **without** a `data` key — and the caller receives the whole
envelope `{success, timestamp, traceId}` cast to `T`.

Latent today (void endpoints return `ApiResponse<String>`), but it will bite the first `ApiResponse<Void>`.

**Fix:** unwrap on `"success" in json` alone, or drop `NON_NULL` from `data`.

### 3.5 `credentials: "include"` is set on a Bearer-token API

Harmless but it forces exact-origin CORS forever and rules out `allowedOriginPatterns("*")`. Auth is entirely
`Authorization: Bearer` — no cookies are used. Drop it unless refresh tokens will live in an httpOnly cookie
(a reasonable choice for 2.4 — decide before removing).

---

## 4. Contract mismatches

### 4.1 Pagination has two shapes

| Endpoint                      | Serialized shape                                                |
| :---------------------------- | :-------------------------------------------------------------- |
| `GET /api/courses`            | `PageResponse` → `{ items, page, pageSize, total, totalPages }` |
| `GET /api/recordings/teacher` | raw Spring `Page` → `{ content, pageable, totalElements, ... }` |
| `GET /api/admin/recordings`   | raw Spring `Page`                                               |

The frontend already special-cases both (`recording.service.ts:128` maps `.content` → `.items`), so nothing
is broken — but returning `PageImpl` directly also triggers Spring Boot's serialization warning and locks
your JSON to Spring internals.

**Fix:** wrap the two recording endpoints in `PageResponse.of(page)`, then simplify `recording.service.ts`.

Note also that `CourseController.listCourses` treats `page` as **1-based** while `PageResponse.of(Page)`
elsewhere emits Spring's **0-based** `page.getNumber()`. Pick one.

### 4.2 Query parameters the frontend sends and the backend ignores

| Caller                 | Sends                                                                | Backend accepts                           |
| :--------------------- | :------------------------------------------------------------------- | :---------------------------------------- |
| `course.service.ts:41` | `search, category, level, maxPrice, minRating, sort, page, pageSize` | only `search, category, page, pageSize`   |
| `job.service.ts:21`    | `search, type, workMode, sort`                                       | **nothing** — `getJobs()` takes no params |

The course filter sidebar (`routes/courses.index.tsx`) and the job filters render, accept input, and do
nothing. `level`, `maxPrice`, `minRating` and `sort` are silently dropped.

**Fix:** add the params to `CourseService.listCourses` and `JobController.getJobs`, or hide the controls
until they're supported. Silent no-ops are worse than absent controls.

### 4.3 `AuthUser` expects fields the backend doesn't send

`types/lms.ts` — `AuthUser extends User`, which requires `createdAt: string` and
`status: "active" | "suspended" | "pending"`. `UserResponse` sends neither. TypeScript is satisfied at the
boundary (`apiRequest<T>` is an unchecked cast) but the fields are `undefined` at runtime.

**Fix:** add `status` and `createdAt` to `UserResponse`, or make them optional on `AuthUser`.

### 4.4 `super_admin` is unreachable as a frontend role

`mapToUserResponse` never emits `"super_admin"` — it collapses to `"admin"`. So `mockRoleDefinitions`'
`super_admin` entry (and `can()`'s `role === "super_admin"` shortcut in `permissions.service.ts:46`)
can never fire. Emit `"super_admin"` once 2.1 is fixed.

---

## 5. Upload path

`recording.service.ts:346` — `uploadLocalFile` builds a raw `XMLHttpRequest` and sends it with **no
`Authorization` header**:

```ts
const xhr = new XMLHttpRequest();
xhr.open(isLocal ? "POST" : "PUT", uploadUrl, true);
// ... no auth header on the local branch
```

The target, `POST /api/recordings/upload-local`, is `@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")` and is
**not** in `SecurityConfig`'s permitAll list. Every local upload will 403.

(The S3 branch is correct — presigned URLs must not carry an `Authorization` header.)

**Fix:** attach the bearer token when `isLocal` is true. Export the existing `authHeaders()` helper from
`api-client.ts` and reuse it rather than re-reading `localStorage`.

---

## 6. Environment & CORS

Current state:

```
edutech/.env       VITE_API_URL=http://localhost:8081/api      VITE_USE_MOCKS=false
application.yml    server.port: 8081
application.yml    app.cors.allowed-origins: localhost:8080, 127.0.0.1:8080,
                                             localhost:5173, 127.0.0.1:5173
```

Three things to settle:

1. **Confirm the dev port.** The Vite port is chosen by `@lovable.dev/vite-tanstack-config`, not by
   `vite.config.ts`. Run `bun dev`, read the printed origin, and make sure it is in
   `CORS_ALLOWED_ORIGINS`. If it isn't, every request fails preflight and the symptom looks like a
   backend outage.
2. **`VITE_APP_URL` is unset** (`lib/env.ts:7` defaults to `""`). Set it — email verification and password
   reset links in §1.2.A will need it.
3. **Consider a Vite dev proxy instead of CORS.** Proxying `/api` → `http://localhost:8081` makes the app
   same-origin in development, removes CORS from the dev loop entirely, and makes `VITE_API_URL=/api`
   (already `lib/env.ts`'s default) the correct production value too.

**Also:** `backend/.env.example` ships a real-looking password (`DB_PASSWORD=@Reddy01`), and the same value
is the hardcoded default in `application-dev.yml`. Replace both with a placeholder.

---

## 7. Recommended order of work

### Phase 1 — make what exists actually work (~1 day)

Nothing new is built; ~70 endpoints start behaving.

1. Fix `SUPER_ADMIN` authority mapping (§2.1)
2. Fix `remember me` token TTL (§2.2)
3. Surface backend error messages in `api-client.ts` (§3.1)
4. Add the 401 `AuthenticationEntryPoint` (§3.3)
5. Add 401 handling + auto-logout in `api-client.ts` (§3.2)
6. Wire `GET /auth/profile` into `useAuth` on mount (§2.3)
7. Widen the `@PreAuthorize` guards to match `mock/ops.ts` (§2.6)
8. Add the bearer token to the local upload XHR (§5)
9. Confirm the dev origin is in `CORS_ALLOWED_ORIGINS` (§6)

### Phase 2 — close the auth story (~2 days)

10. `refreshToken` in `LoginResponse` + `POST /auth/refresh` + retry-on-401 (§2.4)
11. `password_reset_tokens` table + `/auth/forgot-password`, `/auth/reset-password`, `/auth/verify-email` (§1.2.A)
12. `GET /rbac/roles` + `GET /rbac/staff` seeded from `role_permissions`; point `permissionsFor` at it (§2.5)

### Phase 3 — cheap wins on existing tables (~2 days)

13. `/student/leaderboard` — `student_profiles.points`
14. `/admin/staff` — `users` + `roles`
15. `/admin/events` — `live_classes`
16. `POST /admin/trainers/:id/approval`
17. `POST /mentor/action-items`
18. `/student/placement-drives` + `/apply` — `placement_drives`
19. `/enquiries` + table (§1.2.B)
20. Wire the 9 orphaned backend endpoints into the UI (§1.3) — enrollment first

### Phase 4 — new modules, in business-value order

21. Admin career CRUD (§1.2.F) — tables mostly exist
22. Coupons + `/checkout/coupons/validate` (§1.2.E) — required before payments
23. CRM / leads (§1.2.D) — unblocks the `counsellor` role entirely
24. Support desk (§1.2.C) — unblocks the `support_agent` role entirely
25. Articles, attendance, audit logs, WhatsApp settings, mentor assignments (§1.2.H)

### Phase 5 — contract cleanup

26. Unify pagination on `PageResponse` (§4.1)
27. Honour course and job filter params (§4.2)
28. Align `UserResponse` with `AuthUser` (§4.3)
29. Fix the `NON_NULL` unwrap edge case (§3.4)

---

## 8. Documentation drift

- `README.md` (lines 19, 53, 1559) and the header comments in `api-client.ts`, `types/index.ts`,
  `types/lms.ts`, `types/ops.ts` and `lib/authz.ts` all describe a **FastAPI** backend. The backend is
  **Spring Boot**. Six files to correct.
- `docs/PRODUCTION_READINESS_AUDIT.md` states that the teacher and admin controllers are missing. Both now
  exist (`TeacherController.java`, `AdminController.java`). It also uses absolute `file:///c:/Desktop/learntrix/`
  links that don't resolve from this repo root.
