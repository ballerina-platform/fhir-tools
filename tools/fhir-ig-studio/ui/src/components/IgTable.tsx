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

import { Box, Button, TextField, Typography } from '@wso2/oxygen-ui';
import { CirclePlay } from '@wso2/oxygen-ui-icons-react';
import { Fragment, useEffect, useMemo, useRef, useState } from 'react';
import type { CreatedExtension, DefinitionSummary, NewExtension, ProfileElement } from '../api/types';
import ElementConstraintForm from './ElementConstraintForm';
import ProfileWalkthrough from './ProfileWalkthrough';

/**
 * Displays the element table exactly as published FHIR IG sites do, with a collapsible tree and constraint
 * editing inline between the rows.
 *
 * The markup is produced by HL7's own renderer on the service, the same code the IG publisher runs, and arrives
 * complete: inline styles, the `hierarchy` classes, and the tree connector and type icons already base64-inlined.
 *
 * It is not handed to React as one blob. Each source row is rendered as its own `tbody` (several are valid in
 * one table), which keeps every row's markup byte-for-byte while letting React own the sequence of rows. That is
 * what makes a real editor row possible between two rows: earlier attempts injected one into the DOM or
 * portalled into a node inside a single `dangerouslySetInnerHTML` subtree, and React rebuilt that subtree on
 * update, detaching whatever had been put there.
 *
 * The HTML is generated server-side from the package's own definitions rather than being user-authored, and the
 * renderer emits escaped text for every value it takes from them.
 */

/**
 * How far the fold gutter pushes a row's contents right. Both the connector images and the cell's background
 * strip have to move by this much, or the tree's lines come apart.
 */
const GUTTER = 12;

interface ParsedRow {
  /** The row's own markup, with a fold toggle added when it has children. */
  html: string;
  /** Nesting level; -1 for the header row, which is not part of the tree. */
  depth: number;
  /** "Patient.name" or "Patient.extension:race", or null when the row is not an element. */
  key: string | null;
  /** Everything the row says, lower-cased: name, card, type, flags and description. What the filter reads. */
  text: string;
  cells: number;
}

/** Exported so the row/element mapping can be checked without a browser. */
export function parseRows(html: string): ParsedRow[] {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const sourceRows = Array.from(doc.querySelectorAll('tr'));
  const ancestors: string[] = [];

  const rows: ParsedRow[] = sourceRows.map((row) => {
    const firstCell = row.querySelector('td');
    if (!firstCell) {
      return { html: row.outerHTML, depth: -1, key: null, text: '', cells: row.children.length };
    }
    // The renderer draws one connector image per level plus the row's own type icon, so the root sits at 2.
    const depth = Math.max(0, firstCell.querySelectorAll('img').length - 2);
    // Read the name before anything is added to the cell, so a toggle can never end up inside the path.
    const label = (firstCell.textContent ?? '').trim();
    const segment = label.split(':')[0];
    ancestors[depth] = segment;
    ancestors.length = depth + 1;
    const path = ancestors.filter(Boolean).join('.');
    const sliceName = label.includes(':') ? label.slice(label.indexOf(':') + 1) : '';
    return {
      html: row.outerHTML,
      depth,
      key: sliceName ? `${path}:${sliceName}` : path,
      text: (row.textContent ?? '').toLowerCase(),
      cells: row.children.length,
    };
  });

  // A row has children when the row after it sits deeper, which is only knowable once every depth is in hand.
  return rows.map((row, index) => {
    if (row.depth < 0) {
      return row;
    }
    const hasChildren = (rows[index + 1]?.depth ?? -1) > row.depth;
    const holder = doc.createElement('tbody');
    holder.innerHTML = row.html;
    const cell = holder.querySelector('td');
    if (cell) {
      // The gutter goes on every row, not only the ones that fold. Putting it only on parents pushed their
      // names 12px right of their own siblings, which is plain to see now that expanding datatypes has made
      // most complex elements parents.
      const toggle = doc.createElement('span');
      toggle.setAttribute(
        'style',
        `user-select:none;display:inline-block;width:${GUTTER}px;opacity:.65${hasChildren ? ';cursor:pointer' : ''}`,
      );
      if (hasChildren) {
        // The arrow itself is drawn in CSS from the row's open state. Writing it in here would freeze it at
        // whatever it was when the markup was parsed, and rows now start folded.
        toggle.setAttribute('data-fold', String(index));
      }
      cell.insertBefore(toggle, cell.firstChild);
      // The connector glyphs are only half the tree: the runs that join one row to the next are painted by a
      // tiled background strip on the cell itself. The gutter moves the glyphs but not the strip, so without
      // the matching shift the vertical lines stop meeting the junctions they belong to.
      const painted = cell.getAttribute('style') ?? '';
      if (painted.includes('background-image')) {
        cell.setAttribute('style', `${painted};background-position:${GUTTER}px 0`);
      }
    }
    return { ...row, html: holder.innerHTML };
  });
}

export default function IgTable({
  html,
  constrained,
  editing,
}: {
  html: string;
  /**
   * Keys ("Patient.name", "Patient.extension:race") of the elements this profile constrains, i.e. its
   * differential. The tree opens far enough to show every one of them; everything else starts folded.
   */
  constrained?: string[];
  /** Supplied to make rows editable in place; without it the table is read-only. */
  editing?: {
    workspaceId: string;
    file: string;
    elements: ProfileElement[];
    strengths: string[];
    /** Extensions in the package, for binding when slicing an extension element. */
    extensions: DefinitionSummary[];
    onApplied: () => void;
    /** Refreshes the package's definitions after one is authored from inside the editor. */
    onExtensionCreated: (created: CreatedExtension, request: NewExtension) => void;
    /** Hands an element to the Dev Bot, so a change it drafts applies there and nowhere else. */
    onAskAbout?: (path: string, sliceName: string | null) => void;
  };
}) {
  const rows = useMemo(() => parseRows(html), [html]);

  /** Rows with something under them: the only ones a fold applies to. */
  const foldable = useMemo(
    () =>
      rows
        .map((row, index) => ({ row, index }))
        .filter(({ row, index }) => row.depth >= 0 && (rows[index + 1]?.depth ?? -1) > row.depth)
        .map(({ index }) => index),
    [rows],
  );

  /**
   * A base resource has hundreds of elements and a profile usually touches a handful, so opening the whole tree
   * buries the point of the page. The table starts at the first element level with everything below it folded,
   * as published IG sites do, and then opens back up along the path to each constrained element so what this
   * profile actually changes is on screen without a click.
   */
  const defaultCollapsed = useMemo(() => {
    const keepOpen = new Set<string>();
    (constrained ?? []).forEach((key) => {
      const parts = key.split(':')[0].split('.');
      for (let i = 1; i < parts.length; i++) {
        keepOpen.add(parts.slice(0, i).join('.'));
      }
    });
    return new Set(
      foldable.filter((index) => {
        const row = rows[index];
        return row.depth >= 1 && !(row.key && keepOpen.has(row.key.split(':')[0]));
      }),
    );
  }, [rows, foldable, constrained]);

  const foldableSet = useMemo(() => new Set(foldable), [foldable]);

  const [collapsed, setCollapsed] = useState<Set<number>>(defaultCollapsed);
  /**
   * Which element has its editor open, held by path rather than by row number.
   *
   * <p>Row numbers move: a refreshed table can gain a slice or lose a fold, and the editor then belongs to a
   * different element than the one that was clicked. Worse, the whole table is rebuilt whenever its markup is
   * re-fetched, which happens in the background -- so an editor keyed by position was being closed moments
   * after it opened, and a click would appear to do nothing at all.
   */
  const [editingKey, setEditingKey] = useState<string | null>(null);
  const [filter, setFilter] = useState('');
  const [walkthrough, setWalkthrough] = useState(false);
  const query = filter.trim().toLowerCase();

  /**
   * While a filter is on, the fold state steps aside: the tree shows the rows that match and the ancestors that
   * place them, so a match is never orphaned from the path it sits on. Matching reads the whole row rather than
   * the name alone, so "must support", a type, or a word from the description all find their element.
   */
  const matches = useMemo(() => {
    if (!query) {
      return null;
    }
    const hit = rows.map(
      (row) => row.depth >= 0 && ((row.key ?? '').toLowerCase().includes(query) || row.text.includes(query)),
    );
    const keep = [...hit];
    for (let i = rows.length - 1; i >= 0; i--) {
      if (!keep[i]) {
        continue;
      }
      let depth = rows[i].depth;
      for (let j = i - 1; j >= 0 && depth > 0; j--) {
        if (rows[j].depth >= 0 && rows[j].depth < depth) {
          keep[j] = true;
          depth = rows[j].depth;
        }
      }
    }
    return { hit, keep, count: hit.filter(Boolean).length };
  }, [rows, query]);

  const firstHit = matches ? matches.hit.indexOf(true) : -1;
  const firstHitRef = useRef<HTMLTableSectionElement | null>(null);
  useEffect(() => {
    const row = firstHitRef.current;
    if (firstHit >= 0 && typeof row?.scrollIntoView === 'function') {
      row.scrollIntoView({ block: 'center' });
    }
  }, [firstHit]);

  useEffect(() => {
    setCollapsed(defaultCollapsed);
    setFilter('');
    // The open editor is deliberately left alone. Its element is still there after a refresh -- that is the
    // whole point of keying it by path -- and closing it on every background re-fetch is what made clicking a
    // row feel unreliable, and made an applied change look as though it had reset the form.
  }, [defaultCollapsed]);

  /** Hidden when any ancestor is collapsed, so folding a parent folds everything under it. */
  const hidden = useMemo(() => {
    if (matches) {
      return rows.map((row, index) => row.depth >= 0 && !matches.keep[index]);
    }
    const result = new Array(rows.length).fill(false);
    collapsed.forEach((start) => {
      for (let i = start + 1; i < rows.length && rows[i].depth > rows[start].depth; i++) {
        result[i] = true;
      }
    });
    return result;
  }, [rows, collapsed, matches]);

  const elementFor = (index: number): ProfileElement | null => {
    const key = rows[index]?.key;
    if (!editing || !key) {
      return null;
    }
    return (
      editing.elements.find(
        (element) => (element.sliceName ? `${element.path}:${element.sliceName}` : element.path) === key,
      ) ?? null
    );
  };

  const toggleFold = (index: number) =>
    setCollapsed((current) => {
      const next = new Set(current);
      if (next.has(index)) {
        next.delete(index);
      } else {
        next.add(index);
      }
      return next;
    });

  const columns = Math.max(...rows.map((row) => row.cells), 1);

  return (
    <Box
      sx={(theme) => ({
        overflowX: 'auto',
        // Matches the IG publisher's table styling; everything else travels with the row markup.
        '& table': { borderCollapse: 'collapse', fontFamily: 'Verdana, sans-serif', fontSize: 11 },
        '& td, & th': { verticalAlign: 'top', padding: '0px 4px' },
        '& tr.hierarchy, & td.hierarchy, & th.hierarchy': { borderColor: 'divider' },
        '& img': { verticalAlign: 'top', border: 0 },
        // Anchors are inert: nothing is served behind them, so they should not look clickable.
        '& a': { color: 'inherit', textDecoration: 'none', cursor: 'default', pointerEvents: 'none' },
        // The fold arrow lives in the row markup but its direction belongs to React's state, so it is drawn
        // from the tbody's open flag rather than written into the cell.
        '& [data-fold]': { textAlign: 'center' },
        '& tbody[data-open="1"] [data-fold]::after': { content: '"\u25be"' },
        '& tbody[data-open="0"] [data-fold]::after': { content: '"\u25b8"' },
        // HL7's markup paints every row white or #F7F7F7 inline but leaves the text colour to the page, so in
        // dark mode the theme's light text landed on white. Its backgrounds give way to the theme's instead
        // (inline styles only yield to !important), and the striping is kept as a faint tint.
        ...theme.applyStyles('dark', {
          '& [style*="background-color: white"]': { backgroundColor: 'transparent !important' },
          '& [style*="#F7F7F7"]': { backgroundColor: 'rgba(255, 255, 255, 0.04) !important' },
          // Its #F0F0F0 rules are near-white too, and would draw a bright line under the header.
          // theme.vars is the live CSS variable; theme.palette holds the light value when colour schemes switch.
          '& [style*="#F0F0F0"]': { borderColor: `${(theme.vars ?? theme).palette.divider} !important` },
          // The tree connectors are dark lines drawn for a white page; inverted, they stay visible. Only those:
          // the element and type icons carry a title and are coloured, so they read on either background.
          '& img.hierarchy:not([title])': { filter: 'invert(1)' },
        }),
      })}
    >
      <ProfileWalkthrough open={walkthrough} onClose={() => setWalkthrough(false)} />

      <Box sx={{ display: 'flex', gap: 1, mb: 1, alignItems: 'center', flexWrap: 'wrap' }}>
        <TextField
          size="small"
          value={filter}
          onChange={(event) => setFilter(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Escape') {
              setFilter('');
            }
          }}
          placeholder="Filter elements"
          title="Matches the name, path, type, flags or description. Esc clears."
          sx={{ width: 260 }}
        />
        {matches && (
          <Typography variant="caption" color={matches.count ? 'text.secondary' : 'error'}>
            {matches.count
              ? `${matches.count} of ${rows.filter((row) => row.depth >= 0).length} elements`
              : 'No element matches'}
          </Typography>
        )}
        {matches && (
          <Button size="small" onClick={() => setFilter('')}>
            Clear
          </Button>
        )}
        {editing && !matches && (
          <Button
            size="small"
            startIcon={<CirclePlay size={14} />}
            title="Walk through extending a profile and authoring an extension"
            onClick={() => setWalkthrough(true)}
          >
            How to
          </Button>
        )}
        {foldable.length > 0 && !matches && (
          <>
            <Button size="small" variant="outlined" onClick={() => setCollapsed(new Set())}>
              Expand all
            </Button>
            <Button size="small" variant="outlined" onClick={() => setCollapsed(new Set(foldable))}>
              Collapse all
            </Button>
            {defaultCollapsed.size > 0 && (
              <Button
                size="small"
                variant="outlined"
                title="The first element level, opened out to whatever this profile constrains"
                onClick={() => setCollapsed(defaultCollapsed)}
              >
                Reset
              </Button>
            )}
          </>
        )}
      </Box>

      <table>
        {rows.map((row, index) => {
          const element = elementFor(index);
          const open = editingKey !== null && row.key === editingKey;
          return (
            <Fragment key={index}>
              <tbody
                // The row markup is HL7's, untouched apart from the fold toggle; React only owns the sequence.
                dangerouslySetInnerHTML={{ __html: row.html }}
                data-open={foldableSet.has(index) ? (collapsed.has(index) ? '0' : '1') : undefined}
                ref={index === firstHit ? firstHitRef : undefined}
                style={{
                  display: hidden[index] ? 'none' : '',
                  cursor: element ? 'pointer' : undefined,
                  background: open
                    ? 'rgba(25,118,210,0.10)'
                    : matches?.hit[index]
                      ? 'rgba(255,193,7,0.22)'
                      : undefined,
                }}
                onClick={(event) => {
                  const fold = (event.target as HTMLElement | null)?.closest('[data-fold]');
                  if (fold) {
                    toggleFold(Number(fold.getAttribute('data-fold')));
                    return;
                  }
                  if (element) {
                    setEditingKey((current) => (current === row.key ? null : row.key));
                  }
                }}
              />
              {open && element && editing && (
                <tbody>
                  <tr>
                    <td colSpan={columns} style={{ padding: 0 }}>
                      <Box
                        sx={{
                          m: '4px 0 8px',
                          p: 2,
                          borderLeft: 3,
                          borderColor: 'primary.main',
                          bgcolor: 'action.hover',
                          fontFamily: (theme) => theme.typography.fontFamily,
                          fontSize: 14,
                        }}
                      >
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                          <Typography variant="subtitle2" sx={{ fontFamily: 'monospace', flex: 1 }}>
                            {element.path}
                            {element.sliceName ? `:${element.sliceName}` : ''}
                          </Typography>
                          <Button size="small" onClick={() => setEditingKey(null)}>
                            Close
                          </Button>
                        </Box>
                        <ElementConstraintForm
                          workspaceId={editing.workspaceId}
                          file={editing.file}
                          element={element}
                          strengths={editing.strengths}
                          extensions={editing.extensions}
                          onApplied={editing.onApplied}
                          onExtensionCreated={editing.onExtensionCreated}
                          onAskAbout={editing.onAskAbout}
                        />
                      </Box>
                    </td>
                  </tr>
                </tbody>
              )}
            </Fragment>
          );
        })}
      </table>
    </Box>
  );
}
