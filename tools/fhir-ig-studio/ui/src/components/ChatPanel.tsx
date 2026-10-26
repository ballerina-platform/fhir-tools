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
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  IconButton,
  Paper,
  TextField,
  Typography,
} from '@wso2/oxygen-ui';
import { Check, Copy, Send, X } from '@wso2/oxygen-ui-icons-react';
import { useEffect, useRef, useState } from 'react';
import { api, ApiError } from '../api/client';
import type {
  ChatMessage,
  ChatProposal,
  ElementChanges,
  ModelStatus,
  ProfileElement,
  TaggedElement,
} from '../api/types';

/** How many suggestions the @ menu lists at once; typing more narrows it rather than scrolling a long list. */
const MAX_SUGGESTIONS = 40;

/** An element as it is written in the chat: its path, and its slice when it is one. */
function elementLabel(element: { path: string; sliceName: string | null }): string {
  return element.sliceName ? `${element.path}:${element.sliceName}` : element.path;
}

/**
 * The @ reference being typed at the caret, if there is one: an @ at the start of the text or after a space,
 * followed by no space up to the caret. An @ inside a word, like an email address, is not a reference.
 */
export function mentionAt(text: string, caret: number): { start: number; query: string } | null {
  const before = text.slice(0, caret);
  const at = before.lastIndexOf('@');
  if (at < 0 || (at > 0 && !/\s/.test(before[at - 1]))) {
    return null;
  }
  const query = before.slice(at + 1);
  return /\s/.test(query) ? null : { start: at, query };
}

/**
 * The elements an @ query suggests, best first.
 *
 * <p>The resource type that starts every path is optional, so "name.fam" finds Patient.name.family. Paths that
 * start with what was typed rank above ones that merely contain it, and within each group the profile's own
 * order is kept, so a parent always comes before its children.
 */
export function suggestElements(elements: ProfileElement[], query: string): ProfileElement[] {
  const q = query.toLowerCase();
  if (!q) {
    return elements.slice(0, MAX_SUGGESTIONS);
  }
  const starts: ProfileElement[] = [];
  const contains: ProfileElement[] = [];
  for (const element of elements) {
    const label = elementLabel(element).toLowerCase();
    const relative = label.slice(label.indexOf('.') + 1);
    if (label.startsWith(q) || relative.startsWith(q)) {
      starts.push(element);
    } else if (label.includes(q)) {
      contains.push(element);
    }
  }
  return [...starts, ...contains].slice(0, MAX_SUGGESTIONS);
}

/** The fields a proposal may change, in the order they read best. */
const FIELDS: { key: keyof ElementChanges; label: string }[] = [
  { key: 'min', label: 'Min' },
  { key: 'max', label: 'Max' },
  { key: 'mustSupport', label: 'Must support' },
  { key: 'types', label: 'Types' },
  { key: 'bindingStrength', label: 'Binding' },
  { key: 'bindingValueSet', label: 'Value set' },
  { key: 'short', label: 'Short' },
];

function show(value: unknown): string {
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  return Array.isArray(value) ? value.join(', ') : String(value);
}

/** One way of switching the chat on, as lines someone can copy rather than retype. */
function SetupBlock({ title, lines }: { title: string; lines: string[] }) {
  const [copied, setCopied] = useState(false);
  return (
    <Box sx={{ mb: 1.25 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        <Typography variant="caption" sx={{ fontWeight: 600, flex: 1 }}>
          {title}
        </Typography>
        <IconButton
          size="small"
          onClick={() => {
            void navigator.clipboard?.writeText(lines.join('\n')).then(
              () => {
                setCopied(true);
                setTimeout(() => setCopied(false), 1500);
              },
              () => undefined,
            );
          }}
        >
          {copied ? <Check size={13} /> : <Copy size={13} />}
        </IconButton>
      </Box>
      <Box
        component="pre"
        sx={{
          m: 0,
          px: 1,
          py: 0.75,
          borderRadius: 1,
          bgcolor: 'action.hover',
          border: 1,
          borderColor: 'divider',
          fontSize: 10.5,
          overflowX: 'auto',
          whiteSpace: 'pre',
        }}
      >
        {lines.join('\n')}
      </Box>
    </Box>
  );
}

/** How one proposal is getting on, held by the panel so that applying several can drive them all. */
interface ProposalState {
  status: 'idle' | 'applying' | 'applied' | 'error';
  error?: string;
}

/** A change the model drafted, shown as what it would do, with nothing applied until asked. */
function Proposal({
  proposal,
  state,
  onApply,
  onDismiss,
}: {
  proposal: ChatProposal;
  state: ProposalState;
  onApply: () => void;
  onDismiss: () => void;
}) {
  const isExtension = proposal.kind === 'extension';
  const rows = FIELDS.filter((field) => proposal.changes?.[field.key] !== undefined);

  return (
    <Paper variant="outlined" sx={{ p: 1.5, mt: 1, borderColor: 'primary.main' }}>
      <Typography variant="caption" sx={{ fontFamily: 'monospace', fontWeight: 700 }}>
        {proposal.path}
        {proposal.sliceName ? `:${proposal.sliceName}` : ''}
      </Typography>

      {isExtension && (
        <Box sx={{ mt: 0.75, mb: 1 }}>
          <Typography variant="caption" sx={{ display: 'block' }}>
            {proposal.extension ? 'Author extension' : 'Bind extension'}{' '}
            <Box component="span" sx={{ fontWeight: 700 }}>
              {proposal.extension?.title || proposal.extension?.id || proposal.extensionUrl}
            </Box>
            {proposal.extension?.valueTypes?.length
              ? ` carrying ${proposal.extension.valueTypes.join(' | ')}`
              : ''}
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
            then slice it onto <code>{proposal.path}.extension</code> as{' '}
            <code>{proposal.sliceName}</code> ({proposal.min ?? 0}..{proposal.max ?? '1'})
          </Typography>
        </Box>
      )}

      <Box sx={{ mt: 0.75, mb: 1 }}>
        {rows.map((field) => (
          <Box key={String(field.key)} sx={{ display: 'flex', gap: 1, alignItems: 'baseline' }}>
            <Typography variant="caption" color="text.secondary" sx={{ width: 84, flexShrink: 0 }}>
              {field.label}
            </Typography>
            <Typography variant="caption" sx={{ fontFamily: 'monospace' }}>
              {show(proposal.current?.[field.key as keyof NonNullable<ChatProposal['current']>])}
              {' \u2192 '}
              <Box component="span" sx={{ fontWeight: 700, color: 'primary.main' }}>
                {show(proposal.changes?.[field.key])}
              </Box>
            </Typography>
          </Box>
        ))}
      </Box>

      {proposal.reason && (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
          {proposal.reason}
        </Typography>
      )}

      {state.error && (
        <Alert severity="error" sx={{ mb: 1 }}>
          <Typography variant="caption">{state.error}</Typography>
        </Alert>
      )}

      {state.status === 'applied' ? (
        <Typography variant="caption" color="success.main">
          Applied.
        </Typography>
      ) : (
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            size="small"
            variant="contained"
            disabled={state.status === 'applying'}
            startIcon={
              state.status === 'applying' ? <CircularProgress size={12} color="inherit" /> : undefined
            }
            onClick={onApply}
          >
            {state.status === 'error' ? 'Retry' : 'Apply'}
          </Button>
          <Button size="small" onClick={onDismiss}>
            Dismiss
          </Button>
        </Box>
      )}
    </Paper>
  );
}

/**
 * A conversation about the open package, answered by a model the reader runs themselves.
 *
 * <p>It proposes rather than edits. A drafted change is shown as the values it would replace and the ones it
 * would set, and is written only when Apply is pressed -- at which point it goes through exactly the same
 * profiling-rule validation as a change made by hand. That keeps a model that has misread a profile from
 * quietly corrupting one, and means the reader always sees a change before it happens.
 */
export default function ChatPanel({
  workspaceId,
  file,
  elements,
  tagged,
  onTag,
  onUntag,
  onApplied,
  onClose,
}: {
  workspaceId: string;
  file: string | null;
  /** Every element of the open profile, offered when the reader types @ to reference one. */
  elements: ProfileElement[];
  /** Elements picked in the table; the model is told a change applies to these and no others. */
  tagged: TaggedElement[];
  /** Picks an element to talk about, as Edit with Dev Bot in the table does. */
  onTag: (path: string, sliceName: string | null) => void;
  onUntag: (index: number) => void;
  /**
   * Refreshes the tables after a proposal is applied, and re-points the tag at whatever was just changed --
   * a follow-up almost always means the thing that was created, not the element it hangs off.
   */
  onApplied: (affected: TaggedElement) => void;
  onClose: () => void;
}) {
  const [status, setStatus] = useState<ModelStatus | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [proposals, setProposals] = useState<Record<number, ChatProposal[]>>({});
  /** Replies that declined an out-of-scope question, shown muted so they read as a boundary, not an answer. */
  const [declined, setDeclined] = useState<Set<number>>(new Set());
  const [draft, setDraft] = useState('');
  const [thinking, setThinking] = useState(false);
  /** What has been applied in this conversation, so "make it mandatory" has a referent. Session only. */
  const [applied, setApplied] = useState<{ summary: string }[]>([]);
  /** How each proposal is getting on, keyed by the turn it came from and its place in that turn. */
  const [states, setStates] = useState<Record<string, ProposalState>>({});

  const stateOf = (key: string): ProposalState => states[key] ?? { status: 'idle' };

  /** What a proposal describes, in a sentence, so a later question can refer back to it. */
  const summarise = (proposal: ChatProposal) =>
    proposal.kind === 'extension'
      ? `Added extension slice '${proposal.sliceName}' on ${proposal.path}.extension`
      : `Changed ${proposal.path}${proposal.sliceName ? `:${proposal.sliceName}` : ''} — ${JSON.stringify(proposal.changes)}`;

  /**
   * Writes one proposal, and reports back what it touched.
   *
   * <p>Does not refresh the tables: applying several at once would otherwise re-fetch and re-render a large
   * element table once per change, so the caller refreshes when the whole set is done.
   */
  const writeProposal = async (proposal: ChatProposal): Promise<TaggedElement> => {
    if (proposal.kind === 'extension') {
      // The extension has to exist before an element can be sliced to it, so author it first and use the url
      // that comes back rather than one the model guessed at.
      let url = proposal.extensionUrl;
      if (proposal.extension) {
        const created = await api.createExtension(workspaceId, {
          ...proposal.extension,
          contexts: proposal.extension.contexts?.length ? proposal.extension.contexts : [proposal.path],
        });
        url = created.url ?? url;
      }
      await api.addSlice(workspaceId, proposal.file, {
        path: `${proposal.path}.extension`,
        sliceName: proposal.sliceName ?? 'extension',
        min: proposal.min ?? 0,
        max: proposal.max ?? '1',
        mustSupport: proposal.mustSupport ?? false,
        extensionUrl: url,
        description: proposal.reason,
      });
      return { path: `${proposal.path}.extension`, sliceName: proposal.sliceName ?? null };
    }
    await api.applyElementChanges(
      workspaceId,
      proposal.file,
      proposal.path,
      proposal.sliceName ?? null,
      proposal.changes ?? {},
    );
    return { path: proposal.path, sliceName: proposal.sliceName ?? null };
  };

  /**
   * Applies a set of proposals in the order they were drafted.
   *
   * <p>One at a time rather than together: they are changes to one definition, and each is validated against
   * what the one before it left behind. One that the rules refuse does not stop the others -- it is reported
   * on its own card and the rest still go through, which is what someone who asked for four changes and can
   * have three would want.
   */
  const applySet = async (keys: string[], chosen: ChatProposal[]) => {
    setStates((current) => {
      const next = { ...current };
      keys.forEach((key) => {
        next[key] = { status: 'applying' };
      });
      return next;
    });
    let last: TaggedElement | null = null;
    let anyApplied = false;
    for (let i = 0; i < chosen.length; i++) {
      try {
        last = await writeProposal(chosen[i]);
        anyApplied = true;
        setApplied((current) => [...current, { summary: summarise(chosen[i]) }]);
        setStates((current) => ({ ...current, [keys[i]]: { status: 'applied' } }));
      } catch (e) {
        setStates((current) => ({
          ...current,
          [keys[i]]: { status: 'error', error: e instanceof ApiError ? e.message : String(e) },
        }));
      }
    }
    // One refresh for the whole set, and the tag follows whatever was changed last.
    if (anyApplied && last) {
      onApplied(last);
    }
  };
  const [error, setError] = useState<string | null>(null);
  const [lookups, setLookups] = useState<string[]>([]);
  const endRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLTextAreaElement | null>(null);
  /** The @ reference being typed, while the suggestion menu is open. */
  const [mention, setMention] = useState<{ start: number; query: string } | null>(null);
  const [highlight, setHighlight] = useState(0);
  const suggestions = mention ? suggestElements(elements, mention.query) : [];
  const menuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    // Keeps the highlighted suggestion in view as the arrow keys move through a list taller than the menu.
    const row = menuRef.current?.querySelector<HTMLElement>(`[data-index="${highlight}"]`);
    if (typeof row?.scrollIntoView === 'function') {
      row.scrollIntoView({ block: 'nearest' });
    }
  }, [highlight]);

  /** Re-reads the @ reference from the caret, so moving it or typing past a space closes the menu. */
  const trackMention = (text: string, caret: number | null) => {
    const next = caret === null ? null : mentionAt(text, caret);
    setMention(next);
    if (next?.query !== mention?.query) {
      setHighlight(0);
    }
  };

  /**
   * Swaps the typed @ reference for the element's path and tags the element, so the message reads naturally
   * and the model is told, as with Edit with Dev Bot, that a change applies to it.
   */
  const pickSuggestion = (element: ProfileElement) => {
    if (!mention) {
      return;
    }
    const label = elementLabel(element);
    const end = mention.start + 1 + mention.query.length;
    const next = `${draft.slice(0, mention.start)}${label} ${draft.slice(end)}`;
    const caret = mention.start + label.length + 1;
    setDraft(next);
    setMention(null);
    onTag(element.path, element.sliceName);
    requestAnimationFrame(() => {
      inputRef.current?.focus();
      inputRef.current?.setSelectionRange(caret, caret);
    });
  };

  useEffect(() => {
    api.getModelStatus().then(setStatus).catch(() => setStatus(null));
  }, []);

  useEffect(() => {
    // Guarded: an environment without scrollIntoView would otherwise throw here and take the panel down with
    // it, turning a cosmetic scroll into a blank pane.
    const end = endRef.current;
    if (typeof end?.scrollIntoView === 'function') {
      end.scrollIntoView({ block: 'end' });
    }
  }, [messages, thinking]);

  const send = async () => {
    const question = draft.trim();
    if (!question || thinking) {
      return;
    }
    const history: ChatMessage[] = [...messages, { role: 'user', content: question }];
    setMessages(history);
    setDraft('');
    setThinking(true);
    setError(null);
    try {
      const answer = await api.chat(workspaceId, file, history, tagged, applied);
      setLookups((answer.lookups ?? []).map((l) => l.what));
      setMessages((current) => {
        const next = [...current, { role: 'assistant' as const, content: answer.reply }];
        if (answer.proposals.length > 0) {
          setProposals((p) => ({ ...p, [next.length - 1]: answer.proposals }));
        }
        if (answer.outOfScope) {
          setDeclined((d) => new Set(d).add(next.length - 1));
        }
        return next;
      });
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setThinking(false);
    }
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', minWidth: 0 }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1,
          px: 1.5,
          py: 1,
          borderBottom: 1,
          borderColor: 'divider',
        }}
      >
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
            FHIR Dev Bot
          </Typography>
        </Box>
        <IconButton size="small" onClick={onClose}>
          <X size={16} />
        </IconButton>
      </Box>

      {status && status.configured === false && (
        <Box sx={{ p: 1.5, overflowY: 'auto' }}>
          <Alert severity="info" sx={{ mb: 1.5 }}>
            <Typography variant="caption">
              The chat is an optional extra and is not switched on. Follow the instructions below to enable it.
            </Typography>
          </Alert>
          <Typography variant="caption" sx={{ fontWeight: 700, display: 'block', mb: 0.5 }}>
            To enable it
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.25 }}>
            Point the service at a chat API and restart it. Any OpenAI-compatible API works as it is; name a
            provider only for Azure or Anthropic.
          </Typography>
          <SetupBlock
            title="A model on this machine"
            lines={[
              'brew install ollama && brew services start ollama',
              'ollama pull qwen2.5-coder:7b',
              'export FHIR_TOOL_SERVICE_MODEL_URL=http://localhost:11434/v1',
              'export FHIR_TOOL_SERVICE_MODEL=qwen2.5-coder:7b',
            ]}
          />
          <SetupBlock
            title="OpenAI, or anything that imitates it"
            lines={[
              'export FHIR_TOOL_SERVICE_MODEL_URL=https://api.openai.com/v1',
              'export FHIR_TOOL_SERVICE_MODEL=gpt-4o-mini',
              'export FHIR_TOOL_SERVICE_MODEL_API_KEY=sk-...',
            ]}
          />
          <SetupBlock
            title="Anthropic"
            lines={[
              'export FHIR_TOOL_SERVICE_MODEL_PROVIDER=anthropic',
              'export FHIR_TOOL_SERVICE_MODEL_URL=https://api.anthropic.com/v1',
              'export FHIR_TOOL_SERVICE_MODEL=claude-sonnet-4-5',
              'export FHIR_TOOL_SERVICE_MODEL_API_KEY=sk-ant-...',
            ]}
          />
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
            A hosted provider is sent the open profile and the package's definition names, so a model on this
            machine is the better choice for an unpublished guide.
          </Typography>
        </Box>
      )}

      {status && status.configured !== false && !status.available && (
        <Alert severity="warning" sx={{ m: 1.5 }}>
          <Typography variant="caption">
            {status.detail ?? `No model is answering at ${status.url}.`}{' '}
            {/* A hosted API and a local one fail for different reasons, so they get different advice. */}
            {/^https?:\/\/(localhost|127\.0\.0\.1|\[::1\])/.test(status.url ?? '') ? (
              <>
                Start one — for example <code>ollama serve</code> with{' '}
                <code>ollama pull {status.model}</code>.
              </>
            ) : (
              <>
                Check <code>FHIR_TOOL_SERVICE_MODEL_URL</code>
                {status.authenticated === false && (
                  <>
                    {' '}and set <code>FHIR_TOOL_SERVICE_MODEL_API_KEY</code>
                  </>
                )}
                .
              </>
            )}
          </Typography>
        </Alert>
      )}

      {status?.configured !== false && (
      <Box sx={{ flex: 1, overflowY: 'auto', p: 1.5 }}>
        {messages.length === 0 && (
          <Typography variant="caption" color="text.secondary">
            Pick elements with <strong>Edit with Dev Bot</strong>, or type <strong>@</strong> to reference one,
            and say what should change — every change
            comes back as a proposal you apply, never as an edit already made. Questions about the profile are
            answered here too.
          </Typography>
        )}
        {messages.map((message, index) => (
          <Box key={index} sx={{ mb: 1.5 }}>
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ fontWeight: 700, display: 'block', mb: 0.25 }}
            >
              {message.role === 'user' ? 'You' : 'Model'}
            </Typography>
            <Typography
              variant="body2"
              color={declined.has(index) ? 'text.secondary' : undefined}
              sx={{ whiteSpace: 'pre-wrap', fontStyle: declined.has(index) ? 'italic' : undefined }}
            >
              {message.content}
            </Typography>
            {(proposals[index] ?? []).length > 1 && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
                <Button
                  size="small"
                  variant="contained"
                  disabled={(proposals[index] ?? []).every(
                    (_, n) => stateOf(`${index}:${n}`).status === 'applied',
                  )}
                  onClick={() => {
                    const pending = (proposals[index] ?? [])
                      .map((proposal, n) => ({ proposal, key: `${index}:${n}` }))
                      .filter(({ key }) => stateOf(key).status !== 'applied');
                    void applySet(
                      pending.map(({ key }) => key),
                      pending.map(({ proposal }) => proposal),
                    );
                  }}
                >
                  Apply all{' '}
                  {
                    (proposals[index] ?? []).filter((_, n) => stateOf(`${index}:${n}`).status !== 'applied')
                      .length
                  }{' '}
                  changes
                </Button>
                <Typography variant="caption" color="text.secondary">
                  applied in order; each is validated on its own
                </Typography>
              </Box>
            )}
            {(proposals[index] ?? []).map((proposal, n) => (
              <Proposal
                key={n}
                proposal={proposal}
                state={stateOf(`${index}:${n}`)}
                onApply={() => void applySet([`${index}:${n}`], [proposal])}
                onDismiss={() =>
                  setProposals((current) => ({
                    ...current,
                    [index]: (current[index] ?? []).filter((_, i) => i !== n),
                  }))
                }
              />
            ))}
          </Box>
        ))}
        {lookups.length > 0 && (
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
            Looked up: {lookups.join('; ')}
          </Typography>
        )}
        {thinking && (
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            <CircularProgress size={14} />
            <Typography variant="caption" color="text.secondary">
              Thinking…
            </Typography>
          </Box>
        )}
        {error && (
          <Alert severity="error" sx={{ mt: 1 }}>
            <Typography variant="caption">{error}</Typography>
          </Alert>
        )}
        <div ref={endRef} />
      </Box>
      )}

      {status?.configured !== false && tagged.length > 0 && (
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, px: 1.5, pt: 1.5 }}>
          {tagged.map((element, index) => (
            <Chip
              key={`${element.path}:${element.sliceName ?? ''}`}
              size="small"
              label={element.sliceName ? `${element.path}:${element.sliceName}` : element.path}
              onDelete={() => onUntag(index)}
              sx={{ maxWidth: '100%', '& .MuiChip-label': { fontFamily: 'monospace', fontSize: 11 } }}
            />
          ))}
        </Box>
      )}

      {status?.configured !== false && (
      <Box sx={{ position: 'relative', display: 'flex', gap: 1, p: 1.5, borderTop: 1, borderColor: 'divider' }}>
        {mention && (
          <Paper
            ref={menuRef}
            elevation={6}
            role="listbox"
            aria-label="Elements of this profile"
            sx={{
              position: 'absolute',
              left: 12,
              right: 12,
              bottom: '100%',
              mb: 0.5,
              maxHeight: 260,
              overflowY: 'auto',
              zIndex: 1,
              py: 0.5,
            }}
          >
            {suggestions.length === 0 ? (
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', px: 1.5, py: 0.75 }}>
                {elements.length === 0
                  ? 'Open a profile to reference its elements.'
                  : `No element matches "${mention.query}".`}
              </Typography>
            ) : (
              suggestions.map((element, index) => (
                <Box
                  key={element.id ?? elementLabel(element)}
                  data-index={index}
                  role="option"
                  aria-selected={index === highlight}
                  // Picking with the mouse must not take focus from the input, or the caret is lost.
                  onMouseDown={(event) => event.preventDefault()}
                  onMouseEnter={() => setHighlight(index)}
                  onClick={() => pickSuggestion(element)}
                  sx={{
                    px: 1.5,
                    py: 0.5,
                    cursor: 'pointer',
                    bgcolor: index === highlight ? 'action.selected' : undefined,
                  }}
                >
                  <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1 }}>
                    <Typography
                      variant="caption"
                      sx={{
                        fontFamily: 'monospace',
                        fontWeight: element.constrained ? 700 : 400,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        flex: 1,
                        minWidth: 0,
                      }}
                    >
                      {elementLabel(element)}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ flexShrink: 0, fontSize: 10.5 }}>
                      {element.current.min ?? ''}..{element.current.max ?? ''}{' '}
                      {/* Primitive ids are typed by FHIRPath url; the name after the last / is what reads. */}
                      {element.current.types.map((type) => type.slice(type.lastIndexOf('/') + 1)).join(' | ')}
                    </Typography>
                  </Box>
                  {element.current.short && (
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      sx={{
                        display: 'block',
                        fontSize: 10.5,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {element.current.short}
                    </Typography>
                  )}
                </Box>
              ))
            )}
          </Paper>
        )}
        <TextField
          fullWidth
          size="small"
          multiline
          maxRows={4}
          inputRef={inputRef}
          placeholder={
            tagged.length > 0
              ? 'What should change about these? Type @ to add an element'
              : 'Ask about this profile, or type @ to reference an element'
          }
          value={draft}
          onChange={(event) => {
            setDraft(event.target.value);
            trackMention(event.target.value, event.target.selectionStart);
          }}
          onSelect={(event) => {
            const target = event.target as HTMLTextAreaElement;
            trackMention(target.value, target.selectionStart);
          }}
          onBlur={() => setMention(null)}
          onKeyDown={(event) => {
            if (mention) {
              if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                event.preventDefault();
                if (suggestions.length > 0) {
                  const step = event.key === 'ArrowDown' ? 1 : -1;
                  setHighlight((current) => (current + step + suggestions.length) % suggestions.length);
                }
                return;
              }
              if ((event.key === 'Enter' || event.key === 'Tab') && suggestions.length > 0) {
                event.preventDefault();
                pickSuggestion(suggestions[Math.min(highlight, suggestions.length - 1)]);
                return;
              }
              if (event.key === 'Escape') {
                event.preventDefault();
                // Stops the drawer around the panel from closing on the same key.
                event.stopPropagation();
                setMention(null);
                return;
              }
            }
            if (event.key === 'Enter' && !event.shiftKey) {
              event.preventDefault();
              void send();
            }
          }}
        />
        <IconButton color="primary" disabled={thinking || !draft.trim()} onClick={() => void send()}>
          <Send size={18} />
        </IconButton>
      </Box>
      )}
    </Box>
  );
}
