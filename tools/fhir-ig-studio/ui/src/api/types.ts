/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/** Generation modes exposed by the service. */
export type GenerationMode = 'package' | 'template';

/** Where the FHIR definitions for a run come from. */
export type SourceType = 'registry' | 'upload' | 'default';

export type JobStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED';

export interface SuggestedIg {
  name: string;
  title: string;
  fhirVersion: string;
  description: string;
}

export interface ServiceMeta {
  service: string;
  version: string;
  modes: GenerationMode[];
  sourceTypes: SourceType[];
  registryUrl: string;
  defaultPackages: Record<string, string>;
  suggestedIgs: SuggestedIg[];
  defaults: {
    orgName: string;
    packageVersion: string;
    aggregate: boolean;
    minimal: boolean;
  };
  workerThreads: number;
  jobTimeoutSeconds: number;
}

/** One hit from a registry catalog search. */
export interface RegistrySearchResult {
  name: string;
  title: string;
  fhirVersion: string | null;
  description: string | null;
}

export interface RegistrySearchResponse {
  query: string;
  total: number;
  results: RegistrySearchResult[];
  message?: string;
}

/** The FHIR resource types a package profiles: the APIs a generated facade can expose. */
export interface IgResourceTypes {
  name: string;
  version: string;
  resourceTypes: string[];
}

/** One profiling-rule finding raised when a definition is saved. */
export interface RuleFinding {
  severity: 'error' | 'warning';
  path: string;
  message: string;
}

/** One element of a profile, with what it currently says and what its base allows. */
export interface ProfileElement {
  path: string;
  sliceName: string | null;
  id: string | null;
  constrained: boolean;
  current: {
    min: number | null;
    max: string | null;
    mustSupport: boolean | null;
    short: string | null;
    definition: string | null;
    comment: string | null;
    types: string[];
    bindingStrength: string | null;
    bindingValueSet: string | null;
  };
  limits: {
    minMin: number | null;
    maxMax: string | null;
    allowedTypes: string[] | null;
    minBindingStrength: string | null;
    knownToBase: boolean;
  };
}

/** The constraint changes an editor may send for one element. */
export interface ElementChanges {
  min?: number | null;
  max?: string | null;
  mustSupport?: boolean | null;
  short?: string | null;
  types?: string[];
  bindingStrength?: string | null;
  bindingValueSet?: string | null;
}

/** A FHIR package registry the picker can point at. */
export interface RegistryOption {
  url: string;
  name: string;
  isDefault: boolean;
}

/** An opened FHIR package whose definitions can be browsed and edited. */
export interface PackageWorkspace {
  workspaceId: string;
  packageName: string;
  version: string;
  registryUrl: string | null;
  fhirVersion: string | null;
  definitionCount: number;
  defaultProjectName: string;
  editedCount: number;
  /** False for uploaded archives: there is no published copy to restore them from. */
  resettable?: boolean;
  /** What the guide says about itself, read from the package's own manifest. */
  title: string | null;
  description: string | null;
  publisher: string | null;
  canonical: string | null;
}


/** One StructureDefinition in an opened package. */
export interface DefinitionSummary {
  file: string;
  id: string | null;
  name: string | null;
  title: string | null;
  url: string | null;
  resourceType: string | null;
  kind: string | null;
  derivation: string | null;
  status: string | null;
  description: string | null;
  baseDefinition: string | null;
  /** Where an extension may be used; empty for anything that is not an extension. */
  contexts: string[];
  edited: boolean;
}

/** A slice to add to a repeating element. */
export interface NewSlice {
  path: string;
  sliceName: string;
  min: number;
  max: string;
  mustSupport: boolean;
  extensionUrl?: string;
  description?: string;
}

/** An extension just created, complete enough to select without reloading the definition list. */
export interface CreatedExtension {
  created: boolean;
  file: string;
  id: string | null;
  name: string | null;
  title: string | null;
  url: string | null;
  contexts: string[];
}

/** What the editor needs to author a new extension. */
/** Something a new profile can be based on: a resource in this package, or one from the FHIR core. */
export interface ProfileBase {
  url: string;
  label: string;
  resourceType: string;
  /** "package" for this guide's own resources, otherwise the FHIR version, e.g. "R4". */
  source: string;
}

export interface NewProfile {
  id: string;
  title?: string;
  description?: string;
  baseUrl: string;
}

export interface CreatedProfile {
  created: boolean;
  file: string;
  id: string;
  name: string;
  url: string;
  resourceType: string;
}

export interface NewExtension {
  id: string;
  title?: string;
  description?: string;
  contexts: string[];
  valueTypes: string[];
  required: boolean;
  repeats: boolean;
}

export interface IgVersions {
  name: string;
  latest: string | null;
  versions: string[];
  description: string | null;
  fhirVersion: string | null;
  mappedPackage: string | null;
}

export interface SpecSource {
  type: SourceType;
  igName?: string | null;
  igVersion?: string | null;
  registryUrl?: string | null;
  forceDownload?: boolean;
  specId?: string | null;
  fhirVersion?: string;
}

export interface GenerationRequest {
  mode: GenerationMode;
  source: SpecSource;
  packageName?: string;
  orgName?: string;
  packageVersion?: string;
  dependentPackage?: string | null;
  igModuleName?: string | null;
  dependentIgs?: string[];
  includedProfiles?: string[];
  excludedProfiles?: string[];
  aggregate?: boolean;
  resources?: string[];
  minimal?: boolean;
  label?: string;
}

export interface JobSummary {
  id: string;
  mode: GenerationMode;
  label: string;
  status: JobStatus;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
  durationMillis: number | null;
  error: string | null;
  /** Which stage the run is in, and how far along; the log itself is not exposed. */
  stage?: string;
  stageTitle?: string;
  progress?: number;
}

/** One named step of a generation, as the service defines them. */
export interface JobStage {
  key: string;
  title: string;
  percent: number;
}

/** What the download contains, by name only, so the page can print a command that actually works. */
export interface JobFacade {
  archiveRoot: string;
  project: string;
  jar: string;
  port: number;
  sampleResource: string;
}

export interface JobDetail extends JobSummary {
  request: GenerationRequest;
  facade?: JobFacade;
  /** The archive has been streamed at least once. */
  downloaded?: boolean;
  /** The generated files have been reclaimed; the archive can no longer be fetched. */
  collected?: boolean;
  collectedAt?: string | null;
  artifacts: {
    fileCount: number;
    directoryCount: number;
    totalBytes: number;
  };
}

export interface LogSlice {
  status: JobStatus;
  from: number;
  nextIndex: number;
  lines: string[];
  done: boolean;
}

export interface ArtifactNode {
  name: string;
  path: string;
  type: 'file' | 'directory';
  size: number;
  children: ArtifactNode[] | null;
}

export interface ArtifactTree {
  root: string;
  tree: ArtifactNode;
}

export interface ArtifactContent {
  path: string;
  size: number;
  truncated: boolean;
  binary?: boolean;
  content: string | null;
  message?: string;
}

export interface UploadedSpec {
  specId: string;
  name: string;
  fileCount: number;
  definitionCount: number;
  totalBytes: number;
}

/** Whether a local model is answering, and which one the service will ask. */
export interface ModelStatus {
  /** False when no chat API is configured; the tool works fully without one. */
  configured?: boolean;
  url?: string;
  model?: string;
  available: boolean;
  /** "openai", "azure" or "anthropic" — which chat API the service is speaking. */
  provider?: string;
  /** Whether an API key is configured. Never the key itself. */
  authenticated?: boolean;
  detail?: string;
}

/** An element the reader picked out of the table to talk about. */
export interface TaggedElement {
  path: string;
  sliceName: string | null;
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

/**
 * A change the model drafted. Nothing is written until it is applied.
 *
 * Two shapes: `constrain` narrows an existing element, and `extension` hangs a new extension slice off one.
 * The second takes two service calls -- author the extension, then slice the element to it -- which is why it
 * could not be expressed as an element change.
 */
export interface ChatProposal {
  kind?: 'constrain' | 'extension';
  file: string;
  path: string;
  sliceName?: string | null;
  reason?: string;
  /** The element's values as they stand, so the panel can show a change rather than a destination. */
  current?: ProfileElement['current'];
  /** Present when kind is 'constrain'. */
  changes?: ElementChanges;
  /** Present when kind is 'extension' and a new extension should be authored. */
  extension?: Omit<NewExtension, 'contexts'> & { contexts?: string[] };
  /** Present when kind is 'extension' and an existing extension should be bound instead. */
  extensionUrl?: string;
  min?: number;
  max?: string;
  mustSupport?: boolean;
}

export interface ChatAnswer {
  reply: string;
  proposals: ChatProposal[];
  /** What the agent read on the way to answering; shown so a slow turn is legible. */
  lookups?: { what: string }[];
  /** The question was outside the Dev Bot's scope, and the reply is the service's standard refusal. */
  outOfScope?: boolean;
}
