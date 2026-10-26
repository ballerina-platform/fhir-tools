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

import { Box, Button, IconButton, Paper, Typography } from '@wso2/oxygen-ui';
import { CirclePlay, Download, Layers, Rocket, X } from '@wso2/oxygen-ui-icons-react';
import type { ReactNode } from 'react';

/** Where dismissing the guide is remembered, alongside the other first-run hints. */
const KEY = 'fhir-codegen.hint.package-workflow';

/** Whether the guide has been put away in this browser. Unavailable storage just shows it again. */
export function workflowGuideDismissed(): boolean {
  try {
    return window.localStorage.getItem(KEY) === 'dismissed';
  } catch {
    return false;
  }
}

export function rememberWorkflowGuide(dismissed: boolean): void {
  try {
    if (dismissed) {
      window.localStorage.setItem(KEY, 'dismissed');
    } else {
      window.localStorage.removeItem(KEY);
    }
  } catch {
    // Storage being unavailable only costs the guide reappearing next time.
  }
}

function Step({
  number,
  icon,
  title,
  children,
}: {
  number: number;
  icon: ReactNode;
  title: string;
  children: ReactNode;
}) {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, minWidth: 0 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <Box
          sx={{
            width: 28,
            height: 28,
            borderRadius: '50%',
            flexShrink: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            bgcolor: 'primary.main',
            color: 'primary.contrastText',
          }}
        >
          {icon}
        </Box>
        <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
          {number}. {title}
        </Typography>
      </Box>
      <Typography variant="body2" color="text.secondary" component="div">
        {children}
      </Typography>
    </Box>
  );
}

/**
 * The three things this page is for, in the order they are usually done.
 *
 * <p>Shown on a package until it is dismissed, and brought back from the page header. It describes the flow
 * rather than repeating the page's own controls: the one action it offers is the walkthrough, which shows the
 * steps being done.
 */
export default function WorkflowGuide({
  onShowWalkthrough,
  onDismiss,
}: {
  onShowWalkthrough: () => void;
  onDismiss: () => void;
}) {
  return (
    <Paper variant="outlined" sx={{ p: 2.5, mb: 3, position: 'relative' }}>
      <IconButton
        size="small"
        title="Hide the guide. Bring it back with the ? button above."
        onClick={onDismiss}
        sx={{ position: 'absolute', top: 8, right: 8 }}
      >
        <X size={16} />
      </IconButton>
      <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 2, pr: 4 }}>
        From implementation guide to running FHIR server
      </Typography>
      <Box
        sx={{
          display: 'grid',
          gap: 3,
          // The middle step says the most, so it gets more room.
          gridTemplateColumns: { xs: '1fr', md: '1fr 1.35fr 1fr' },
        }}
      >
        <Step number={1} icon={<Download size={15} />} title="Load an implementation guide">
          Search the FHIR package registry or pick a common guide on the packages page, or choose{' '}
          <strong>Package source</strong> there to upload your own definitions. Its profiles, extensions and
          value sets are then listed on the left; pick one to see its element table.
        </Step>
        <Step number={2} icon={<Layers size={15} />} title="Easy profiling">
          Click a row to constrain it in place, or author your own with <strong>New profile</strong> and{' '}
          <strong>New extension</strong>. Or let the Dev Bot do it: tag elements with{' '}
          <strong>Edit with Dev Bot</strong> or <strong>@</strong> in the chat, say what should change, and{' '}
          <strong>Apply</strong> what it proposes. Every change is checked against the profiling rules.{' '}
          <strong>Save as project</strong> keeps your IG; <strong>My packages</strong> downloads it.
        </Step>
        <Step number={3} icon={<Rocket size={15} />} title="Generate the FHIR facade">
          <strong>Generate FHIR facade</strong> instantly turns the profiles into an executable FHIR facade: an
          API per resource with its search parameters, and a capability statement at <code>/metadata</code>,
          ready to download and run.
        </Step>
      </Box>
      <Button size="small" variant="outlined" startIcon={<CirclePlay size={14} />} onClick={onShowWalkthrough} sx={{ mt: 2 }}>
        Show me how
      </Button>
    </Paper>
  );
}
