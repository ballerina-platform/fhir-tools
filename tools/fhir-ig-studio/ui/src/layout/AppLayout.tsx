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

import {
  AppShell,
  Box,
  ColorSchemeToggle,
  Footer,
  Header,
  Sidebar,
  Tooltip,
} from '@wso2/oxygen-ui';
import { BookOpen, Boxes, FolderOpen, WSO2 } from '@wso2/oxygen-ui-icons-react';
import { Link as NavigateLink, Outlet, useLocation } from 'react-router';
import BuildWatcher from './BuildWatcher';

/** Maps the current route onto the sidebar item id. */
function activeItemFor(pathname: string): string {
  if (pathname.startsWith('/projects')) return 'projects';
  if (pathname.startsWith('/specs')) return 'projects';
  if (pathname.startsWith('/packages') || pathname.startsWith('/jobs')) return 'packages';
  return 'generate';
}

export default function AppLayout() {
  const location = useLocation();

  return (
    <AppShell initialCollapsed={false} collapseOnSelectOnMobile>
      <AppShell.Navbar>
        <Header>
          <Header.Toggle />
          {/* The brand is the way back to the start, which is where people reach for it. */}
          <Box
            component={NavigateLink}
            to="/"
            aria-label="FHIR IG Studio home"
            sx={{ display: 'flex', alignItems: 'center', textDecoration: 'none', color: 'inherit' }}
          >
            <Header.Brand>
              <Header.BrandLogo>
                <Box sx={{ display: 'flex', color: 'primary.main' }}>
                  <WSO2 size={22} />
                </Box>
              </Header.BrandLogo>
              <Header.BrandTitle>FHIR IG Studio</Header.BrandTitle>
            </Header.Brand>
          </Box>
          <Header.Spacer />
          <Header.Actions>
            <Tooltip title="Ballerina health tool documentation">
              <Box
                component="a"
                href="https://ballerina.io/learn/health-tool/"
                target="_blank"
                rel="noreferrer"
                sx={{ display: 'flex', color: 'text.secondary', mr: 1 }}
              >
                <BookOpen size={18} />
              </Box>
            </Tooltip>
            <ColorSchemeToggle />
          </Header.Actions>
        </Header>
      </AppShell.Navbar>

      <AppShell.Sidebar>
        <Sidebar activeItem={activeItemFor(location.pathname)}>
          <Sidebar.Nav>
            <Sidebar.Category>
              <Sidebar.Item id="packages" link={<NavigateLink to="/" />}>
                <Sidebar.ItemIcon>
                  <Boxes />
                </Sidebar.ItemIcon>
                <Sidebar.ItemLabel>FHIR packages</Sidebar.ItemLabel>
              </Sidebar.Item>
            </Sidebar.Category>
            <Sidebar.Category>
              <Sidebar.CategoryLabel>Your work</Sidebar.CategoryLabel>
              <Sidebar.Item id="projects" link={<NavigateLink to="/projects" />}>
                <Sidebar.ItemIcon>
                  <FolderOpen />
                </Sidebar.ItemIcon>
                <Sidebar.ItemLabel>My packages</Sidebar.ItemLabel>
              </Sidebar.Item>
            </Sidebar.Category>
          </Sidebar.Nav>

        </Sidebar>
      </AppShell.Sidebar>

      <AppShell.Main>
        <Outlet />
        <BuildWatcher />
      </AppShell.Main>

      <AppShell.Footer>
        <Footer>
          <Footer.Copyright>
            © {new Date().getFullYear()} |
            <Box sx={{ verticalAlign: 'middle', mt: 0.2, mx: 0.5, display: 'inline-block' }}>
              <WSO2 size={12} />
            </Box>
            WSO2 LLC.
          </Footer.Copyright>
        </Footer>
      </AppShell.Footer>
    </AppShell>
  );
}
