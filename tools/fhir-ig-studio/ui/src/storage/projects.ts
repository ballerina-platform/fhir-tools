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

import type { NewExtension, PackageWorkspace } from '../api/types';

/**
 * The browser is where a person's work lives.
 *
 * <p>The service is a shared scratch space: several people can point at one instance, and its workspace
 * directory is fair game to clean out. Neither is true of localStorage, so that is what holds which packages
 * are yours and what you changed in them.
 *
 * <p>What is stored is the *differential*, not the definition. A FHIR snapshot is derived -- the service
 * regenerates it after every edit -- so keeping a copy would be storing the same information twice in a form
 * about sixty times larger: 1.3 KB against 75 KB for a real profile. Reopening a package therefore means
 * handing each differential back and letting the service rebuild the rest.
 *
 * <p>A whole package cannot live here at all: they run 3-7 MB against a roughly 5 MB origin quota. Only the
 * changes do, which is why the number of packages is capped rather than the bytes being merely hoped about.
 */

const KEY = 'fhir-codegen.projects';

/** How many packages may be under edit at once. Well inside the quota; the point is a list a person can hold. */
export const MAX_PACKAGES = 10;

/** Written bytes are kept under this, leaving the rest of the origin's quota for everything else. */
const BUDGET_BYTES = 3_500_000;

/** A package someone has taken up, and the changes they have made to it. */
export interface StoredProject {
  workspaceId: string;
  packageName: string;
  version: string;
  registryUrl: string | null;
  title: string | null;
  fhirVersion: string | null;
  /** Null until the work is given a name; it is listed either way. */
  projectName: string | null;
  savedAt: string | null;
  /** Definition file name -> that definition's differential, which is the whole of an edit to it. */
  edits: Record<string, unknown>;
  /** Extensions authored here, replayed by asking the service to create them again. */
  extensions: NewExtension[];
}

/** Raised when a new package would exceed the cap, so the caller can say which limit was hit. */
export class ProjectLimitError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ProjectLimitError';
  }
}

/** Raised when the browser refuses the write, or it would exceed the budget this tool holds itself to. */
export class StorageFullError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'StorageFullError';
  }
}

function read(): StoredProject[] {
  try {
    const raw = window.localStorage.getItem(KEY);
    if (!raw) {
      return [];
    }
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? (parsed as StoredProject[]) : [];
  } catch {
    // Unreadable storage is treated as empty rather than fatal: the packages themselves are still on the
    // service, so the worst case is a list that has to be rebuilt, not work that cannot be reached.
    return [];
  }
}

function write(projects: StoredProject[]): void {
  const payload = JSON.stringify(projects);
  if (payload.length > BUDGET_BYTES) {
    throw new StorageFullError(
      'There is no room left in browser storage for this change. Remove a package from My packages to free some.',
    );
  }
  try {
    window.localStorage.setItem(KEY, payload);
  } catch {
    throw new StorageFullError(
      'The browser refused to store this change. Remove a package from My packages, or free up site data.',
    );
  }
}

/** Most recently saved first; work that was never named follows, by package name. */
function ordered(projects: StoredProject[]): StoredProject[] {
  return [...projects].sort((a, b) => {
    if (a.savedAt !== b.savedAt) {
      return (b.savedAt ?? '').localeCompare(a.savedAt ?? '');
    }
    return a.packageName.localeCompare(b.packageName);
  });
}

export const projectStore = {
  list(): StoredProject[] {
    return ordered(read());
  },

  get(workspaceId: string): StoredProject | null {
    return read().find((project) => project.workspaceId === workspaceId) ?? null;
  },

  remove(workspaceId: string): void {
    write(read().filter((project) => project.workspaceId !== workspaceId));
  },

  /** What is used and what is left, for telling someone where they stand before they hit the wall. */
  usage(): { count: number; max: number; bytes: number; budget: number } {
    const projects = read();
    return {
      count: projects.length,
      max: MAX_PACKAGES,
      bytes: JSON.stringify(projects).length,
      budget: BUDGET_BYTES,
    };
  },

  /**
   * Takes up a package, creating its entry if this is the first change to it.
   *
   * @throws ProjectLimitError when a new package would pass the cap
   */
  open(workspace: PackageWorkspace): StoredProject {
    const projects = read();
    const existing = projects.find((project) => project.workspaceId === workspace.workspaceId);
    if (existing) {
      return existing;
    }
    if (projects.length >= MAX_PACKAGES) {
      throw new ProjectLimitError(
        `Only ${MAX_PACKAGES} packages can be under edit at once, because the changes are kept in this browser. `
          + 'Remove one from My packages to take up another.',
      );
    }
    const created: StoredProject = {
      workspaceId: workspace.workspaceId,
      packageName: workspace.packageName,
      version: workspace.version,
      registryUrl: workspace.registryUrl,
      title: workspace.title,
      fhirVersion: workspace.fhirVersion,
      projectName: null,
      savedAt: null,
      edits: {},
      extensions: [],
    };
    write([...projects, created]);
    return created;
  },

  /** Records a definition's differential as it now stands. Replaces any earlier one for the same file. */
  recordEdit(workspace: PackageWorkspace, file: string, differential: unknown): void {
    this.open(workspace);
    const projects = read();
    const project = projects.find((p) => p.workspaceId === workspace.workspaceId)!;
    project.edits = { ...project.edits, [file]: differential };
    write(projects);
  },

  /** Records an extension authored here, so reopening the package can create it again. */
  recordExtension(workspace: PackageWorkspace, extension: NewExtension): void {
    this.open(workspace);
    const projects = read();
    const project = projects.find((p) => p.workspaceId === workspace.workspaceId)!;
    project.extensions = [
      ...project.extensions.filter((candidate) => candidate.id !== extension.id),
      extension,
    ];
    write(projects);
  },

  /** Names the work, which is what makes it findable later. */
  save(workspace: PackageWorkspace, name: string): StoredProject {
    this.open(workspace);
    const projects = read();
    const project = projects.find((p) => p.workspaceId === workspace.workspaceId)!;
    project.projectName = name.trim();
    project.savedAt = new Date().toISOString();
    write(projects);
    return project;
  },

  /** Drops every change to one package without forgetting the package itself. */
  clearEdits(workspaceId: string): void {
    const projects = read();
    const project = projects.find((p) => p.workspaceId === workspaceId);
    if (!project) {
      return;
    }
    project.edits = {};
    project.extensions = [];
    write(projects);
  },
};
