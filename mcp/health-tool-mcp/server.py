// Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).

// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

from fastmcp import FastMCP
import subprocess
import os
import shutil
import json
import uuid
import time
import traceback
import re
import logging
import logging.handlers
from pathlib import Path
from typing import Optional, Annotated

# Create an MCP server
mcp = FastMCP("Health Tool MCP Server")

# -----------------------------
# Configuration constants
# -----------------------------
LOG_DIR = os.environ.get("MCP_LOG_DIR") or os.path.join(os.getcwd(), "logs")
LOG_FILE = os.path.join(LOG_DIR, "mcp_io.jsonl")

_log = logging.getLogger(__name__)

def _int_env(name: str, default: int) -> int:
    raw = os.environ.get(name)
    if raw is None or raw.strip() == "":
        return default
    try:
        return int(raw)
    except ValueError:
        _log.warning("Invalid %s=%r; falling back to default %d", name, raw, default)
        return default

SUBPROCESS_TIMEOUT = _int_env("MCP_SUBPROCESS_TIMEOUT", 300)
MIN_FREE_DISK_MB = _int_env("MCP_MIN_FREE_DISK_MB", 100)

# Ballerina identifier: must start with lowercase letter, then lowercase alphanumeric/underscore
_BAL_NAME_RE = re.compile(r"^[a-z][a-z0-9_]*$")

# -----------------------------
# Logging utilities
# -----------------------------
def _ensure_log_dir() -> None:
    try:
        os.makedirs(LOG_DIR, exist_ok=True)
    except Exception:
        pass

_jsonl_logger = logging.getLogger("mcp_io")
_jsonl_logger.setLevel(logging.DEBUG)
_jsonl_logger.propagate = False

def _setup_jsonl_logger() -> None:
    """Configure the rotating JSONL logger (idempotent)."""
    if _jsonl_logger.handlers:
        return
    try:
        _ensure_log_dir()
        handler = logging.handlers.RotatingFileHandler(
            LOG_FILE,
            maxBytes=10 * 1024 * 1024,  # 10 MB per file
            backupCount=5,
            encoding="utf-8",
        )
        handler.setFormatter(logging.Formatter("%(message)s"))
        _jsonl_logger.addHandler(handler)
    except Exception:
        pass

def get_caller_identity() -> str:
    """Best-effort caller identity for "with whom" logging.

    Priority order:
    - MCP client name via env var MCP_CLIENT_NAME
    - OS user (USER/USERNAME)
    - Fallback to "unknown"
    """
    try:
        return (
            os.environ.get("MCP_CLIENT_NAME")
            or os.environ.get("USER")
            or os.environ.get("USERNAME")
            or "unknown"
        )
    except Exception:
        return "unknown"

def log_event(
    *,
    event_type: str,  # "input" | "output"
    tool_name: str,
    request_id: str,
    caller: str,
    payload: dict | None,
    status: Optional[str] = None,
    start_time: Optional[float] = None,
    end_time: Optional[float] = None,
) -> None:
    """Write a single JSON line entry with all relevant details."""
    try:
        _setup_jsonl_logger()
        entry = {
            "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "event": event_type,
            "tool": tool_name,
            "request_id": request_id,
            "caller": caller,
            "status": status,
            "duration_ms": (
                int(((end_time or 0) - (start_time or 0)) * 1000)
                if start_time and end_time
                else None
            ),
            "payload": payload,
        }
        _jsonl_logger.info(json.dumps(entry, ensure_ascii=False))
    except Exception:
        # Never let logging break tool execution
        pass

def _log_output_and_return(
    *,
    tool_name: str,
    request_id: str,
    caller: str,
    response_str: str,
    start_time: float,
) -> str:
    status = None
    payload = None
    try:
        payload = json.loads(response_str)
        status = payload.get("status")
    except Exception:
        payload = {"raw": response_str}
    log_event(
        event_type="output",
        tool_name=tool_name,
        request_id=request_id,
        caller=caller,
        payload=payload,
        status=status,
        start_time=start_time,
        end_time=time.time(),
    )
    return response_str

# Helper function to find the Ballerina executable might have a better way
def find_bal_executable() -> Optional[str]:
    """Find the Ballerina executable in common installation paths."""
    # First, try to find it in PATH
    bal_path = shutil.which("bal")
    if bal_path:
        return bal_path
    
    # Common Windows installation paths
    common_paths = [
        r"C:\Program Files\Ballerina",
        r"C:\Program Files (x86)\Ballerina",
        os.path.expanduser(r"~\AppData\Local\Programs\Ballerina"),
    ]
    
    for base_path in common_paths:
        if os.path.exists(base_path):
            # Look for bal.exe in bin subdirectories
            for root, _, files in os.walk(base_path):
                for candidate in ("bal.exe", "bal"):
                    if candidate in files:
                        return os.path.join(root, candidate)

    return None

def get_user_workspace() -> Optional[str]:
    """Get the user's workspace directory from environment variables.
    
    MCP clients typically set environment variables to indicate the workspace.
    Check common environment variables in priority order.
    """
    # Check common workspace environment variables
    workspace_vars = [
        "MCP_WORKSPACE",
        "WORKSPACE",
        "PROJECT_ROOT",
    ]
    
    for var in workspace_vars:
        workspace = os.environ.get(var)
        if workspace and os.path.isdir(workspace):
            return workspace
    
    return None

def normalize_path(path: str, base_dir: Optional[str] = None) -> str:
    """Convert relative paths to absolute paths.
    
    Args:
        path: The path to normalize
        base_dir: Base directory for relative paths. If None, uses user workspace or MCP server dir.
    """
    if not path:
        return path
    
    # Expand user home directory
    path = os.path.expanduser(path)
    
    # If already absolute, return as-is
    if os.path.isabs(path):
        return os.path.abspath(path)
    
    # For relative paths, resolve against base_dir
    if base_dir:
        return os.path.abspath(os.path.join(base_dir, path))
    
    # Try to use user's workspace as base
    workspace = get_user_workspace()
    if workspace:
        return os.path.abspath(os.path.join(workspace, path))
    
    # Fallback to absolute path from current directory
    return os.path.abspath(path)

SKIP_DIRS = {
    "__pycache__", ".git", "node_modules", ".venv", "venv",
    "target", "build", "dist", ".idea", ".vscode", "spec"
}

def format_tool_output(
    tool_name: str,
    cmd: list[str],
    bal_exe: str,
    return_code: int,
    stdout: str,
    stderr: str,
    output_location: str,
    artifact_name: str = "",
    working_directory: Optional[str] = None,
    was_overwritten: bool = False,
    run_start_time: Optional[float] = None
) -> str:
    """Format consistent JSON output for all generation tools."""

    # Check if files were overwritten
    # For fhirTemplateGeneration: use mtime comparison (passed as parameter)
    # For other tools: check stdout/stderr for overwrite messages
    if not was_overwritten:
        was_overwritten = (
            "already exists" in stdout.lower() or 
            "overwrite" in stdout.lower() or
            "already exists" in stderr.lower() or
            "overwrite" in stderr.lower() or
            "already exist" in stdout.lower() or  # handle singular/plural variations
            "already exist" in stderr.lower()
        )

    # Collect generated files
    # Only count files created/modified during this run to avoid
    # pre-existing files masking actual failures
    generated_files: list[str] = []
    try:
        for root, dirs, files in os.walk(output_location):
            # Skip unwanted directories (in-place modification of dirs list)
            dirs[:] = [d for d in dirs if d not in SKIP_DIRS]

            for file in files:
                full_path = os.path.join(root, file)
                # If we know when the run started, only include files
                # that were created or modified during this execution
                if run_start_time is not None:
                    try:
                        if os.path.getmtime(full_path) < run_start_time:
                            continue
                    except OSError:
                        continue
                relative_path = os.path.relpath(full_path, output_location)
                generated_files.append(relative_path.replace("\\", "/"))
    except Exception:
        pass

    # -----------------------------
    # Final MCP response
    # Determine success: either return_code is 0 OR files were generated
    # (Ballerina CLI sometimes returns 1 even on success)
    is_success = return_code == 0 or len(generated_files) > 0
    
    # Use relative path if working_directory is available
    display_location = (
        os.path.relpath(output_location, working_directory)
        if working_directory
        else output_location
    )
    
    # Build next_steps based on overwrite status
    if is_success:
        next_steps = [
            f"Files generated successfully in '{display_location}' folder",
        ]
        
        if was_overwritten:
            next_steps.extend([
                "⚠️  WARNING: Existing files were OVERWRITTEN",
                "Use 'git diff' or your version control tool to review changes",
                "Consider committing your work before regeneration to track differences",
            ])
        else:
            next_steps.extend([
                "Review the generated files in your project",
                "The package is ready to use"
            ])
    else:
        next_steps = [
            "Generation failed. Check stderr for details.",
            "Do not attempt to create files manually.",
            "Fix the input parameters and retry the tool."
        ]
    
    result = {
        "status": "success" if is_success else "failed",
        "tool": tool_name,
        "summary": (
            f"Generated {artifact_name}" + 
            (" - ⚠️  Existing files were OVERWRITTEN. Use version control (git diff, etc.) to review changes." if was_overwritten and is_success else "")
            if is_success
            else f"Failed to generate {artifact_name}"
        ),
        "overwritten": was_overwritten if is_success else None,
        "execution": {
            "executable": bal_exe,
            "command": " ".join(cmd),
            "return_code": return_code,
            "stdout": stdout.strip() if stdout else None,
            "stderr": stderr.strip() if stderr else None
        },
        "output": {
            "location": display_location,
            "files_generated": generated_files,
            "file_count": len(generated_files),
        },
        "next_steps": next_steps
    }
    return json.dumps(result, indent=2)

def format_error_output(tool_name: str, error_type: str, error_message: str) -> str:
    """Format consistent JSON error output for all generation tools."""
    result = {
        "status": "failed",
        "tool": tool_name,
        "summary": error_type,
        "error": error_message,
        "next_steps": [
            "Check the error message above",
            "Verify input parameters and try again"
        ]
    }
    return json.dumps(result, indent=2)

def _validate_bal_name(value: str, field: str) -> Optional[str]:
    """Return an error message if value is not a valid Ballerina identifier, else None."""
    if not _BAL_NAME_RE.match(value):
        return f"'{field}' must start with a lowercase letter and contain only lowercase letters, digits, and underscores (got: '{value}')"
    return None

def _validate_within_workspace(path: str, field: str) -> Optional[str]:
    """If a workspace is configured (MCP_WORKSPACE/WORKSPACE/PROJECT_ROOT), return an
    error when the resolved path is outside it; otherwise None.

    Both sides go through os.path.realpath() so symlinks and ``..`` traversal
    cannot escape the workspace. When no workspace env var is set, returns None
    so existing single-user setups keep working.
    """
    workspace = get_user_workspace()
    if not workspace:
        return None
    workspace_canonical = os.path.realpath(workspace)
    path_canonical = os.path.realpath(path)
    try:
        common = os.path.commonpath([workspace_canonical, path_canonical])
    except ValueError:
        return (
            f"'{field}' must be within the configured workspace "
            f"({workspace_canonical}); got: {path_canonical}"
        )
    if common != workspace_canonical:
        return (
            f"'{field}' must be within the configured workspace "
            f"({workspace_canonical}); got: {path_canonical}"
        )
    return None

def _check_disk_space(path: str, tool_name: str, request_id: str, caller: str, start_time: float) -> Optional[str]:
    """Return an error response string if disk space is below MIN_FREE_DISK_MB, else None."""
    try:
        check_path = path if os.path.exists(path) else os.path.dirname(path)
        if not os.path.exists(check_path):
            check_path = os.getcwd()
        free_mb = shutil.disk_usage(check_path).free / (1024 * 1024)
        if free_mb < MIN_FREE_DISK_MB:
            return _log_output_and_return(
                tool_name=tool_name,
                request_id=request_id,
                caller=caller,
                response_str=format_error_output(
                    tool_name,
                    "Insufficient disk space",
                    f"Less than {MIN_FREE_DISK_MB} MB free on disk ({free_mb:.1f} MB available). Free up space and retry.",
                ),
                start_time=start_time,
            )
    except Exception:
        pass
    return None

def _check_bal_at_startup() -> None:
    """Log Ballerina version at startup for diagnostics."""
    try:
        bal = find_bal_executable()
        if not bal:
            log_event(
                event_type="startup",
                tool_name="server",
                request_id="startup",
                caller=get_caller_identity(),
                payload={"warning": "Ballerina CLI not found on PATH. Tools will fail until installed."},
                status="warn",
            )
            return
        result = subprocess.run(
            [bal, "version"], capture_output=True, text=True, timeout=10
        )
        log_event(
            event_type="startup",
            tool_name="server",
            request_id="startup",
            caller=get_caller_identity(),
            payload={"bal_version": (result.stdout.strip() or result.stderr.strip())},
            status="ok",
        )
    except Exception:
        pass

_check_bal_at_startup()


@mcp.tool(
    description="Generate a Ballerina package from FHIR IG definitions. Required: absolute path to definitions under spec/<ig_name>. If missing, install via npm and move to spec:<newline>npm --registry https://packages.simplifier.net install <ig_name><newline>Examples: US Core hl7.fhir.us.core@8.0.1, CARIN BB hl7.fhir.us.carin-bb@2.1.0, PDex hl7.fhir.us.davinci-pdex@2.1.0. Command: bal health fhir -m package --package-name <name> -o <output> <definitions_path>"
)
def fhirPackageGeneration(
    fhir_spec_directory: Annotated[Optional[str], "Required: Absolute path to FHIR IG definitions under spec/<ig_name>."] = None,
    ig_name: Annotated[Optional[str], "Optional: IG name (e.g., 'hl7.fhir.us.core'). Used only for package name inference."] = None,
    package_name: Annotated[Optional[str], "Optional: Name for the generated Ballerina package. If omitted, inferred from IG name."] = None,
    working_directory: Annotated[Optional[str], "Optional: Project directory. Auto-detected from path if not provided."] = None,
    org_name: Annotated[Optional[str], "Optional: Organization name"] = None,
) -> str:
    """Generate a Ballerina package from a FHIR Implementation Guide.

        Usage:
        - Provide absolute definitions path under your project spec folder (e.g., "/home/user/project/spec/hl7.fhir.us.core").
        - If definitions are not available, install via npm and move to spec:
            npm --registry https://packages.simplifier.net install <ig_name>
            Examples:
                - US Core: hl7.fhir.us.core@8.0.1
                - CARIN BB: hl7.fhir.us.carin-bb@2.1.0
                - PDex: hl7.fhir.us.davinci-pdex@2.1.0

    Args:
        fhir_spec_directory: Required. Absolute path to FHIR IG definitions.
        ig_name: Optional. IG name (e.g., "hl7.fhir.us.core", "hl7.fhir.us.carin-bb").
        package_name: Optional. Inferred from IG if omitted.
        working_directory: Optional. Auto-detected from path; if definitions path is <project>/spec/<ig-name>, uses <project>.
        org_name: Optional.

    Returns:
        JSON with status, generated files, and next steps (or setup instructions).
    """
    # Begin logging context
    _request_id = str(uuid.uuid4())
    _caller = get_caller_identity()
    _start_time = time.time()

    # Resolve inputs: require absolute definitions path under spec/<ig_name>
    setup_hint = (
        "Place IG definitions under spec/<ig_name> and provide the absolute path.\n"
        "If not available, install via npm and move to spec:\n"
        "npm --registry https://packages.simplifier.net install <ig_name>\n"
        "Examples: US Core hl7.fhir.us.core@8.0.1, CARIN BB hl7.fhir.us.carin-bb@2.1.0, PDex hl7.fhir.us.davinci-pdex@2.1.0"
        "\nAfter moving files, provide the absolute path to the definitions again to generate the package."
        "\nProvide user with the above instructions only keeping the message short and clear."
    )
    if not fhir_spec_directory:
        return _log_output_and_return(
            tool_name="fhirPackageGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output(
                "fhirPackageGeneration",
                "Missing definitions path",
                setup_hint
            ),
            start_time=_start_time,
        )
    fhir_spec_directory = normalize_path(fhir_spec_directory)

    ws_err = _validate_within_workspace(fhir_spec_directory, "fhir_spec_directory")
    if ws_err:
        return _log_output_and_return(
            tool_name="fhirPackageGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("fhirPackageGeneration", "Validation error", ws_err),
            start_time=_start_time,
        )

    # If definitions path doesn't exist, return concise setup guidance
    if not os.path.exists(fhir_spec_directory):
        return _log_output_and_return(
            tool_name="fhirPackageGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output(
                "fhirPackageGeneration",
                "FHIR definitions not found",
                f"Path not found: {fhir_spec_directory}. {setup_hint}"
            ),
            start_time=_start_time,
        )
    
    if not os.path.isdir(fhir_spec_directory):
        return _log_output_and_return(
            tool_name="fhirPackageGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output(
                "fhirPackageGeneration",
                "Validation error",
                f"Not a directory: {fhir_spec_directory}. {setup_hint}"
            ),
            start_time=_start_time,
        )
    
    # Check if directory contains files (not empty)
    try:
        files = os.listdir(fhir_spec_directory)
        actual_files = [f for f in files if not f.startswith('.')]
        if not actual_files:
            return _log_output_and_return(
                tool_name="fhirPackageGeneration",
                request_id=_request_id,
                caller=_caller,
                response_str=format_error_output(
                    "fhirPackageGeneration",
                    "Empty directory",
                    f"Empty definitions at: {fhir_spec_directory}. {setup_hint}"
                ),
                start_time=_start_time,
            )
    except Exception as e:
        return _log_output_and_return(
            tool_name="fhirPackageGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("fhirPackageGeneration", "Directory error", f"Error reading directory {fhir_spec_directory}: {e}"),
            start_time=_start_time,
        )
    
    # Auto-detect working_directory if not provided
    if not working_directory:
        # If path is like <project>/spec/<ig-name>, use <project> as working directory
        path_parts = fhir_spec_directory.split(os.sep)
        if 'spec' in path_parts:
            spec_index = len(path_parts) - 1 - path_parts[::-1].index('spec')
            working_directory = os.sep.join(path_parts[:spec_index])
        else:
            # Fallback: use parent directory
            working_directory = os.path.dirname(fhir_spec_directory)
    
    # Normalize working directory
    working_directory = normalize_path(working_directory)

    ws_err = _validate_within_workspace(working_directory, "working_directory")
    if ws_err:
        return _log_output_and_return(
            tool_name="fhirPackageGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("fhirPackageGeneration", "Validation error", ws_err),
            start_time=_start_time,
        )

    # Validate working directory exists
    if not os.path.exists(working_directory) or not os.path.isdir(working_directory):
        return _log_output_and_return(
            tool_name="fhirPackageGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("fhirPackageGeneration", "Validation error", f"Auto-detected working directory not found: {working_directory}. Please provide working_directory parameter explicitly."),
            start_time=_start_time,
        )

    # Infer package name if not provided
    if not package_name or not package_name.strip():
        base_candidate = ig_name or os.path.basename(fhir_spec_directory.rstrip('/\\'))
        base = (base_candidate or "").lower()
        try:
            if "hl7.fhir.us.core" in base:
                package_name = "uscore"
            elif "carin" in base:
                package_name = "carinbb"
            elif "pdex" in base:
                package_name = "pdex"
            else:
                token = base.split('@')[0]
                token = token.split('.')[-1]
                package_name = token.replace('-', '') or "fhirpkg"
        except Exception:
            package_name = "fhirpkg"

    # Validate package_name and org_name format
    name_err = _validate_bal_name(package_name, "package_name")
    if name_err:
        return _log_output_and_return(
            tool_name="fhirPackageGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("fhirPackageGeneration", "Validation error", name_err),
            start_time=_start_time,
        )
    if org_name:
        org_err = _validate_bal_name(org_name, "org_name")
        if org_err:
            return _log_output_and_return(
                tool_name="fhirPackageGeneration",
                request_id=_request_id,
                caller=_caller,
                response_str=format_error_output("fhirPackageGeneration", "Validation error", org_err),
                start_time=_start_time,
            )

    # Log input parameters
    log_event(
        event_type="input",
        tool_name="fhirPackageGeneration",
        request_id=_request_id,
        caller=_caller,
        payload={
            "fhir_spec_directory": fhir_spec_directory,
            "ig_name": ig_name,
            "package_name": package_name,
            "working_directory": working_directory,
            "org_name": org_name,
        },
    )
    
    # Find the bal executable
    bal_exe = find_bal_executable()
    if not bal_exe:
        return _log_output_and_return(
            tool_name="fhirPackageGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("fhirPackageGeneration", "Environment error", "'bal' command not found. Please ensure Ballerina is installed and in your PATH."),
            start_time=_start_time,
        )
    
    # Set output location to modules/ folder in working directory
    output_location = os.path.join(working_directory, "modules")

    # Build the command
    cmd = [
        bal_exe, "health", "fhir",
        "--mode", "package",
        "--package-name", package_name, 
    ]
    
    # Output location (always set, defaulted above)
    cmd.extend(["--output", output_location])
    
    # Add optional organization name
    if org_name:
        cmd.extend(["--org-name", org_name])
    
    cmd.append(fhir_spec_directory)

    disk_err = _check_disk_space(output_location, "fhirPackageGeneration", _request_id, _caller, _start_time)
    if disk_err:
        return disk_err

    try:
        # Create environment with full PATH
        env = os.environ.copy()

        # Auto-confirm overwrite prompts so generation can replace existing packages
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=SUBPROCESS_TIMEOUT,
            env=env,
            shell=False,
            input="y\ny\n",
            cwd=working_directory
        )
        response = format_tool_output(
            tool_name="fhirPackageGeneration",
            cmd=cmd,
            bal_exe=bal_exe,
            return_code=result.returncode,
            stdout=result.stdout,
            stderr=result.stderr,
            output_location=output_location,
            artifact_name=f"Ballerina package '{package_name}'",
            working_directory=working_directory,
            run_start_time=_start_time
        )
        return _log_output_and_return(
            tool_name="fhirPackageGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=response,
            start_time=_start_time,
        )
        
    except subprocess.TimeoutExpired:
        return _log_output_and_return(
            tool_name="fhirPackageGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("fhirPackageGeneration", "Timeout error", f"Command timed out after {SUBPROCESS_TIMEOUT} seconds"),
            start_time=_start_time,
        )
    except Exception as e:
        log_event(
            event_type="error",
            tool_name="fhirPackageGeneration",
            request_id=_request_id,
            caller=_caller,
            payload={"traceback": traceback.format_exc()},
        )
        return _log_output_and_return(
            tool_name="fhirPackageGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("fhirPackageGeneration", "Execution error", str(e)),
            start_time=_start_time,
        )

@mcp.tool(
    description="Generate FHIR API templates from a FHIR Implementation Guide. Required: absolute path to definitions under spec/<ig_name>. If missing, install via npm and move to spec:<newline>npm --registry https://packages.simplifier.net install <ig_name><newline>Examples: US Core hl7.fhir.us.core@8.0.1, CARIN BB hl7.fhir.us.carin-bb@2.1.0, PDex hl7.fhir.us.davinci-pdex@2.1.0. Command: bal health fhir -m template --dependent-package <package> [--included-profile <url>]* [--excluded-profile <url>]* -o <output> <definitions_path>"
)
def fhirTemplateGeneration(
    dependent_package: Annotated[str, "REQUIRED: Fully qualified Ballerina package (e.g., 'ballerinax/health.fhir.r4.uscore501', 'ballerinax/health.fhir.r4international401')"],
    fhir_spec_directory: Annotated[str, "REQUIRED: Absolute path to FHIR IG definitions under spec/<ig_name>."] ,
    working_directory: Annotated[Optional[str], "Optional: Project directory. Auto-detected from path if not provided."] = None,
    org_name: Annotated[Optional[str], "Optional: Organization name for generated templates"] = None,
    included_profiles: Annotated[Optional[list[str]], "Optional: FHIR profile URLs to ONLY include. Reduces generation time by skipping unwanted profiles."] = None,
    excluded_profiles: Annotated[Optional[list[str]], "Optional: FHIR profile URLs to EXCLUDE from generation."] = None,
) -> str:
    """Generate FHIR API templates from a FHIR Implementation Guide using Ballerina Health Tool.

    This tool generates Ballerina service templates based on FHIR profiles in an IG.
    Equivalent to: bal health fhir -m template -o <output> --org-name <org> --dependent-package <package> <definitions_path>

    IMPORTANT: Both fhir_spec_directory (absolute definitions path) AND dependent_package are REQUIRED.
    If definitions are not available locally, install via npm and move to spec:
        npm --registry https://packages.simplifier.net install <ig_name>
        Examples:
            - US Core: hl7.fhir.us.core@8.0.1
            - CARIN BB: hl7.fhir.us.carin-bb@2.1.0
            - PDex: hl7.fhir.us.davinci-pdex@2.1.0

    Args:
        fhir_spec_directory: Required. Absolute path to FHIR IG definitions under spec/<ig_name>.
        dependent_package: Required. Fully qualified Ballerina package name (e.g., 'ballerinax/health.fhir.r4.uscore501').
        working_directory: Optional. Auto-detected from path; if definitions path is <project>/spec/<ig-name>, uses <project>.
        org_name: Optional. Organization name for generated templates (e.g., 'healthcare_samples').
        included_profiles: Optional. List of FHIR profile URLs to ONLY include in generation.
        excluded_profiles: Optional. List of FHIR profile URLs to EXCLUDE from generation.

    Returns:
        Output message from the health tool command execution
    """
    # Begin logging context
    _request_id = str(uuid.uuid4())
    _caller = get_caller_identity()
    _start_time = time.time()

    # Resolve inputs: require absolute definitions path under spec/<ig_name>
    setup_hint = (
        "Place IG definitions under spec/<ig_name> and provide the absolute path.\n"
        "If not available, install via npm and move to spec:\n"
        "npm --registry https://packages.simplifier.net install <ig_name>\n"
        "Examples: US Core hl7.fhir.us.core@8.0.1, CARIN BB hl7.fhir.us.carin-bb@2.1.0, PDex hl7.fhir.us.davinci-pdex@2.1.0"
        "\nAfter moving files, provide the absolute path to the definitions again to generate the templates."
        "\nProvide user with the above instructions only keeping the message short and clear."
    )

    # Validate dependent package
    if not dependent_package or not dependent_package.strip():
        return _log_output_and_return(
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("fhirTemplateGeneration", "Validation error", "dependent_package is required (e.g., ballerinax/health.fhir.r4.uscore501)"),
            start_time=_start_time,
        )

    # Validate required spec path
    if not fhir_spec_directory:
        return _log_output_and_return(
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output(
                "fhirTemplateGeneration",
                "Missing definitions path",
                setup_hint,
            ),
            start_time=_start_time,
        )

    fhir_spec_directory = normalize_path(fhir_spec_directory)

    ws_err = _validate_within_workspace(fhir_spec_directory, "fhir_spec_directory")
    if ws_err:
        return _log_output_and_return(
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("fhirTemplateGeneration", "Validation error", ws_err),
            start_time=_start_time,
        )

    # If definitions path doesn't exist, return concise setup guidance
    if not os.path.exists(fhir_spec_directory):
        return _log_output_and_return(
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output(
                "fhirTemplateGeneration",
                "FHIR definitions not found",
                f"Path not found: {fhir_spec_directory}. {setup_hint}",
            ),
            start_time=_start_time,
        )

    if not os.path.isdir(fhir_spec_directory):
        return _log_output_and_return(
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output(
                "fhirTemplateGeneration",
                "Validation error",
                f"Not a directory: {fhir_spec_directory}. {setup_hint}",
            ),
            start_time=_start_time,
        )

    # Check if directory contains files (not empty)
    try:
        files = os.listdir(fhir_spec_directory)
        actual_files = [f for f in files if not f.startswith('.')]
        if not actual_files:
            return _log_output_and_return(
                tool_name="fhirTemplateGeneration",
                request_id=_request_id,
                caller=_caller,
                response_str=format_error_output(
                    "fhirTemplateGeneration",
                    "Empty directory",
                    f"Empty definitions at: {fhir_spec_directory}. {setup_hint}",
                ),
                start_time=_start_time,
            )
    except Exception as e:
        return _log_output_and_return(
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output(
                "fhirTemplateGeneration",
                "Directory error",
                f"Error reading directory {fhir_spec_directory}: {e}",
            ),
            start_time=_start_time,
        )

    # Auto-detect working_directory if not provided
    if not working_directory:
        # If path is like <project>/spec/<ig-name>, use <project> as working directory
        path_parts = fhir_spec_directory.split(os.sep)
        if 'spec' in path_parts:
            spec_index = len(path_parts) - 1 - path_parts[::-1].index('spec')
            working_directory = os.sep.join(path_parts[:spec_index])
        else:
            # Fallback: use parent directory
            working_directory = os.path.dirname(fhir_spec_directory)

    # Normalize working directory
    working_directory = normalize_path(working_directory)

    ws_err = _validate_within_workspace(working_directory, "working_directory")
    if ws_err:
        return _log_output_and_return(
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("fhirTemplateGeneration", "Validation error", ws_err),
            start_time=_start_time,
        )

    # Validate working directory exists
    if not os.path.exists(working_directory) or not os.path.isdir(working_directory):
        return _log_output_and_return(
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output(
                "fhirTemplateGeneration",
                "Validation error",
                f"Auto-detected working directory not found: {working_directory}. Please provide working_directory parameter explicitly.",
            ),
            start_time=_start_time,
        )

    # Validate org_name format
    if org_name:
        org_err = _validate_bal_name(org_name, "org_name")
        if org_err:
            return _log_output_and_return(
                tool_name="fhirTemplateGeneration",
                request_id=_request_id,
                caller=_caller,
                response_str=format_error_output("fhirTemplateGeneration", "Validation error", org_err),
                start_time=_start_time,
            )

    # Log input parameters
    log_event(
        event_type="input",
        tool_name="fhirTemplateGeneration",
        request_id=_request_id,
        caller=_caller,
        payload={
            "fhir_spec_directory": fhir_spec_directory,
            "dependent_package": dependent_package,
            "working_directory": working_directory,
            "org_name": org_name,
            "included_profiles": included_profiles,
            "excluded_profiles": excluded_profiles,
        },
    )

    # Find the bal executable
    bal_exe = find_bal_executable()
    if not bal_exe:
        return _log_output_and_return(
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output(
                "fhirTemplateGeneration",
                "Environment error",
                "'bal' command not found. Please ensure Ballerina is installed and in your PATH.",
            ),
            start_time=_start_time,
        )

    # Choose output location: keep working directory (consistent with existing behavior)
    try:
        output_location = working_directory
    except Exception as e:
        return _log_output_and_return(
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output(
                "fhirTemplateGeneration",
                "Directory error",
                f"Error preparing output directory: {e}",
            ),
            start_time=_start_time,
        )

    # Build the command
    cmd = [
        bal_exe, "health", "fhir",
        "--mode", "template",
        "--dependent-package", dependent_package,
    ]

    # Output location (always set, defaulted above)
    cmd.extend(["--output", output_location])

    # Add optional organization name
    if org_name:
        cmd.extend(["--org-name", org_name])

    # Add included profiles (can be specified multiple times)
    if included_profiles:
        for profile in included_profiles:
            cmd.extend(["--included-profile", profile])

    # Add excluded profiles (can be specified multiple times)
    if excluded_profiles:
        for profile in excluded_profiles:
            cmd.extend(["--excluded-profile", profile])
    cmd.extend(["--aggregate","--minimal"])
    cmd.append(fhir_spec_directory)

    disk_err = _check_disk_space(output_location, "fhirTemplateGeneration", _request_id, _caller, _start_time)
    if disk_err:
        return disk_err

    try:
        # Create environment with full PATH
        env = os.environ.copy()

        # Capture file mtimes before generation to detect overwrites
        output_path = Path(output_location)
        mtime_before = {}
        if output_path.exists():
            try:
                for p in output_path.rglob("*"):
                    if p.is_file():
                        mtime_before[str(p)] = p.stat().st_mtime
            except Exception:
                pass

        # Execute the command
        # Auto-confirm overwrite prompts so generation can replace existing packages
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=SUBPROCESS_TIMEOUT,
            env=env,
            shell=False,
            input="y\ny\n",
            cwd=working_directory,
        )

        # Check for overwritten files by comparing mtimes
        was_overwritten = False
        if mtime_before and output_path.exists():
            try:
                for p in output_path.rglob("*"):
                    if p.is_file():
                        p_str = str(p)
                        if p_str in mtime_before:
                            # File existed before and was modified
                            if p.stat().st_mtime > mtime_before[p_str]:
                                was_overwritten = True
                                break
            except Exception:
                pass

        # Store overwrite flag in result for format_tool_output
        result.was_overwritten = was_overwritten
        response = format_tool_output(
            tool_name="fhirTemplateGeneration",
            cmd=cmd,
            bal_exe=bal_exe,
            return_code=result.returncode,
            stdout=result.stdout,
            stderr=result.stderr,
            output_location=output_location,
            artifact_name="FHIR API templates",
            working_directory=working_directory,
            was_overwritten=was_overwritten,
            run_start_time=_start_time,
        )
        return _log_output_and_return(
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=response,
            start_time=_start_time,
        )

    except subprocess.TimeoutExpired:
        return _log_output_and_return(
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output(
                "fhirTemplateGeneration",
                "Timeout error",
                f"Command timed out after {SUBPROCESS_TIMEOUT} seconds",
            ),
            start_time=_start_time,
        )
    except Exception as e:
        log_event(
            event_type="error",
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            payload={"traceback": traceback.format_exc()},
        )
        return _log_output_and_return(
            tool_name="fhirTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output(
                "fhirTemplateGeneration",
                "Execution error",
                str(e),
            ),
            start_time=_start_time,
        )

@mcp.tool(
    description="Generate a Ballerina CDS service template (CDS 2.0) from TOML hook definitions. Auto-detects your project directory. Command: bal health cds [--org-name <org>] [--package-name <name>] [--package-version <version>] -o <working_directory> -i <input.toml>"
)
def cdsTemplateGeneration(
    input_file: Annotated[str, "REQUIRED: Path to TOML file containing CDS hook definitions (absolute or relative path)"],
    working_directory: Annotated[Optional[str], "Optional: Absolute path to your project directory. If not provided, uses the current working directory."] = None,
    org_name: Annotated[Optional[str], "Optional: Organization name for the generated template"] = None,
    package_name: Annotated[Optional[str], "Optional: Package name for the generated template"] = None,
    package_version: Annotated[Optional[str], "Optional: Package version for the generated template"] = None,
) -> str:
    """Generate Ballerina CDS service templates using the Health Tool.

    This tool generates a Ballerina service project based on CDS hook definitions (CDS 2.0), including validation and prefetch scaffolding.
    Equivalent to: bal health cds --org-name <org> --package-name <name> --package-version <version> -o <output> -i <input.toml>

    Args:
        input_file: REQUIRED. Path to TOML file containing CDS hook definitions
        org_name: Optional. Organization name for the generated template
        package_name: Optional. Package name for the generated template
        package_version: Optional. Package version for the generated template
        
    Example usage:
        cdsTemplateGeneration(
            input_file="working_directory/cds-definitions.toml",
            org_name="wso2",
            package_name="cds_service",
            package_version="1.0.0",
        )

    Returns:
        Output message from the health tool command execution
    """
    # Begin logging context
    _request_id = str(uuid.uuid4())
    _caller = get_caller_identity()
    _start_time = time.time()

    # Auto-detect working directory if not provided
    if not working_directory:
        working_directory = get_user_workspace()
        if not working_directory:
            return _log_output_and_return(
                tool_name="cdsTemplateGeneration",
                request_id=_request_id,
                caller=_caller,
                response_str=format_error_output(
                    "cdsTemplateGeneration",
                    "Missing workspace",
                    "Could not determine workspace directory. Please provide 'working_directory' parameter or set MCP_WORKSPACE environment variable."
                ),
                start_time=_start_time,
            )
    
    # Normalize paths (convert relative to absolute, using working_directory as base)
    working_directory = normalize_path(working_directory)

    ws_err = _validate_within_workspace(working_directory, "working_directory")
    if ws_err:
        return _log_output_and_return(
            tool_name="cdsTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("cdsTemplateGeneration", "Validation error", ws_err),
            start_time=_start_time,
        )

    input_file = normalize_path(input_file, working_directory)

    ws_err = _validate_within_workspace(input_file, "input_file")
    if ws_err:
        return _log_output_and_return(
            tool_name="cdsTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("cdsTemplateGeneration", "Validation error", ws_err),
            start_time=_start_time,
        )

    # Log input parameters
    log_event(
        event_type="input",
        tool_name="cdsTemplateGeneration",
        request_id=_request_id,
        caller=_caller,
        payload={
            "input_file": input_file,
            "working_directory": working_directory,
            "org_name": org_name,
            "package_name": package_name,
            "package_version": package_version,
        },
    )

    # Validate input file
    if not os.path.exists(input_file):
        return _log_output_and_return(
            tool_name="cdsTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("cdsTemplateGeneration", "Validation error", f"CDS definitions file does not exist: {input_file}"),
            start_time=_start_time,
        )

    if not os.path.isfile(input_file):
        return _log_output_and_return(
            tool_name="cdsTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("cdsTemplateGeneration", "Validation error", f"Path is not a file: {input_file}"),
            start_time=_start_time,
        )

    if not input_file.lower().endswith(".toml"):
        return _log_output_and_return(
            tool_name="cdsTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("cdsTemplateGeneration", "Validation error", "Input file must be a TOML file (.toml)"),
            start_time=_start_time,
        )

    # Validate working directory exists
    if not os.path.exists(working_directory):
        return _log_output_and_return(
            tool_name="cdsTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("cdsTemplateGeneration", "Validation error", f"Working directory does not exist: {working_directory}"),
            start_time=_start_time,
        )
    
    if not os.path.isdir(working_directory):
        return _log_output_and_return(
            tool_name="cdsTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("cdsTemplateGeneration", "Validation error", f"Working directory is not a directory: {working_directory}"),
            start_time=_start_time,
        )
    
    # Validate org_name and package_name format
    if org_name:
        org_err = _validate_bal_name(org_name, "org_name")
        if org_err:
            return _log_output_and_return(
                tool_name="cdsTemplateGeneration",
                request_id=_request_id,
                caller=_caller,
                response_str=format_error_output("cdsTemplateGeneration", "Validation error", org_err),
                start_time=_start_time,
            )
    if package_name:
        pkg_err = _validate_bal_name(package_name, "package_name")
        if pkg_err:
            return _log_output_and_return(
                tool_name="cdsTemplateGeneration",
                request_id=_request_id,
                caller=_caller,
                response_str=format_error_output("cdsTemplateGeneration", "Validation error", pkg_err),
                start_time=_start_time,
            )

    # Find the bal executable
    bal_exe = find_bal_executable()
    if not bal_exe:
        return _log_output_and_return(
            tool_name="cdsTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("cdsTemplateGeneration", "Environment error", "'bal' command not found. Please ensure Ballerina is installed and in your PATH."),
            start_time=_start_time,
        )


    output_location = working_directory

    # Build the command
    cmd = [
        bal_exe, "health", "cds",
    ]

    # Optional parameters
    if org_name:
        cmd.extend(["--org-name", org_name])
    if package_name:
        cmd.extend(["--package-name", package_name])
    if package_version:
        cmd.extend(["--package-version", package_version])

    cmd.extend(["--output", output_location])
    cmd.extend(["--minimal"])
    cmd.extend(["--input", input_file])

    disk_err = _check_disk_space(output_location, "cdsTemplateGeneration", _request_id, _caller, _start_time)
    if disk_err:
        return disk_err

    try:
        env = os.environ.copy()

        # Auto-confirm overwrite prompts so generation can replace existing packages
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=SUBPROCESS_TIMEOUT,
            env=env,
            shell=False,
            input="y\ny\n",
            cwd=working_directory
        )
        response = format_tool_output(
            tool_name="cdsTemplateGeneration",
            cmd=cmd,
            bal_exe=bal_exe,
            return_code=result.returncode,
            stdout=result.stdout,
            stderr=result.stderr,
            output_location=output_location,
            artifact_name="CDS service template",
            working_directory=working_directory,
            run_start_time=_start_time
        )
        return _log_output_and_return(
            tool_name="cdsTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=response,
            start_time=_start_time,
        )

    except subprocess.TimeoutExpired:
        return _log_output_and_return(
            tool_name="cdsTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("cdsTemplateGeneration", "Timeout error", f"Command timed out after {SUBPROCESS_TIMEOUT} seconds"),
            start_time=_start_time,
        )
    except Exception as e:
        log_event(
            event_type="error",
            tool_name="cdsTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            payload={"traceback": traceback.format_exc()},
        )
        return _log_output_and_return(
            tool_name="cdsTemplateGeneration",
            request_id=_request_id,
            caller=_caller,
            response_str=format_error_output("cdsTemplateGeneration", "Execution error", str(e)),
            start_time=_start_time,
        )


if __name__ == "__main__":
    mcp.run()
