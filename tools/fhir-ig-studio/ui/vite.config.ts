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

import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react-swc';

/** Port the codegen service listens on; matches ServiceConfig's default. */
const SERVICE_PORT = process.env.FHIR_TOOL_SERVICE_PORT ?? '9095';

export default defineConfig({
  // Absolute base. A relative one resolves against the current route, so on a nested route such as
  // /packages/<id> the browser asks for /packages/assets/... ; the service answers that with the SPA
  // fallback, the page loads index.html as its own script, and nothing renders.
  base: '/',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: `http://localhost:${SERVICE_PORT}`, changeOrigin: true },
      '/healthz': { target: `http://localhost:${SERVICE_PORT}`, changeOrigin: true },
    },
  },
  plugins: [react()],
});
