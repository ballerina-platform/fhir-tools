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

import { Box, IconButton, Paper, Tooltip, Typography } from '@wso2/oxygen-ui';
import { Copy, Terminal } from '@wso2/oxygen-ui-icons-react';
import { useEffect, useRef } from 'react';

/** Colours a log line by the severity marker the tool emits. */
function colorFor(line: string): string {
  if (line.startsWith('[ERROR]')) return 'error.main';
  if (line.startsWith('[WARN]')) return 'warning.main';
  if (line.startsWith('[SERVICE]')) return 'text.disabled';
  if (line.startsWith('[INFO]')) return 'text.secondary';
  return 'text.primary';
}

interface LogConsoleProps {
  lines: string[];
  /** Keeps the view pinned to the newest line while a job is still running. */
  follow?: boolean;
  emptyMessage?: string;
  maxHeight?: number | string;
}

export default function LogConsole({
  lines,
  follow = false,
  emptyMessage = 'Waiting for output…',
  maxHeight = 360,
}: LogConsoleProps) {
  const endRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (follow) {
      endRef.current?.scrollIntoView({ block: 'end' });
    }
  }, [lines, follow]);

  return (
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
        <Terminal size={16} />
        <Typography variant="subtitle2">Generation log</Typography>
        <Box sx={{ flex: 1 }} />
        <Typography variant="caption" color="text.secondary">
          {lines.length} lines
        </Typography>
        <Tooltip title="Copy log">
          <IconButton size="small" onClick={() => void navigator.clipboard?.writeText(lines.join('\n'))}>
            <Copy size={14} />
          </IconButton>
        </Tooltip>
      </Box>
      <Box
        sx={{
          maxHeight,
          overflow: 'auto',
          px: 2,
          py: 1.5,
          fontFamily: 'monospace',
          fontSize: 12.5,
          lineHeight: 1.7,
          bgcolor: 'background.default',
        }}
      >
        {lines.length === 0 ? (
          <Typography variant="body2" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
            {emptyMessage}
          </Typography>
        ) : (
          lines.map((line, index) => (
            <Box
              key={`${index}-${line.slice(0, 24)}`}
              component="pre"
              sx={{
                color: colorFor(line),
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                fontFamily: 'inherit',
                fontSize: 'inherit',
              }}
            >
              {line}
            </Box>
          ))
        )}
        <div ref={endRef} />
      </Box>
    </Paper>
  );
}
