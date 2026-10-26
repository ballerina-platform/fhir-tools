# FHIR Codegen UI

Web front end for the [FHIR codegen service](../service). Built with React, Vite and the
[WSO2 Oxygen UI](https://github.com/wso2/oxygen-ui) design system, so it matches the look of other WSO2 consoles.

## Prerequisites

- Node.js 20.19+ or 22.12+ (Vite 7 requirement)
- The codegen service running on port 9095

## Development

```shell
npm install
npm run dev          # http://localhost:5173, proxies /api to the service
```

Set `FHIR_TOOL_SERVICE_PORT` before `npm run dev` if the service listens elsewhere.

## Smoke check

```shell
npm run build
npm run smoke -- "/packages/hl7.fhir.us.core-6.1.0" "USCorePatientProfile"
```

Loads the built bundle in a DOM against a running service and fails on anything the page throws, clicking each
label given after the route. It catches what `tsc` cannot -- a hook reaching a binding before it is initialised,
for one -- and clicking matters: those faults usually need state that only exists after an interaction.

## Production build

```shell
npm run build        # emits dist/
```

The service serves `dist/` automatically when it is present. Point it elsewhere with
`-Dfhir.tool.service.uiDir=/path/to/dist`.

## What the UI does

- **Generate** — a four-step wizard: pick the artifact (package or service template), choose where the FHIR
  definitions come from (registry, uploaded bundle or HL7 core), set the package and API options, then review the
  exact request body before submitting. Registry versions and the "already published as" hint are read live from
  the service.
- **Generations** — every run with its status, duration and a zip download.
- **Generation detail** — the worker log streamed while the job runs, then the generated file tree with syntax
  highlighted previews.
- **Specifications** — upload a `.zip` of FHIR definitions to generate from profiles that are not on the registry.

## Layout

```
src/
├── api/          typed REST client and request/response shapes
├── components/   artifact browser, log console, status chip
├── layout/       Oxygen UI app shell (header, sidebar, footer)
└── pages/        Generate wizard, Generations, Generation detail, Specifications
```
