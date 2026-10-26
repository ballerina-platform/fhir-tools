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
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography,
} from '@wso2/oxygen-ui';
import { useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import type { CreatedProfile, ProfileBase, RuleFinding } from '../api/types';

/**
 * Authors a new profile.
 *
 * <p>A profile is a constraint on something that already exists, so the one choice that matters is what it is
 * based on: a resource this guide already profiles, or a plain FHIR resource. Everything else about the new
 * definition -- its kind, its type, its derivation, its root element -- follows from that, so the service
 * fills it in rather than asking.
 *
 * <p>It is created constraining nothing. The element table is where constraints are made, and a profile that
 * narrowed something the moment it was created would be asserting a rule nobody asked for.
 */
export default function NewProfileDialog({
  open,
  workspaceId,
  onClose,
  onCreated,
}: {
  open: boolean;
  workspaceId: string;
  onClose: () => void;
  onCreated: (created: CreatedProfile) => void;
}) {
  const [bases, setBases] = useState<ProfileBase[]>([]);
  const [base, setBase] = useState<ProfileBase | null>(null);
  const [id, setId] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
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
    setBase(null);
    setError(null);
    setFindings([]);
    setLoading(true);
    api
      .listProfileBases(workspaceId)
      .then(setBases)
      .catch(() => setBases([]))
      .finally(() => setLoading(false));
  }, [open, workspaceId]);

  const create = async () => {
    if (!base) {
      return;
    }
    setSaving(true);
    setError(null);
    setFindings([]);
    try {
      onCreated(
        await api.createProfile(workspaceId, {
          id: id.trim(),
          title: title.trim() || undefined,
          description: description.trim() || undefined,
          baseUrl: base.url,
        }),
      );
    } catch (e) {
      const payload = e instanceof ApiError ? (e.payload as { findings?: RuleFinding[] } | null) : null;
      setFindings(payload?.findings ?? []);
      setError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>New profile</DialogTitle>
      <DialogContent>
        <Autocomplete
          options={bases}
          loading={loading}
          value={base}
          onChange={(_, value) => setBase(value)}
          getOptionLabel={(option) => `${option.label} (${option.resourceType})`}
          // This package's own resources first: profiling one of those is the common case, and scrolling
          // past a hundred core resources to find it would be the wrong way round.
          groupBy={(option) => (option.source === 'package' ? 'In this package' : `FHIR ${option.source}`)}
          isOptionEqualToValue={(a, b) => a.url === b.url}
          renderInput={(params) => (
            <TextField
              {...params}
              required
              label="Based on"
              helperText="The resource or profile this one constrains. Everything else follows from it."
              InputProps={{
                ...params.InputProps,
                endAdornment: (
                  <>
                    {loading ? <CircularProgress size={14} /> : null}
                    {params.InputProps.endAdornment}
                  </>
                ),
              }}
            />
          )}
          sx={{ mt: 1, mb: 2 }}
        />

        <TextField
          fullWidth
          required
          size="small"
          label="Id"
          placeholder="au-core-patient"
          value={id}
          onChange={(event) => setId(event.target.value)}
          helperText="Names the file and completes the canonical URL, which follows this package's own."
          sx={{ mb: 2 }}
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
        />

        {base && (
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2 }}>
            Creates a <strong>{base.resourceType}</strong> profile deriving from {base.url}. It starts
            constraining nothing — narrow it from the element table.
          </Typography>
        )}

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
          disabled={!base || id.trim().length === 0 || saving}
          startIcon={saving ? <CircularProgress size={14} color="inherit" /> : undefined}
          onClick={() => void create()}
        >
          {saving ? 'Creating…' : 'Create profile'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
