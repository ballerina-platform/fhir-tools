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

import { Chip, CircularProgress } from '@wso2/oxygen-ui';
import { Ban, CircleCheck, CircleX, Clock } from '@wso2/oxygen-ui-icons-react';
import type { JobStatus } from '../api/types';

const APPEARANCE: Record<
  JobStatus,
  { label: string; color: 'default' | 'primary' | 'success' | 'error' | 'warning' }
> = {
  QUEUED: { label: 'Queued', color: 'default' },
  RUNNING: { label: 'Running', color: 'primary' },
  SUCCEEDED: { label: 'Succeeded', color: 'success' },
  FAILED: { label: 'Failed', color: 'error' },
  CANCELLED: { label: 'Cancelled', color: 'warning' },
};

function iconFor(status: JobStatus) {
  switch (status) {
    case 'RUNNING':
      return <CircularProgress size={12} thickness={6} color="inherit" />;
    case 'SUCCEEDED':
      return <CircleCheck size={14} />;
    case 'FAILED':
      return <CircleX size={14} />;
    case 'CANCELLED':
      return <Ban size={14} />;
    default:
      return <Clock size={14} />;
  }
}

export default function StatusChip({ status, size = 'small' }: { status: JobStatus; size?: 'small' | 'medium' }) {
  const appearance = APPEARANCE[status];
  return (
    <Chip
      size={size}
      variant="outlined"
      color={appearance.color}
      icon={iconFor(status)}
      label={appearance.label}
    />
  );
}
