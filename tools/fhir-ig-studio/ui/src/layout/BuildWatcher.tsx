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

import { Alert, Button, Snackbar } from '@wso2/oxygen-ui';
import { useCallback, useEffect, useState } from 'react';

/**
 * Tells the reader when the page they are using is no longer the page the service is serving.
 *
 * <p>The bundle is content-hashed and served immutable, which is right -- but it means a tab left open keeps
 * running whatever it loaded, however many times the service has been rebuilt since. A fix then appears not to
 * work, and the obvious conclusion is that the fix is wrong rather than that it was never loaded. The index
 * document is served no-store, so asking for it is enough to learn which bundle is current.
 */
export default function BuildWatcher() {
  const [stale, setStale] = useState(false);

  const check = useCallback(async () => {
    try {
      const html = await fetch('/', { cache: 'no-store' }).then((r) => r.text());
      const served = html.match(/assets\/(index-[A-Za-z0-9_-]+\.js)/)?.[1];
      const running = Array.from(document.querySelectorAll('script[src]'))
        .map((s) => (s as HTMLScriptElement).src.match(/(index-[A-Za-z0-9_-]+\.js)/)?.[1])
        .find(Boolean);
      if (served && running && served !== running) {
        setStale(true);
      }
    } catch {
      // Offline or the service is restarting; silence is the right answer either way.
    }
  }, []);

  useEffect(() => {
    void check();
    const timer = setInterval(() => void check(), 60_000);
    const onFocus = () => void check();
    window.addEventListener('focus', onFocus);
    return () => {
      clearInterval(timer);
      window.removeEventListener('focus', onFocus);
    };
  }, [check]);

  return (
    <Snackbar open={stale} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
      <Alert
        severity="info"
        action={
          <Button color="inherit" size="small" onClick={() => window.location.reload()}>
            Reload
          </Button>
        }
      >
        This page is running an older build of the tool. Reload to pick up the current one.
      </Alert>
    </Snackbar>
  );
}
