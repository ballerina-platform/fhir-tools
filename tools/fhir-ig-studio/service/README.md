# FHIR Codegen Service

Exposes the FHIR **package** and **template** generation of the Ballerina health tool over HTTP, and hosts the
web UI that drives it. It is the same code path as `bal health fhir -m package|template`, so a job accepted here
produces the same artifacts as the equivalent CLI invocation.

## Prerequisites

Java 21, Maven, and a GitHub token for the GitHub Packages repositories the pom reads from. The
[FHIR IG Studio README](../README.md#build-from-source) has the details, including the `settings.xml` entries.

## Running

```shell
mvn clean package
java -jar target/health-service.jar
```

The banner prints the UI and API URLs. Build the UI first (see [`../ui`](../ui)) if you want more than the
REST API; the service picks up `../ui/dist` automatically when it is present.

### Configuration

Every setting is read from a system property, falling back to an environment variable, then a default.

| System property | Environment variable | Default | Purpose |
| --- | --- | --- | --- |
| `fhir.tool.service.port` | `FHIR_TOOL_SERVICE_PORT` | `9095` | Listen port |
| `fhir.tool.service.host` | `FHIR_TOOL_SERVICE_HOST` | `0.0.0.0` | Bind address |
| `fhir.tool.service.workspace` | `FHIR_TOOL_SERVICE_WORKSPACE` | `~/.fhir-tool-service` | Job output, uploads and IG cache |
| `fhir.tool.service.uiDir` | `FHIR_TOOL_SERVICE_UI_DIR` | probes `ui/dist` | Built UI assets |
| `fhir.tool.service.workers` | `FHIR_TOOL_SERVICE_WORKERS` | `2` | Concurrent generations |
| `fhir.tool.service.jobTimeoutSeconds` | `FHIR_TOOL_SERVICE_JOB_TIMEOUT` | `900` | Per-job timeout |
| `fhir.tool.service.maxUploadMb` | `FHIR_TOOL_SERVICE_MAX_UPLOAD_MB` | `200` | Upload size limit |
| `fhir.tool.service.corsAllowOrigin` | `FHIR_TOOL_SERVICE_CORS_ORIGIN` | `*` | `Access-Control-Allow-Origin` |
| `fhir.tool.service.workspaceTtlDays` | `FHIR_TOOL_SERVICE_WORKSPACE_TTL_DAYS` | `7` | Days an unused working copy is kept; `0` disables the sweep |
| `fhir.tool.service.modelProvider` | `FHIR_TOOL_SERVICE_MODEL_PROVIDER` | inferred | `openai`, `azure` or `anthropic` |
| `fhir.tool.service.modelUrl` | `FHIR_TOOL_SERVICE_MODEL_URL` | empty | Base URL of the chat API; **setting this switches the chat on** |
| `fhir.tool.service.model` | `FHIR_TOOL_SERVICE_MODEL` | `qwen2.5-coder:7b` | Model or deployment name |
| `fhir.tool.service.modelApiKey` | `FHIR_TOOL_SERVICE_MODEL_API_KEY` | empty | API key, for a hosted provider |

#### The chat model — optional, off by default

The service starts with no chat. Browsing packages, profiling, authoring extensions and generating a facade
all work without one, and nothing is reported as broken: `GET /api/model` answers `configured: false`, the chat
endpoint answers `501`, and opening the panel in the UI explains how to switch it on rather than showing an
error.

Setting `FHIR_TOOL_SERVICE_MODEL_URL` is what enables it. For a machine someone works on, put it in
`~/.fhir-tool-service/service.properties` instead, so it outlives a restart:

```properties
fhir.tool.service.modelUrl=http://localhost:11434/v1
fhir.tool.service.model=qwen2.5-coder:7b
```

That file is read at startup and is the weakest of the three sources: a `-D` argument or an environment
variable still wins, so a deployment can override what a developer left behind. Point somewhere else with
`FHIR_TOOL_SERVICE_CONFIG`. Nothing is assumed about a model running locally,
so a developer with Ollama already on this machine still has to say so.

Once on, the FHIR Dev Bot answers questions about the open package and drafts changes to it. It never edits
anything: a change comes back as a proposal, and applying one goes through the same profiling-rule validation
as an edit made by hand.

Any OpenAI-compatible API works without setting a provider — Ollama, LM Studio, llama.cpp, vLLM, Groq,
Together, OpenRouter, OpenAI itself. The provider only has to be named when the wire format or the
authentication header differs, and `anthropic.com` and `openai.azure.com` addresses are recognised on their
own.

```shell
# A model on this machine (the default: no key, nothing to set)
ollama serve && ollama pull qwen2.5-coder:7b

# OpenAI, or anything that imitates it
export FHIR_TOOL_SERVICE_MODEL_URL=https://api.openai.com/v1
export FHIR_TOOL_SERVICE_MODEL=gpt-4o-mini
export FHIR_TOOL_SERVICE_MODEL_API_KEY=sk-...

# Anthropic
export FHIR_TOOL_SERVICE_MODEL_URL=https://api.anthropic.com/v1
export FHIR_TOOL_SERVICE_MODEL=claude-sonnet-4-5
export FHIR_TOOL_SERVICE_MODEL_API_KEY=sk-ant-...

# Azure OpenAI: the deployment and api-version are part of the URL
export FHIR_TOOL_SERVICE_MODEL_URL='https://my-resource.openai.azure.com/openai/deployments/gpt-4o/chat/completions?api-version=2024-02-01'
export FHIR_TOOL_SERVICE_MODEL=gpt-4o
export FHIR_TOOL_SERVICE_MODEL_API_KEY=...
```

`GET /api/model` reports which provider is in use, whether a key is configured and whether the provider is
answering. It never returns the key, and a key echoed back by a provider in an error is removed before the
error reaches the browser.

Set the key from the environment rather than a command line: a `-D` argument is visible to anything that can
list processes. Note that the whole conversation — the open profile's elements, its description, the package's
definition names — is sent to whichever address is configured, so a local model is the right choice for an
unpublished guide.

### Workspace layout

```
<workspace>/
├── .fhir-ig-cache/          downloaded IG tarballs, shared by all jobs
├── spec-cache/<ig>-<ver>/   extracted IG definitions, keyed by resolved version
├── specs/<specId>/          uploaded specification bundles
└── jobs/<jobId>/
    ├── request.json         the submitted request
    ├── worker-input.json    the fully resolved worker instruction
    ├── logs.txt             captured worker output
    └── output/              generated artifacts
```

## Why generation runs in a separate process

Each job is executed by a forked JVM running `GenerationWorker` on the server's own classpath. The codegen
libraries keep JVM-wide mutable state that is never reset between runs (for example the `DataTypesRegistry`
singleton in `fhir-to-bal-lib`), and the CLI code paths reachable from the handlers can call `System.exit`.
Forking gives every job a clean slate and stops a failing job from taking the server down.

## API

All endpoints are rooted at `/api` and speak JSON unless noted.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/healthz` | Liveness probe (outside `/api`) |
| `GET` | `/api/meta` | Service version, modes, defaults and suggested IGs |
| `GET` | `/api/igs` | Suggested implementation guides |
| `GET` | `/api/igs/{name}/versions` | Published versions from the registry (`?refresh=true` bypasses the cache) |
| `GET` | `/api/specs` | Uploaded specifications |
| `POST` | `/api/specs?name=…` | Upload a `.zip` of FHIR definitions as the raw body |
| `DELETE` | `/api/specs/{specId}` | Delete an uploaded specification |
| `POST` | `/api/jobs` | Submit a generation request; responds `202` with the job |
| `GET` | `/api/jobs` | All jobs, newest first |
| `GET` | `/api/jobs/{id}` | Job detail, including artifact counts |
| `GET` | `/api/jobs/{id}/logs?from=N` | Log lines from index `N`, for live streaming |
| `GET` | `/api/jobs/{id}/tree` | Generated file tree |
| `GET` | `/api/jobs/{id}/file?path=…` | Preview one generated file |
| `GET` | `/api/jobs/{id}/archive` | Download the artifacts as a zip |
| `POST` | `/api/jobs/{id}/cancel` | Kill a running job |
| `DELETE` | `/api/jobs/{id}` | Delete a job and its artifacts |

Status codes: `400` malformed request, `404` unknown job, spec or IG, `422` a request that breaks a generation
rule, `502` the registry could not be reached.

### Submitting a job

```shell
curl -X POST http://localhost:9095/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{
        "mode": "template",
        "source": { "type": "registry", "igName": "hl7.fhir.us.core", "igVersion": "6.1.0" },
        "packageName": "uscore610",
        "orgName": "ballerinax",
        "packageVersion": "1.0.0",
        "igModuleName": "uscore",
        "aggregate": true,
        "resources": ["Patient", "Observation"]
      }'
```

`source.type` selects where the definitions come from:

- `registry` — download `igName` at `igVersion` from the FHIR package registry. Omit `igVersion` to pin the
  registry's `latest`; the resolved version is echoed back in the job.
- `upload` — use the bundle previously uploaded as `specId`.
- `default` — use the configured HL7 core IG for `fhirVersion` (`r4` or `r5`).

The remaining fields map one-to-one onto the CLI options: `packageName`, `orgName`, `packageVersion`,
`dependentPackage`, `igModuleName`, `dependentIgs`, `includedProfiles`, `excludedProfiles`, `aggregate`,
`resources` and `minimal`.

### Following a job

```shell
JOB=$(curl -s -X POST http://localhost:9095/api/jobs -H 'Content-Type: application/json' -d @request.json \
        | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

curl -s "http://localhost:9095/api/jobs/$JOB/logs?from=0"
curl -s -o artifacts.zip "http://localhost:9095/api/jobs/$JOB/archive"
```

## Known limitation

Package generation against the **default HL7 R5 core** specification fails inside `fhir-to-bal-lib` with
`Cannot invoke "String.replaceAll(String, String)" because "igCode" is null`, because the R5 core
`ImplementationGuide` resource carries no name. This is a pre-existing tool issue and reproduces through the
plain CLI code path; the service reports it as a failed job with the stack trace in the log. R4 core and named
IGs (such as `hl7.fhir.us.core`) are unaffected.
