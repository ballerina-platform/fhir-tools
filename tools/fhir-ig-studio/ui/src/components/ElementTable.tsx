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

import { Alert, Box, Tooltip, Typography } from '@wso2/oxygen-ui';

/**
 * Renders a StructureDefinition's element list the way published FHIR IG sites do: a tree of element names with
 * the flag glyphs (S, Σ, ?!, I, TU), cardinality, type, and the short description with its binding.
 *
 * The two views answer different questions and both matter, which is why the viewer offers each: the
 * differential is only what this guide changes, and the snapshot is the fully resolved element list including
 * everything inherited from the base resource.
 */

export interface FhirElement {
  id?: string;
  path?: string;
  sliceName?: string;
  min?: number;
  max?: string;
  short?: string;
  definition?: string;
  mustSupport?: boolean;
  isModifier?: boolean;
  isSummary?: boolean;
  constraint?: { key?: string; human?: string; severity?: string }[];
  type?: { code?: string; profile?: string[]; targetProfile?: string[] }[];
  binding?: { strength?: string; valueSet?: string; description?: string };
  slicing?: unknown;
  contentReference?: string;
}

/** The last segment of a dotted FHIR path, which is what IG tables show as the element name. */
function leafName(element: FhirElement): string {
  const path = element.path ?? '';
  const leaf = path.includes('.') ? path.slice(path.lastIndexOf('.') + 1) : path;
  return element.sliceName ? `${leaf}:${element.sliceName}` : leaf;
}

function depthOf(element: FhirElement): number {
  const path = element.path ?? '';
  return Math.max(0, path.split('.').length - 1);
}

/** "0..1" as IG tables print it; a missing bound is left blank rather than guessed. */
function cardinality(element: FhirElement): string {
  if (element.min === undefined && element.max === undefined) {
    return '';
  }
  return `${element.min ?? ''}..${element.max ?? ''}`;
}

/** The trailing segment of a canonical URL, so long profile URLs stay readable in the Type column. */
function shortUrl(url: string): string {
  const withoutVersion = url.split('|')[0];
  return withoutVersion.slice(withoutVersion.lastIndexOf('/') + 1);
}

function typeLabel(element: FhirElement): string {
  if (element.contentReference) {
    return `see ${element.contentReference.replace('#', '')}`;
  }
  if (!Array.isArray(element.type) || element.type.length === 0) {
    return '';
  }
  return element.type
    .map((type) => {
      const code = type.code ?? '';
      // Reference(Patient | Practitioner) is how IG sites render reference targets.
      if (Array.isArray(type.targetProfile) && type.targetProfile.length > 0) {
        return `${code}(${type.targetProfile.map(shortUrl).join(' | ')})`;
      }
      if (Array.isArray(type.profile) && type.profile.length > 0) {
        return `${code}(${type.profile.map(shortUrl).join(' | ')})`;
      }
      return code;
    })
    .filter(Boolean)
    .join(', ');
}

/** The flag glyphs used across published IGs, each with the meaning shown on hover. */
function flagsOf(element: FhirElement): { glyph: string; title: string }[] {
  const flags: { glyph: string; title: string }[] = [];
  if (element.mustSupport) {
    flags.push({ glyph: 'S', title: 'Must Support' });
  }
  if (element.isSummary) {
    flags.push({ glyph: 'Σ', title: 'Included in summary' });
  }
  if (element.isModifier) {
    flags.push({ glyph: '?!', title: 'Is a modifier element' });
  }
  if (Array.isArray(element.constraint) && element.constraint.length > 0) {
    flags.push({
      glyph: 'I',
      title: element.constraint
        .map((c) => `${c.key ?? ''}: ${c.human ?? ''}`)
        .join('\n'),
    });
  }
  return flags;
}

export default function ElementTable({
  elements,
  emptyMessage,
}: {
  elements: FhirElement[];
  emptyMessage: string;
}) {
  if (elements.length === 0) {
    return <Alert severity="info">{emptyMessage}</Alert>;
  }

  const headerSx = {
    p: 0.75,
    borderBottom: 1,
    borderColor: 'divider',
    fontSize: 12,
    fontWeight: 700,
    textAlign: 'left' as const,
    whiteSpace: 'nowrap' as const,
  };
  const cellSx = { p: 0.75, borderBottom: 1, borderColor: 'divider', fontSize: 12, verticalAlign: 'top' };

  return (
    <Box sx={{ overflowX: 'auto' }}>
      <Box component="table" sx={{ width: '100%', borderCollapse: 'collapse', minWidth: 720 }}>
        <Box component="thead">
          <Box component="tr">
            <Box component="th" sx={headerSx}>
              Name
            </Box>
            <Box component="th" sx={headerSx}>
              Flags
            </Box>
            <Box component="th" sx={headerSx}>
              Card.
            </Box>
            <Box component="th" sx={headerSx}>
              Type
            </Box>
            <Box component="th" sx={{ ...headerSx, whiteSpace: 'normal' }}>
              Description &amp; Constraints
            </Box>
          </Box>
        </Box>
        <Box component="tbody">
          {elements.map((element, index) => {
            const depth = depthOf(element);
            const flags = flagsOf(element);
            const isSlice = Boolean(element.sliceName);
            return (
              <Box
                component="tr"
                key={`${element.id ?? element.path ?? index}-${index}`}
                sx={{ '&:hover': { bgcolor: 'action.hover' } }}
              >
                <Box
                  component="td"
                  sx={{
                    ...cellSx,
                    // Indentation carries the tree structure, as in the published IG tables.
                    pl: 0.75 + depth * 1.6,
                    fontFamily: 'monospace',
                    whiteSpace: 'nowrap',
                    fontWeight: depth === 0 ? 700 : 400,
                    color: isSlice ? 'primary.main' : 'text.primary',
                  }}
                >
                  {depth > 0 && (
                    <Box component="span" sx={{ opacity: 0.35, mr: 0.5 }}>
                      {'·'.repeat(Math.min(depth, 6))}
                    </Box>
                  )}
                  {leafName(element)}
                </Box>
                <Box component="td" sx={{ ...cellSx, whiteSpace: 'nowrap' }}>
                  {flags.map((flag) => (
                    <Tooltip key={flag.glyph} title={flag.title}>
                      <Box
                        component="span"
                        sx={{
                          display: 'inline-block',
                          mr: 0.5,
                          px: 0.5,
                          borderRadius: 0.5,
                          bgcolor: 'action.selected',
                          fontSize: 11,
                          fontWeight: 700,
                          cursor: 'help',
                        }}
                      >
                        {flag.glyph}
                      </Box>
                    </Tooltip>
                  ))}
                </Box>
                <Box component="td" sx={{ ...cellSx, whiteSpace: 'nowrap', fontFamily: 'monospace' }}>
                  {cardinality(element)}
                </Box>
                <Box component="td" sx={{ ...cellSx, fontFamily: 'monospace' }}>
                  {typeLabel(element)}
                </Box>
                <Box component="td" sx={cellSx}>
                  <Typography variant="caption" sx={{ display: 'block' }}>
                    {element.short}
                  </Typography>
                  {element.binding?.valueSet && (
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.25 }}>
                      Binding: {shortUrl(element.binding.valueSet)}
                      {element.binding.strength ? ` (${element.binding.strength})` : ''}
                    </Typography>
                  )}
                </Box>
              </Box>
            );
          })}
        </Box>
      </Box>
    </Box>
  );
}
