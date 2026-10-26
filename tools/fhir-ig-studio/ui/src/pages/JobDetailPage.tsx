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
  Grid,
  PageContent,
  PageTitle,
  StatCard,
  Tab,
  Tabs,
  Typography,
} from '@wso2/oxygen-ui';
import { Ban, Download, FileCode, FolderOpen, HardDrive, Play, RefreshCw } from '@wso2/oxygen-ui-icons-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Link as NavigateLink, useParams } from 'react-router';
import { api } from '../api/client';
import type { ArtifactTree, JobDetail, JobStage } from '../api/types';
import ArtifactBrowser from '../components/ArtifactBrowser';
import FacadeNextSteps from '../components/FacadeNextSteps';
import GenerationProgress from '../components/GenerationProgress';
import StatusChip from '../components/StatusChip';

/** How often a running job is polled for new log output. */
const POLL_INTERVAL_MS = 1200;

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDuration(millis: number | null): string {
  if (millis == null) return '—';
  if (millis < 1000) return `${millis} ms`;
  return `${(millis / 1000).toFixed(1)} s`;
}

export default function JobDetailPage() {
  const { jobId = '' } = useParams<{ jobId: string }>();
  const [job, setJob] = useState<JobDetail | null>(null);
  /** Whether this page has handed the archive over, so leaving can let the service reclaim the files. */
  const downloaded = useRef(false);
  /** Only what the service chose to surface: stage titles and any warning or error, never raw output. */
  const [lines, setLines] = useState<string[]>([]);
  const [stages, setStages] = useState<JobStage[]>([]);
  const [tree, setTree] = useState<ArtifactTree | null>(null);
  const [tab, setTab] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const cursor = useRef(0);

  const loadTree = useCallback(async () => {
    try {
      setTree(await api.getTree(jobId));
    } catch (e) {
      setError((e as Error).message);
    }
  }, [jobId]);

  /**
   * Leaving the page is what releases the generated files.
   *
   * <p>A facade is around 115 MB and the service keeps every job, so the files cannot stay for good. Download
   * alone is too early a signal: a zip that arrives truncated is worth a second try, and only the person can
   * judge that. Once they move on having taken it, they are done with it. Sent on unmount and on the tab going
   * away; the service ignores it unless the archive really was downloaded.
   */
  useEffect(() => {
    api.listStages().then(setStages).catch(() => setStages([]));
  }, []);

  useEffect(() => {
    const release = () => {
      if (downloaded.current) {
        api.releaseJob(jobId);
      }
    };
    window.addEventListener('pagehide', release);
    return () => {
      window.removeEventListener('pagehide', release);
      release();
    };
  }, [jobId]);

  // Poll status and the log tail until the job reaches a terminal state, then load the artifact tree once.
  useEffect(() => {
    let stopped = false;
    cursor.current = 0;
    setLines([]);
    setTree(null);
    setTab(0);

    const tick = async () => {
      if (stopped) return;
      try {
        const [detail, slice] = await Promise.all([api.getJob(jobId), api.getLogs(jobId, cursor.current)]);
        if (stopped) return;
        setJob(detail);
        if (slice.lines.length > 0) {
          cursor.current = slice.nextIndex;
          setLines((current) => [...current, ...slice.lines]);
        }
        setError(null);
        if (slice.done) {
          if (detail.status === 'SUCCEEDED') {
            // The file tree is loaded so the Files tab is ready when it is wanted, but the page stays where it
            // is. Finishing a generation is the moment to read what to do with it, and that is on Progress --
            // jumping to a directory listing answers a question nobody asked yet.
            await loadTree();
          }
          return;
        }
      } catch (e) {
        if (!stopped) setError((e as Error).message);
      }
      if (!stopped) {
        window.setTimeout(() => void tick(), POLL_INTERVAL_MS);
      }
    };
    void tick();
    return () => {
      stopped = true;
    };
  }, [jobId, loadTree]);

  const running = job != null && (job.status === 'QUEUED' || job.status === 'RUNNING');

  return (
    <PageContent>
      <PageTitle>
        <PageTitle.BackButton component={<NavigateLink to="/" />} />
        <PageTitle.Header>{job?.label ?? jobId}</PageTitle.Header>
        <PageTitle.SubHeader>
          {job ? `${job.mode === 'package' ? 'Ballerina package' : 'Service template'} · ${job.id}` : jobId}
        </PageTitle.SubHeader>
        <PageTitle.Actions>
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            {job && <StatusChip status={job.status} size="medium" />}
            {running && (
              <Button
                variant="outlined"
                color="warning"
                startIcon={<Ban size={16} />}
                onClick={() => void api.cancelJob(jobId)}
              >
                Cancel
              </Button>
            )}
            {job?.status === 'SUCCEEDED' && (
              <>
                <Button variant="outlined" startIcon={<RefreshCw size={16} />} onClick={() => void loadTree()}>
                  Refresh files
                </Button>
                <Button
                  variant="contained"
                  startIcon={<Download size={16} />}
                  component="a"
                  href={api.archiveUrl(jobId)}
                  disabled={job.collected}
                  onClick={() => {
                    downloaded.current = true;
                  }}
                >
                  {job.collected ? 'Downloaded' : 'Download zip'}
                </Button>
              </>
            )}
          </Box>
        </PageTitle.Actions>
      </PageTitle>

      {error && (
        <Alert severity="warning" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}
      {job?.error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {job.error}
        </Alert>
      )}

      {job && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid size={{ xs: 12, sm: 4 }}>
            <StatCard
              value={job.artifacts.fileCount}
              label="Generated files"
              icon={<FileCode size={22} />}
              iconColor="primary"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <StatCard
              value={formatBytes(job.artifacts.totalBytes)}
              label="Total size"
              icon={<HardDrive size={22} />}
              iconColor="info"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <StatCard
              value={formatDuration(job.durationMillis)}
              label="Duration"
              icon={<Play size={22} />}
              iconColor={job.status === 'FAILED' ? 'error' : 'success'}
            />
          </Grid>
        </Grid>
      )}

      <Tabs value={tab} onChange={(_, value: number) => setTab(value)} sx={{ mb: 2 }}>
        <Tab label="Progress" />
        <Tab
          label={
            <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
              Files
              {job?.status === 'SUCCEEDED' && <Chip size="small" label={job.artifacts.fileCount} />}
            </Box>
          }
          disabled={job?.status !== 'SUCCEEDED'}
        />
      </Tabs>

      {tab === 0 && (
        <GenerationProgress
          stages={stages}
          currentStage={job?.stage ?? 'queued'}
          percent={job?.progress ?? 0}
          running={running}
          failed={job?.status === 'FAILED'}
          // Stage titles are already drawn as steps; what is left is anything that went wrong.
          messages={lines.filter((line) => line.startsWith('[ERROR]') || line.startsWith('[WARN]'))}
        />
      )}

      {/* The point of the whole run: what to do with what was just built. */}
      {tab === 0 && job?.status === 'SUCCEEDED' && job.facade && (
        <FacadeNextSteps facade={job.facade} collected={Boolean(job.collected)} />
      )}

      {tab === 1 &&
        (tree ? (
          <ArtifactBrowser jobId={jobId} tree={tree.tree} />
        ) : (
          <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center', p: 4, justifyContent: 'center' }}>
            <CircularProgress size={20} />
            <Typography variant="body2" color="text.secondary">
              Reading generated files…
            </Typography>
          </Box>
        ))}

      {job?.status === 'SUCCEEDED' && tab === 1 && (
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mt: 2, color: 'text.secondary' }}>
          <FolderOpen size={14} />
          <Typography variant="caption">
            {job.artifacts.fileCount} files in {job.artifacts.directoryCount} directories
          </Typography>
        </Box>
      )}
    </PageContent>
  );
}
