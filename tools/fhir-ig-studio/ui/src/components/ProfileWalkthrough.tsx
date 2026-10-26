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
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Tab,
  Tabs,
  Typography,
} from '@wso2/oxygen-ui';
import { ChevronLeft, ChevronRight, MousePointer2, Pause, Play } from '@wso2/oxygen-ui-icons-react';
import { useEffect, useState } from 'react';

/**
 * A played-through demonstration of the two things you can author here.
 *
 * Reading that an editor opens under a row is not the same as watching it happen, and the two flows this tool
 * supports are each several steps long with the interesting part -- what the profiling rules will and will not
 * let you do -- only visible once you are inside. So the stage is a small, honest mock of the real explorer and
 * each beat moves it: the pointer travels, the filter fills, the editor slides open, a slice appears.
 *
 * It is a mock rather than a recording so it stays true when the real UI is restyled, costs no assets, and can
 * be stepped through at the reader's own pace. Nothing here talks to the service.
 */

interface Beat {
  title: string;
  caption: string;
  /** Where the pointer rests, as a percentage of the stage. */
  pointer?: { x: number; y: number };
  /** Draws a click ripple when the pointer arrives. */
  click?: boolean;
  /** Which mock tree rows are on screen; all of them when absent. */
  rows?: number[];
  /** Index into TREE of the row the editor belongs to; -1 means the authored slice row. */
  active?: number;
  filter?: string;
  editor?: boolean;
  min?: string;
  mustSupport?: boolean;
  /** The extension panel inside the editor. */
  slice?: 'picker' | null;
  /** The new-extension dialog, drawn over everything. */
  dialog?: boolean;
  /** The authored extension has landed in the tree as a slice. */
  added?: boolean;
  /** Marks the element as changed, the way an edited row is marked. */
  applied?: boolean;
  /** The Dev Bot panel, slid in from the right as it is on the page. */
  chat?: ChatBeat;
  /** The Dev Bot's proposals have been applied: both rows it changed are marked and show their new cardinality. */
  botApplied?: boolean;
  /** The control that pulses to say "this is the one". */
  glow?: 'apply' | 'addExtension' | 'new' | 'create' | 'generate' | 'devbot' | 'askBot' | 'mention' | 'send'
    | 'applyAll' | null;
}

/** What the mock Dev Bot panel is showing. */
interface ChatBeat {
  /** Elements picked to talk about, as chips above the input. */
  tags?: string[];
  /** Text in the input. */
  draft?: string;
  /** The @ menu over the input, first entry highlighted. */
  mention?: string[];
  /** The question once sent, which brings the reply and its proposals with it. */
  sent?: string;
  /** The model's answer to it. */
  reply?: string;
  /** One card per proposed change: the element, and each field's value before and after. */
  cards?: { path: string; changes: [field: string, from: string, to: string][] }[];
  /** Whether the proposals in the reply are still waiting or have been applied. */
  proposals?: 'pending' | 'applied';
}

const PROFILES = ['AUBaseAllergyIntolerance', 'AUBasePatient', 'AUBaseCondition'];

const TREE = [
  { name: 'AllergyIntolerance', depth: 0, card: '', type: '' },
  { name: 'identifier', depth: 1, card: '0..*', type: 'Identifier' },
  { name: 'clinicalStatus', depth: 1, card: '0..1', type: 'CodeableConcept' },
  { name: 'coding', depth: 2, card: '0..*', type: 'Coding' },
  { name: 'code', depth: 3, card: '0..1', type: 'code' },
  { name: 'verificationStatus', depth: 1, card: '0..1', type: 'CodeableConcept' },
  { name: 'patient', depth: 1, card: '1..1', type: 'Reference' },
];

/** The row an authored extension adds, slotted under the element it was bound to. */
const SLICE_ROW = { name: 'extension:severity-note', depth: 2, card: '0..1', type: 'Extension' };

/** The two elements the Dev Bot steps work on, as the chat writes them. */
const CLINICAL = 'AllergyIntolerance.clinicalStatus';
const VERIFICATION = 'AllergyIntolerance.verificationStatus';
const QUESTION = 'Make these mandatory';
const BOTH_REQUIRED = {
  reply: 'Both can be required: their base minimum is 0.',
  cards: [
    { path: CLINICAL, changes: [['Min', '0', '1']] as [string, string, string][] },
    { path: VERIFICATION, changes: [['Min', '0', '1']] as [string, string, string][] },
  ],
};

const EXTEND: Beat[] = [
  {
    title: 'Open the profile you want to extend',
    caption:
      'Every definition the package ships is listed on the left. You always start from a published profile and '
      + 'narrow it — there is no blank file to fill in, because a profile only means something against the base '
      + 'definition it derives from.',
    pointer: { x: 13, y: 27 },
    click: true,
  },
  {
    title: 'Find the element',
    caption:
      'Typing in the filter cuts several hundred elements down to the one you want. It reads the whole row, so a '
      + 'name, a path, a type, a flag or a word from the description all find their element.',
    filter: 'clinicalStatus',
    rows: [0, 2, 3, 4],
    pointer: { x: 34, y: 12 },
  },
  {
    title: 'Open its editor',
    caption:
      'Click the row. The editor opens in place, directly beneath it, so the tree never scrolls away from under '
      + 'you and you keep the context of what you are constraining.',
    filter: 'clinicalStatus',
    rows: [0, 2, 3, 4],
    active: 2,
    editor: true,
    pointer: { x: 40, y: 34 },
    click: true,
  },
  {
    title: 'Tighten the constraint',
    caption:
      'Only what the profiling rules permit is offered. Cardinality can narrow but never widen, types can be '
      + 'dropped from the base set but not added, and a binding can only be strengthened — each field shows the '
      + 'bound it is held to.',
    filter: 'clinicalStatus',
    rows: [0, 2, 3, 4],
    active: 2,
    editor: true,
    min: '1',
    mustSupport: true,
    pointer: { x: 41, y: 55 },
    click: true,
  },
  {
    title: 'Apply it',
    caption:
      'Apply constraint writes the change into the differential and revalidates the whole definition against the '
      + 'base spec. Anything the rules reject comes back explained instead of being saved.',
    filter: 'clinicalStatus',
    rows: [0, 2, 3, 4],
    active: 2,
    editor: true,
    min: '1',
    mustSupport: true,
    applied: true,
    glow: 'apply',
    pointer: { x: 36, y: 78 },
    click: true,
  },
  {
    title: 'Or ask the Dev Bot to make it',
    caption:
      'The same constraint can be described instead of set. Edit with Dev Bot hands the element to the chat; say '
      + 'what you want in your own words and it comes back as a proposal to Apply, checked by the same rules. The '
      + 'Edit with the Dev Bot tab walks through it step by step.',
    filter: 'clinicalStatus',
    rows: [0, 2, 3, 4],
    active: 2,
    editor: true,
    glow: 'askBot',
    chat: {
      tags: [CLINICAL],
      sent: 'Make it mandatory and must support',
      reply: 'clinicalStatus can be required: its base minimum is 0.',
      cards: [{ path: CLINICAL, changes: [['Min', '0', '1'], ['Must support', 'false', 'true']] }],
      proposals: 'pending',
    },
    pointer: { x: 46, y: 53 },
    click: true,
  },
  {
    title: 'Generate the facade to try it',
    caption:
      'The edited package becomes a Ballerina FHIR facade carrying your profiles as modules, so the IG can be run '
      + 'against rather than only read.',
    applied: true,
    glow: 'generate',
    pointer: { x: 88, y: 6 },
    click: true,
  },
];

const EXTENSION: Beat[] = [
  {
    title: 'Pick the element to carry it',
    caption:
      'Any element can take an extension — a backbone element, one nested inside an expanded datatype, even a '
      + 'primitive, since every FHIR element has an extension list of its own.',
    active: 2,
    editor: true,
    pointer: { x: 40, y: 34 },
    click: true,
  },
  {
    title: 'Add extension',
    caption:
      'At the foot of the editor, Add extension slices the element’s extension list for you, with the url '
      + 'discriminator and the slicing rules already set the way FHIR requires.',
    active: 2,
    editor: true,
    // The picker is what the *next* beat shows: opening it here would take away the button being pointed at.
    glow: 'addExtension',
    pointer: { x: 30, y: 72 },
    click: true,
  },
  {
    title: 'Bind one, or author a new one',
    caption:
      'The picker lists every extension in the package with its id and canonical URL, searchable, and sorts the '
      + 'ones whose context fits this element to the top. If none of them fits, New builds one without leaving '
      + 'the row.',
    active: 2,
    editor: true,
    slice: 'picker',
    glow: 'new',
    pointer: { x: 62, y: 66 },
    click: true,
  },
  {
    title: 'Describe the new extension',
    caption:
      'Only what actually distinguishes one extension from another is asked for: an id, where it may be used, and '
      + 'what it carries. The kind, the base definition, the fixed url and the 0..0 that makes it simple rather '
      + 'than nested are all boilerplate, so the service writes them.',
    active: 2,
    editor: true,
    slice: 'picker',
    dialog: true,
    glow: 'create',
    pointer: { x: 66, y: 79 },
    click: true,
  },
  {
    title: 'It lands as a slice',
    caption:
      'The extension is written into the package as its own StructureDefinition and bound to the element as a '
      + 'named slice. From here it is an ordinary row: constrain it like any other.',
    added: true,
    active: -1,
    applied: true,
    pointer: { x: 42, y: 40 },
  },
];

const DEVBOT: Beat[] = [
  {
    title: 'Open the Dev Bot',
    caption:
      'The Dev Bot answers questions about the open profile and drafts changes to it from a description in your '
      + 'own words. It runs on a model you choose, which can be one on your own machine.',
    glow: 'devbot',
    chat: {},
    pointer: { x: 80, y: 5 },
    click: true,
  },
  {
    title: 'Point it at an element',
    caption:
      'Edit with Dev Bot, at the foot of any row’s editor, hands that element to the chat. The bot is told a '
      + 'change applies to the elements you picked and to no others, so it cannot wander off across the profile.',
    active: 2,
    editor: true,
    glow: 'askBot',
    chat: { tags: [CLINICAL] },
    pointer: { x: 46, y: 60 },
    click: true,
  },
  {
    title: 'Or reference one with @',
    caption:
      'Typing @ in the chat lists the profile’s elements, narrowing as you type; the resource type is optional, '
      + 'so "verif" is enough. Picking one tags it just the same.',
    glow: 'mention',
    chat: { tags: [CLINICAL], draft: `${QUESTION} @verif`, mention: [VERIFICATION, `${VERIFICATION}.coding`] },
    pointer: { x: 78, y: 80 },
    click: true,
  },
  {
    title: 'Say what should change',
    caption:
      'Describe the change rather than the edit: here, both elements become mandatory. Questions work too — ask '
      + 'what an element is for, or what a profile already constrains.',
    glow: 'send',
    chat: { tags: [CLINICAL, VERIFICATION], draft: `${QUESTION} ${VERIFICATION}` },
    pointer: { x: 95, y: 93 },
    click: true,
  },
  {
    title: 'Review what it proposes',
    caption:
      'Nothing has changed yet. Each change comes back as a card showing the value it would replace and the one '
      + 'it would set, so a model that has misread the profile is caught before anything is written.',
    glow: 'applyAll',
    chat: { tags: [CLINICAL, VERIFICATION], sent: `${QUESTION} ${VERIFICATION}`, ...BOTH_REQUIRED, proposals: 'pending' },
    pointer: { x: 64, y: 39 },
    click: true,
  },
  {
    title: 'Apply, and it is checked like any edit',
    caption:
      'Apply writes the changes through exactly the same profiling-rule validation as an edit made by hand. A '
      + 'change the rules refuse is explained on its own card, and the rest still go through.',
    botApplied: true,
    chat: { tags: [CLINICAL, VERIFICATION], sent: `${QUESTION} ${VERIFICATION}`, ...BOTH_REQUIRED, proposals: 'applied' },
    pointer: { x: 28, y: 37 },
  },
];

const TRACKS = [
  { label: 'Extend a profile', beats: EXTEND },
  { label: 'Edit with the Dev Bot', beats: DEVBOT },
  { label: 'Create an extension', beats: EXTENSION },
];

const HOLD_MS = 5200;

export default function ProfileWalkthrough({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [track, setTrack] = useState(0);
  const [step, setStep] = useState(0);
  const [playing, setPlaying] = useState(true);

  const beats = TRACKS[track].beats;
  const beat = beats[step];
  const last = step === beats.length - 1;

  useEffect(() => {
    if (open) {
      setTrack(0);
      setStep(0);
      setPlaying(true);
    }
  }, [open]);

  useEffect(() => {
    if (!open || !playing || last) {
      return undefined;
    }
    const timer = setTimeout(() => setStep((current) => current + 1), HOLD_MS);
    return () => clearTimeout(timer);
  }, [open, playing, step, last]);

  const rows = beat.rows ?? TREE.map((_, index) => index);
  // The authored slice sits directly under the element it was bound to.
  const shown = beat.added ? [...rows.slice(0, 3), -1, ...rows.slice(3)] : rows;

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle component="div" sx={{ pb: 0 }}>
        How to author with this package
        <Tabs
          value={track}
          onChange={(_, value) => {
            setTrack(value as number);
            setStep(0);
            setPlaying(true);
          }}
          sx={{ mt: 1 }}
        >
          {TRACKS.map((entry) => (
            <Tab key={entry.label} label={entry.label} />
          ))}
        </Tabs>
      </DialogTitle>

      <DialogContent>
        <Stage beat={beat} shown={shown} stepKey={`${track}:${step}`} />

        <Box sx={{ mt: 2, minHeight: 104 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
            {step + 1}. {beat.title}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            {beat.caption}
          </Typography>
        </Box>
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2, gap: 1 }}>
        <Box sx={{ display: 'flex', gap: 0.75, flex: 1 }}>
          {beats.map((entry, index) => (
            <Box
              key={entry.title}
              onClick={() => {
                setStep(index);
                setPlaying(false);
              }}
              title={entry.title}
              sx={{
                width: index === step ? 22 : 8,
                height: 8,
                borderRadius: 4,
                cursor: 'pointer',
                transition: 'width .3s, background-color .3s',
                bgcolor: index === step ? 'primary.main' : 'action.disabled',
              }}
            />
          ))}
        </Box>
        <IconButton size="small" disabled={step === 0} onClick={() => { setStep(step - 1); setPlaying(false); }}>
          <ChevronLeft size={18} />
        </IconButton>
        <IconButton
          size="small"
          title={playing ? 'Pause' : 'Play'}
          onClick={() => {
            if (last) {
              setStep(0);
            }
            setPlaying(!playing || last);
          }}
        >
          {playing && !last ? <Pause size={18} /> : <Play size={18} />}
        </IconButton>
        <IconButton size="small" disabled={last} onClick={() => { setStep(step + 1); setPlaying(false); }}>
          <ChevronRight size={18} />
        </IconButton>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

/** The mock explorer the beats move. Everything is always mounted so state changes can be transitioned. */
function Stage({ beat, shown, stepKey }: { beat: Beat; shown: number[]; stepKey: string }) {
  const glow = (name: NonNullable<Beat['glow']>) =>
    beat.glow === name
      ? { boxShadow: '0 0 0 3px rgba(25,118,210,0.35)', borderColor: 'primary.main' }
      : {};

  return (
    <Box
      sx={{
        position: 'relative',
        height: 360,
        border: 1,
        borderColor: 'divider',
        borderRadius: 1,
        overflow: 'hidden',
        bgcolor: 'background.paper',
        fontSize: 11,
        userSelect: 'none',
      }}
    >
      {/* Page header, with the action the last beat points at. */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          px: 1.5,
          py: 1,
          borderBottom: 1,
          borderColor: 'divider',
          bgcolor: 'action.hover',
        }}
      >
        <Typography sx={{ fontSize: 11, fontWeight: 700, flex: 1 }}>AU Base Implementation Guide</Typography>
        <Pill label="Dev Bot" sx={{ mr: 0.75, ...glow('devbot') }} />
        <Box
          sx={{
            px: 1,
            py: 0.4,
            border: 1,
            borderColor: 'divider',
            borderRadius: 0.75,
            bgcolor: 'primary.main',
            color: 'primary.contrastText',
            fontSize: 10,
            transition: 'box-shadow .3s',
            ...glow('generate'),
          }}
        >
          Generate FHIR facade
        </Box>
      </Box>

      <Box sx={{ display: 'flex', height: 'calc(100% - 33px)' }}>
        {/* Definition list. */}
        <Box sx={{ width: 150, borderRight: 1, borderColor: 'divider', p: 0.75, flexShrink: 0 }}>
          {PROFILES.map((name, index) => (
            <Box
              key={name}
              sx={{
                px: 0.75,
                py: 0.6,
                mb: 0.25,
                borderRadius: 0.5,
                fontSize: 10,
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                transition: 'background-color .4s',
                bgcolor: index === 0 ? 'primary.main' : 'transparent',
                color: index === 0 ? 'primary.contrastText' : 'text.secondary',
              }}
            >
              {name}
            </Box>
          ))}
        </Box>

        {/* Element table. */}
        <Box sx={{ flex: 1, p: 0.75, minWidth: 0 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, mb: 0.75 }}>
            <Box
              sx={{
                width: 130,
                px: 0.75,
                py: 0.5,
                border: 1,
                borderColor: beat.filter ? 'primary.main' : 'divider',
                borderRadius: 0.75,
                fontSize: 10,
                color: beat.filter ? 'text.primary' : 'text.disabled',
                transition: 'border-color .3s',
              }}
            >
              {beat.filter || 'Filter elements'}
            </Box>
            {['Expand all', 'Collapse all', 'Reset'].map((label) => (
              <Box
                key={label}
                sx={{ px: 0.75, py: 0.5, border: 1, borderColor: 'divider', borderRadius: 0.75, fontSize: 10 }}
              >
                {label}
              </Box>
            ))}
          </Box>

          {shown.map((rowIndex, position) => {
            const row = rowIndex === -1 ? SLICE_ROW : TREE[rowIndex];
            const isActive = beat.active !== undefined && rowIndex === beat.active;
            // The Dev Bot track changes two rows, clinicalStatus and verificationStatus, rather than one.
            const botChanged = beat.botApplied && (rowIndex === 2 || rowIndex === 5);
            const changed = (beat.applied && (rowIndex === 2 || rowIndex === -1)) || botChanged;
            return (
              <Box key={`${rowIndex}-${position}`}>
                <Box
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1,
                    px: 0.5,
                    py: 0.45,
                    borderRadius: 0.5,
                    transition: 'background-color .4s',
                    bgcolor: isActive ? 'rgba(25,118,210,0.12)' : 'transparent',
                  }}
                >
                  <Box sx={{ width: row.depth * 14, flexShrink: 0 }} />
                  <Box
                    sx={{
                      borderLeft: row.depth ? 1 : 0,
                      borderBottom: row.depth ? 1 : 0,
                      borderColor: 'divider',
                      width: 8,
                      height: 8,
                      flexShrink: 0,
                    }}
                  />
                  <Typography
                    sx={{
                      fontSize: 10,
                      fontFamily: 'monospace',
                      fontWeight: rowIndex === -1 ? 700 : 400,
                      color: rowIndex === -1 ? 'secondary.main' : 'text.primary',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {row.name}
                  </Typography>
                  {changed && (
                    <Box sx={{ width: 6, height: 6, borderRadius: 3, bgcolor: 'warning.main', flexShrink: 0 }} />
                  )}
                  <Box sx={{ flex: 1 }} />
                  <Typography sx={{ fontSize: 10, color: 'text.secondary', width: 34 }}>
                    {(rowIndex === 2 && beat.min === '1') || botChanged ? '1..1' : row.card}
                  </Typography>
                  <Typography sx={{ fontSize: 10, color: 'text.secondary', width: 104, whiteSpace: 'nowrap' }}>
                    {row.type}
                  </Typography>
                </Box>

                {/* The editor, opening under its own row. */}
                {isActive && (
                  <Box
                    sx={{
                      overflow: 'hidden',
                      transition: 'max-height .45s ease, opacity .35s ease',
                      maxHeight: beat.editor ? 200 : 0,
                      opacity: beat.editor ? 1 : 0,
                      ml: 2,
                      borderLeft: 3,
                      borderColor: 'primary.main',
                      bgcolor: 'action.hover',
                      px: 1,
                      py: beat.editor ? 0.75 : 0,
                      mt: beat.editor ? 0.5 : 0,
                    }}
                  >
                    <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mb: 0.75 }}>
                      <Field label="Min" value={beat.min ?? '0'} hint="≥ 0" changed={beat.min === '1'} />
                      <Field label="Max" value="1" hint="≤ 1" />
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                        <Box
                          sx={{
                            width: 22,
                            height: 12,
                            borderRadius: 6,
                            p: '2px',
                            display: 'flex',
                            justifyContent: beat.mustSupport ? 'flex-end' : 'flex-start',
                            bgcolor: beat.mustSupport ? 'primary.main' : 'action.disabled',
                            transition: 'background-color .3s',
                          }}
                        >
                          <Box sx={{ width: 8, height: 8, borderRadius: 4, bgcolor: '#fff' }} />
                        </Box>
                        <Typography sx={{ fontSize: 10 }}>Must support</Typography>
                      </Box>
                    </Box>

                    {beat.slice !== 'picker' ? (
                      <Box sx={{ display: 'flex', gap: 0.75 }}>
                        <Pill label="+ Add extension" sx={glow('addExtension')} />
                        <Pill label="+ Add slice" />
                        <Pill label="Edit with Dev Bot" sx={glow('askBot')} />
                      </Box>
                    ) : (
                      <Box sx={{ display: 'flex', gap: 0.75, alignItems: 'center' }}>
                        <Box
                          sx={{
                            flex: 1,
                            px: 0.75,
                            py: 0.5,
                            border: 1,
                            borderColor: 'divider',
                            borderRadius: 0.75,
                            fontSize: 10,
                            color: 'text.disabled',
                          }}
                        >
                          Search extensions…
                        </Box>
                        <Pill label="New" sx={glow('new')} />
                      </Box>
                    )}

                    <Box sx={{ mt: 0.75 }}>
                      <Pill label="Apply constraint" filled sx={glow('apply')} />
                    </Box>
                  </Box>
                )}
              </Box>
            );
          })}
        </Box>
      </Box>

      <ChatMock beat={beat} glow={glow} />

      {/* The new-extension dialog, over the stage. */}
      <Box
        sx={{
          position: 'absolute',
          inset: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          bgcolor: 'rgba(0,0,0,0.28)',
          transition: 'opacity .35s ease',
          opacity: beat.dialog ? 1 : 0,
          pointerEvents: 'none',
        }}
      >
        <Box
          sx={{
            width: 250,
            p: 1.25,
            borderRadius: 1,
            bgcolor: 'background.paper',
            boxShadow: 6,
            transform: beat.dialog ? 'scale(1)' : 'scale(.94)',
            transition: 'transform .35s ease',
          }}
        >
          <Typography sx={{ fontSize: 11, fontWeight: 700, mb: 0.75 }}>New extension</Typography>
          {[
            ['Id', 'severity-note'],
            ['Used on', 'AllergyIntolerance.clinicalStatus'],
            ['Value type', 'string'],
          ].map(([label, value]) => (
            <Box key={label} sx={{ mb: 0.6 }}>
              <Typography sx={{ fontSize: 8, color: 'text.secondary' }}>{label}</Typography>
              <Box
                sx={{
                  px: 0.6,
                  py: 0.35,
                  border: 1,
                  borderColor: 'divider',
                  borderRadius: 0.5,
                  fontSize: 9,
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }}
              >
                {value}
              </Box>
            </Box>
          ))}
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 0.75 }}>
            <Pill label="Create extension" filled sx={glow('create')} />
          </Box>
        </Box>
      </Box>

      {/* The pointer, travelling between beats. */}
      {beat.pointer && (
        <Box
          sx={{
            position: 'absolute',
            left: `${beat.pointer.x}%`,
            top: `${beat.pointer.y}%`,
            zIndex: 30,
            pointerEvents: 'none',
            color: 'text.primary',
            transition: 'left .6s cubic-bezier(.4,0,.2,1), top .6s cubic-bezier(.4,0,.2,1)',
          }}
        >
          <MousePointer2 size={16} fill="currentColor" />
          {beat.click && (
            <Box
              key={stepKey}
              sx={{
                position: 'absolute',
                left: -9,
                top: -9,
                width: 26,
                height: 26,
                borderRadius: '50%',
                border: 2,
                borderColor: 'primary.main',
                animation: 'wt-ripple 1.1s ease-out .45s',
                opacity: 0,
                '@keyframes wt-ripple': {
                  '0%': { transform: 'scale(.3)', opacity: 0.9 },
                  '100%': { transform: 'scale(1.5)', opacity: 0 },
                },
              }}
            />
          )}
        </Box>
      )}
    </Box>
  );
}

function Field({
  label,
  value,
  hint,
  changed,
}: {
  label: string;
  value: string;
  hint: string;
  changed?: boolean;
}) {
  return (
    <Box>
      <Typography sx={{ fontSize: 8, color: 'text.secondary' }}>{label}</Typography>
      <Box
        sx={{
          width: 34,
          px: 0.5,
          py: 0.3,
          border: 1,
          borderRadius: 0.5,
          fontSize: 10,
          textAlign: 'center',
          transition: 'border-color .3s, background-color .3s',
          borderColor: changed ? 'primary.main' : 'divider',
          bgcolor: changed ? 'rgba(25,118,210,0.12)' : 'transparent',
        }}
      >
        {value}
      </Box>
      <Typography sx={{ fontSize: 8, color: 'text.disabled' }}>{hint}</Typography>
    </Box>
  );
}

function Pill({ label, filled, sx }: { label: string; filled?: boolean; sx?: object }) {
  return (
    <Box
      sx={{
        display: 'inline-block',
        px: 0.75,
        py: 0.4,
        border: 1,
        borderColor: 'divider',
        borderRadius: 0.75,
        fontSize: 10,
        transition: 'box-shadow .3s, border-color .3s',
        ...(filled ? { bgcolor: 'primary.main', color: 'primary.contrastText', borderColor: 'primary.main' } : {}),
        ...sx,
      }}
    >
      {label}
    </Box>
  );
}

/** The Dev Bot panel: picked elements, the @ menu, the conversation and its proposals. */
function ChatMock({
  beat,
  glow,
}: {
  beat: Beat;
  glow: (name: NonNullable<Beat['glow']>) => object;
}) {
  const chat = beat.chat;
  const applied = chat?.proposals === 'applied';
  const cards = chat?.cards ?? [];
  return (
    <Box
      sx={{
        position: 'absolute',
        top: 33,
        right: 0,
        bottom: 0,
        width: '42%',
        zIndex: 10,
        display: 'flex',
        flexDirection: 'column',
        borderLeft: 1,
        borderColor: 'divider',
        // Solid: the theme's paper is translucent, and the table would show through.
        bgcolor: 'background.default',
        boxShadow: 6,
        transform: chat ? 'translateX(0)' : 'translateX(105%)',
        transition: 'transform .45s ease',
      }}
    >
      <Typography sx={{ fontSize: 10, fontWeight: 700, px: 1, py: 0.75, borderBottom: 1, borderColor: 'divider' }}>
        FHIR Dev Bot
      </Typography>

      <Box sx={{ flex: 1, p: 1, overflow: 'hidden' }}>
        {!chat?.sent ? (
          <Typography sx={{ fontSize: 9, color: 'text.secondary' }}>
            Pick elements with Edit with Dev Bot, or type @ to reference one, and say what should change.
          </Typography>
        ) : (
          <>
            <Typography sx={{ fontSize: 8, fontWeight: 700, color: 'text.secondary' }}>You</Typography>
            <Typography sx={{ fontSize: 9, mb: 0.75 }}>{chat.sent}</Typography>
            <Typography sx={{ fontSize: 8, fontWeight: 700, color: 'text.secondary' }}>Model</Typography>
            <Typography sx={{ fontSize: 9, mb: 0.5 }}>{chat.reply}</Typography>
            {!applied && cards.length > 1 && (
              <Box sx={{ mb: 0.5 }}>
                <Pill label={`Apply all ${cards.length} changes`} filled sx={glow('applyAll')} />
              </Box>
            )}
            {cards.map((card) => (
              <Box
                key={card.path}
                sx={{ border: 1, borderColor: 'primary.main', borderRadius: 0.75, px: 0.75, py: 0.5, mb: 0.5 }}
              >
                <Typography sx={{ fontSize: 8.5, fontFamily: 'monospace', fontWeight: 700 }}>{card.path}</Typography>
                {card.changes.map(([field, from, to]) => (
                  <Typography key={field} sx={{ fontSize: 8.5, fontFamily: 'monospace' }}>
                    {field} {from} {'\u2192'}{' '}
                    <Box component="span" sx={{ fontWeight: 700, color: 'primary.main' }}>
                      {to}
                    </Box>
                  </Typography>
                ))}
                {applied ? (
                  <Typography sx={{ fontSize: 8.5, color: 'success.main' }}>Applied.</Typography>
                ) : cards.length === 1 ? (
                  // A lone proposal is applied from its own card; there is no Apply all to point at.
                  <Box sx={{ mt: 0.4 }}>
                    <Pill label="Apply" filled sx={glow('applyAll')} />
                  </Box>
                ) : (
                  <Typography sx={{ fontSize: 8.5, color: 'text.secondary' }}>Apply · Dismiss</Typography>
                )}
              </Box>
            ))}
          </>
        )}
      </Box>

      {(chat?.tags?.length ?? 0) > 0 && (
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.4, px: 1, pb: 0.5 }}>
          {chat?.tags?.map((tag) => (
            <Box
              key={tag}
              sx={{ px: 0.6, py: 0.2, borderRadius: 2, bgcolor: 'action.selected', fontSize: 8, fontFamily: 'monospace' }}
            >
              {tag}
            </Box>
          ))}
        </Box>
      )}

      <Box sx={{ position: 'relative', display: 'flex', gap: 0.5, alignItems: 'center', p: 0.75, borderTop: 1, borderColor: 'divider' }}>
        {chat?.mention && (
          <Box
            sx={{
              position: 'absolute',
              left: 6,
              right: 6,
              bottom: '100%',
              mb: 0.25,
              bgcolor: 'background.default',
              boxShadow: 6,
              borderRadius: 0.75,
              py: 0.25,
            }}
          >
            {chat.mention.map((path, index) => (
              <Typography
                key={path}
                sx={{
                  fontSize: 8.5,
                  fontFamily: 'monospace',
                  px: 0.75,
                  py: 0.35,
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  bgcolor: index === 0 ? 'action.selected' : 'transparent',
                  ...(index === 0 ? glow('mention') : {}),
                }}
              >
                {path}
              </Typography>
            ))}
          </Box>
        )}
        <Box
          sx={{
            flex: 1,
            px: 0.75,
            py: 0.5,
            border: 1,
            borderColor: chat?.draft ? 'primary.main' : 'divider',
            borderRadius: 0.75,
            fontSize: 9,
            color: chat?.draft ? 'text.primary' : 'text.disabled',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
          }}
        >
          {chat?.draft || 'Ask, or type @ to reference an element'}
        </Box>
        <Pill label="Send" filled sx={glow('send')} />
      </Box>
    </Box>
  );
}
