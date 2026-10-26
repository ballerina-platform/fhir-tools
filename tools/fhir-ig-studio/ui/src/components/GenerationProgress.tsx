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

import { Alert, Box, CircularProgress, LinearProgress, Typography } from '@wso2/oxygen-ui';
import { Check } from '@wso2/oxygen-ui-icons-react';
import type { JobStage } from '../api/types';

/**
 * What a generation is doing, without saying where it is doing it.
 *
 * <p>The generator's own log names the directory it extracted a package into and the directory it wrote into,
 * which on a hosted service is somebody else's server. So the log is not shown. What a person waiting on a
 * generation actually wants is how far along it is and whether it is still moving, and that is a named stage
 * and a bar.
 */
export default function GenerationProgress({
  stages,
  currentStage,
  percent,
  running,
  failed,
  messages,
}: {
  stages: JobStage[];
  currentStage: string;
  percent: number;
  running: boolean;
  failed: boolean;
  /** Anything that went wrong, already stripped of server paths by the service. */
  messages: string[];
}) {
  // "queued" and "done" bracket the run; the steps worth listing are the ones in between.
  const steps = stages.filter((stage) => stage.key !== 'queued' && stage.key !== 'done');
  const currentIndex = steps.findIndex((stage) => stage.key === currentStage);
  const finished = currentStage === 'done';

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
        <Typography variant="body2" sx={{ fontWeight: 600, flex: 1 }}>
          {failed
            ? 'Generation failed'
            : finished
              ? 'Done'
              : (steps[currentIndex]?.title ?? 'Starting…')}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {percent}%
        </Typography>
      </Box>

      <LinearProgress
        variant={running && percent === 0 ? 'indeterminate' : 'determinate'}
        value={percent}
        color={failed ? 'error' : 'primary'}
        sx={{ height: 8, borderRadius: 4, mb: 2.5 }}
      />

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.25 }}>
        {steps.map((stage, index) => {
          const done = finished || (currentIndex >= 0 && index < currentIndex);
          const active = !finished && index === currentIndex;
          return (
            <Box key={stage.key} sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
              <Box
                sx={{
                  width: 20,
                  height: 20,
                  borderRadius: '50%',
                  flexShrink: 0,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  bgcolor: done ? 'success.main' : active ? 'transparent' : 'action.disabledBackground',
                  color: 'success.contrastText',
                }}
              >
                {done && <Check size={13} />}
                {active && !failed && <CircularProgress size={16} />}
              </Box>
              <Typography
                variant="body2"
                color={done || active ? 'text.primary' : 'text.disabled'}
                sx={{ fontWeight: active ? 600 : 400 }}
              >
                {stage.title}
              </Typography>
            </Box>
          );
        })}
      </Box>

      {messages.length > 0 && (
        <Alert severity={failed ? 'error' : 'warning'} sx={{ mt: 2.5 }}>
          <Box component="ul" sx={{ m: 0, pl: 2.5 }}>
            {messages.map((message, index) => (
              <Box component="li" key={index}>
                <Typography variant="caption">{message}</Typography>
              </Box>
            ))}
          </Box>
        </Alert>
      )}
    </Box>
  );
}
