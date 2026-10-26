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
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  CircularProgress,
  MenuItem,
  PageContent,
  PageTitle,
  Paper,
  TextField,
  Typography,
} from '@wso2/oxygen-ui';
import { Search, Upload } from '@wso2/oxygen-ui-icons-react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { api, ApiError } from '../api/client';
import type { IgVersions, RegistrySearchResult, ServiceMeta } from '../api/types';

/** Remembered across visits so a team pointed at their own registry does not re-pick it every time. */
const REGISTRY_STORAGE_KEY = 'fhir-package-generator.registryUrl';

/** Dropdown entries that are actions rather than a registry to point at. */
const CUSTOM_OPTION = '__custom__';
const UPLOAD_OPTION = '__upload__';

/**
 * Entry point: find a published FHIR package and open it. Opening downloads the package and hands back a
 * workspace of its StructureDefinitions, which the explorer then shows and lets you edit.
 */
export default function PackagesPage() {
  const navigate = useNavigate();
  const [meta, setMeta] = useState<ServiceMeta | null>(null);
  const [defaultRegistryUrl, setDefaultRegistryUrl] = useState('');
  const [registryUrl, setRegistryUrl] = useState('');
  const [customRegistry, setCustomRegistry] = useState(false);
  const [uploadMode, setUploadMode] = useState(false);
  const [searchMessage, setSearchMessage] = useState<string | null>(null);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<RegistrySearchResult[]>([]);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  const [selected, setSelected] = useState<RegistrySearchResult | null>(null);
  const [versions, setVersions] = useState<IgVersions | null>(null);
  const [version, setVersion] = useState('');
  const [loadingVersions, setLoadingVersions] = useState(false);
  const [opening, setOpening] = useState(false);
  const [openError, setOpenError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  useEffect(() => {
    api.getMeta().then(setMeta).catch(() => setMeta(null));
    api
      .listRegistries()
      .then((response) => {
        setDefaultRegistryUrl(response.defaultUrl);
        const remembered = localStorage.getItem(REGISTRY_STORAGE_KEY);
        const initial = remembered ?? response.defaultUrl;
        setRegistryUrl(initial);
        // Anything other than the configured default is, by definition, a custom registry.
        setCustomRegistry(initial !== response.defaultUrl);
      })
      .catch(() => setDefaultRegistryUrl(''));
  }, []);

  // Changing registry invalidates everything already on screen: results and versions came from the old one.
  useEffect(() => {
    if (!registryUrl) return;
    localStorage.setItem(REGISTRY_STORAGE_KEY, registryUrl);
    setResults([]);
    setSelected(null);
  }, [registryUrl]);

  // Debounced so typing a package id does not fire a request per keystroke.
  useEffect(() => {
    const term = query.trim();
    if (term.length < 2) {
      setResults([]);
      setSearchError(null);
      return;
    }
    let cancelled = false;
    setSearching(true);
    const timer = setTimeout(() => {
      api
        .searchIgs(term, registryUrl || undefined)
        .then((response) => {
          if (!cancelled) {
            setResults(response.results);
            setSearchError(null);
            // A registry that does not implement catalog search says so here rather than failing.
            setSearchMessage(response.message ?? null);
          }
        })
        .catch((e: Error) => {
          if (!cancelled) {
            setResults([]);
            setSearchError(e.message);
          }
        })
        .finally(() => {
          if (!cancelled) setSearching(false);
        });
    }, 300);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [query, registryUrl]);

  // Versions are only read once a package is picked, so the list stays one request per selection.
  useEffect(() => {
    if (!selected) {
      setVersions(null);
      setVersion('');
      return;
    }
    let cancelled = false;
    setLoadingVersions(true);
    api
      .getIgVersions(selected.name, registryUrl || undefined)
      .then((result) => {
        if (cancelled) return;
        setVersions(result);
        setVersion(result.latest ?? result.versions[0] ?? '');
      })
      .catch(() => {
        if (!cancelled) setVersions(null);
      })
      .finally(() => {
        if (!cancelled) setLoadingVersions(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selected, registryUrl]);

  const open = async () => {
    if (!selected || !version) return;
    setOpening(true);
    setOpenError(null);
    try {
      const workspace = await api.openPackage(selected.name, version, registryUrl || undefined);
      navigate(`/packages/${encodeURIComponent(workspace.workspaceId)}`);
    } catch (e) {
      setOpenError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setOpening(false);
    }
  };

  const defaultRegistryHost = (() => {
    try {
      return defaultRegistryUrl ? new URL(defaultRegistryUrl).host : '';
    } catch {
      return '';
    }
  })();

  /** An uploaded archive becomes a workspace just like a downloaded package, so it opens the same explorer. */
  const upload = async (file: File) => {
    setUploading(true);
    setUploadError(null);
    try {
      const spec = await api.uploadSpec(file, file.name.replace(/\.zip$/i, ''));
      navigate(`/packages/${encodeURIComponent(spec.specId)}`);
    } catch (e) {
      setUploadError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setUploading(false);
    }
  };

  const shown: RegistrySearchResult[] =
    query.trim().length < 2
      ? (meta?.suggestedIgs ?? []).map((ig) => ({
          name: ig.name,
          title: ig.title,
          fhirVersion: ig.fhirVersion,
          description: ig.description,
        }))
      : results;

  return (
    <PageContent>
      <PageTitle>
        <PageTitle.Header>FHIR packages</PageTitle.Header>
        <PageTitle.SubHeader>
          Search the FHIR package registry, browse what an implementation guide defines, adjust its structure
          definitions, and generate a Ballerina facade to try it out.
        </PageTitle.SubHeader>
      </PageTitle>

      <Paper variant="outlined" sx={{ p: 3, mb: 3 }}>
        {/* One control for where a package comes from: a registry, your own registry, or a file you have. */}
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: uploadMode ? 0 : 2 }}>
          <TextField
            select
            size="small"
            label="Package source"
            sx={{ minWidth: 280 }}
            value={uploadMode ? UPLOAD_OPTION : customRegistry ? CUSTOM_OPTION : registryUrl}
            onChange={(event) => {
              const value = event.target.value;
              setUploadMode(value === UPLOAD_OPTION);
              setCustomRegistry(value === CUSTOM_OPTION);
              if (value !== UPLOAD_OPTION && value !== CUSTOM_OPTION) {
                setRegistryUrl(value);
              }
            }}
          >
            <MenuItem value={defaultRegistryUrl}>
              Default package registry{defaultRegistryHost ? ` (${defaultRegistryHost})` : ''}
            </MenuItem>
            <MenuItem value={CUSTOM_OPTION}>Custom FHIR registry</MenuItem>
            <MenuItem value={UPLOAD_OPTION}>Upload a package</MenuItem>
          </TextField>
          {customRegistry && !uploadMode && (
            <TextField
              size="small"
              sx={{ flex: 1, minWidth: 280 }}
              label="Registry URL"
              placeholder="https://packages.example.org"
              value={registryUrl}
              onChange={(event) => setRegistryUrl(event.target.value.trim())}
              helperText="Any registry implementing the FHIR package registry API."
            />
          )}
          {uploadMode && (
            <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap', flex: 1 }}>
              <Button
                component="label"
                variant="contained"
                startIcon={uploading ? <CircularProgress size={14} color="inherit" /> : <Upload size={16} />}
                disabled={uploading}
              >
                {uploading ? 'Uploading…' : 'Choose a file'}
                <input
                  hidden
                  type="file"
                  accept=".zip,.tgz,application/zip,application/gzip"
                  onChange={(event) => {
                    const file = event.target.files?.[0];
                    event.target.value = '';
                    if (file) void upload(file);
                  }}
                />
              </Button>
              <Typography variant="caption" color="text.secondary" sx={{ flex: 1, minWidth: 220 }}>
                A <code>.zip</code> or <code>.tgz</code> of FHIR definitions. It opens the same way a downloaded
                package does.
              </Typography>
            </Box>
          )}
        </Box>

        {/* Searching only makes sense against a registry, so it gives way to the upload control. */}
        {!uploadMode && (
          <TextField
            fullWidth
            autoFocus
            label="Search the FHIR package registry"
            placeholder="us.core, carin, davinci, eu.base …"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            error={Boolean(searchError)}
            helperText={
              searchError ??
              (searching
                ? 'Searching…'
                : query.trim().length < 2
                  ? 'Type at least two characters, or pick one of the common guides below.'
                  : `${results.length} package${results.length === 1 ? '' : 's'} found`)
            }
            InputProps={{
              startAdornment: <Search size={16} style={{ marginRight: 8, opacity: 0.6 }} />,
              endAdornment: searching ? <CircularProgress size={16} /> : null,
            }}
          />
        )}

        {uploadError && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {uploadError}
          </Alert>
        )}
      </Paper>

      {!uploadMode && query.trim().length < 2 && (
        <Typography variant="overline" color="text.secondary">
          Common implementation guides
        </Typography>
      )}

      <Box
        sx={{
          display: uploadMode ? 'none' : 'grid',
          gap: 1.5,
          gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
          mt: 1,
        }}
      >
        {shown.map((entry) => (
          <Card
            key={entry.name}
            variant="outlined"
            sx={{
              borderColor: selected?.name === entry.name ? 'primary.main' : 'divider',
              borderWidth: selected?.name === entry.name ? 2 : 1,
            }}
          >
            <CardActionArea onClick={() => setSelected(entry)}>
              <CardContent>
                <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mb: 0.5 }}>
                  <Typography variant="subtitle2" fontWeight={600}>
                    {entry.name}
                  </Typography>
                  {entry.fhirVersion && (
                    <Chip size="small" label={`FHIR ${entry.fhirVersion.toUpperCase()}`} />
                  )}
                </Box>
                {entry.description && (
                  <Typography variant="caption" color="text.secondary">
                    {entry.description.slice(0, 180)}
                    {entry.description.length > 180 ? '…' : ''}
                  </Typography>
                )}
              </CardContent>
            </CardActionArea>
          </Card>
        ))}
      </Box>

      {!uploadMode && searchMessage && query.trim().length >= 2 && !searching && (
        <Alert severity="warning" sx={{ mt: 2 }}>
          {searchMessage}
        </Alert>
      )}

      {!uploadMode && shown.length === 0 && query.trim().length >= 2 && !searching && !searchError && (
        <Alert
          severity="info"
          sx={{ mt: 2 }}
          action={
            <Button
              size="small"
              onClick={() =>
                setSelected({ name: query.trim(), title: query.trim(), fhirVersion: null, description: null })
              }
            >
              Open “{query.trim()}” anyway
            </Button>
          }
        >
          No packages on this registry match “{query}”.
        </Alert>
      )}

      {selected && !uploadMode && (
        <Paper variant="outlined" sx={{ p: 3, mt: 3 }}>
          <Typography variant="h6">{selected.name}</Typography>
          <Box sx={{ display: 'flex', gap: 1, mt: 0.75, flexWrap: 'wrap' }}>
            {(versions?.fhirVersion ?? selected.fhirVersion) && (
              <Chip
                size="small"
                label={`FHIR ${(versions?.fhirVersion ?? selected.fhirVersion ?? '').toUpperCase()}`}
              />
            )}
            {versions?.mappedPackage && (
              <Chip size="small" color="info" variant="outlined" label={`Published as ${versions.mappedPackage}`} />
            )}
          </Box>
          {/* The registry's description of the guide, so there is something to go on before opening it. */}
          {(versions?.description ?? selected.description) && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1.5 }}>
              {(versions?.description ?? selected.description ?? '').slice(0, 600)}
              {(versions?.description ?? selected.description ?? '').length > 600 ? '…' : ''}
            </Typography>
          )}
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-start', flexWrap: 'wrap', mt: 2 }}>
            <TextField
              select
              label="Version"
              value={version}
              disabled={loadingVersions || !versions}
              onChange={(event) => setVersion(event.target.value)}
              sx={{ minWidth: 220 }}
              helperText={
                loadingVersions
                  ? 'Reading registry metadata…'
                  : versions?.latest
                    ? `Latest is ${versions.latest}`
                    : ' '
              }
            >
              {(versions?.versions ?? []).map((v) => (
                <MenuItem key={v} value={v}>
                  {v}
                  {v === versions?.latest ? ' (latest)' : ''}
                </MenuItem>
              ))}
            </TextField>
            <Button
              variant="contained"
              disabled={!version || opening}
              onClick={() => void open()}
              startIcon={opening ? <CircularProgress size={14} color="inherit" /> : undefined}
              sx={{ mt: 1 }}
            >
              {opening ? 'Opening…' : 'Open package'}
            </Button>
          </Box>
          {opening && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
              Downloading and extracting the package. The first open of a guide can take a little while.
            </Typography>
          )}
          {openError && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {openError}
            </Alert>
          )}
        </Paper>
      )}
    </PageContent>
  );
}
