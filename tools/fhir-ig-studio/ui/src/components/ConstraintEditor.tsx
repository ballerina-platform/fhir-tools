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

import { Alert, Box, Chip, CircularProgress, TextField, Typography } from '@wso2/oxygen-ui';
import { useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';
import type { CreatedExtension, DefinitionSummary, NewExtension, ProfileElement } from '../api/types';
import ElementConstraintForm from './ElementConstraintForm';

/**
 * Edits a profile one element at a time, with every control bounded by what the base definition allows.
 *
 * The intent is that an illegal profile cannot be expressed here: the cardinality options stop at the base's
 * bounds, the type picker only offers the base's types, and the binding strength list starts at the base's
 * strength. The service re-checks the result against the same rules, so this is a guide rather than the only
 * safeguard -- an element the base says nothing about is left editable and simply relies on that check.
 */

export default function ConstraintEditor({
  workspaceId,
  file,
  extensions,
  onSaved,
  onExtensionCreated,
}: {
  workspaceId: string;
  file: string;
  extensions: DefinitionSummary[];
  onSaved: () => void;
  onExtensionCreated: (created: CreatedExtension, request: NewExtension) => void;
}) {
  const [elements, setElements] = useState<ProfileElement[]>([]);
  const [strengths, setStrengths] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [filter, setFilter] = useState('');

  const [selectedPath, setSelectedPath] = useState<string | null>(null);

  const load = useMemo(
    () => () => {
      setLoading(true);
      setLoadError(null);
      api
        .listElements(workspaceId, file)
        .then((result) => {
          setElements(result.elements);
          setStrengths(result.bindingStrengths);
        })
        .catch((e: Error) => setLoadError(e.message))
        .finally(() => setLoading(false));
    },
    [workspaceId, file],
  );

  useEffect(() => {
    setSelectedPath(null);
    load();
  }, [load]);

  const selected = elements.find((element) => key(element) === selectedPath) ?? null;

  const filtered = elements.filter((element) =>
    filter.trim() ? element.path.toLowerCase().includes(filter.trim().toLowerCase()) : true,
  );

  if (loading && elements.length === 0) {
    return <CircularProgress size={20} />;
  }
  if (loadError) {
    return <Alert severity="error">{loadError}</Alert>;
  }

  return (
    <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: '300px 1fr' } }}>
      <Box sx={{ maxHeight: '55vh', overflow: 'auto', pr: 1 }}>
        <TextField
          fullWidth
          size="small"
          label="Filter elements"
          value={filter}
          onChange={(event) => setFilter(event.target.value)}
          sx={{ mb: 1.5 }}
        />
        {filtered.map((element) => (
          <Box
            key={key(element)}
            onClick={() => setSelectedPath(key(element))}
            sx={{
              px: 1,
              py: 0.6,
              borderRadius: 1,
              cursor: 'pointer',
              bgcolor: selectedPath === key(element) ? 'action.selected' : 'transparent',
              '&:hover': { bgcolor: 'action.hover' },
            }}
          >
            <Typography variant="caption" sx={{ fontFamily: 'monospace' }}>
              {element.path}
              {element.sliceName ? `:${element.sliceName}` : ''}
            </Typography>
            {element.constrained && (
              <Chip size="small" label="profiled" sx={{ ml: 0.75, height: 16, fontSize: 10 }} />
            )}
          </Box>
        ))}
      </Box>

      <Box>
        {!selected ? (
          <Typography variant="body2" color="text.secondary">
            Pick an element to constrain it. Only changes the base definition permits are offered.
          </Typography>
        ) : (
          <>
            <Typography variant="subtitle2" sx={{ fontFamily: 'monospace' }}>
              {selected.path}
              {selected.sliceName ? `:${selected.sliceName}` : ''}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {selected.limits.knownToBase
                ? `Base allows ${selected.limits.minMin ?? 0}..${selected.limits.maxMax ?? '*'}`
                : 'The base does not describe this element directly, so it is unconstrained here.'}
            </Typography>

            <Box sx={{ mt: 2 }}>
              <ElementConstraintForm
                workspaceId={workspaceId}
                file={file}
                element={selected}
                strengths={strengths}
                extensions={extensions}
                onExtensionCreated={onExtensionCreated}
                onApplied={() => {
                  load();
                  onSaved();
                }}
              />
            </Box>

          </>
        )}
      </Box>
    </Box>
  );
}

function key(element: ProfileElement): string {
  return element.sliceName ? `${element.path}:${element.sliceName}` : element.path;
}
