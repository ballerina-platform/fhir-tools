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

import { Alert, Box, IconButton, Paper, Tooltip, Typography } from '@wso2/oxygen-ui';
import { Check, Copy } from '@wso2/oxygen-ui-icons-react';
import { useState } from 'react';
import type { JobFacade } from '../api/types';

/** A command someone is meant to run, with a button so they do not have to retype it. */
function Command({ children }: { children: string }) {
  const [copied, setCopied] = useState(false);
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 1,
        mt: 0.75,
        px: 1.25,
        py: 0.75,
        borderRadius: 1,
        bgcolor: 'action.hover',
        border: 1,
        borderColor: 'divider',
      }}
    >
      <Typography
        component="code"
        sx={{ fontFamily: 'monospace', fontSize: 12, flex: 1, overflowX: 'auto', whiteSpace: 'pre' }}
      >
        {children}
      </Typography>
      <Tooltip title={copied ? 'Copied' : 'Copy'}>
        <IconButton
          size="small"
          onClick={() => {
            void navigator.clipboard?.writeText(children).then(
              () => {
                setCopied(true);
                setTimeout(() => setCopied(false), 1500);
              },
              () => undefined,
            );
          }}
        >
          {copied ? <Check size={14} /> : <Copy size={14} />}
        </IconButton>
      </Tooltip>
    </Box>
  );
}

function Step({ n, title, children }: { n: number; title: string; children?: React.ReactNode }) {
  return (
    <Box sx={{ display: 'flex', gap: 1.5, mb: 2 }}>
      <Box
        sx={{
          width: 22,
          height: 22,
          borderRadius: '50%',
          flexShrink: 0,
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: 12,
          fontWeight: 700,
        }}
      >
        {n}
      </Box>
      <Box sx={{ minWidth: 0, flex: 1 }}>
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
          {title}
        </Typography>
        {children}
      </Box>
    </Box>
  );
}

/**
 * What to do with the facade that was just generated.
 *
 * <p>A finished generation is a zip on a page, which is not obviously a running FHIR server. These are the four
 * steps between the two, with the commands filled in for this particular build rather than left as placeholders
 * to work out. The archive's own README covers the rest, including changing the service and rebuilding it.
 */
export default function FacadeNextSteps({ facade, collected }: { facade: JobFacade; collected: boolean }) {
  const project = facade.project || 'fhir-service';
  const jar = facade.jar;
  const root = facade.archiveRoot;

  return (
    <Paper variant="outlined" sx={{ p: 2.5, mt: 2 }}>
      <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>
        Try out your facade
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Four steps from here to a running FHIR API.
      </Typography>

      <Step n={1} title={collected ? 'Download the zip — already collected' : 'Download the zip'}>
        <Typography variant="caption" color="text.secondary">
          {collected
            ? 'This one has been downloaded and its files removed from the server. Generate again if you need another copy.'
            : 'Use the Download zip button above. The files are removed from the server once you have taken them and left this page.'}
        </Typography>
      </Step>

      <Step n={2} title="Extract it">
        <Command>{`unzip ${root}.zip`}</Command>
      </Step>

      {jar ? (
        <Step n={3} title="Start the facade">
          <Typography variant="caption" color="text.secondary">
            Needs Java 21 or newer. Nothing else — the jar carries the Ballerina runtime.
          </Typography>
          <Command>{`java -jar ${root}/${project}/target/bin/${jar}`}</Command>
        </Step>
      ) : (
        <Step n={3} title="Build, then start the facade">
          <Typography variant="caption" color="text.secondary">
            This download has no jar — open the project in WSO2 Integrator and use the Build button, as the
            README explains, then run the jar it produces.
          </Typography>
        </Step>
      )}

      <Step n={4} title="Call it">
        <Command>{`curl -i http://localhost:${facade.port}/fhir/r4/${facade.sampleResource}`}</Command>
        <Alert severity="info" sx={{ mt: 1 }}>
          <Typography variant="caption">
            Expect <strong>501 Not Implemented</strong>. The routing, the FHIR request and response handling and
            the profile types are generated for you; the resource functions are stubs until you implement them.
            <br />
            <strong>README.md</strong> at the root of the zip covers that, and how to open the project in WSO2
            Integrator to change it.
          </Typography>
        </Alert>
      </Step>
    </Paper>
  );
}
