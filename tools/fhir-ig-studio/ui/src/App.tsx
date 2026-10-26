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

import { Route, Routes } from 'react-router';
import AppLayout from './layout/AppLayout';
import ExplorerPage from './pages/ExplorerPage';
import JobDetailPage from './pages/JobDetailPage';
import PackagesPage from './pages/PackagesPage';
import ProjectsPage from './pages/ProjectsPage';
import SpecsPage from './pages/SpecsPage';

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<PackagesPage />} />
        <Route path="/packages/:workspaceId" element={<ExplorerPage />} />
        {/* The generation result; there is no generations list, the tool keeps no history. */}
        <Route path="/jobs/:jobId" element={<JobDetailPage />} />
        <Route path="/projects" element={<ProjectsPage />} />
        <Route path="/specs" element={<SpecsPage />} />
        <Route path="*" element={<PackagesPage />} />
      </Route>
    </Routes>
  );
}
