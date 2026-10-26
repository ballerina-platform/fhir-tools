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
  Autocomplete,
  Box,
  Button,
  Chip,
  CircularProgress,
  FormControlLabel,
  MenuItem,
  Switch,
  TextField,
  Typography,
} from '@wso2/oxygen-ui';
import { MessageSquare, Plus, Save } from '@wso2/oxygen-ui-icons-react';
import NewExtensionDialog from './NewExtensionDialog';
import { useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import type {
  CreatedExtension,
  DefinitionSummary,
  ElementChanges,
  NewExtension,
  ProfileElement,
  RuleFinding,
} from '../api/types';

/**
 * The constraint controls for one element, bounded by what its base definition allows.
 *
 * Shared by the Constraints tab and the inline editor in the element tables, so both offer exactly the same
 * choices. The bounds shape the controls; the service re-checks the result, so this is a guide rather than the
 * only safeguard.
 */

/** Upper bounds a profile may choose. "*" only stays on offer when the base itself is unbounded. */
function maxOptions(baseMax: string | null): string[] {
  if (!baseMax) {
    return ['0', '1', '*'];
  }
  if (baseMax === '*') {
    return ['0', '1', '2', '3', '5', '10', '*'];
  }
  const numeric = Number(baseMax);
  if (!Number.isFinite(numeric)) {
    return [baseMax];
  }
  return Array.from({ length: numeric + 1 }, (_, index) => String(index));
}

export default function ElementConstraintForm({
  workspaceId,
  file,
  element,
  strengths,
  extensions,
  onApplied,
  onExtensionCreated,
  onAskAbout,
  dense,
}: {
  workspaceId: string;
  file: string;
  element: ProfileElement;
  strengths: string[];
  /** Extensions in this package, offered when adding one to an element. */
  extensions?: DefinitionSummary[];
  onApplied: () => void;
  /** Called after an extension is authored here, so the package's definition list can catch up. */
  /** Also carries the request, so an authored extension can be recreated when the package is reopened. */
  onExtensionCreated?: (created: CreatedExtension, request: NewExtension) => void;
  /** Hands this element to the Dev Bot, so a change it drafts applies here and nowhere else. */
  onAskAbout?: (path: string, sliceName: string | null) => void;
  dense?: boolean;
}) {
  const [sliceMode, setSliceMode] = useState<'extension' | 'slice' | null>(null);
  const [sliceName, setSliceName] = useState('');
  const [sliceExtension, setSliceExtension] = useState('');
  const [sliceMax, setSliceMax] = useState('1');
  const [sliceRequired, setSliceRequired] = useState(false);
  const [sliceMustSupport, setSliceMustSupport] = useState(true);
  const [sliceDescription, setSliceDescription] = useState('');
  const [creatingExtension, setCreatingExtension] = useState(false);
  const [justCreated, setJustCreated] = useState<DefinitionSummary | null>(null);
  const [slicingError, setSlicingError] = useState<string | null>(null);
  const [addingSlice, setAddingSlice] = useState(false);
  const [draft, setDraft] = useState<ElementChanges>({});
  const [saving, setSaving] = useState(false);
  const [findings, setFindings] = useState<RuleFinding[]>([]);
  const [saveError, setSaveError] = useState<string | null>(null);

  // Start from what the element says today, so applying without touching anything changes nothing.
  useEffect(() => {
    setDraft({
      min: element.current.min,
      max: element.current.max,
      mustSupport: element.current.mustSupport ?? false,
      short: element.current.short ?? '',
      types: element.current.types,
      bindingStrength: element.current.bindingStrength,
      bindingValueSet: element.current.bindingValueSet ?? '',
    });
    setFindings([]);
    setSaveError(null);
    setSliceMode(null);
    setSliceName('');
    setSliceExtension('');
    setSliceMax('1');
    setSliceRequired(false);
    setSliceMustSupport(true);
    setSliceDescription('');
    setSlicingError(null);
  }, [element]);

  const allowedStrengths = element.limits.minBindingStrength
    ? strengths.slice(strengths.indexOf(element.limits.minBindingStrength))
    : strengths;

  const apply = async () => {
    setSaving(true);
    setSaveError(null);
    setFindings([]);
    try {
      const result = await api.applyElementChanges(
        workspaceId,
        file,
        element.path,
        element.sliceName,
        draft,
      );
      setFindings(result.findings ?? []);
      onApplied();
    } catch (e) {
      const payload = e instanceof ApiError ? (e.payload as { findings?: RuleFinding[] } | null) : null;
      setFindings(payload?.findings ?? []);
      setSaveError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  };

  /**
   * Every complex element carries an extension child, so an extension can be added at any level: on the
   * resource through Patient.extension, but equally on Patient.name through Patient.name.extension. The
   * element the extension would hang off is this one unless this one is already the extension child.
   */
  const isExtensionElement =
    element.path.endsWith('.extension') || element.path.endsWith('.modifierExtension');
  // Every element in FHIR carries Element.extension, primitives included -- Patient.gender.extension is as
  // legal as Patient.name.extension, it is just never spelled out in a resource snapshot, which only expands
  // complex elements. So the offer follows the spec rather than what the snapshot happens to list.
  const extensionPath = isExtensionElement ? element.path : `${element.path}.extension`;
  /** The element an extension would extend, which is what its context has to name. */
  const extendedPath = isExtensionElement
    ? element.path.slice(0, element.path.lastIndexOf('.'))
    : element.path;

  // Slicing divides up repeats, so it only means anything where the element can occur more than once.
  const isRoot = !element.path.includes('.');
  const repeats =
    !isRoot && (element.current.max === '*' || Number(element.current.max) > 1);
  const resourceType = element.path.split('.')[0];

  /**
   * An extension declares the contexts it may be used on, so prefer the ones naming this element, its
   * datatype, or the resource. If none do, the rest are still offered rather than leaving no way forward:
   * context is advice about intent, and the profiling rules are enforced on save either way.
   */
  const applies = (candidate: DefinitionSummary) =>
    candidate.contexts.some(
      (context) =>
        context === extendedPath ||
        context === resourceType ||
        element.current.types.includes(context),
    );
  // Everything in the package is offered, with the ones declared for this element grouped first; plus any
  // just created here, which the definition list has not caught up with yet.
  const known = [...(extensions ?? [])];
  if (justCreated && !known.some((candidate) => candidate.url === justCreated.url)) {
    known.unshift(justCreated);
  }
  const pickable = [...known].sort(
    (a, b) => Number(applies(b)) - Number(applies(a)),
  );

  const addSlice = async () => {
    setAddingSlice(true);
    setSlicingError(null);
    try {
      await api.addSlice(workspaceId, file, {
        path: sliceMode === 'extension' ? (extensionPath ?? element.path) : element.path,
        sliceName: sliceName.trim(),
        min: sliceRequired ? 1 : 0,
        max: sliceMax,
        mustSupport: sliceMustSupport,
        extensionUrl: sliceMode === 'extension' ? sliceExtension : undefined,
        description: sliceDescription.trim() || undefined,
      });
      setSliceMode(null);
      onApplied();
    } catch (e) {
      const payload = e instanceof ApiError ? (e.payload as { findings?: RuleFinding[] } | null) : null;
      setSlicingError(
        payload?.findings?.find((f) => f.severity === 'error')?.message ??
          (e instanceof ApiError ? e.message : String(e)),
      );
    } finally {
      setAddingSlice(false);
    }
  };

  return (
    <Box>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
        {element.limits.knownToBase
          ? `Base allows ${element.limits.minMin ?? 0}..${element.limits.maxMax ?? '*'}`
          : 'The base does not describe this element directly, so it is unconstrained here.'}
      </Typography>

      <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'flex-start' }}>
        <TextField
          select
          size="small"
          label="Min"
          sx={{ minWidth: 100 }}
          value={draft.min ?? ''}
          onChange={(event) =>
            setDraft({ ...draft, min: event.target.value === '' ? null : Number(event.target.value) })
          }
          helperText={`≥ ${element.limits.minMin ?? 0}`}
        >
          {Array.from({ length: 11 }, (_, index) => index)
            .filter((value) => value >= (element.limits.minMin ?? 0))
            .map((value) => (
              <MenuItem key={value} value={value}>
                {value}
              </MenuItem>
            ))}
        </TextField>

        <TextField
          select
          size="small"
          label="Max"
          sx={{ minWidth: 100 }}
          value={draft.max ?? ''}
          onChange={(event) => setDraft({ ...draft, max: event.target.value })}
          helperText={`≤ ${element.limits.maxMax ?? '*'}`}
        >
          {maxOptions(element.limits.maxMax).map((value) => (
            <MenuItem key={value} value={value}>
              {value}
            </MenuItem>
          ))}
        </TextField>

        <FormControlLabel
          sx={{ mt: 0.5 }}
          control={
            <Switch
              size="small"
              checked={Boolean(draft.mustSupport)}
              onChange={(event) => setDraft({ ...draft, mustSupport: event.target.checked })}
            />
          }
          label={<Typography variant="body2">Must support</Typography>}
        />

        {allowedStrengths.length > 0 && (
          <TextField
            select
            size="small"
            label="Binding"
            sx={{ minWidth: 150 }}
            value={draft.bindingStrength ?? ''}
            onChange={(event) => setDraft({ ...draft, bindingStrength: event.target.value || null })}
            helperText={
              element.limits.minBindingStrength
                ? `≥ ${element.limits.minBindingStrength}`
                : 'none on the base'
            }
          >
            <MenuItem value="">(none)</MenuItem>
            {allowedStrengths.map((strength) => (
              <MenuItem key={strength} value={strength}>
                {strength}
              </MenuItem>
            ))}
          </TextField>
        )}
      </Box>

      {(element.limits.allowedTypes?.length ?? 0) > 1 && (
        <Box sx={{ mt: 1.5 }}>
          <Typography variant="caption" color="text.secondary">
            Types — narrow to a subset of what the base allows
          </Typography>
          <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap', mt: 0.5 }}>
            {(element.limits.allowedTypes ?? []).map((type) => {
              const chosen = (draft.types ?? []).includes(type);
              return (
                <Chip
                  key={type}
                  label={type}
                  size="small"
                  color={chosen ? 'primary' : 'default'}
                  variant={chosen ? 'filled' : 'outlined'}
                  onClick={() =>
                    setDraft({
                      ...draft,
                      types: chosen
                        ? (draft.types ?? []).filter((t) => t !== type)
                        : [...(draft.types ?? []), type],
                    })
                  }
                />
              );
            })}
          </Box>
        </Box>
      )}

      {!dense && (
        <TextField
          fullWidth
          size="small"
          label="Short description"
          sx={{ mt: 2 }}
          value={draft.short ?? ''}
          onChange={(event) => setDraft({ ...draft, short: event.target.value })}
        />
      )}

      {saveError && (
        <Alert severity="error" sx={{ mt: 1.5 }}>
          {saveError}
        </Alert>
      )}
      {findings.length > 0 && (
        <Alert severity={findings.some((f) => f.severity === 'error') ? 'error' : 'warning'} sx={{ mt: 1.5 }}>
          <Box component="ul" sx={{ m: 0, pl: 2.5 }}>
            {findings.map((finding, index) => (
              <Box component="li" key={`${finding.path}-${index}`}>
                <Typography variant="caption">
                  {finding.path ? `${finding.path} — ` : ''}
                  {finding.message}
                </Typography>
              </Box>
            ))}
          </Box>
        </Alert>
      )}

      {(extensionPath || repeats) && (
        <Box sx={{ mt: 2, pt: 1.5, borderTop: 1, borderColor: 'divider' }}>
          {sliceMode === null ? (
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              {extensionPath && (
                <Button size="small" startIcon={<Plus size={14} />} onClick={() => setSliceMode('extension')}>
                  Add extension
                </Button>
              )}
              {/* Slicing an extension element by hand is what "Add extension" already does properly. */}
              {repeats && !isExtensionElement && (
                <Button size="small" startIcon={<Plus size={14} />} onClick={() => setSliceMode('slice')}>
                  Add slice
                </Button>
              )}
            </Box>
          ) : (
            <>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                {sliceMode === 'extension'
                  ? `Adds an extension to ${extendedPath}, as a slice on ${extensionPath}.`
                  : `Names a subset of the repeats of ${element.path}.`}
              </Typography>
              <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'flex-start' }}>
                {sliceMode === 'extension' && (
                  <Autocomplete
                    size="small"
                    sx={{ minWidth: 320 }}
                    options={pickable}
                    // Applicable first, but everything stays reachable: an extension without a matching
                    // context may still be the one you meant.
                    groupBy={(option) => (applies(option) ? 'Declared for this element' : 'Other extensions')}
                    value={pickable.find((candidate) => candidate.url === sliceExtension) ?? null}
                    onChange={(_, value) => {
                      setSliceExtension(value?.url ?? '');
                      if (value && !sliceName) {
                        // Slices are normally named after the extension they bind.
                        setSliceName((value.id ?? '').replace(/[^A-Za-z0-9-]/g, '-'));
                      }
                    }}
                    getOptionLabel={(option) => option.title ?? option.name ?? option.id ?? ''}
                    isOptionEqualToValue={(option, value) => option.url === value.url}
                    // Searching the id and the contexts matters as much as the title: the id is what the
                    // slice gets named after, and the context is why an extension is the right one.
                    filterOptions={(options, state) => {
                      const term = state.inputValue.trim().toLowerCase();
                      if (!term) {
                        return options;
                      }
                      return options.filter((option) =>
                        [option.title, option.name, option.id, option.url, ...option.contexts]
                          .filter(Boolean)
                          .some((field) => String(field).toLowerCase().includes(term)),
                      );
                    }}
                    renderOption={(props, option) => (
                      <li {...props} key={option.file}>
                        <Box sx={{ width: '100%', py: 0.25 }}>
                          <Typography variant="body2" fontWeight={600}>
                            {option.title ?? option.name ?? option.id}
                          </Typography>
                          <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                            {option.id}
                            {option.contexts.length > 0 ? ` · on ${option.contexts.join(', ')}` : ''}
                          </Typography>
                        </Box>
                      </li>
                    )}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        required
                        label="Extension"
                        helperText={
                          pickable.length === 0
                            ? 'No extensions in this package yet.'
                            : `Search ${pickable.length} by name, id or context`
                        }
                      />
                    )}
                  />
                )}
                {sliceMode === 'extension' && (
                  <Button
                    size="small"
                    startIcon={<Plus size={14} />}
                    sx={{ mt: 0.5 }}
                    onClick={() => setCreatingExtension(true)}
                  >
                    New
                  </Button>
                )}
                <TextField
                  required
                  size="small"
                  label="Slice name"
                  sx={{ minWidth: 180 }}
                  value={sliceName}
                  onChange={(event) => setSliceName(event.target.value)}
                />
                <TextField
                  select
                  size="small"
                  label="Max"
                  sx={{ minWidth: 90 }}
                  value={sliceMax}
                  onChange={(event) => setSliceMax(event.target.value)}
                >
                  {['1', '2', '5', '*'].map((value) => (
                    <MenuItem key={value} value={value}>
                      {value}
                    </MenuItem>
                  ))}
                </TextField>
                <FormControlLabel
                  sx={{ mt: 0.5 }}
                  control={
                    <Switch
                      size="small"
                      checked={sliceRequired}
                      onChange={(event) => setSliceRequired(event.target.checked)}
                    />
                  }
                  label={<Typography variant="body2">Required</Typography>}
                />
                <FormControlLabel
                  sx={{ mt: 0.5 }}
                  control={
                    <Switch
                      size="small"
                      checked={sliceMustSupport}
                      onChange={(event) => setSliceMustSupport(event.target.checked)}
                    />
                  }
                  label={<Typography variant="body2">Must support</Typography>}
                />
              </Box>
              <TextField
                fullWidth
                size="small"
                label="Description"
                sx={{ mt: 1.5 }}
                value={sliceDescription}
                onChange={(event) => setSliceDescription(event.target.value)}
                helperText="What this slice is for. Shown as the element's short text."
              />
              {slicingError && (
                <Alert severity="error" sx={{ mt: 1.5 }}>
                  {slicingError}
                </Alert>
              )}
              <Box sx={{ display: 'flex', gap: 1, mt: 1.5 }}>
                <Button
                  size="small"
                  variant="contained"
                  disabled={
                    addingSlice || !sliceName.trim() || (sliceMode === 'extension' && !sliceExtension)
                  }
                  startIcon={addingSlice ? <CircularProgress size={14} color="inherit" /> : undefined}
                  onClick={() => void addSlice()}
                >
                  {addingSlice ? 'Adding…' : sliceMode === 'extension' ? 'Add extension' : 'Add slice'}
                </Button>
                <Button size="small" onClick={() => setSliceMode(null)}>
                  Cancel
                </Button>
              </Box>
            </>
          )}
        </Box>
      )}

      <NewExtensionDialog
        open={creatingExtension}
        workspaceId={workspaceId}
        suggestedContexts={[extendedPath, resourceType]}
        defaultContexts={[extendedPath]}
        onClose={() => setCreatingExtension(false)}
        onCreated={(created, request) => {
          setCreatingExtension(false);
          // Select it at once. The definition list is refreshed too, but this keeps the form usable without
          // waiting for that to come back.
          const summary: DefinitionSummary = {
            file: created.file,
            id: created.id,
            name: created.name,
            title: created.title,
            url: created.url,
            resourceType: 'Extension',
            kind: 'complex-type',
            derivation: 'constraint',
            status: 'draft',
            description: null,
            baseDefinition: 'http://hl7.org/fhir/StructureDefinition/Extension',
            contexts: created.contexts,
            edited: true,
          };
          setJustCreated(summary);
          setSliceExtension(created.url ?? '');
          if (!sliceName) {
            setSliceName((created.id ?? '').replace(/[^A-Za-z0-9-]/g, '-'));
          }
          onExtensionCreated?.(created, request);
        }}
      />

      <Box sx={{ display: 'flex', gap: 1, mt: 1.5, alignItems: 'center' }}>
        <Button
          size="small"
          variant="contained"
          startIcon={saving ? <CircularProgress size={14} color="inherit" /> : <Save size={14} />}
          disabled={saving}
          onClick={() => void apply()}
        >
          {saving ? 'Applying…' : 'Apply constraint'}
        </Button>
        {onAskAbout && (
          <Button
            size="small"
            startIcon={<MessageSquare size={14} />}
            onClick={() => onAskAbout(element.path, element.sliceName ?? null)}
          >
            Edit with Dev Bot
          </Button>
        )}
      </Box>
    </Box>
  );
}
