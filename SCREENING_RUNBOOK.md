# Novelty screening — demo bring-up runbook

How to bring the whole batch-screening demo online: the novelty pipeline on the DGX
Spark, this workflow service, and the `/screening` dashboard. Triage evidence for
reviewers — never an automatic accept/reject.

## The data path

```
Angular (:4200 or :5173)
   -> Spring workflow app (:8080)          <- Postgres (:5555), CIB Seven (:7001)
      -> NoveltyApiClient -> localhost:8090  --SSH tunnel-->  Spark :8080 (pipeline)  -> Ollama (:11434)
```

`novelty.api-base-url` defaults to `http://localhost:8090`, so the runner reaches the
pipeline through the tunnel with no extra config.

## The tunnel

The Spark is SSH-only (behind a VPN). A `spark` host alias in `~/.ssh/config` forwards
local **8090 -> the Spark's 8080** (the VPN/jump specifics live in that local config, not
here). The key is cached in the ssh-agent so it's non-interactive.

```bash
ssh -f -N spark        # open the tunnel (background)
ssh -O exit spark      # close it
curl -s localhost:8090/health   # verify: {"status":"ok","index":{"count":...}}
```

## Bring-up, in order

### 1. Spark side — pipeline serving the DEMO (synthetic) corpus
The demo catches planted duplicates only if the pipeline serves the *matching synthetic*
index — screening synthetic submissions against the real corpus flags nothing.

```bash
ssh -f -N spark
ssh spark 'cd ~/mist && source env.sh && tmux kill-session -t demo 2>/dev/null; \
  tmux new -d -s demo "cd ~/mist && source env.sh && NOVELTY_INDEX_DIR=index_syn_demo4 \
  python -m uvicorn novelty.server:app --host 0.0.0.0 --port 8080 > /tmp/demo.log 2>&1"'
curl -s localhost:8090/health          # expect index count 5000 (the synthetic set)
```
(Ollama must be up on the Spark: `ssh spark pgrep -x ollama`.)

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

# 1. Spark pipeline + tunnel
ssh spark 'pgrep -x ollama'                              # must print a pid
ssh -o ClearAllForwardings=yes spark 'tmux ls'           # expect session "demo"
# only if "demo" is missing:
ssh spark 'tmux new -d -s demo "cd ~/mist && source env.sh && NOVELTY_INDEX_DIR=index_syn_demo4 python -m uvicorn novelty.server:app --host 0.0.0.0 --port 8080 > /tmp/demo.log 2>&1"'
ssh -f -N spark
curl -s localhost:8090/health                            # expect "count":5000

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
ssh -O exit spark
pkill -f spring-boot:run; pkill -f "ng serve --port 5173"
docker-compose down
```

`$API_KEY` = `SHA-256("<JWT_SECRET>:camunda-service")`; with the default dev secret that is
the fixed value `9743db53a467fffcbceacc8a6640caf0a500817bf0d8192b02cdbf9f1f601aff`.

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
