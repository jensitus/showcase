# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A workflow showcase consisting of two independently running applications:

- **`workflow/`** — Spring Boot 3.2.5 (Java 17) backend: conference abstract submissions (call for papers) and the novelty batch screening. Integrates with CIB Seven (a Camunda 7 fork) as the external BPMN process engine.
- **`frontend/`** — Angular 20 SPA: task list, BPMN diagram viewer, and form rendering for workflow user tasks.

Infrastructure: PostgreSQL (port 5555 via Docker Compose), CIB Seven engine (port 7001, run separately).

## Commands

### Backend (workflow/)
```bash
cd workflow
./mvnw compile          # compile
./mvnw spring-boot:run  # run (requires Postgres + CIB Seven)
./mvnw test             # tests use H2 in-memory, no external deps needed
./mvnw -Dtest=UserControllerTest test  # run a single test class
```

### Frontend (frontend/)
```bash
cd frontend
npm install
ng serve                # dev server at http://localhost:4200
ng build                # production build
ng test                 # Karma/Jasmine unit tests
```

### Infrastructure
```bash
docker-compose up -d    # starts only PostgreSQL on port 5555
```

CIB Seven must be started separately (it is not in docker-compose.yaml). Default URL: `http://localhost:7001/engine-rest`.

## Architecture

### Backend domain modules (under `workflow/src/main/java/org/service_b/workflow/`)

| Package | Responsibility |
|---|---|
| `submission/` | Conference abstract submission persistence + state |
| `screening/` | Novelty batch runner: external-task workers calling the novelty pipeline API (see SCREENING_RUNBOOK.md) |
| `workflow/` | All CIB Seven integration: REST client, task lifecycle, process management |
| `security/` | JWT auth, API key auth, user management, email verification |
| `sse/` | Server-Sent Events for real-time frontend updates |
| `shared/` | Mail service, utilities (DataItem, MapObject, converters) |

### Three BPMN processes

1. **Abstract Submission** (`abstract-submission.bpmn`) — process key `abstract_submission_lifecycle`, tenant `cfp`
2. **Novelty Check** (`novelty-check.bpmn`) — process key `novelty_check`, tenant `cfp`: called by (1) for a
   valid submission. Screen against PubMed → if not flagged, ask the larger model → result in
   `submission_novelty`; an error event subprocess records "unavailable" when the pipeline stays
   unreachable. Workers: `NoveltyCheckWorkers`.
3. **Novelty Batch Screening** (`novelty_batch.bpmn`) — process key `novelty_batch`, tenant `screening`

The BPMN files live in the engine module, `cibdemo/src/main/resources/processes/<tenant>/`,
and CIB Seven deploys them at startup under their tenant, as listed in
`cibdemo/src/main/resources/META-INF/processes.xml` (scanning is off: a new .bpmn must be
added there; `ProcessesXmlTest` fails otherwise). Only changed files are redeployed.

The insurance showcase (process `insurance_showcase`, tenant `insurance`, with the customer
module) was removed; its last state is the tag `insurance-showcase-v1` (also branch
`archive/insurance`). Infrastructure names still say "insurance" (k8s namespace, image names,
`insurance-secrets`, the Postgres user) because renaming them means migrating the live deployment.

### CIB Seven integration pattern

This backend acts as both process starter and task handler:

1. **Starting a process**: `RestClientService.startCib7Process()` POSTs to the CIB Seven REST API. For submissions, `processService.preCreateProcess()` is called *before* starting the engine so that the early task callback can find the process entity.
2. **Task callbacks**: CIB Seven calls back `POST /api/tasks` (handled by `TaskController`) when a user task is created. `TaskService.createTask()` saves the task locally and runs task enrichers.
3. **Task enrichers**: `TaskEnricher` implementations (`CfpTaskEnricher`) populate `additionalInfo`, `config`, and `configData` JSON fields on the task so the frontend can render it generically.
4. **External tasks**: `FetchAndLockService` polls CIB Seven for service tasks; `CfpExternalTaskService` and `ScreeningBatchService` process and complete them.
5. **Completing tasks**: Frontend POSTs to `/api/workflows/complete-task`; backend forwards completion to CIB Seven via `CibSevenRestClient`.

### Task DTO structure

`TaskDto` carries three JSON-serialized fields the frontend renders generically:
- `additionalInfo` — display-only key/value groups (e.g. submission data, review summary)
- `config` — which input fields are required and their types (`text`, `textarea`, `select`)
- `configData` — default values and display order for each input field

### Frontend architecture

- `app/workflow/` — BPMN viewer (bpmn-js), process list (deployment is the engine's job: `cibdemo/.../META-INF/processes.xml`)
- `app/task/` — generic task form renderer, task-specific components
- `app/submission/` — abstract submission flow
- `app/screening/` — novelty screening dashboard (`/screening`)
- `app/auth/` — login, register, JWT handling, email verification
- `app/shared/` — navigation, layout, i18n helpers

The dev proxy (`proxy.config.json`) rewrites `/api/` calls to `http://localhost:8080` and `/wm/` to the same. The Angular environment files (`src/environments/`) set the backend and CIB Seven base URLs.

### Database migrations

Liquibase manages the schema. Changesets are in `workflow/src/main/resources/db/changelog/` and ordered in `db.changelog-master.yaml`. Add new changesets there; never modify existing ones.

### Security

Two authentication mechanisms coexist in `SecurityConfig`:
- **JWT** (via `JwtAuthenticationFilter`) for browser users
- **API key** (via `ApiKeyAuthenticationFilter`) for service-to-service calls (e.g. CIB Seven callbacks)

The `showcase.simulate-task-failure` property (default `false`) can force a task creation error to trigger a CIB Seven incident — keep this `false` in all non-test environments.

### Configuration via environment variables

| Env var | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5555/workflow` | Database URL |
| `CIBSEVEN_BASE_URL` | `http://localhost:7001/engine-rest` | CIB Seven engine |
| `JWT_SECRET` | (insecure default) | Must be overridden in production |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | (empty) | Bootstrap admin account |
| `SMTP_HOST/PORT/USERNAME/PASSWORD` | protonmail defaults | Email sending |
