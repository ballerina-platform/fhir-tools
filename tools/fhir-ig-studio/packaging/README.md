# FHIR IG Studio

A web tool for working with FHIR implementation guides: load a guide from a FHIR package registry or your own
files, profile it by hand or with the AI Dev Bot, and generate an executable FHIR facade from it.

## Requirements

- **Java 21 or later** — the prerequisite. It runs the Studio, and the generated FHIR facades run on it too.
  Get one from <https://adoptium.net/> if `java -version` reports anything older.
- **A chat model** (optional) — only for the Dev Bot. See [The Dev Bot](#the-dev-bot) below.

## Run it

```shell
./run.sh        # macOS and Linux
run.bat         # Windows
```

Then open <http://localhost:9095/> and pick an implementation guide. Once a guide is open, a short guide to the
workflow sits above its definitions (the **?** button brings it back if you hide it), and **Show me how** plays
it through.

Stop it with Ctrl+C. Opened packages, uploads and generated facades are kept under `~/.fhir-tool-service`.

## Settings

Every setting can be passed to the run script as a Java option, set as an environment variable, or written to
`~/.fhir-tool-service/service.properties` so it survives a restart. The ones you are most likely to want:

| Option | Environment variable | Default | |
| --- | --- | --- | --- |
| `-Dfhir.tool.service.port=…` | `FHIR_TOOL_SERVICE_PORT` | `9095` | Port to listen on |
| `-Dfhir.tool.service.host=…` | `FHIR_TOOL_SERVICE_HOST` | `0.0.0.0` | Address to bind; `127.0.0.1` keeps it to this machine |
| `-Dfhir.tool.service.workspace=…` | `FHIR_TOOL_SERVICE_WORKSPACE` | `~/.fhir-tool-service` | Where packages and facades are kept |

```shell
./run.sh -Dfhir.tool.service.port=9096
```

## The Dev Bot

The Dev Bot answers questions about the open profile and drafts changes to it. It is off until it is pointed at
a chat model. Any OpenAI-compatible API works, as does Anthropic. To use a model on your own machine with
[Ollama](https://ollama.com/):

```shell
ollama pull qwen2.5-coder:7b
```

and add to `~/.fhir-tool-service/service.properties`:

```properties
fhir.tool.service.modelUrl=http://localhost:11434/v1
fhir.tool.service.model=qwen2.5-coder:7b
```

For a hosted model, also set `fhir.tool.service.modelApiKey` (or the `FHIR_TOOL_SERVICE_MODEL_API_KEY`
environment variable). The open profile is sent to whichever model is configured, so prefer a local one for an
unpublished guide.

## More

Source, full configuration reference and REST API:
<https://github.com/ballerina-platform/fhir-tools/tree/main/tools/fhir-ig-studio>
