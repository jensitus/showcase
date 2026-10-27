## Workflow Showcase

BPMN processes executed on [CIB Seven](https://cibseven.org) (a Camunda 7 fork), with a
Spring Boot backend (`workflow/`) and an Angular frontend (`frontend/`): a task list, a live
BPMN viewer and generic task forms.

Two showcases:

- **Call for Papers** (tenant `cfp`) — submit a conference abstract and follow it through
  review, acceptance, confirmation, registration and presentation.
- **Novelty screening** (tenant `screening`) — screen a batch of submitted abstracts against
  earlier work via the novelty pipeline, with a dashboard at `/screening`. See
  [SCREENING_RUNBOOK.md](SCREENING_RUNBOOK.md).

### Run it

```bash
docker-compose up -d                          # PostgreSQL on port 5555
# start CIB Seven separately (default http://localhost:7001/engine-rest)
cd workflow && ./mvnw spring-boot:run         # backend on 8080
cd frontend && npm install && npx ng serve    # frontend on 4200
```

Architecture, commands and conventions: [CLAUDE.md](CLAUDE.md).

### The insurance showcase

An insurance application process (customers, household and liability insurance) was part of
this app until October 2026. It is kept, runnable, at the tag `insurance-showcase-v1` (and
the branch `archive/insurance`): `git checkout insurance-showcase-v1`.
