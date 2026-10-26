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

import { Alert } from '@wso2/oxygen-ui';
import { useState, type ReactNode } from 'react';

/** Where a dismissal is remembered. Namespaced so the key says which tool wrote it. */
const PREFIX = 'fhir-codegen.hint.';

/**
 * Advice for someone meeting a part of the tool for the first time, which stays out of the way afterwards.
 *
 * Dismissal is remembered in the browser rather than on the service: the tool is stateless by design, and a
 * hint that has been read is exactly the kind of thing that should not become server-side state. A browser that
 * refuses storage simply shows the hint again, which is the harmless failure.
 */
export default function FirstRunHint({
  id,
  children,
}: {
  /** Identifies the hint, so dismissing one does not silence the others. */
  id: string;
  children: ReactNode;
}) {
  const key = PREFIX + id;
  const [dismissed, setDismissed] = useState(() => {
    try {
      return window.localStorage.getItem(key) === 'dismissed';
    } catch {
      return false;
    }
  });

  if (dismissed) {
    return null;
  }

  return (
    <Alert
      severity="info"
      onClose={() => {
        try {
          window.localStorage.setItem(key, 'dismissed');
        } catch {
          // Storage being unavailable only costs the hint reappearing next time.
        }
        setDismissed(true);
      }}
      sx={{ mb: 1.5, alignItems: 'flex-start' }}
    >
      {children}
    </Alert>
  );
}
