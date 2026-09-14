# AGENTS.md

This file provides guidance to AI coding agents (Claude Code, Codex, Cursor, etc.) when working with code in this repository.

## What this repo is

`fhir-tools` builds the **Ballerina Health Tool** (`bal health`) — a `bal` CLI tool for generating Ballerina
artifacts (packages, service templates, client connectors) from FHIR Implementation Guides (IGs) and CDS Hooks
definitions. It is a multi-module Maven project (Java 21) whose final output is packaged as a Ballerina tool
(`health-tool-ballerina`) and published to Ballerina Central. A separate Python MCP server exposes the same
`bal health` commands as AI-callable tools.

## Prerequisites & one-time setup

- Java 17+ (repo compiles with `maven.compiler.source/target` = 21)
- Ballerina Swan Lake **2201.12.3** (`bal` must be on `PATH`) — required to run the `bal pack`/`bal test` steps
  invoked from Maven
- A GitHub Personal Access Token with read access to GitHub Packages, added to `~/.m2/settings.xml` with server id
  `ballerina-language-repo` (needed to resolve `ballerina-lang`/`ballerina-cli` artifacts):
  ```xml
  <servers>
      <server>
          <id>ballerina-language-repo</id>
          <username>{Github_username}</username>
          <password>{Github_PAT}</password>
      </server>
  </servers>
  ```
- The [`wso2/open-healthcare-codegen-tool-framework`](https://github.com/wso2/open-healthcare-codegen-tool-framework)
  repo (currently pinned to `v2.1.2` via `version.healthcare.tool.framework` in the root `pom.xml`) must be built
  and `mvn clean install`ed locally first — this repo's modules depend on its `commons`/`fhir-core` artifacts, which
  are not on Maven Central.
- If `mvn clean install` fails resolving `org.wso2.healthcare.codegen.tool.framework:*` even after installing the
  framework above, check `~/.m2/repository/org/wso2/healthcare/codegen/tool/framework/*/*.lastUpdated` — Maven
  caches a *negative* resolution result there if it ever tried (and failed) to fetch these artifacts remotely
  before the local install existed. Delete that directory tree and rebuild; these artifacts only ever come from
  the local install, never from `ballerina-language-repo` or Central.

## Build & test

```shell
# from repo root, after the codegen-tool-framework has been installed locally
mvn clean install
```

This builds every module in the order declared in the root `pom.xml` and finishes by running `bal pack` to produce
the `health-tool-ballerina` Ballerina package under `ballerina/target/health-tool-ballerina`.

- Build a single module (respecting the reactor for its dependencies): `mvn clean install -pl native/health-cli -am`
- **Only `native/health-cli` has JUnit coverage** (JUnit 5, added alongside the `--ig` registry-download feature).
  `fhir-to-bal-template`, `fhir-to-bal-connector`, and `cds-bal-template` have no `src/test` directory and no test
  dependency at all — see "Verifying template-generation changes" below for how correctness of *those* modules is
  actually checked.
- Beyond JUnit, `native/health-cli`'s `packageGenTests` Maven profile (active by default) drives a second,
  integration-style check via `exec-maven-plugin`:
  1. `TestRunner.java` (`native/health-cli/src/test/java/TestRunner.java`) invokes the FHIR **package**-gen handler
     directly (bypassing the CLI/picocli layer) against the bundled fixtures — `profiles.USCore` for FHIR **R4**
     and `profiles.EuropeBase` for FHIR **R5** (system property `fhirVersion` selects which one runs), plus a CDS
     template run against `cds.hooks/tool-config.toml`. This never exercises template-mode generation.
  2. The generated Ballerina packages are then exercised with `bal test` against `.bal` test files copied in from
     `native/fhir-to-bal-lib/src/test/resources/ballerina.tests/{r4,r5}`.
  - To iterate on this locally: `mvn test -pl native/health-cli -am`. To change what's exercised, edit the fixtures
    under `native/health-cli/src/test/resources/profiles.*` or the `.bal` files under
    `native/fhir-to-bal-lib/src/test/resources/ballerina.tests/`.
- CI (`.github/workflows/ci.yml`) does the same thing on every PR: checks out this repo plus
  `open-healthcare-codegen-tool-framework` at the pinned tag, builds the framework, then runs `mvn clean install`
  on this repo.
- Release/publish (`.github/workflows/cd.yml`) is a manual `workflow_dispatch` (inputs: `bal_central_environment` =
  `STAGE`/`DEV`/`PROD`) that runs the same build, then `bal push`es `ballerina/target/health-tool-ballerina` to that
  Ballerina Central environment. On `PROD` it additionally cuts a GitHub Release tagged from the current `pom.xml`
  version, bumps the patch version across **all** module `pom.xml` files, and opens a "prepare for next dev cycle"
  PR back to `main`. This is the only supported publish path — there is no CLI command to unpublish or delete an
  already-published version, only `bal deprecate <org>/<name>[:<version>]` (reversible with `--undo`; does not
  delete anything, just discourages future dependency resolution onto it).

### Keeping versions in sync

The version is duplicated across `pom.xml` (root), `ballerina/pom.xml`, and every `native/*/pom.xml`. When bumping
the version manually, update all of them (see the `sed` list in `cd.yml`'s "Update version in the pom files" step
for the authoritative file list).

## Architecture

### Module graph (build order, from the root `pom.xml`)

```
native/fhir-to-bal-template   ─┐
native/fhir-to-bal-lib         │  independent Java codegen libraries
native/cds-bal-template        │  (each wraps a Tool from open-healthcare-codegen-tool-framework)
native/fhir-to-bal-connector  ─┘
native/health-cli              — CLI entry point; depends on the four modules above at runtime via reflection
ballerina                      — packages health-cli + all native jars into a `bal tool` distributable
```

- **`native/fhir-to-bal-lib`** — generates a Ballerina **package** (data types, resources, extensions) from a FHIR
  IG's `StructureDefinition`/`ValueSet`/`CodeSystem` JSON files. Version-specific logic lives under
  `packagegen/tool/modelgen/versions/{r4,r5}`; Velocity templates live in `src/main/resources/templates`.
- **`native/fhir-to-bal-template`** — generates a Ballerina **service template** (FHIR API scaffolding: resource
  handlers, OpenAPI spec, `Component.yaml`) that depends on a previously generated package (or an IG module
  embedded locally — see below). Also version-specific under `project/tool/versions/{r4,r5}`; templates in
  `src/main/resources/template`. See "Working in the Velocity templates" below before editing these.
- **`native/fhir-to-bal-connector`** — generates a Ballerina **client connector** for a remote FHIR server, driven
  by that server's `CapabilityStatement` (see `model/CapabilityStatement.java`, `model/ConnectorOperation.java`).
- **`native/cds-bal-template`** — generates a Ballerina **CDS Hooks** service template from a TOML hook-definitions
  file (`BallerinaCDSProjectTool`, templates in `src/main/resources/template`). Its `cdsBalService.vm` copyright
  header is written entirely as `##`-prefixed Velocity comments, so it's stripped at render time — CDS-generated
  files currently ship with no copyright header at all (a latent bug, not a deliberate omission).
- **`native/health-cli`** — the actual `bal health` command. `HealthCmd` (picocli root command) dispatches to
  `FhirSubCmd` / `Hl7SubCmd` / `CdsSubCmd`. Each subcommand resolves a `Handler` via
  `handler/HandlerFactory.createHandler(subCommand, mode, ...)`, keyed by `"<subcommand>:<mode>"` (e.g.
  `fhir:package`, `fhir:template`, `fhir:connector`, `cds:template`). Handlers **reflectively load** the
  corresponding `Tool`/`ToolConfig` classes from the codegen modules above (see the hardcoded class names in
  `FhirPackageGenHandler`, e.g. `org.wso2....packagegen.tool.BallerinaPackageGenTool`) rather than depending on
  them at compile time — keep those fully-qualified class name strings in sync if you rename/move classes in the
  `native/fhir-to-bal-*` modules. Per-mode default configuration (base packages, dependent packages, repository
  URLs per FHIR version, FHIR-registry defaults, etc.) lives in the JSON resources `tool-config.json` /
  `cds-tool-config.json` / `connector-tool-config.json` under `native/health-cli/src/main/resources`, and is
  overridden at runtime by CLI flags via `ToolConfig.overrideConfig(...)`.
- **`ballerina`** — not a real Ballerina package at rest; `src/main/resources/health-tool-ballerina` is a template
  that Maven's `maven-antrun-plugin`/`exec-maven-plugin` fill in at `package`/`install` time: it copies all the
  native jars into `resources/`, generates `BalTool.toml` listing them as dependencies, and runs `bal pack`. This
  is the artifact that gets `bal push`ed to Ballerina Central.
- **`mcp/health-tool-mcp`** — standalone Python **FastMCP** server (`server.py`) that shells out to the installed
  `bal health` CLI and exposes `fhirPackageGeneration`, `fhirTemplateGeneration`, and `cdsTemplateGeneration` as MCP
  tools for AI clients (e.g. Claude Desktop). It is independent of the Maven build — see
  `mcp/health-tool-mcp/README.md` for its own setup (`uv sync` / `pip install -r requirements.txt`,
  `uv run fastmcp run server.py`). Configured via `MCP_WORKSPACE`, `MCP_LOG_DIR`, `MCP_SUBPROCESS_TIMEOUT`,
  `MCP_MIN_FREE_DISK_MB`, `MCP_CLIENT_NAME` env vars; logs structured JSONL to `$MCP_LOG_DIR/mcp_io.jsonl`. Always
  passes `--minimal` for template generation, so it's unaffected by `--flat` (see below) — it was already flat.

### `bal health` command surface

```shell
bal health fhir -m package --package-name my.package.name -o output-dir spec-path
bal health fhir -m package --package-name my.package.name --ig hl7.fhir.us.core@8.0.1 -o output-dir
bal health fhir -m template --ig hl7.fhir.us.core -o output-dir
bal health fhir -m template --ig hl7.fhir.us.carin-bb.r4@2.1.0 --ig hl7.fhir.us.davinci-pdex-plan-net@1.2.0 -o output-dir
bal health fhir -m template -o output-dir spec-path
bal health fhir -m connector --config configuration-file-path -o output-dir
```

A local `spec-path` and `--ig <name>[@version]` (a registry download — see below) are alternative ways to supply
FHIR definitions to `package`/`template` mode. A local `spec-path` holds one IG's FHIR definition JSON files
directly (`StructureDefinition-*.json`, `ValueSet-*.json`, `CodeSystem-*.json`, ...) — real fixtures live under
`native/health-cli/src/test/resources/profiles.*` (each one flat, a single IG's files with no subfolders). Despite
what it might look like from `.fhir-ig-cache`'s naming, a spec directory containing several *different* IGs as
sibling subfolders is **not** something the tool or the underlying framework auto-discovers/merges on its own —
confirmed by testing (see "Multi-IG generation" below for what actually makes cross-IG merging work: repeatable
`--ig`, not a multi-subfolder local path).
`--ig` is repeatable in template mode for multi-IG generation (see below); every other mode/usage takes exactly
one.

Template mode defaults to a single **aggregated** service (`--aggregate false` for one service per FHIR resource
instead), written under `<output>/fhir-service/` unless `--flat` is passed (writes directly into `<output>` instead
— keeps `Ballerina.toml`/`.gitignore`/OAS/`.choreo`, unlike `--minimal`, which also flattens but drops all of
those). `--package-name` is mandatory in package mode; in aggregated template mode it's optional and names the
generated project itself (default `FHIRServerTemplate`) — it has no effect with `--aggregate false`.

### FHIR registry downloads (`--ig`)

`--ig <name>[@version]` (npm-style, e.g. `hl7.fhir.us.core@8.0.1`; a bare name resolves to the latest published
version, auto-selected non-interactively whenever there's no TTY) downloads an Implementation Guide from the FHIR
package registry (default `https://packages.fhir.org`, override with `--registry-url`) instead of requiring a
local `spec-path`. Implemented in `FhirIgPackageDownloader` (`native/health-cli/.../core/utils/`) and orchestrated
by `SpecificationPathResolver.resolve()`.

Two distinct, persistent locations are involved, not one:
- **`.fhir-ig-cache/<name>-<version>.tgz`** (override with `--ig-cache-dir`) — the raw downloaded archive, cached
  purely to avoid re-hitting the network on a future run. To pre-seed it yourself, the file must match this exact
  naming convention; if `--ig-cache-dir` is explicitly set and misses, a `[WARN]` says so before falling back to
  the network.
- **`spec/<sanitized-name>/`** — the *extracted* JSON files the codegen framework actually reads (it needs real
  files on disk, not a `.tgz`). This mirrors the tool's original, pre-registry-download convention of a
  user-supplied local spec directory, so it's treated as real project content, not disposable cache — unlike
  `.fhir-ig-cache/`, it is not gitignored automatically.

`downloadAndExtract()` short-circuits (no network, no extraction) if `spec/<name>/` already has valid content —
*unless* the on-disk `package.json` version doesn't match what was requested, in which case it re-fetches and logs
why (`FhirIgPackageDownloader.isVersionMismatch()` / `SpecificationPathUtils.readInstalledPackageVersion()`).
`--force-ig-download` always re-fetches regardless of either check.

`tool-config.json`'s `fhir.igRegistry.packageMappings` can suggest a known published Ballerina package for an
exact `<ig-name>@<version>` match. For a single `--ig` this is **advisory only** — it prints an `[INFO]` suggestion
and still embeds the IG locally by default; it does not auto-apply `--dependent-package` the way the
international-base-detection path does (which *does* silently substitute a published package). For 2+ `--ig`
(multi-IG generation, below), a mapping is **required and auto-applied** for every IG — there's no other way to
end up with a mixed set of dependent packages. See `tool-config.json` for the current confirmed, version-pinned
mappings — note a package's own version-ish naming suffix does *not* reliably indicate the IG version it targets
(e.g. `carinbb200` targets IG `2.1.0`, not `2.0.0`); don't infer a mapping from a package name alone, get it
confirmed by someone who knows.

### Working in the Velocity templates (`fhir-to-bal-template/src/main/resources/template/*.vm`)

Two non-obvious constraints are easy to violate and only surface as errors at `bal build` time on the *generated*
output, never at `mvn compile` time on the templates themselves:

- **Anything declared inside a `service ... on new Listener(...) { ... }` block is a service member, not a free
  function.** Calling it by bare name from another function *inside the same block* fails with "undefined
  function" — Ballerina requires `self.` to reach a service's own members. Helper functions meant to be called
  from a resource function (e.g. per-profile search stubs) must be declared *outside* the `service { ... }` block,
  at module level, alongside the type aliases.
- **`ballerinax/health.fhir.r4` ships its own compiler plugin with FHIR-specific resource-function rules**
  (diagnostic code `FHIR_103`, distinct from ordinary Ballerina `BCE` errors) — it rejects search parameters
  declared as ordinary resource-function parameters. Every generated resource function takes only
  `FHIRContext fhirContext`; any additional search parameter (e.g. `_profile`) must be read from inside the
  function body via `fhirContext.getRequestSearchParameter("<name>")` (returns
  `readonly & RequestSearchParameter[]?`; each entry has a plain `.value` string field) — never as a second
  declared parameter.

`BallerinaService`/`FHIRProfile` (`fhir-to-bal-template/.../model/`) key generation by bare FHIR resource *type*,
not by profile: when a resource type is profiled more than once — even within a single IG (e.g. US Core's ~26
profiles of `Observation`) — every profile accumulates onto the *same* `BallerinaService`
(`R4/R5BallerinaProjectTool.addResourceProfile()`), producing a union type alias. The generated search function
dispatches across them by `_profile` (per the gotcha above); every other resource function (read-by-id,
POST/PUT/PATCH/DELETE, history) stays a single generic stub regardless of profile count, since `_profile` is the
one signal the tool can dispatch on before it has a typed value in hand.

Embedding an IG as a local module (the default in template mode when `--dependent-package` isn't given) works by
running the **package** generator into a scratch directory, `<output>/.generated-ig-package/`, then copying the
resulting `.bal` sources into `<output>/modules/<ig-module-name>/`
(`FhirTemplateGenHandler.generateIgModuleSource()` / `BallerinaProjectGenerator.generateIgModule()`). The scratch
directory is deleted before a fresh generation but **not after** — it's currently left behind as a duplicate of
what's in `modules/`, a known cleanup gap.

### Verifying template-generation changes locally

Since `fhir-to-bal-template` has no automated tests, checking a template/Velocity change means actually generating
and compiling the output:

```shell
mvn clean install
cd ballerina/target/health-tool-ballerina
bal push --repository=local
bal tool pull health:<version> --repository=local   # version comes from pom.xml, e.g. health:4.0.0

cd /somewhere/scratch
bal health fhir -m template --ig hl7.fhir.us.core -o ./out   # or whatever exercises the change
cd out/fhir-service   # or ./out if --flat was used
bal build             # the real check: Velocity syntax errors and the compiler-plugin rule above
                       # only ever surface here
```

To revert to whichever `health` version was active before testing: `bal tool remove health:<version>
--repository=local` (not `bal tool pull <old-version>` — pulling a version that's already cached locally does
*not* reactivate it; removing the active version falls back to the next installed one automatically).

## Multi-IG generation

`--ig` is repeatable in template mode: `--ig hl7.fhir.us.carin-bb.r4@2.1.0 --ig hl7.fhir.us.davinci-pdex-plan-net@1.2.0`
merges profiles from both IGs for the *same* resource type onto one service, dispatched by `_profile` — e.g.
`Organization` ends up as `carinbb200:C4BBOrganization|davinciplannet120:PlannetOrganization`, each profile pulling
from its own correctly-imported package. This is a direct generalization of the multi-profile-within-one-IG
mechanism above (`addResourceProfile()`/`hasMultipleProfiles()`), not a new mechanism — verified end-to-end
(`bal build` succeeds) against the real motivating case (CARIN BB + Da Vinci PDex Plan-Net `Organization`/
`Practitioner`, matching a real hand-written facade project's `OrganizationService`/`PractitionerService`).

**Constraints:**
- Multi-IG (2+ `--ig` values) only works in template mode.
- Every IG in a multi-IG run must have a `packageMappings` entry (`tool-config.json`) — unlike single-IG, where a
  mapping is advisory-only, multi-IG **auto-applies** it per IG (there is no other way to end up with a mixed set
  of dependent packages). No mapping for one of the IGs → a clear error at resolve time, not a silent embed.
  There's no "embed one unmapped IG locally while the others use published packages" support — an IG without a
  mapping fails the whole run.
- `--dependent-package` (a single global string) is rejected when 2+ `--ig` are given — it can't apply per IG.
- Not validated: mixing FHIR versions (an r4 IG with an r5 IG) in one run. Nothing rejects this today.

### The real root cause (found by reading the actual framework source, not guessed)

The external `open-healthcare-codegen-tool-framework` never auto-discovers IG subfolders from a directory — a
directory containing several IG subfolders side by side is **not** a thing the framework understands on its own.
It only ever processes exactly the IGs explicitly given to it: `FHIRR4/R5SpecParser.parse()` iterates
`FHIRToolConfig.getIgConfigs()` (a `Map<String, IGConfig>`, each entry an explicit `name` + `dirPath`) and calls
`parseIG(toolConfig, igName, dirPath)` once per entry — the map key becomes the literal key under which
`FHIRImplementationGuide` is stored in `getFhirImplementationGuides()`, independent of any file content.

`health-cli`'s `Handler.initializeLib()` (the *only* thing that ever populated this for FHIR) never used that
per-IG-config mechanism at all — it called `specParser.parseIG(fhirToolConfig, "FHIR", specificationPath)` directly,
exactly once, hardcoded to the literal name `"FHIR"`, treating the *entire* given path as one IG's own directory.
That's why single-IG always worked regardless of naming (nothing ever depended on the real IG name), and why
pointing it at a directory with several real subfolders failed outright (`listFiles()` on the parent finds no
`.json` files directly inside it, so the one "FHIR" IG's resources map stays empty → "No services found to
generate", or "No resources found in the IG: FHIR" from the package-gen embed step) — **not** a directory-walking
gap to fix, but a call site that needed to call `parseIG()` once per real IG instead of once with a placeholder
name.

### What changed to make it work

- **`FhirSubCmd`**: `--ig` is `String[]` (repeatable); validated per element; rejects multi-IG outside template
  mode and multi-IG combined with `--dependent-package`.
- **`SpecificationPathResolver.resolve()`**: single `--ig` is untouched (own branch, same `spec/<name>/` leaf path
  as before). 2+ `--ig` downloads each into its own `spec/<name>/` (same per-IG download machinery), validates
  every one resolves via `packageMappings` (throws a clear `BallerinaHealthException` otherwise), and returns
  their shared `spec/` parent as the specification path plus a `List<ResolvedIg>` (name, version, mapped package).
- **`Handler`/`HandlerFactory`**: `Handler` gained a default `init(printStream, path, resolvedIgs)` overload
  (falls back to the existing 2-arg `init` when not overridden — every handler except `FhirTemplateGenHandler`
  behaves exactly as before). `HandlerFactory` gained a matching `createHandler(..., resolvedIgs)` overload; only
  the `fhir:template` case passes `resolvedIgs` through.
- **`FhirTemplateGenHandler`**: for 2+ resolved IGs, bypasses `Handler.initializeLib()`'s single hardcoded
  `parseIG` call with `initializeMultiIgLib()`, which calls `specParser.parseIG(fhirToolConfig, resolvedIg.igName(),
  <that IG's own spec/<name>/ dir>)` once per IG — giving the framework real per-IG identities instead of one
  placeholder. It also builds one `IncludedIGConfig` per IG (keyed by that same real name, `importStatement` =
  that IG's own `packageMappings`-resolved package) instead of the single `HealthCmdConstants.CMD_DEFAULT_IG_NAME`
  ("FHIR") entry used for single-IG/local-spec-path — and skips the embed-module and international-base-default
  logic entirely for multi-IG (neither applies once every IG already has a resolved package).
- **`AbstractBallerinaProjectTool.populateIGs()`**: single-IG's existing behavior (rename the one IG's `igMap` key
  and `IncludedIGConfig.importStatement` to the tool-wide `versionConfig.getNamePrefix()`-derived value) is
  preserved byte-for-byte for exactly one `IncludedIGConfig` entry. For 2+ entries, that rename is skipped
  entirely — each IG keeps its own real name as its `igMap` key and its own already-correct `importStatement`,
  since collapsing them onto one shared key/value (as single-IG does) would make the second IG silently overwrite
  the first.
- **`FHIRProfile.setPackagePrefix()`**: looks up `config.getIncludedIGConfigs().get(this.getIgName())` first,
  falling back to the old single global `config.getVersionConfig().getDependentPackage()` only if that profile's
  own IG isn't found — so each profile's type-alias prefix comes from its own IG's resolved package rather than
  always the tool-wide default. Backward-compatible for single-IG: after `populateIGs()`'s existing rename, both
  maps land on the same key, so the lookup returns the same value the old code path computed.
- **`ServiceGenerator`/`AggregatedServiceGenerator`**: additionally import every distinct profile-level import
  (`profile.getImportsList()`), but **only when there are 2+ `IncludedIGConfig` entries**. This guard matters:
  `profile.getImportsList()` is populated early (during `populateBalService()`, before embed-mode's real
  local-module import or an explicit `--dependent-package`'s org get resolved), so for single-IG it holds a
  stale/differently-orged value that would produce a broken or duplicate/conflicting import if surfaced — it was
  harmless before only because nothing read it. Multi-IG's `importStatement` values are never rewritten this way
  (previous point), so they're safe to surface unconditionally there.

### Known issue found (and confirmed pre-existing, unrelated to multi-IG)

Generating from a large spec in aggregated mode can hit `redeclared symbol 'search<ProfileName>'` at `bal build`
time: the per-profile dispatch-skeleton stub functions (see "Working in the Velocity templates" above) are named
by profile name alone, module-level, with no resource-type qualification. If two *different* resource types both
carry a same-named profile (observed with the raw international-base R4 core spec's generic `ExampleLipidProfile`
example profiles, reused verbatim across unrelated resources), their stub functions collide. Confirmed via
`git stash` + rebuild that this reproduces identically on the commit before multi-IG generation was implemented —
a pre-existing gap in the single-IG multi-profile dispatch work, not something multi-IG introduced. Not yet fixed;
a real fix needs the stub function name qualified by resource type, not just profile name.

## `packageMappings` (`tool-config.json`'s `fhir.igRegistry.packageMappings`)

`IgRegistryConfig.resolveDependentPackage(igName, igVersion)` is version-aware: it looks up
`packageMappings.get(igName + "@" + igVersion)`, matching `--ig`'s own npm-style grammar, so a mapping only ever
applies to the exact IG version it was confirmed for — it was originally keyed by bare IG name alone, which meant
a mapping intended for one specific version silently matched *any* requested version of that IG (confirmed live:
the `hl7.fhir.us.core` entry used to fire even when `--ig` resolved to latest/`9.0.0`, not just its intended
`6.1.0`). Don't reintroduce a bare-name key — it would bring the same bug back.

**Still advisory-only, not auto-applying, for single-IG runs.** A match prints an `[INFO]` suggestion; the IG
still gets embedded locally by default unless the user adds `--dependent-package` themselves. Whether an exact
match should instead **auto-apply** `--dependent-package` for a single `--ig` (the way the international-base-
detection path already does silently) is still an open decision, not yet made — but multi-IG generation (above)
*does* decide this narrowly for its own case: resolution there is always auto-applied per IG (enforced — a
multi-IG run fails outright if any IG has no mapping), since there's no other way to end up with a mixed set of
dependent packages.

See `tool-config.json` for the currently confirmed, version-pinned mappings. A package's own version-ish naming
suffix does not reliably indicate the IG version it targets — these were confirmed by someone who knows, not
inferred from the name (e.g. `carinbb200` targets IG `2.1.0`, not `2.0.0`). Get confirmation before adding more;
don't guess from the package name.
