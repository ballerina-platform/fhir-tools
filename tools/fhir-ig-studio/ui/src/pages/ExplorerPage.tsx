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

import {
  Alert,
  Badge,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Drawer,
  IconButton,
  PageContent,
  PageTitle,
  Paper,
  Tab,
  Tabs,
  TextField,
  Tooltip,
  Typography,
} from '@wso2/oxygen-ui';
import {
  ArrowLeft,
  CircleQuestionMark,
  MessageSquare,
  PanelLeftClose,
  PanelLeftOpen,
  Play,
  Plus,
  RotateCcw,
  Save,
} from '@wso2/oxygen-ui-icons-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { api, ApiError } from '../api/client';
import type {
  CreatedExtension,
  TaggedElement,
  DefinitionSummary,
  NewExtension,
  PackageWorkspace,
  ProfileElement,
  RuleFinding,
} from '../api/types';
import ElementTable, { type FhirElement } from '../components/ElementTable';
import ConstraintEditor from '../components/ConstraintEditor';
import ChatPanel from '../components/ChatPanel';
import NewExtensionDialog from '../components/NewExtensionDialog';
import NewProfileDialog from '../components/NewProfileDialog';
import { MAX_PACKAGES, projectStore, type StoredProject } from '../storage/projects';
import IgTable from '../components/IgTable';
import ProfileWalkthrough from '../components/ProfileWalkthrough';
import WorkflowGuide, { rememberWorkflowGuide, workflowGuideDismissed } from '../components/WorkflowGuide';

/**
 * Both element views a published IG site offers. The differential is what this guide changes; the snapshot is
 * the fully resolved list including everything inherited from the base resource.
 */
function readViews(raw: string): { differential: FhirElement[]; snapshot: FhirElement[] } {
  try {
    const json = JSON.parse(raw);
    return {
      differential: (json?.differential?.element ?? []) as FhirElement[],
      snapshot: (json?.snapshot?.element ?? []) as FhirElement[],
    };
  } catch {
    return { differential: [], snapshot: [] };
  }
}

/**
 * Browses an opened package the way Simplifier does: the guide's StructureDefinitions on the left, the selected
 * one on the right as either its element list or its raw JSON. The JSON is editable, and generating a facade
 * uses the definitions as they currently stand, so a change can be tried out immediately.
 */
/** Extensions are profiles of the Extension datatype rather than of a resource. */
function isExtension(definition: DefinitionSummary): boolean {
  return definition.resourceType === 'Extension';
}

type DefinitionKind = 'resource' | 'datatype' | 'extension' | 'other';

/**
 * What a definition is, in the terms the sidebar divides a package by. A guide is usually a mix of all three:
 * AU Base, for instance, is 32 resource profiles, 38 datatype profiles and 35 extensions, and looking for one
 * kind means wading past the other two.
 */
function kindOf(definition: DefinitionSummary): DefinitionKind {
  if (isExtension(definition)) {
    return 'extension';
  }
  if (definition.kind === 'resource') {
    return 'resource';
  }
  if (definition.kind === 'complex-type' || definition.kind === 'primitive-type') {
    return 'datatype';
  }
  return 'other';
}

/** Tag order, which is also the order the list is grouped in. */
const KINDS: [DefinitionKind, string][] = [
  ['resource', 'Resources'],
  ['datatype', 'Data types'],
  ['extension', 'Extensions'],
  ['other', 'Other'],
];

export default function ExplorerPage() {
  const { workspaceId = '' } = useParams();
  const navigate = useNavigate();

  const [workspace, setWorkspace] = useState<PackageWorkspace | null>(null);
  const [definitions, setDefinitions] = useState<DefinitionSummary[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [filter, setFilter] = useState('');
  /** The kinds the list is narrowed to; empty means every kind. */
  const [kinds, setKinds] = useState<Set<DefinitionKind>>(new Set());

  const [selected, setSelected] = useState<DefinitionSummary | null>(null);
  const [content, setContent] = useState('');
  const [draft, setDraft] = useState('');
  const [loadingContent, setLoadingContent] = useState(false);
  // Snapshot first: it shows the whole resource as the profile leaves it, which is what most people open a
  // profile to see. Differential is one tab away for what the guide itself changed.
  const [tab, setTab] = useState(1);

  const [tableHtml, setTableHtml] = useState<Record<string, string | null>>({});
  const [profileElements, setProfileElements] = useState<ProfileElement[]>([]);
  const [bindingStrengths, setBindingStrengths] = useState<string[]>([]);
  const [tableLoading, setTableLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [findings, setFindings] = useState<RuleFinding[]>([]);
  const [resetting, setResetting] = useState(false);

  const [newExtensionOpen, setNewExtensionOpen] = useState(false);
  const [newProfileOpen, setNewProfileOpen] = useState(false);
  const [descriptionOpen, setDescriptionOpen] = useState(false);
  const [projectName, setProjectName] = useState('');
  const [generating, setGenerating] = useState(false);
  const [savingProject, setSavingProject] = useState(false);
  const [chatOpen, setChatOpen] = useState(false);
  /** The how-to-use-this-page guide, until it is put away; the header's ? button brings it back. */
  const [guideOpen, setGuideOpen] = useState(() => !workflowGuideDismissed());
  const [walkthrough, setWalkthrough] = useState(false);
  /** Collapses the definition list, so the element table gets the room instead. */
  const [listCollapsed, setListCollapsed] = useState(false);
  /**
   * Bumped by every edit, and part of the rendered table's cache key.
   *
   * <p>The key used to carry the definition's JSON length, on the assumption that an edit changes it. Mostly it
   * does -- but "mostly" is the wrong standard for deciding whether to refetch, and a change that happens to
   * leave the length alone left the old table on screen looking as though nothing had been applied.
   */
  const [editVersion, setEditVersion] = useState(0);
  /** Elements picked out of the table to talk about, in the order they were picked. */
  const [tagged, setTagged] = useState<TaggedElement[]>([]);

  /** Tagging opens the chat if it is closed: picking an element to ask about implies wanting to ask. */
  const tagElement = useCallback((path: string, sliceName: string | null) => {
    setChatOpen(true);
    setTagged((current) =>
      current.some((entry) => entry.path === path && entry.sliceName === sliceName)
        ? current
        : [...current, { path, sliceName }],
    );
  }, []);
  const [generateError, setGenerateError] = useState<string | null>(null);
  /** This package's entry in browser storage, once it has any changes. */
  const [stored, setStored] = useState<StoredProject | null>(() => projectStore.get(workspaceId));
  /** Storage refused a change -- the cap, or the browser itself. Shown rather than swallowed. */
  const [storeError, setStoreError] = useState<string | null>(null);
  const [replaying, setReplaying] = useState(false);

  const refresh = useCallback(async () => {
    try {
      const [ws, defs] = await Promise.all([
        api.getWorkspace(workspaceId),
        api.listDefinitions(workspaceId),
      ]);
      setWorkspace(ws);
      setDefinitions(defs);
      setProjectName((current) => current || ws.defaultProjectName);
      setLoadError(null);
    } catch (e) {
      setLoadError(e instanceof ApiError ? e.message : String(e));
    }
  }, [workspaceId]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  useEffect(() => {
    setTagged([]);
  }, [selected?.file]);

  useEffect(() => {
    if (!selected) return;
    let cancelled = false;
    setLoadingContent(true);
    setSaveError(null);
    api
      .getDefinition(workspaceId, selected.file)
      .then((raw) => {
        if (cancelled) return;
        // Stored pretty-printed so the editor is readable; packages ship these minified.
        let pretty = raw;
        try {
          pretty = JSON.stringify(JSON.parse(raw), null, 2);
        } catch {
          /* leave as-is when it will not parse; the editor still shows it */
        }
        setContent(pretty);
        setDraft(pretty);
      })
      .catch((e: Error) => {
        if (!cancelled) setSaveError(e.message);
      })
      .finally(() => {
        if (!cancelled) setLoadingContent(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selected, workspaceId]);

  // The element tables come from HL7's own renderer on the service, so they match published IG sites exactly.
  // They are fetched per view and cached per definition; a package on a FHIR version the renderer does not
  // cover answers "unsupported" and the built-in table below is used instead.
  useEffect(() => {
    if (!selected || tab > 1) return;
    const view = tab === 0 ? 'differential' : 'snapshot';
    const key = `${selected.file}|${view}|${editVersion}`;
    if (key in tableHtml) return;
    let cancelled = false;
    setTableLoading(true);
    api
      .getDefinitionTable(workspaceId, selected.file, view)
      .then((result) => {
        if (!cancelled) setTableHtml((current) => ({ ...current, [key]: result.html ?? null }));
      })
      .catch(() => {
        if (!cancelled) setTableHtml((current) => ({ ...current, [key]: null }));
      })
      .finally(() => {
        if (!cancelled) setTableLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selected, tab, workspaceId, editVersion, tableHtml]);

  // The element model backs the inline editors in both tables, so it is loaded with the definition.
  const loadElements = useCallback(() => {
    if (!selected) {
      setProfileElements([]);
      return;
    }
    api
      .listElements(workspaceId, selected.file)
      .then((result) => {
        setProfileElements(result.elements);
        setBindingStrengths(result.bindingStrengths);
      })
      .catch(() => setProfileElements([]));
  }, [selected, workspaceId]);

  useEffect(() => {
    loadElements();
  }, [loadElements]);

  /** After an inline edit the definition has changed, so every cached view of it is stale. */
  const afterElementEdit = useCallback(() => {
    setTableHtml({});
    setEditVersion((version) => version + 1);
    loadElements();
    void refresh();
    if (selected) {
      api
        .getDefinition(workspaceId, selected.file)
        .then((raw) => {
          const parsed = JSON.parse(raw);
          const pretty = JSON.stringify(parsed, null, 2);
          setContent(pretty);
          setDraft(pretty);
          // The change is kept in this browser, as its differential. The snapshot is left behind deliberately:
          // the service regenerates it, and storing it would cost sixty times the space for nothing.
          if (workspace && parsed.differential) {
            try {
              projectStore.recordEdit(workspace, selected.file, parsed.differential);
              setStored(projectStore.get(workspaceId));
              setStoreError(null);
            } catch (e) {
              setStoreError(e instanceof Error ? e.message : String(e));
            }
          }
        })
        .catch(() => undefined);
    }
  }, [loadElements, refresh, selected, workspaceId, workspace]);

  // Stable identity: rebuilding this object each render would restart the table's effects every time.
  const tableEditing = useMemo(
    () =>
      selected && profileElements.length > 0
        ? {
            workspaceId,
            file: selected.file,
            elements: profileElements,
            strengths: bindingStrengths,
            extensions: definitions.filter(isExtension),
            onApplied: afterElementEdit,
            // Only the definition list needs refreshing; leaving the tables alone keeps the editor open.
            onExtensionCreated: (_created: CreatedExtension, request: NewExtension) => {
              void refresh();
              rememberExtension(request);
            },
            onAskAbout: tagElement,
          }
        : undefined,
    [selected, profileElements, bindingStrengths, definitions, workspaceId, afterElementEdit, refresh,
      tagElement],
  );

  const dirty = draft !== content;
  const views = useMemo(() => readViews(draft), [draft]);

  /**
   * What this profile constrains, which is what a reader came for. Both element views open out to these, so the
   * snapshot does not arrive as several hundred untouched base elements with the changes buried among them.
   */
  const constrainedKeys = useMemo(
    () =>
      views.differential
        .filter((element) => Boolean(element.path))
        .map((element) =>
          element.sliceName ? `${element.path}:${element.sliceName}` : (element.path as string),
        ),
    [views.differential],
  );

  const byText = definitions.filter((definition) => {
    const term = filter.trim().toLowerCase();
    if (!term) return true;
    return (
      (definition.name ?? '').toLowerCase().includes(term) ||
      (definition.resourceType ?? '').toLowerCase().includes(term) ||
      (definition.id ?? '').toLowerCase().includes(term)
    );
  });

  // Counted before the tags are applied, so a tag's number does not move when another tag is pressed.
  const counts = new Map<DefinitionKind, number>();
  byText.forEach((definition) => {
    const kind = kindOf(definition);
    counts.set(kind, (counts.get(kind) ?? 0) + 1);
  });

  // No tag pressed means no opinion, which shows everything rather than nothing.
  const filtered = kinds.size === 0 ? byText : byText.filter((d) => kinds.has(kindOf(d)));

  // Grouped under the same names as the tags, so pressing one is visibly the same cut as reading one heading.
  const groups: [string, DefinitionSummary[]][] = KINDS.map(([kind, label]) => [
    label,
    filtered.filter((d) => kindOf(d) === kind),
  ]);

  /**
   * Where a new extension might go, offered in the order it is likely to be wanted.
   *
   * <p>The open definition's own element paths come first: an extension is nearly always being added to the
   * element someone is looking at, not to the resource as a whole. Offering only resource types -- which is
   * what this did -- left "AllergyIntolerance" as the sole choice, so every extension ended up declared
   * against the root however deep the element it was really for.
   */
  const contextSuggestions = useMemo(() => {
    const paths = profileElements
      .map((element) => (element.sliceName ? `${element.path}:${element.sliceName}` : element.path))
      // The root element repeats the resource type, which is already offered below.
      .filter((path) => path.includes('.'));
    const resourceTypes = Array.from(
      new Set(definitions.filter((d) => d.kind === 'resource' && d.resourceType).map((d) => d.resourceType!)),
    ).sort();
    return Array.from(new Set([...paths, ...resourceTypes]));
  }, [profileElements, definitions]);

  const save = async () => {
    if (!selected) return;
    setSaving(true);
    setSaveError(null);
    setFindings([]);
    try {
      const result = await api.saveDefinition(workspaceId, selected.file, draft);
      setContent(draft);
      setFindings(result.findings ?? []);
      await refresh();
    } catch (e) {
      // A rejected save carries why: the profiling rules the edit broke, not just a status.
      const payload = e instanceof ApiError ? (e.payload as { findings?: RuleFinding[] } | null) : null;
      setFindings(payload?.findings ?? []);
      setSaveError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  };

  const resetAll = async () => {
    setResetting(true);
    try {
      await api.resetWorkspace(workspaceId);
      setSelected(null);
      setContent('');
      setDraft('');
      await refresh();
    } catch (e) {
      setLoadError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setResetting(false);
    }
  };

  /**
   * Names the work so it can be found again under My packages. The edits themselves are already on disk, so
   * this is not what keeps them safe -- it is what makes them findable.
   */
  /** Keeps an authored extension's request, which is what lets it be created again on a fresh service. */
  const rememberExtension = useCallback(
    (request: NewExtension) => {
      if (!workspace) {
        return;
      }
      try {
        projectStore.recordExtension(workspace, request);
        setStored(projectStore.get(workspaceId));
      } catch (e) {
        setStoreError(e instanceof Error ? e.message : String(e));
      }
    },
    [workspace, workspaceId],
  );

  /**
   * Puts this browser's changes back onto the service.
   *
   * <p>The service's copy of a package is scratch: it can be cleaned out, and another person's browser never
   * had these changes at all. So whatever is stored here and missing there is replayed -- extensions first,
   * since a differential may bind one as a slice.
   */
  const replayStoredEdits = useCallback(async () => {
    const project = projectStore.get(workspaceId);
    if (!project || definitions.length === 0) {
      return;
    }
    const present = new Set(definitions.map((definition) => definition.file));
    const alreadyEdited = new Set(definitions.filter((definition) => definition.edited).map((d) => d.file));
    const missingExtensions = project.extensions.filter(
      (extension) => ![...present].some((file) => file.includes(extension.id)),
    );
    const missingEdits = Object.keys(project.edits).filter(
      (file) => present.has(file) && !alreadyEdited.has(file),
    );
    if (missingExtensions.length === 0 && missingEdits.length === 0) {
      return;
    }
    setReplaying(true);
    try {
      for (const extension of missingExtensions) {
        await api.createExtension(workspaceId, extension).catch(() => undefined);
      }
      for (const file of missingEdits) {
        await api.applyDifferential(workspaceId, file, project.edits[file]).catch(() => undefined);
      }
      await refresh();
      setTableHtml({});
    } finally {
      setReplaying(false);
    }
  }, [workspaceId, definitions, refresh]);

  useEffect(() => {
    void replayStoredEdits();
    // Only when the definition list first arrives; replaying again on every change would fight the edits.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [workspaceId, definitions.length]);

  const saveProject = () => {
    if (!workspace) {
      return;
    }
    setSavingProject(true);
    setStoreError(null);
    try {
      setStored(projectStore.save(workspace, projectName.trim() || workspace.defaultProjectName));
    } catch (e) {
      setStoreError(e instanceof Error ? e.message : String(e));
    } finally {
      setSavingProject(false);
    }
  };

  const generate = async () => {
    setGenerating(true);
    setGenerateError(null);
    try {
      const job = await api.generateFacade(workspaceId, projectName.trim());
      navigate(`/jobs/${job.id}`);
    } catch (e) {
      setGenerateError(e instanceof ApiError ? e.message : String(e));
      setGenerating(false);
    }
  };

  if (loadError) {
    return (
      <PageContent>
        <Alert severity="error" sx={{ mb: 2 }}>
          {loadError}
        </Alert>
        <Button startIcon={<ArrowLeft size={16} />} onClick={() => navigate('/')}>
          Back to packages
        </Button>
      </PageContent>
    );
  }

  return (
    <PageContent>
      <PageTitle>
        <PageTitle.Header>{workspace?.title ?? workspace?.packageName ?? workspaceId}</PageTitle.Header>
        <PageTitle.SubHeader>
          {workspace
            ? `${workspace.packageName}${workspace.version ? `@${workspace.version}` : ''} · ${
                workspace.definitionCount
              } structure definitions${
                workspace.fhirVersion ? ` · FHIR ${workspace.fhirVersion.toUpperCase()}` : ''
              }${workspace.publisher ? ` · ${workspace.publisher}` : ''}`
            : 'Loading…'}
        </PageTitle.SubHeader>
      </PageTitle>

      {workspace?.description && (
        <Box sx={{ mb: 2.5 }}>
          <Typography
            variant="body2"
            color="text.secondary"
            sx={
              descriptionOpen
                ? undefined
                : {
                    // Published descriptions run to several paragraphs, so show a few lines by default.
                    display: '-webkit-box',
                    WebkitLineClamp: 3,
                    WebkitBoxOrient: 'vertical',
                    overflow: 'hidden',
                  }
            }
          >
            {workspace.description}
          </Typography>
          {workspace.description.length > 260 && (
            <Button size="small" sx={{ mt: 0.5, px: 0 }} onClick={() => setDescriptionOpen((open) => !open)}>
              {descriptionOpen ? 'Show less' : 'Show more'}
            </Button>
          )}
          {workspace.canonical && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
              {workspace.canonical}
            </Typography>
          )}
        </Box>
      )}

      {/* Generating is the point of the tool, so it stays visible rather than sitting at the end of a wizard. */}
      <Paper variant="outlined" sx={{ p: 2.5, mb: 3 }}>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-start', flexWrap: 'wrap' }}>
          <Button variant="outlined" startIcon={<ArrowLeft size={16} />} onClick={() => navigate('/')}>
            Packages
          </Button>
          <Box sx={{ flex: 1, minWidth: 220 }}>
            <TextField
              fullWidth
              size="small"
              label="Project name"
              value={projectName}
              onChange={(event) => setProjectName(event.target.value)}
              helperText={
                stored?.projectName
                  ? `Saved in this browser · ${projectStore.usage().count} of ${MAX_PACKAGES} packages`
                  : `Defaults to ${workspace?.defaultProjectName ?? '…'} for this package.`
              }
            />
          </Box>
          {(workspace?.editedCount ?? 0) > 0 && workspace?.resettable !== false && (
            <Button
              variant="outlined"
              color="warning"
              startIcon={resetting ? <CircularProgress size={14} color="inherit" /> : <RotateCcw size={16} />}
              disabled={resetting}
              onClick={() => void resetAll()}
              sx={{ mt: 0.5 }}
            >
              Discard {workspace?.editedCount} edit{workspace?.editedCount === 1 ? '' : 's'}
            </Button>
          )}
          <Tooltip title={guideOpen ? 'Hide the guide' : 'How to use this page'}>
            <IconButton
              color={guideOpen ? 'primary' : 'default'}
              onClick={() => {
                rememberWorkflowGuide(guideOpen);
                setGuideOpen(!guideOpen);
              }}
              sx={{ mt: 0.5, border: 1, borderColor: 'divider' }}
            >
              <CircleQuestionMark size={18} />
            </IconButton>
          </Tooltip>
          <Tooltip
            title={
              tagged.length > 0
                ? `Edit ${tagged.length} selected element${tagged.length === 1 ? '' : 's'} with the Dev Bot`
                : 'Ask the Dev Bot about this package, or have it draft changes'
            }
          >
            <IconButton
              color={chatOpen ? 'primary' : 'default'}
              onClick={() => setChatOpen((open) => !open)}
              sx={{ mt: 0.5, border: 1, borderColor: 'divider' }}
            >
              <Badge color="primary" badgeContent={tagged.length} invisible={tagged.length === 0}>
                <MessageSquare size={18} />
              </Badge>
            </IconButton>
          </Tooltip>
          <Button
            variant="outlined"
            startIcon={savingProject ? <CircularProgress size={14} color="inherit" /> : <Save size={16} />}
            disabled={savingProject || !workspace}
            onClick={saveProject}
            sx={{ mt: 0.5 }}
          >
            {stored?.projectName ? 'Saved' : 'Save as project'}
          </Button>
          <Button
            variant="contained"
            startIcon={generating ? <CircularProgress size={14} color="inherit" /> : <Play size={16} />}
            disabled={generating || !workspace}
            onClick={() => void generate()}
            sx={{ mt: 0.5 }}
          >
            {generating ? 'Generating…' : 'Generate FHIR facade'}
          </Button>
        </Box>
        {storeError && (
          <Alert severity="warning" sx={{ mt: 2 }} onClose={() => setStoreError(null)}>
            {storeError}
          </Alert>
        )}
        {replaying && (
          <Alert severity="info" sx={{ mt: 2 }}>
            Putting your stored changes back onto this package…
          </Alert>
        )}
        {generateError && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {generateError}
          </Alert>
        )}
      </Paper>

      {guideOpen && (
        <WorkflowGuide
          onShowWalkthrough={() => setWalkthrough(true)}
          onDismiss={() => {
            rememberWorkflowGuide(true);
            setGuideOpen(false);
          }}
        />
      )}
      <ProfileWalkthrough open={walkthrough} onClose={() => setWalkthrough(false)} />

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          // Room for the panel only where there is room to spare. Reserving it from lg (1200px) left the
          // element table with a couple of hundred pixels and its text wrapping one word per line; below xl
          // the panel simply overlays, which is what a drawer is for and is easily dismissed.
          pr: { xs: 0, xl: chatOpen ? '432px' : 0 },
          transition: 'padding-right .2s, grid-template-columns .2s',
          gridTemplateColumns: {
            xs: '1fr',
            md: listCollapsed ? '52px 1fr' : '340px 1fr',
          },
          // Without this a grid item refuses to shrink below its content, and the element table is wider than
          // any screen -- so the whole page grew a horizontal scrollbar instead of the table keeping its own.
          '& > *': { minWidth: 0 },
        }}
      >
        <Paper variant="outlined" sx={{ p: listCollapsed ? 0.5 : 2, maxHeight: '70vh', overflow: 'auto' }}>
          {listCollapsed ? (
            <Tooltip title={`Show the definition list (${definitions.length})`} placement="right">
              <IconButton size="small" onClick={() => setListCollapsed(false)}>
                <PanelLeftOpen size={18} />
              </IconButton>
            </Tooltip>
          ) : (
            <>
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 0.5 }}>
            <Tooltip title="Collapse the list and give the table the room" placement="left">
              <IconButton size="small" onClick={() => setListCollapsed(true)}>
                <PanelLeftClose size={18} />
              </IconButton>
            </Tooltip>
          </Box>
          <TextField
            fullWidth
            size="small"
            label="Filter definitions"
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
            sx={{ mb: 1 }}
          />

          <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mb: 1.5 }}>
            {KINDS.filter(([kind]) => (counts.get(kind) ?? 0) > 0 || kinds.has(kind)).map(([kind, label]) => {
              const on = kinds.has(kind);
              return (
                <Chip
                  key={kind}
                  size="small"
                  label={`${label} ${counts.get(kind) ?? 0}`}
                  color={on ? 'primary' : 'default'}
                  variant={on ? 'filled' : 'outlined'}
                  onClick={() =>
                    setKinds((current) => {
                      const next = new Set(current);
                      if (next.has(kind)) {
                        next.delete(kind);
                      } else {
                        next.add(kind);
                      }
                      return next;
                    })
                  }
                />
              );
            })}
            {kinds.size > 0 && (
              <Chip size="small" variant="outlined" label="All" onClick={() => setKinds(new Set())} />
            )}
          </Box>

          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <Button
              fullWidth
              size="small"
              variant="outlined"
              startIcon={<Plus size={16} />}
              onClick={() => setNewProfileOpen(true)}
            >
              New profile
            </Button>
            <Button
              fullWidth
              size="small"
              variant="outlined"
              startIcon={<Plus size={16} />}
              onClick={() => setNewExtensionOpen(true)}
            >
              New extension
            </Button>
          </Box>
          {groups.map(([groupName, members]) =>
            members.length === 0 ? null : (
              <Box key={groupName} sx={{ mb: 1.5 }}>
                <Typography
                  variant="overline"
                  color="text.secondary"
                  sx={{ display: 'block', px: 1, lineHeight: 2 }}
                >
                  {groupName} ({members.length})
                </Typography>
                {members.map((definition) => (
                  <Box
                    key={definition.file}
                    onClick={() => setSelected(definition)}
                    sx={{
                      p: 1,
                      mb: 0.5,
                      borderRadius: 1,
                      cursor: 'pointer',
                      bgcolor: selected?.file === definition.file ? 'action.selected' : 'transparent',
                      '&:hover': { bgcolor: 'action.hover' },
                    }}
                  >
                    <Box sx={{ display: 'flex', gap: 0.75, alignItems: 'center' }}>
                      <Typography variant="body2" fontWeight={600} noWrap>
                        {definition.name ?? definition.id}
                      </Typography>
                      {definition.edited && <Chip size="small" color="warning" label="edited" />}
                    </Box>
                    <Typography variant="caption" color="text.secondary">
                      {/* For an extension, where it may be used says far more than its type ("Extension"). */}
                      {isExtension(definition)
                        ? definition.contexts.length > 0
                          ? `on ${definition.contexts.join(', ')}`
                          : 'extension'
                        : definition.resourceType}
                    </Typography>
                  </Box>
                ))}
              </Box>
            ),
          )}
          {filtered.length === 0 && (
            <Typography variant="caption" color="text.secondary">
              No definitions match “{filter}”.
            </Typography>
          )}
            </>
          )}
        </Paper>

        <Paper variant="outlined" sx={{ p: 2.5, minHeight: 420 }}>
          {!selected ? (
            <Typography variant="body2" color="text.secondary">
              Select a structure definition to see what it constrains, and edit it if you want the generated
              facade to differ from the published guide.
            </Typography>
          ) : (
            <>
              <Typography variant="h6">{selected.title ?? selected.name}</Typography>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', my: 1 }}>
                {selected.resourceType && <Chip size="small" label={selected.resourceType} />}
                {selected.derivation && <Chip size="small" variant="outlined" label={selected.derivation} />}
                {selected.status && <Chip size="small" variant="outlined" label={selected.status} />}
              </Box>
              {selected.url && (
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                  {selected.url}
                </Typography>
              )}
              {selected.description && (
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  {selected.description.slice(0, 400)}
                  {selected.description.length > 400 ? '…' : ''}
                </Typography>
              )}

              <Divider sx={{ mb: 1 }} />
              <Tabs value={tab} onChange={(_, value) => setTab(value as number)} sx={{ mb: 2 }}>
                <Tab label={`Differential (${views.differential.length})`} />
                <Tab label={`Snapshot (${views.snapshot.length})`} />
                <Tab label="Constraints" />
                <Tab label={dirty ? 'JSON •' : 'JSON'} />
              </Tabs>

              {loadingContent && <CircularProgress size={20} />}

              {!loadingContent && tab <= 1 && (() => {
                const view = tab === 0 ? 'differential' : 'snapshot';
                const key = `${selected.file}|${view}|${editVersion}`;
                const html = tableHtml[key];
                if (tableLoading && !(key in tableHtml)) {
                  return <CircularProgress size={20} />;
                }
                if (html) {
                  return (
                    <IgTable
                      html={html}
                      constrained={constrainedKeys}
                      editing={tableEditing}
                    />
                  );
                }
                // The service could not render this one (unsupported FHIR version, or the definitions are
                // mid-edit); the built-in table still describes it.
                return (
                  <ElementTable
                    elements={tab === 0 ? views.differential : views.snapshot}
                    emptyMessage={
                      tab === 0
                        ? 'This definition has no differential: it does not constrain the base resource.'
                        : 'This definition ships no snapshot. Published guides usually include one; the differential view shows what it constrains.'
                    }
                  />
                );
              })()}

              {!loadingContent && tab === 2 && (
                <ConstraintEditor
                  workspaceId={workspaceId}
                  file={selected.file}
                  extensions={definitions.filter(isExtension)}
                  onSaved={afterElementEdit}
                  onExtensionCreated={(_created: CreatedExtension, request: NewExtension) => {
                    void refresh();
                    rememberExtension(request);
                  }}
                />
              )}

              {!loadingContent && tab === 3 && (

                <>
                  <TextField
                    fullWidth
                    multiline
                    minRows={18}
                    maxRows={30}
                    value={draft}
                    onChange={(event) => setDraft(event.target.value)}
                    slotProps={{ input: { sx: { fontFamily: 'monospace', fontSize: 12 } } }}
                  />
                  {saveError && (
                    <Alert severity="error" sx={{ mt: 2 }}>
                      {saveError}
                    </Alert>
                  )}
                  {findings.length > 0 && (
                    <Alert
                      severity={findings.some((f) => f.severity === 'error') ? 'error' : 'warning'}
                      sx={{ mt: 2 }}
                    >
                      <Typography variant="body2" fontWeight={600} gutterBottom>
                        Checked against {selected.baseDefinition?.split('/').pop() ?? 'the base definition'}
                      </Typography>
                      <Box component="ul" sx={{ m: 0, pl: 2.5 }}>
                        {findings.map((finding, index) => (
                          <Box component="li" key={`${finding.path}-${index}`} sx={{ mb: 0.5 }}>
                            <Typography variant="caption">
                              <strong>{finding.severity === 'error' ? 'Error' : 'Warning'}</strong>
                              {finding.path ? ` · ${finding.path}` : ''} — {finding.message}
                            </Typography>
                          </Box>
                        ))}
                      </Box>
                    </Alert>
                  )}
                  <Box sx={{ display: 'flex', gap: 1.5, mt: 2, alignItems: 'center' }}>
                    <Button
                      variant="contained"
                      startIcon={saving ? <CircularProgress size={14} color="inherit" /> : <Save size={16} />}
                      disabled={!dirty || saving}
                      onClick={() => void save()}
                    >
                      {saving ? 'Saving…' : 'Save'}
                    </Button>
                    <Button variant="outlined" disabled={!dirty || saving} onClick={() => setDraft(content)}>
                      Revert
                    </Button>
                    {dirty && (
                      <Typography variant="caption" color="text.secondary">
                        Unsaved changes — save before generating to include them.
                      </Typography>
                    )}
                  </Box>
                </>
              )}
            </>
          )}
        </Paper>

      </Box>

      {/*
        An overlay rather than a column: as a third column it had to take width from somewhere, and the element
        table has none to give. Deliberately not modal -- no backdrop, no focus trap, and pointer events only on
        the panel itself -- because the whole point is to pick an element in the table while talking about it,
        and a modal drawer makes the table unclickable the moment the conversation opens.
      */}
      <Drawer
        anchor="right"
        open={chatOpen}
        onClose={() => setChatOpen(false)}
        variant="temporary"
        hideBackdrop
        disableEnforceFocus
        disableScrollLock
        ModalProps={{ keepMounted: true }}
        sx={{ pointerEvents: 'none' }}
        PaperProps={{ sx: { width: 420, maxWidth: '92vw', pointerEvents: 'auto', boxShadow: 6 } }}
      >
        <ChatPanel
          workspaceId={workspaceId}
          file={selected?.file ?? null}
          elements={profileElements}
          tagged={tagged}
          onTag={tagElement}
          onUntag={(index) => setTagged((current) => current.filter((_, i) => i !== index))}
          onApplied={(affected) => {
            afterElementEdit();
            // The conversation has moved on to whatever was just created.
            setTagged([affected]);
          }}
          onClose={() => setChatOpen(false)}
        />
      </Drawer>
      <NewProfileDialog
        open={newProfileOpen}
        workspaceId={workspaceId}
        onClose={() => setNewProfileOpen(false)}
        onCreated={(created) => {
          setNewProfileOpen(false);
          void refresh().then(() => {
            // Open what was just made: that is where the constraining happens.
            setSelected((current) => current ?? null);
            api
              .listDefinitions(workspaceId)
              .then((all) => {
                const made = all.find((d) => d.file === created.file);
                if (made) {
                  setSelected(made);
                }
              })
              .catch(() => undefined);
          });
        }}
      />
      <NewExtensionDialog
        open={newExtensionOpen}
        workspaceId={workspaceId}
        suggestedContexts={contextSuggestions}
        defaultContexts={selected?.resourceType ? [selected.resourceType] : undefined}
        onClose={() => setNewExtensionOpen(false)}
        onCreated={(created, request) => {
          rememberExtension(request);
          setNewExtensionOpen(false);
          // Show the new extension straight away, and refresh the list it now belongs to.
          void refresh().then(() => {
            api
              .listDefinitions(workspaceId)
              .then((all) => {
                const match = all.find((definition) => definition.file === created.file);
                if (match) {
                  setSelected(match);
                }
              })
              .catch(() => undefined);
          });
        }}
      />
    </PageContent>
  );
}
