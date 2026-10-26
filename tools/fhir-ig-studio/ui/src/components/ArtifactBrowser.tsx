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
  Box,
  CircularProgress,
  CodeBlock,
  Collapse,
  Grid,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Paper,
  Typography,
} from '@wso2/oxygen-ui';
import {
  ChevronDown,
  ChevronRight,
  FileCode,
  FileJson,
  FileText,
  FolderOpen,
} from '@wso2/oxygen-ui-icons-react';
import { useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';
import type { ArtifactContent, ArtifactNode } from '../api/types';

/** CodeBlock only highlights a fixed language set, so map generated file types onto the closest one. */
function languageFor(path: string): 'typescript' | 'json' | 'bash' | 'markup' {
  if (path.endsWith('.bal')) return 'typescript';
  if (path.endsWith('.json')) return 'json';
  if (path.endsWith('.toml') || path.endsWith('.yaml') || path.endsWith('.yml')) return 'bash';
  return 'markup';
}

function iconFor(path: string) {
  if (path.endsWith('.bal')) return <FileCode size={16} />;
  if (path.endsWith('.json')) return <FileJson size={16} />;
  return <FileText size={16} />;
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** Picks a sensible file to show first: the aggregated service, else the first file found. */
function firstInterestingFile(node: ArtifactNode): string | null {
  const files: string[] = [];
  const walk = (current: ArtifactNode) => {
    if (current.type === 'file') {
      files.push(current.path);
      return;
    }
    (current.children ?? []).forEach(walk);
  };
  walk(node);
  return (
    files.find((path) => path.endsWith('service.bal')) ??
    files.find((path) => path.endsWith('.bal')) ??
    files[0] ??
    null
  );
}

interface TreeProps {
  node: ArtifactNode;
  depth: number;
  selected: string | null;
  expanded: Set<string>;
  onToggle: (path: string) => void;
  onSelect: (path: string) => void;
}

function TreeEntry({ node, depth, selected, expanded, onToggle, onSelect }: TreeProps) {
  if (node.type === 'file') {
    return (
      <ListItemButton
        dense
        selected={selected === node.path}
        onClick={() => onSelect(node.path)}
        sx={{ pl: 1 + depth * 1.5, borderRadius: 1 }}
      >
        <ListItemIcon sx={{ minWidth: 26 }}>{iconFor(node.path)}</ListItemIcon>
        <ListItemText
          primary={node.name}
          secondary={formatBytes(node.size)}
          primaryTypographyProps={{ variant: 'body2', noWrap: true }}
          secondaryTypographyProps={{ variant: 'caption' }}
        />
      </ListItemButton>
    );
  }

  const isOpen = expanded.has(node.path);
  const children = node.children ?? [];
  return (
    <>
      <ListItemButton dense onClick={() => onToggle(node.path)} sx={{ pl: 1 + depth * 1.5, borderRadius: 1 }}>
        <ListItemIcon sx={{ minWidth: 26 }}>
          {isOpen ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
        </ListItemIcon>
        <ListItemText
          primary={node.name || 'output'}
          secondary={`${children.length} item${children.length === 1 ? '' : 's'}`}
          primaryTypographyProps={{ variant: 'body2', fontWeight: 500, noWrap: true }}
          secondaryTypographyProps={{ variant: 'caption' }}
        />
      </ListItemButton>
      <Collapse in={isOpen} unmountOnExit>
        {children.map((child) => (
          <TreeEntry
            key={child.path}
            node={child}
            depth={depth + 1}
            selected={selected}
            expanded={expanded}
            onToggle={onToggle}
            onSelect={onSelect}
          />
        ))}
      </Collapse>
    </>
  );
}

export default function ArtifactBrowser({ jobId, tree }: { jobId: string; tree: ArtifactNode }) {
  const initial = useMemo(() => firstInterestingFile(tree), [tree]);
  const [selected, setSelected] = useState<string | null>(initial);
  const [expanded, setExpanded] = useState<Set<string>>(() => {
    // Expand the root and the branch leading to the initially selected file.
    const paths = new Set<string>(['']);
    if (initial) {
      const segments = initial.split('/');
      segments.pop();
      let prefix = '';
      for (const segment of segments) {
        prefix = prefix ? `${prefix}/${segment}` : segment;
        paths.add(prefix);
      }
    }
    return paths;
  });
  const [content, setContent] = useState<ArtifactContent | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!selected) {
      setContent(null);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    api
      .getFile(jobId, selected)
      .then((result) => {
        if (!cancelled) setContent(result);
      })
      .catch((e: Error) => {
        if (!cancelled) setError(e.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [jobId, selected]);

  const toggle = (path: string) =>
    setExpanded((current) => {
      const next = new Set(current);
      if (next.has(path)) {
        next.delete(path);
      } else {
        next.add(path);
      }
      return next;
    });

  return (
    <Grid container spacing={2}>
      <Grid size={{ xs: 12, md: 4 }}>
        <Paper variant="outlined" sx={{ maxHeight: 520, overflow: 'auto', p: 0.5 }}>
          <List dense disablePadding>
            <TreeEntry
              node={tree}
              depth={0}
              selected={selected}
              expanded={expanded}
              onToggle={toggle}
              onSelect={setSelected}
            />
          </List>
        </Paper>
      </Grid>
      <Grid size={{ xs: 12, md: 8 }}>
        <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 1,
              px: 2,
              py: 1,
              borderBottom: 1,
              borderColor: 'divider',
              bgcolor: 'action.hover',
            }}
          >
            <FolderOpen size={16} />
            <Typography variant="subtitle2" noWrap sx={{ flex: 1 }}>
              {selected ?? 'Select a file'}
            </Typography>
            {content && !content.truncated && (
              <Typography variant="caption" color="text.secondary">
                {formatBytes(content.size)}
              </Typography>
            )}
          </Box>
          <Box sx={{ maxHeight: 470, overflow: 'auto', p: selected ? 0 : 3 }}>
            {loading && (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                <CircularProgress size={22} />
              </Box>
            )}
            {!loading && error && (
              <Typography variant="body2" color="error" sx={{ p: 3 }}>
                {error}
              </Typography>
            )}
            {!loading && !error && !selected && (
              <Typography variant="body2" color="text.secondary">
                Pick a generated file from the tree to preview it.
              </Typography>
            )}
            {!loading && !error && content && content.truncated && (
              <Typography variant="body2" color="text.secondary" sx={{ p: 3 }}>
                {content.message ?? 'This file is too large to preview.'}
              </Typography>
            )}
            {!loading && !error && content && content.binary && (
              <Typography variant="body2" color="text.secondary" sx={{ p: 3 }}>
                This file is not text. Download the archive to inspect it.
              </Typography>
            )}
            {!loading && !error && content && !content.truncated && !content.binary && (
              <CodeBlock code={content.content ?? ''} language={languageFor(content.path)} showLineNumbers />
            )}
          </Box>
        </Paper>
      </Grid>
    </Grid>
  );
}
