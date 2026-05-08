# Health Tool MCP Server

An MCP (Model Context Protocol) server that exposes Ballerina `bal health` CLI commands as AI-callable tools, enabling automated generation of FHIR packages, FHIR API service templates, and CDS service templates.

## Prerequisites

- **Python** >= 3.10
- **[uv](https://docs.astral.sh/uv/)** package manager
- **[Ballerina](https://ballerina.io/downloads/)** (`bal` CLI must be on `PATH`)

Verify your setup:
```bash
bal version
python3 --version
uv --version
```

## Setup

Navigate to this directory and install dependencies. Pick **one** of the options below.

### Option A: Using `uv` (recommended)

```bash
cd health-tool-mcp
uv sync
```

`uv.lock` will be generated automatically on the first run — no need to create it manually.

### Option B: Using `venv` + `pip`

If you don't have `uv` installed, you can use a standard Python virtual environment:

```bash
cd health-tool-mcp

# Create a virtual environment
python3 -m venv .venv

# Activate it
# macOS/Linux:
source .venv/bin/activate
# Windows (PowerShell):
.venv\Scripts\Activate.ps1

# Install dependencies
pip install -r requirements.txt
```

## Running the Server

With `uv`:
```bash
uv run fastmcp run server.py
```

With `venv` (after activation):
```bash
fastmcp run server.py
```

The server communicates over **stdio** (standard MCP transport).

## Connecting to Claude Desktop

Add the following to your Claude Desktop config (`claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "health-tool": {
      "command": "uv",
      "args": [
        "run",
        "fastmcp",
        "run",
        "/absolute/path/to/health-tool-mcp/server.py"
      ],
      "env": {
        "MCP_WORKSPACE": "/absolute/path/to/your/ballerina/project"
      }
    }
  }
}
```

If you set up the server with `venv` instead of `uv`, point `command` at the `fastmcp` binary inside your venv (e.g., `/absolute/path/to/health-tool-mcp/.venv/bin/fastmcp` on macOS/Linux or `...\.venv\Scripts\fastmcp.exe` on Windows) and use `args: ["run", "/absolute/path/to/health-tool-mcp/server.py"]`.

## Tools

### `fhirPackageGeneration`
Generates a Ballerina package from FHIR IG definitions.

**Required:** Absolute path to definitions under `spec/<ig_name>`.

If definitions are not available locally, install via npm first:
```bash
npm --registry https://packages.simplifier.net install hl7.fhir.us.core@8.0.1
# then move the installed package into your project's spec/ folder
```

---

### `fhirTemplateGeneration`
Generates Ballerina FHIR API service templates from an IG.

**Required:** `dependent_package` (e.g., `ballerinax/health.fhir.r4.uscore501`) and the absolute path to definitions.

Supports profile filtering via `included_profiles` / `excluded_profiles`.

---

### `cdsTemplateGeneration`
Generates a Ballerina CDS (Clinical Decision Support) service template from a TOML hook definitions file.

**Required:** Path to a `.toml` file containing CDS hook definitions.

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `MCP_LOG_DIR` | `<cwd>/logs` | Directory for structured JSONL logs |
| `MCP_SUBPROCESS_TIMEOUT` | `300` | Max seconds for `bal` CLI calls |
| `MCP_MIN_FREE_DISK_MB` | `100` | Minimum free disk (MB) before refusing generation |
| `MCP_WORKSPACE` | — | Default project directory (used when `working_directory` is not passed to a tool) |
| `MCP_CLIENT_NAME` | — | Caller identity written to logs (set automatically by MCP clients) |

## Logging

All tool invocations are logged to `$MCP_LOG_DIR/mcp_io.jsonl` in structured JSONL format. Logs rotate automatically at 10 MB with up to 5 backups retained.

A startup event with the detected `bal` version is written each time the server starts.
