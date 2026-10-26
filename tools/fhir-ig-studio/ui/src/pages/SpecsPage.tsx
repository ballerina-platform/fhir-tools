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
  CircularProgress,
  IconButton,
  PageContent,
  PageTitle,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@wso2/oxygen-ui';
import { RefreshCw, Trash2, Upload } from '@wso2/oxygen-ui-icons-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from '../api/client';
import type { UploadedSpec } from '../api/types';

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function SpecsPage() {
  const fileInput = useRef<HTMLInputElement | null>(null);
  const [specs, setSpecs] = useState<UploadedSpec[] | null>(null);
  const [name, setName] = useState('');
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setSpecs(await api.listSpecs());
      setError(null);
    } catch (e) {
      setError((e as Error).message);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const upload = async (file: File) => {
    setUploading(true);
    setError(null);
    setMessage(null);
    try {
      const spec = await api.uploadSpec(file, name.trim() || file.name.replace(/\.zip$/i, ''));
      setMessage(`Uploaded ${spec.name}: ${spec.definitionCount} structure definitions.`);
      setName('');
      await load();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setUploading(false);
      if (fileInput.current) {
        fileInput.current.value = '';
      }
    }
  };

  return (
    <PageContent>
      <PageTitle>
        <PageTitle.Header>Specifications</PageTitle.Header>
        <PageTitle.SubHeader>
          Upload your own FHIR definitions as a zip archive to generate from profiles that are not on the registry.
        </PageTitle.SubHeader>
        <PageTitle.Actions>
          <Button variant="outlined" startIcon={<RefreshCw size={16} />} onClick={() => void load()}>
            Refresh
          </Button>
        </PageTitle.Actions>
      </PageTitle>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      {message && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setMessage(null)}>
          {message}
        </Alert>
      )}

      <Paper variant="outlined" sx={{ p: 3, mb: 3 }}>
        <Typography variant="subtitle1" gutterBottom>
          Upload a specification
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          The archive should contain the StructureDefinition, ValueSet and CodeSystem JSON files of the guide. A
          FHIR package tarball extracted to a <code>package/</code> folder is also accepted.
        </Typography>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-start', flexWrap: 'wrap' }}>
          <TextField
            size="small"
            label="Name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            helperText="Optional. Defaults to the file name."
            sx={{ minWidth: 260 }}
          />
          <Button
            variant="contained"
            startIcon={uploading ? <CircularProgress size={14} color="inherit" /> : <Upload size={16} />}
            disabled={uploading}
            onClick={() => fileInput.current?.click()}
          >
            {uploading ? 'Uploading…' : 'Choose .zip'}
          </Button>
          <input
            ref={fileInput}
            type="file"
            accept=".zip,application/zip"
            hidden
            onChange={(event) => {
              const file = event.target.files?.[0];
              if (file) void upload(file);
            }}
          />
        </Box>
      </Paper>

      {specs && specs.length === 0 ? (
        <Paper variant="outlined" sx={{ p: 5, textAlign: 'center' }}>
          <Typography variant="body2" color="text.secondary">
            No specifications uploaded yet.
          </Typography>
        </Paper>
      ) : (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell align="right">Structure definitions</TableCell>
                <TableCell align="right">Files</TableCell>
                <TableCell align="right">Size</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(specs ?? []).map((spec) => (
                <TableRow key={spec.specId} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight={500}>
                      {spec.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {spec.specId}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">{spec.definitionCount}</TableCell>
                  <TableCell align="right">{spec.fileCount}</TableCell>
                  <TableCell align="right">{formatBytes(spec.totalBytes)}</TableCell>
                  <TableCell align="right">
                    <Tooltip title="Delete specification">
                      <IconButton
                        size="small"
                        onClick={() => {
                          void api.deleteSpec(spec.specId).then(load);
                        }}
                      >
                        <Trash2 size={15} />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </PageContent>
  );
}
