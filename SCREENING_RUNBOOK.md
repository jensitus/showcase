# Novelty screening — demo bring-up runbook

How to bring the whole batch-screening demo online: the novelty pipeline on the DGX
Spark, this workflow service, and the `/screening` dashboard. Triage evidence for
reviewers — never an automatic accept/reject.

> **CHANGED 2026-09-29 — read this before following the steps below.**
>
> 1. **The pipeline is no longer hand-started.** Both APIs on the Spark are systemd units
>    with `Restart=always`, so a `tmux`-started server is reclaimed within seconds and any
>    index you set by hand silently reverts. Use `systemctl`; change an index in the unit
>    file (`deploy/novelty-*.service` in the pipeline repo), never on a command line.
>
>    | unit | Spark port | local (tunnel) | corpora |
>    |---|---|---|---|
>    | `novelty-demo` | 8080 | **8090** | `all` (PubMed, 881,519), `cardiac`, `breast`, `thorax` |
>    | `novelty-congress` | 8082 | **8092** | `excl-check`, `congress-history`, `congress-A2` |
>
> 2. **`~/.ssh/config` forwards BOTH ports on one connection**, so plain `ssh -f -N spark`
>    opens 8090 and 8092. A separate `-L 8092:...` is redundant and only yields
>    "Address already in use" noise.
>
> 3. **A congress batch needs two more fields in the trigger** — `corpus` and `beforeYear` —
>    and `NOVELTY_API_BASE_URL` pointed at 8092. See "Screening a congress" below. Omitting
>    them does not error: the run completes and reports nonsense.
>
> 4. **The synthetic demo's index is not served by either unit** (see step 1 below).

## The data path

```
Angular (:4200 or :5173)
   -> Spring workflow app (:8080)          <- Postgres (:5555), CIB Seven (:7001)
      -> NoveltyApiClient -> localhost:8090  --SSH tunnel-->  Spark :8080  (PubMed corpora)
                           -> localhost:8092  --SSH tunnel-->  Spark :8082  (congress corpora)
                                                                    -> Ollama (:11434)
```

`novelty.api-base-url` defaults to `http://localhost:8090` (the PubMed pipeline), so the
synthetic demo needs no extra config. **A congress run must override it to 8092** — the
default is deliberately left alone so this documented demo keeps working. With `corpus`
sent explicitly, a wrong pairing fails loudly (`400 unknown corpus`) instead of screening
against the wrong thing.

## The tunnel

The Spark is SSH-only (behind a VPN). A `spark` host alias in `~/.ssh/config` forwards
**two** ports on one connection — local 8090 -> Spark 8080 (PubMed) and local 8092 ->
Spark 8082 (congress). The VPN/jump specifics live in that local config, not here. The key
is cached in the ssh-agent so it's non-interactive.

```bash
ssh -f -N spark                  # opens BOTH forwards
pkill -f 'ssh -f -N spark'       # close it (no ControlMaster, so -O exit won't work)
curl -s localhost:8090/health    # PubMed corpora
curl -s localhost:8092/health    # congress corpora
```

Add `-o ClearAllForwardings=yes` to admin `ssh spark '...'` commands to silence the
harmless "Address already in use" warning when a tunnel is already open. Do **not** pass a
separate `-L 8092:localhost:8082` — the config already does it, and a second attempt only
produces that warning.

## Bring-up, in order

### 1. Spark side — the pipeline

Both APIs run as systemd units and come back after a reboot, so normally there is nothing
to start. Just open the tunnel and check:

```bash
ssh -f -N spark                  # forwards BOTH 8090 and 8092
curl -s localhost:8090/health    # PubMed:   all=881,519 ...
curl -s localhost:8092/health    # congress: excl-check / congress-history / congress-A2
ssh spark 'systemctl status novelty-demo novelty-congress --no-pager | grep Active'
ssh spark 'pgrep -x ollama'      # the LLM judge needs it
```

If a unit is down: `ssh spark 'sudo systemctl restart novelty-demo'` (sudo needs a
password). Loading 881,519 vectors takes ~4 min, the three congress indexes ~90s — the
port does not answer until then, so a refused connection right after a restart is normal.

**Do NOT hand-start a server.** `Restart=always` reclaims the port and your copy dies
silently; that is how the demo spent five days serving a stale index. To change what is
served, edit `deploy/novelty-*.service` in the pipeline repo, reinstall, restart.

> **The synthetic demo (`index_syn_demo4`, 5,000 records) is not served by either unit.**
> The steps below that screen synthetic submissions therefore have nothing to match
> against as things stand. Two ways out, neither yet done: add
> `syn-demo=/home/jens/mist/index_syn_demo4` to `novelty-demo`'s `NOVELTY_INDEX_DIRS` and
> pass `"corpus":"syn-demo"` in the trigger (preferred — it is one line and permanent), or
> `sudo systemctl stop novelty-demo`, hand-start the synthetic server, and remember to
> `start` the unit again afterwards.

### 2. Mac side — the workflow stack
```bash
docker-compose up -d                       # Postgres :5555 (dbs: workflow + cib)
cd cibdemo   && ./mvnw spring-boot:run     # CIB Seven :7001 (NOT in docker-compose)
cd ../workflow && ./mvnw spring-boot:run   # app :8080; Liquibase creates screening_chunk
cd ../frontend && ng serve --port 5173     # UI :5173 (see port note below)
```
Then **deploy `novelty_batch.bpmn` under tenant `screening`** (via the deploy endpoint/UI) —
but note the engine's Postgres volume persists deployments, so on an existing box it is
usually already there. Check first:

```bash
curl -s localhost:7001/engine-rest/process-definition/key/novelty_batch/tenant-id/screening
```

### 3. Run a batch + view
```bash
# submissions must MATCH the synthetic index. Either regenerate with the same params:
python3 gen_synthetic.py --out-dir /ABS/PATH/data/demo --corpus-size 5000 --submissions 200 --seed 7
# ...or just copy the validated set off the Spark (guaranteed to match index_syn_demo4):
mkdir -p data/demo && scp spark:/home/jens/mist/data/syn_demo4/submissions.jsonl data/demo/
scp spark:/home/jens/mist/data/syn_demo4/labels.jsonl data/demo/

curl -X POST localhost:8080/api/screening/batch \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: $(printf '%s' "$JWT_SECRET:camunda-service" | shasum -a 256 | awk '{print $1}')" \
  -d '{"exportPath":"/ABS/PATH/data/demo/submissions.jsonl"}'

# open http://localhost:5173/screening
```

~4 minutes for 200 submissions. Expected: **200 screened / 47 flagged (23.5%) / 40 of 40
planted duplicates caught** (100% recall, 85% precision).

---

## Copy-paste bring-up (whole stack, in order)

Verified end-to-end 2026-08-10. Paths assume the repo at
`~/workspace/showcases/workflow-showcase`.

```bash
# 0. SSH key (once per boot)
ssh-add --apple-use-keychain ~/.ssh/id_rsa

# 1. Spark pipeline + tunnel  (systemd runs the pipeline; nothing to start by hand)
ssh spark 'pgrep -x ollama'                              # must print a pid
ssh -o ClearAllForwardings=yes spark 'systemctl is-active novelty-demo novelty-congress'
ssh -f -N spark                                          # opens 8090 AND 8092
curl -s localhost:8090/health                            # PubMed:   all=881,519
curl -s localhost:8092/health                            # congress: congress-A2=87,844
# a unit that is down (sudo needs a password; ~4 min to load 881,519 vectors):
#   ssh spark 'sudo systemctl restart novelty-demo'

# 2. Workflow stack
cd ~/workspace/showcases/workflow-showcase
docker-compose up -d                                     # Postgres :5555
(cd cibdemo   && ./mvnw spring-boot:run &)               # CIB Seven :7001
(cd workflow  && ./mvnw spring-boot:run &)               # Spring app :8080
(cd frontend  && npx ng serve --port 5173 &)             # UI :5173
curl -s localhost:7001/engine-rest/engine
curl -s localhost:8080/api/screening/batches -H "X-API-Key: $API_KEY"

# 3. Demo data
mkdir -p data/demo
scp spark:/home/jens/mist/data/syn_demo4/submissions.jsonl data/demo/
scp spark:/home/jens/mist/data/syn_demo4/labels.jsonl      data/demo/

# 4. Run a batch (~4 min), then view
curl -X POST localhost:8080/api/screening/batch \
  -H 'Content-Type: application/json' -H "X-API-Key: $API_KEY" \
  -d '{"exportPath":"'"$PWD"'/data/demo/submissions.jsonl"}'
open http://localhost:5173/screening

# 5. Teardown
pkill -f 'ssh -f -N spark'   # no ControlMaster, so -O exit does not work
pkill -f spring-boot:run; pkill -f "ng serve --port 5173"
docker-compose down
```

`$API_KEY` = `SHA-256("<JWT_SECRET>:camunda-service")`; with the default dev secret that is
the fixed value `9743db53a467fffcbceacc8a6640caf0a500817bf0d8192b02cdbf9f1f601aff`.

## Screening a congress against only its own past

The case the year cutoff exists for. Screening a congress against a corpus that *contains*
it is meaningless — every submission matches itself at cosine ~1.0 and comes back a
duplicate of itself, and the run completes cleanly while reporting that. So the corpus must
be masked to strictly earlier years.

**1. Point the app at the congress pipeline** (the default is the PubMed one):

```bash
cd workflow && NOVELTY_API_BASE_URL=http://localhost:8092 ./mvnw spring-boot:run
```

**2. Put the submissions somewhere the app can read.** `exportPath` is resolved on the
machine running the app, not on the Spark:

```bash
# <congress> is the congress key, e.g. the year-suffixed export name on the Spark
ssh spark "head -50 ~/mist/data/leave_one_out/subs_<congress>.jsonl" > ~/subs_50.jsonl
```

**3. Trigger, with BOTH new fields:**

```bash
curl -X POST localhost:8080/api/screening/batch \
  -H "X-API-Key: $KEY" -H 'content-type: application/json' \
  -d '{"exportPath":"/ABS/PATH/subs_50.jsonl","corpus":"congress-A2","beforeYear":2026}'
```

- `corpus` is **not optional here.** Without it the pipeline uses the FIRST entry of its
  `NOVELTY_INDEX_DIRS`, which on 8092 is `excl-check` (76,024 records, with the
  not-new-labelled abstracts removed) — quietly a different corpus from `congress-A2`
  (87,844, all congresses 2011-2026), and only the latter is what a cutoff is meant for.
- `beforeYear` is **exclusive**: 2026 means `year < 2026`. It leaves 77,521 of the 87,844
  eligible. Note the pipeline repo's `validate_cross_year.py --cutoff` is INCLUSIVE, so
  `beforeYear N` == `--cutoff N-1`.

**4. Confirm the cutoff was actually applied.** This is the step not to skip: the pipeline
ignores JSON fields it does not understand, so a `beforeYear` sent to an older pipeline is a
silent no-op. `NoveltyApiClient` now refuses to continue unless the value is echoed back, so
a mismatch fails on the first chunk — but verify the output too:

```bash
python3 -c "
import json
rows=[json.loads(l) for l in open('workflow/data/screening/<batchId>/results.jsonl')]
print('stamped :', all(r.get('before_year')==2026 for r in rows))
print('anachron:', sum(1 for r in rows for m in r['top_matches'] if (m['year'] or 0)>=2026))
print('self    :', sum(1 for r in rows for m in r['top_matches'] if m['id']==r['submission_id']))
"
```

All three must read True / 0 / 0. The dashboard also shows a scope line ("screened against
only abstracts published before 2026"), derived from the reports rather than from the run
parameters — if that line is missing, no cutoff was applied.

**What to expect.** ~23% flagged and ~23% reaching the LLM judge. `chunk-size` is 50, so
results append roughly every 90s and progress is visible. A first `[progress]`-equivalent
showing near-100% flagged means the cutoff is not working — stop and check. The full 10,323
of one congress takes ~5 hours.

**A CLI alternative that needs none of this stack** — useful for a long unattended run,
since it depends only on the Spark:

```bash
ssh spark 'tmux new -d -s run "cd ~/mist && source env.sh && python -m novelty.check \
  --submissions ~/mist/data/leave_one_out/subs_<congress>.jsonl \
  --index-dir ~/mist/index_not_new_a2 --before-year 2026 \
  --out ~/mist/data/leave_one_out/<congress>.jsonl --resume > ~/mist/data/leave_one_out/<congress>.log 2>&1"'
```

Then turn it into something the dashboard can read (the `--submissions` join fills the
`title` column; without it every row's title is blank, because a report carries no title):

```bash
python3 flagged_report.py --results <results>.jsonl --submissions <subs>.jsonl \
    --out-dir workflow/data/screening/<someId>
```

The dashboard lists any directory under the work dir containing a `results.json`, so a
completed run can be dropped in without the engine running at all.

## Gotchas (each cost a debugging round once)
- **`exportPath` is resolved relative to the app's working directory**, which — when started
  the way this runbook says (`cd workflow && ./mvnw spring-boot:run`) — is the **`workflow/`
  subdirectory**, not the repo root. **Use an absolute path.** Outputs land in
  `workflow/data/screening/<batchId>/`.
- **Auth:** `/api/**` needs the `X-API-Key` header. The service key is
  `SHA-256("<JWT_SECRET>:camunda-service")` (the command above derives it). With the default
  dev JWT secret it's a fixed value; set `CAMUNDA_SERVICE_API_KEY` to pin your own.
- **Corpus must match.** If `flagged.csv` is empty, the pipeline is serving the wrong index
  (real corpus vs synthetic submissions), or the submissions came from a different `gen_synthetic`
  run than the built index.
- **CIB Seven (:7001)** is the one piece not in docker-compose — it is the `cibdemo/` module;
  stand it up first. Its Cockpit is at <http://localhost:7001/camunda/app/welcome/default/>
  (`demo`/`demo`) — useful to watch the token move or retry a failed external task.
- Deploy the BPMN under tenant **`screening`**, or the workers have nothing to pick up.
- **Frontend port.** The UI calls `http://localhost:8080` absolutely, and `SecurityConfig`
  allow-lists only origins **3000 / 4200 / 5173**. If something else already holds 4200, serve
  on **5173** rather than editing CORS.
- **Stale instances spam the log.** A run started with a bad/relative `exportPath` fails
  `novelty_ingest` and retries forever. Find and remove them:
  ```bash
  curl -s localhost:7001/engine-rest/process-instance
  curl -X DELETE "localhost:7001/engine-rest/process-instance/<id>?skipCustomListeners=true"
  ```
