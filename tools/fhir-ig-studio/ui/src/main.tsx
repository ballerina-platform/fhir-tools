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

import { OxygenUIThemeProvider, AcrylicOrangeTheme, ClassicTheme, WSO2Theme } from '@wso2/oxygen-ui';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, HashRouter } from 'react-router';
import App from './App';
import './index.css';

// The service hosts the built bundle from a relative base, so hash routing keeps deep links working there
// while dev mode keeps clean URLs.
const Router = import.meta.env.PROD ? HashRouter : BrowserRouter;

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <OxygenUIThemeProvider
      themes={[
        { key: 'acrylicOrange', label: 'Acrylic Orange', theme: AcrylicOrangeTheme },
        { key: 'wso2', label: 'WSO2', theme: WSO2Theme },
        { key: 'classic', label: 'Classic', theme: ClassicTheme },
      ]}
      initialTheme="acrylicOrange"
    >
      <Router>
        <App />
      </Router>
    </OxygenUIThemeProvider>
  </StrictMode>,
);
