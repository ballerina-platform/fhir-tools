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
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  PageContent,
  PageTitle,
  Paper,
  Typography,
} from '@wso2/oxygen-ui';
import { Boxes, Download, RefreshCw, Trash2, Upload } from '@wso2/oxygen-ui-icons-react';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { api } from '../api/client';
import { projectStore, type StoredProject } from '../storage/projects';

/** "2026-09-17T08:31:00Z" -> "17 Sep 2026, 08:31", or nothing when it was never saved. */
function when(value: string | null): string {
  if (!value) {
    return '';
  }
  const at = new Date(value);
  return Number.isNaN(at.getTime())
    ? ''
    : at.toLocaleString(undefined, { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

/**
 * What a downloaded package is called: the name given to the project.
 *
 * <p>The project's name is the reader's own word for this work and the only name they will recognise, but it
 * is theirs to type, so it is reduced to something a filesystem will take. Work that was never named falls
 * back to the package it came from.
 */
function fileNameFor(project: StoredProject): string {
  const chosen = (project.projectName || `${project.packageName}-${project.version}`).trim();
  const safe = chosen.replace(/[^A-Za-z0-9._-]+/g, '-').replace(/^-+|-+$/g, '');
  return safe || 'fhir-package';
}

/** Definitions changed plus extensions authored: everything this package carries that the guide did not. */
function changeCount(project: StoredProject): number {
  return Object.keys(project.edits).length + project.extensions.length;
}

/**
 * The packages this tool has been used on, which is the only thing here that is the reader's own.
 *
 * A published guide is the same for everybody and is always a search away; what is worth listing is the guide
 * someone has taken up and changed. So a package appears here as soon as it has been edited, whether or not it
 * has been given a project name yet — work in progress is exactly what a person comes back looking for, and
 * requiring it to be named first would be the surest way to lose it.
 */
export default function ProjectsPage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState<StoredProject[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  /** The package a removal is being confirmed for: it frees a slot but discards the changes with it. */
  const [confirm, setConfirm] = useState<StoredProject | null>(null);

  const load = useCallback(() => {
    setError(null);
    try {
      setProjects(projectStore.list());
    } catch (e) {
      setProjects([]);
      setError(String(e instanceof Error ? e.message : e));
    }
  }, []);

  useEffect(load, [load]);

  return (
    <PageContent>
      <PageTitle>
        <PageTitle.Header>My packages</PageTitle.Header>
        <PageTitle.SubHeader>
          Implementation guides you have opened and changed. Open one to carry on, or generate a facade from it.
        </PageTitle.SubHeader>
      </PageTitle>

      <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
        <Button size="small" variant="outlined" startIcon={<Boxes size={16} />} onClick={() => navigate('/')}>
          Browse FHIR packages
        </Button>
        <Button size="small" startIcon={<RefreshCw size={16} />} onClick={load}>
          Refresh
        </Button>
        <Box sx={{ flex: 1 }} />
        <Button size="small" startIcon={<Upload size={16} />} onClick={() => navigate('/specs')}>
          Uploaded archives
        </Button>
      </Box>


      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {projects === null && <CircularProgress size={20} />}

      {projects !== null && projects.length === 0 && !error && (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="body2" color="text.secondary">
            Nothing here yet. Open a package from the FHIR registry and constrain an element, and it will show up
            here as a package of your own.
          </Typography>
          <Button variant="contained" sx={{ mt: 2 }} onClick={() => navigate('/')}>
            Browse FHIR packages
          </Button>
        </Paper>
      )}

      {projects?.map((project) => (
        <Paper
          key={project.workspaceId}
          variant="outlined"
          sx={{
            p: 2,
            mb: 1.5,
            display: 'flex',
            alignItems: 'center',
            gap: 2,
            cursor: 'pointer',
            '&:hover': { bgcolor: 'action.hover' },
          }}
          onClick={() => navigate(`/packages/${encodeURIComponent(project.workspaceId)}`)}
        >
          <Box sx={{ minWidth: 0, flex: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography variant="subtitle2" noWrap sx={{ fontWeight: 700 }}>
                {project.projectName || project.title || project.packageName}
              </Typography>
              {!project.projectName && <Chip size="small" variant="outlined" label="unsaved" />}
              {changeCount(project) > 0 && (
                <Chip size="small" color="warning" label={`${changeCount(project)} changed`} />
              )}
            </Box>
            <Typography variant="caption" color="text.secondary">
              {project.packageName}
              {project.version ? `@${project.version}` : ''}
              {project.fhirVersion ? ` · FHIR ${project.fhirVersion}` : ''}
              {when(project.savedAt) ? ` · saved ${when(project.savedAt)}` : ''}
            </Typography>
          </Box>
          <Button
            size="small"
            variant="outlined"
            onClick={(event) => {
              event.stopPropagation();
              navigate(`/packages/${encodeURIComponent(project.workspaceId)}`);
            }}
          >
            Open
          </Button>
          <Button
            size="small"
            variant="outlined"
            startIcon={<Download size={14} />}
            onClick={(event) => {
              event.stopPropagation();
              setError(null);
              void api
                .downloadPackage(project.workspaceId, `${fileNameFor(project)}.zip`)
                .catch((e) => setError(e instanceof Error ? e.message : String(e)));
            }}
          >
            Download
          </Button>
          <Button
            size="small"
            color="warning"
            startIcon={<Trash2 size={14} />}
            onClick={(event) => {
              event.stopPropagation();
              setConfirm(project);
            }}
          >
            Remove
          </Button>
        </Paper>
      ))}

      <Dialog open={confirm !== null} onClose={() => setConfirm(null)}>
        <DialogTitle>Remove this package?</DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            {confirm?.projectName || confirm?.packageName} has {confirm ? changeCount(confirm) : 0}{' '}
            change{confirm && changeCount(confirm) === 1 ? '' : 's'} stored in this browser. Removing it frees a
            slot and discards those changes; the published package itself is untouched and can be opened again
            from the registry.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirm(null)}>Cancel</Button>
          <Button
            color="warning"
            variant="contained"
            onClick={() => {
              if (confirm) {
                projectStore.remove(confirm.workspaceId);
              }
              setConfirm(null);
              load();
            }}
          >
            Remove
          </Button>
        </DialogActions>
      </Dialog>
    </PageContent>
  );
}
