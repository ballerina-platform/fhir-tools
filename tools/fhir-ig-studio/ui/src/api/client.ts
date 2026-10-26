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

import { clientId } from './clientId';
import type {
  ArtifactContent,
  ArtifactTree,
  ChatAnswer,
  ChatMessage,
  CreatedExtension,
  CreatedProfile,
  DefinitionSummary,
  ElementChanges,
  GenerationRequest,
  IgResourceTypes,
  IgVersions,
  JobDetail,
  JobStage,
  JobSummary,
  LogSlice,
  ModelStatus,
  NewExtension,
  NewProfile,
  NewSlice,
  PackageWorkspace,
  ProfileBase,
  ProfileElement,
  RegistryOption,
  RegistrySearchResponse,
  RuleFinding,
  ServiceMeta,
  TaggedElement,
  UploadedSpec,
} from './types';

/**
 * The UI is served by the codegen service itself, so requests are relative. During `npm run dev` Vite proxies
 * /api to the service port.
 */
const API_BASE = '/api';

/** Names the browser to the service. Must match the header the service reads. */
const CLIENT_HEADER = 'X-Client-Id';

/** Error carrying the HTTP status so callers can distinguish validation failures from outages. */
export class ApiError extends Error {
  readonly status: number;
  /** The parsed error body, so callers can read structured detail such as profiling-rule findings. */
  readonly payload: unknown;

  constructor(status: number, message: string, payload?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    // Every call carries who is asking, so the service can keep this browser's working copies to itself.
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: { ...(init?.headers ?? {}), [CLIENT_HEADER]: clientId() },
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the codegen service. Is it running?');
  }
  const text = await response.text();
  const payload = text ? safeParse(text) : null;
  if (!response.ok) {
    const message =
      payload && typeof payload === 'object' && 'error' in payload
        ? String((payload as { error: unknown }).error)
        : `Request failed with status ${response.status}`;
    throw new ApiError(response.status, message, payload);
  }
  return payload as T;
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return { error: text };
  }
}

function json(body: unknown): RequestInit {
  return {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  };
}

export const api = {
  getMeta: () => request<ServiceMeta>('/meta'),

  /** The named stages a generation goes through, so the page can show the whole sequence. */
  listStages: () => request<{ stages: JobStage[] }>('/stages').then((r) => r.stages),

  listRegistries: () =>
    request<{ registries: RegistryOption[]; defaultUrl: string }>('/registries'),

  searchIgs: (query: string, registryUrl?: string) =>
    request<RegistrySearchResponse>(
      `/igs/search?q=${encodeURIComponent(query)}${registryUrl ? `&registryUrl=${encodeURIComponent(registryUrl)}` : ''}`,
    ),

  getIgResourceTypes: (igName: string, version: string) =>
    request<IgResourceTypes>(
      `/igs/${encodeURIComponent(igName)}/resources?version=${encodeURIComponent(version)}`,
    ),

  getIgVersions: (igName: string, registryUrl?: string, refresh = false) => {
    const params = new URLSearchParams();
    if (refresh) params.set('refresh', 'true');
    if (registryUrl) params.set('registryUrl', registryUrl);
    const query = params.toString();
    return request<IgVersions>(`/igs/${encodeURIComponent(igName)}/versions${query ? `?${query}` : ''}`);
  },

  openPackage: (name: string, version: string, registryUrl?: string) =>
    request<PackageWorkspace>('/packages/open', json({ name, version, registryUrl })),

  /** Whether a local model is reachable, and which one is configured. */
  getModelStatus: () => request<ModelStatus>('/model'),

  /** Asks about the open package. The answer may carry proposals; none of them are applied. */
  chat: (
    workspaceId: string,
    file: string | null,
    messages: ChatMessage[],
    focus: TaggedElement[] = [],
    applied: { summary: string }[] = [],
  ) =>
    request<ChatAnswer>(`/packages/${encodeURIComponent(workspaceId)}/chat`,
      json({ file, messages, focus, applied })),

  getWorkspace: (workspaceId: string) =>
    request<PackageWorkspace>(`/packages/${encodeURIComponent(workspaceId)}`),

  listDefinitions: (workspaceId: string) =>
    request<{ definitions: DefinitionSummary[] }>(
      `/packages/${encodeURIComponent(workspaceId)}/definitions`,
    ).then((r) => r.definitions),

  getDefinition: (workspaceId: string, file: string) =>
    request<{ file: string; content: string }>(
      `/packages/${encodeURIComponent(workspaceId)}/definitions/${encodeURIComponent(file)}`,
    ).then((r) => r.content),

  /**
   * Puts a differential the browser was holding back onto a definition. The service rebuilds the snapshot and
   * validates against the base, exactly as it does for an edit made in the page.
   */
  applyDifferential: (workspaceId: string, file: string, differential: unknown) =>
    request<{ definition: DefinitionSummary }>(
      `/packages/${encodeURIComponent(workspaceId)}/definitions/${encodeURIComponent(file)}/differential`,
      { ...json({ differential }), method: 'PUT' },
    ),

  getDefinitionTable: (workspaceId: string, file: string, view: 'differential' | 'snapshot') =>
    request<{ file: string; supported: boolean; html?: string }>(
      `/packages/${encodeURIComponent(workspaceId)}/definitions/${encodeURIComponent(file)}/table?view=${view}`,
    ),

  getExtensionValueTypes: (workspaceId: string) =>
    request<{ valueTypes: string[] }>(`/packages/${encodeURIComponent(workspaceId)}/extensions`).then(
      (r) => r.valueTypes,
    ),

  /** What a new profile could be based on: this package's own resources, then the FHIR core's. */
  listProfileBases: (workspaceId: string) =>
    request<{ bases: ProfileBase[] }>(`/packages/${encodeURIComponent(workspaceId)}/profiles`)
      .then((r) => r.bases),

  createProfile: (workspaceId: string, profile: NewProfile) =>
    request<CreatedProfile>(`/packages/${encodeURIComponent(workspaceId)}/profiles`, json(profile)),

  createExtension: (workspaceId: string, extension: NewExtension) =>
    request<CreatedExtension>(
      `/packages/${encodeURIComponent(workspaceId)}/extensions`,
      json(extension),
    ),

  addSlice: (workspaceId: string, file: string, slice: NewSlice) =>
    request<{ created: boolean; findings?: RuleFinding[] }>(
      `/packages/${encodeURIComponent(workspaceId)}/definitions/${encodeURIComponent(file)}/slices`,
      json(slice),
    ),

  listElements: (workspaceId: string, file: string) =>
    request<{ elements: ProfileElement[]; bindingStrengths: string[] }>(
      `/packages/${encodeURIComponent(workspaceId)}/definitions/${encodeURIComponent(file)}/elements`,
    ),

  applyElementChanges: (
    workspaceId: string,
    file: string,
    path: string,
    sliceName: string | null,
    changes: ElementChanges,
  ) =>
    request<{ saved: boolean; findings?: RuleFinding[] }>(
      `/packages/${encodeURIComponent(workspaceId)}/definitions/${encodeURIComponent(file)}/elements`,
      json({ path, sliceName, changes }),
    ),

  saveDefinition: (workspaceId: string, file: string, content: string) =>
    request<{ file: string; saved: boolean; findings?: RuleFinding[] }>(
      `/packages/${encodeURIComponent(workspaceId)}/definitions/${encodeURIComponent(file)}`,
      { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: content },
    ),

  resetWorkspace: (workspaceId: string) =>
    request<PackageWorkspace>(`/packages/${encodeURIComponent(workspaceId)}/reset`, { method: 'POST' }),

  generateFacade: (workspaceId: string, projectName: string) =>
    request<JobDetail>(`/packages/${encodeURIComponent(workspaceId)}/generate`, json({ projectName })),

  listSpecs: () => request<{ specs: UploadedSpec[] }>('/specs').then((r) => r.specs),

  uploadSpec: (file: File, name: string) =>
    request<UploadedSpec>(`/specs?name=${encodeURIComponent(name)}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/zip' },
      body: file,
    }),

  deleteSpec: (specId: string) =>
    request<{ deleted: string }>(`/specs/${encodeURIComponent(specId)}`, { method: 'DELETE' }),

  createJob: (body: GenerationRequest) => request<JobDetail>('/jobs', json(body)),

  listJobs: () => request<{ jobs: JobSummary[] }>('/jobs').then((r) => r.jobs),

  getJob: (id: string) => request<JobDetail>(`/jobs/${encodeURIComponent(id)}`),

  getLogs: (id: string, from: number) =>
    request<LogSlice>(`/jobs/${encodeURIComponent(id)}/logs?from=${from}`),

  getTree: (id: string) => request<ArtifactTree>(`/jobs/${encodeURIComponent(id)}/tree`),

  getFile: (id: string, path: string) =>
    request<ArtifactContent>(`/jobs/${encodeURIComponent(id)}/file?path=${encodeURIComponent(path)}`),

  cancelJob: (id: string) =>
    request<{ cancelled: boolean }>(`/jobs/${encodeURIComponent(id)}/cancel`, { method: 'POST' }),

  deleteJob: (id: string) =>
    request<{ deleted: boolean }>(`/jobs/${encodeURIComponent(id)}`, { method: 'DELETE' }),

  /**
   * Tells the service the job page has been left, so it can reclaim the generated files.
   *
   * <p>Sent with keepalive so it still goes out when the tab is closing. The service ignores it unless the
   * archive was actually downloaded, so a page opened and abandoned loses nothing.
   */
  releaseJob: (id: string) => {
    try {
      void fetch(`${API_BASE}/jobs/${encodeURIComponent(id)}/collect`, {
        method: 'POST',
        keepalive: true,
        headers: { 'X-Client-Id': clientId() },
      }).catch(() => undefined);
    } catch {
      // Best effort: failing to say goodbye only means the files wait for the next chance.
    }
  },

  /**
   * Downloads the package as it now stands, edits included, laid out the way a FHIR package is.
   *
   * <p>Fetched rather than linked. A link is a plain browser navigation, which carries no headers -- so the
   * service saw an anonymous caller and refused a workspace that belongs to this browser. Going through the
   * same request path as everything else keeps one way of saying who is asking, and the file is handed to the
   * browser from memory once it has arrived.
   */
  downloadPackage: async (workspaceId: string, fileName: string): Promise<void> => {
    const suggestedName = fileName;
    // The folder inside the archive is named after the project too, so an unzipped copy is recognisable.
    const folder = fileName.replace(/\.zip$/i, '');
    const response = await fetch(
      `${API_BASE}/packages/${encodeURIComponent(workspaceId)}/archive?name=${encodeURIComponent(folder)}`,
      { headers: { 'X-Client-Id': clientId() } },
    );
    if (!response.ok) {
      const text = await response.text();
      const payload = text ? safeParse(text) : null;
      throw new ApiError(
        response.status,
        payload && typeof payload === 'object' && 'error' in payload
          ? String((payload as { error: unknown }).error)
          : `Download failed with status ${response.status}`,
        payload,
      );
    }
    // The caller's name wins: the project's name lives in this browser and the service has never seen it, so
    // its own Content-Disposition can only ever offer the published package's name as a fallback.
    const disposition = response.headers.get('Content-Disposition') ?? '';
    const fromService = /filename="([^"]+)"/.exec(disposition)?.[1];
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    try {
      const link = document.createElement('a');
      link.href = url;
      link.download = suggestedName || fromService || 'fhir-package.zip';
      document.body.appendChild(link);
      link.click();
      link.remove();
    } finally {
      URL.revokeObjectURL(url);
    }
  },

  archiveUrl: (id: string) => `${API_BASE}/jobs/${encodeURIComponent(id)}/archive`,
};
