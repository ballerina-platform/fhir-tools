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
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Switch,
  TextField,
  Typography,
} from '@wso2/oxygen-ui';
import { useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import type { CreatedExtension, NewExtension, RuleFinding } from '../api/types';

/**
 * Authors a new extension.
 *
 * Almost all of an extension's definition is boilerplate that is identical every time, so this asks only for
 * what actually distinguishes one: what to call it, where it may be used, and what it carries. The service
 * fills in the rest and checks the result against the base Extension definition before writing it.
 */
export default function NewExtensionDialog({
  open,
  workspaceId,
  suggestedContexts,
  defaultContexts,
  onClose,
  onCreated,
}: {
  open: boolean;
  workspaceId: string;
  /** The resource types this package already profiles, which is where an extension usually goes. */
  suggestedContexts: string[];
  /** Pre-filled contexts, so creating from an element already knows where it goes. */
  defaultContexts?: string[];
  onClose: () => void;
  /** The request is handed back alongside the result, so the browser can author it again from scratch. */
  onCreated: (created: CreatedExtension, request: NewExtension) => void;
}) {
  const [valueTypes, setValueTypes] = useState<string[]>([]);
  const [id, setId] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [contexts, setContexts] = useState<string[]>([]);
  const [chosenTypes, setChosenTypes] = useState<string[]>(['string']);
  const [required, setRequired] = useState(false);
  const [repeats, setRepeats] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [findings, setFindings] = useState<RuleFinding[]>([]);

  useEffect(() => {
    if (!open) {
      return;
    }
    setId('');
    setTitle('');
    setDescription('');
    setContexts(defaultContexts ?? []);
    setChosenTypes(['string']);
    setRequired(false);
    setRepeats(false);
    setError(null);
    setFindings([]);
    api.getExtensionValueTypes(workspaceId).then(setValueTypes).catch(() => setValueTypes([]));
  }, [open, workspaceId, defaultContexts]);

  const create = async () => {
    setSaving(true);
    setError(null);
    setFindings([]);
    try {
      const request: NewExtension = {
        id: id.trim(),
        title: title.trim() || undefined,
        description: description.trim() || undefined,
        contexts,
        valueTypes: chosenTypes,
        required,
        repeats,
      };
      const result = await api.createExtension(workspaceId, request);
      onCreated(result, request);
    } catch (e) {
      const payload = e instanceof ApiError ? (e.payload as { findings?: RuleFinding[] } | null) : null;
      setFindings(payload?.findings ?? []);
      setError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  };

  const ready = id.trim().length > 0 && contexts.length > 0 && chosenTypes.length > 0;

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>New extension</DialogTitle>
      <DialogContent>
        <TextField
          fullWidth
          autoFocus
          required
          size="small"
          label="Id"
          placeholder="patient-preferred-pronoun"
          value={id}
          onChange={(event) => setId(event.target.value)}
          helperText="Names the file and completes the canonical URL, which follows this package's own."
          sx={{ mt: 1, mb: 2 }}
        />
        <TextField
          fullWidth
          size="small"
          label="Title"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          sx={{ mb: 2 }}
        />
        <TextField
          fullWidth
          multiline
          minRows={2}
          size="small"
          label="Description"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          sx={{ mb: 2 }}
        />

        <Autocomplete
          multiple
          freeSolo
          size="small"
          options={suggestedContexts}
          value={contexts}
          onChange={(_, value) => setContexts(value as string[])}
          renderInput={(params) => (
            <TextField
              {...params}
              required
              label="Used on"
              helperText="A resource type or element path, such as Patient or Patient.name. Press Enter to add."
            />
          )}
          sx={{ mb: 2 }}
        />

        <Typography variant="caption" color="text.secondary">
          Value type — more than one makes it a choice
        </Typography>
        <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap', mt: 0.75, mb: 2, maxHeight: 132, overflow: 'auto' }}>
          {valueTypes.map((type) => {
            const chosen = chosenTypes.includes(type);
            return (
              <Chip
                key={type}
                label={type}
                size="small"
                color={chosen ? 'primary' : 'default'}
                variant={chosen ? 'filled' : 'outlined'}
                onClick={() =>
                  setChosenTypes(
                    chosen ? chosenTypes.filter((t) => t !== type) : [...chosenTypes, type],
                  )
                }
              />
            );
          })}
        </Box>

        <Box sx={{ display: 'flex', gap: 2 }}>
          <FormControlLabel
            control={<Switch size="small" checked={required} onChange={(e) => setRequired(e.target.checked)} />}
            label={<Typography variant="body2">Required where used</Typography>}
          />
          <FormControlLabel
            control={<Switch size="small" checked={repeats} onChange={(e) => setRepeats(e.target.checked)} />}
            label={<Typography variant="body2">May repeat</Typography>}
          />
        </Box>

        {error && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {error}
          </Alert>
        )}
        {findings.length > 0 && (
          <Alert severity="error" sx={{ mt: 2 }}>
            <Box component="ul" sx={{ m: 0, pl: 2.5 }}>
              {findings.map((finding, index) => (
                <Box component="li" key={index}>
                  <Typography variant="caption">
                    {finding.path ? `${finding.path} — ` : ''}
                    {finding.message}
                  </Typography>
                </Box>
              ))}
            </Box>
          </Alert>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          variant="contained"
          disabled={!ready || saving}
          startIcon={saving ? <CircularProgress size={14} color="inherit" /> : undefined}
          onClick={() => void create()}
        >
          {saving ? 'Creating…' : 'Create extension'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
