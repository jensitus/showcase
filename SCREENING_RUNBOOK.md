# Novelty screening — demo bring-up runbook

How to bring the whole batch-screening demo online: the novelty pipeline on the DGX
Spark, this workflow service, and the `/screening` dashboard. Triage evidence for
reviewers — never an automatic accept/reject.

## The data path

```
Angular (:4200)
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
docker-compose up -d                    # Postgres :5555
# start the CIB Seven engine on :7001  (SEPARATE — it is not in docker-compose)
cd workflow && ./mvnw spring-boot:run   # app :8080; Liquibase creates screening_chunk
cd ../frontend && ng serve              # UI :4200
```
Then **deploy `novelty_batch.bpmn` under tenant `screening`** (via the deploy endpoint/UI).

### 3. Run a batch + view
```bash
# submissions must MATCH the synthetic index (same gen params: seed 7, corpus 5000):
python3 gen_synthetic.py --out-dir /ABS/PATH/data/demo --corpus-size 5000 --submissions 200 --seed 7

curl -X POST localhost:8080/api/screening/batch \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: $(printf '%s' "$JWT_SECRET:camunda-service" | shasum -a 256 | awk '{print $1}')" \
  -d '{"exportPath":"/ABS/PATH/data/demo/submissions.jsonl"}'

# open http://localhost:4200/screening
```

## Gotchas (each cost a debugging round once)
- **`exportPath` is resolved relative to the app's working directory** (this repo root), not
  the abstract-novelty repo. **Use an absolute path.** Outputs land in `data/screening/<batchId>/`.
- **Auth:** `/api/**` needs the `X-API-Key` header. The service key is
  `SHA-256("<JWT_SECRET>:camunda-service")` (the command above derives it). With the default
  dev JWT secret it's a fixed value; set `CAMUNDA_SERVICE_API_KEY` to pin your own.
- **Corpus must match.** If `flagged.csv` is empty, the pipeline is serving the wrong index
  (real corpus vs synthetic submissions), or the submissions came from a different `gen_synthetic`
  run than the built index.
- **CIB Seven (:7001)** is the one piece not in docker-compose — stand it up first.
- Deploy the BPMN under tenant **`screening`**, or the workers have nothing to pick up.
