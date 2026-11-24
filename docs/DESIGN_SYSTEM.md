# Origin Agent design system — Satin (v0.0.4)

> The native app's identity from v0.0.4. Aurora remains the identity of the product website
> (`site/`), which is intentionally unchanged; the phone app now looks and feels different.
> The Constitution remains the source of truth for behavior.

## 1. Why a new look

The v0.0.3 Aurora interface was luminous but too declarative for daily work. The v0.0.4
native app borrows OpenDuet's restraint: neutral paper/graphite surfaces, one violet accent
reserved for attention, and four status colours reserved for status. The result is quieter,
faster to read, and clearly distinct from the marketing site.

Satin makes four ideas visible:

1. **Work, not decoration.** Chat, Tasks, Make and Channels are one continuous workspace.
2. **Truth is attributable.** The serving model and verification state stay on every answer.
3. **Status has one language.** Ready / Needs you / Working / Stopped use the same chips everywhere.
4. **Sovereignty is quiet.** Templates and memory are local drafts, never vague claims of AI output.

## 2. Palette

### Satin Night (default dark)
| Token | Value | Role |
|---|---|---|
| Canvas | `#111113` | Quiet graphite background |
| Surface | `#1C1C1F` | Cards, dialogs, composer |
| Bubble | `#26262B` | User turns and tonal tiles |
| Accent | `#B49AE8` | Active control only |
| Ink | `#F4F4F5` | Primary text |
| Ink 2 | `#A1A1AA` | Secondary text |
| Done / Needs / Working / Stopped | Green / amber / blue / red (dark variants) | Status only |

### Satin Day (light companion)
A warm-neutral `#F6F6F8` canvas, white surfaces and one violet `#6430AB` accent. Both
schemes are checked for WCAG AA on primary text surfaces (`AuroraContrastTest`, kept as the
compatibility test name). Material You dynamic color remains opt-in; the Satin status tokens
stay stable so meaning survives wallpaper-derived schemes.

## 3. Shape and depth

- Cards: 16 dp radius, hairline outline, zero elevation.
- Dialogs: 22 dp radius, same surface language as cards.
- Bottom dock: a floating frosted pill with four tabs and an overflow menu.
- Message turns: small paragraph bubbles separated by real whitespace, matching the short
  OpenDuet voice; user turns use a tonal surface, not a gradient.
- No ambient light wells. One subtle accent wash sits at the very top of the canvas.

## 4. Typography

- Display: system sans, semibold, tight but not extreme tracking.
- Titles: 19–22 sp semibold; body 15–17 sp with comfortable line height.
- Micro-labels: monospace, 11 sp, positive tracking. Used for section labels and capabilities.
- No remote fonts, no bundled font files.

## 5. Motion

- The mic orb uses a low-bouncy spring only while listening.
- Selection and content size changes use short platform tween/spring transitions.
- `Reduce motion` is one shared preference and disables the orb's pulse.
- Nothing animates merely because a screen is open; this matters on 4 GB devices.

## 6. Component inventory

| Component | Purpose |
|---|---|
| `DuetCanvas` / `DuetCard` | Quiet canvas and bordered surfaces |
| `DuetDock` | Chat / Tasks / Make / Channels with overflow menu |
| `DuetTopBar` / `SectionLabel` | Typographic hierarchy |
| `StatusChip` | One status language (ready, needs input, working, problem, neutral) |
| `DuetModelChip` / `ModelLibraryDialog` | Always-truthful active model control and Satin model library |
| `SuggestionPills` | Low-friction conversation starters |
| `OrbButton` / `VoiceSheet` | Opt-in platform dictation with honest unavailable states |
| `DuetTextField` / `DuetPrimaryButton` | Pill-shaped controls |
| Legacy `Aurora*` components | Still compile for secondary screens during transition |

## 7. OpenDuet-inspired workspace

- **Chat** opens with a truthful "today" brief and short, paragraph-per-thought replies.
- **Tasks** is a local board with Up next → Working → Ready for review → Resolved states.
- **Make** creates editable local drafts (design system, design brief, dashboard, slides,
  document, page) from explicit templates. Templates are labelled local drafts; no output is
  attributed to an AI model.
- **Channels** group tasks by purpose; selecting a channel sets the default for new tasks.
- Platform speech recognition powers the orb when available; permission is requested only
  after a tap, and failures say so instead of simulating transcription.

Full OpenDuet/OpenBot functional parity (server, browser computer, connectors, cloud memory,
scheduled routines) is **not** claimed. This is a native offline-first interpretation of its
workspace and voice, not a reimplementation of its infrastructure.

## 8. Accessibility and resource discipline

- Semantic labels on icon controls; status never relies on colour alone.
- Contrast is tested for light and dark palettes.
- All visuals are drawn with Compose primitives. No images, fonts, analytics, CDN requests
  or new dependencies were added.
- Local workspace writes run on `Dispatchers.IO`; inference and connection checks are off the
  main thread.

## 9. Invariants

- The UI never invents a model identity; the model chip and every answer render harness state.
- No local model is suggested unless its weights can be installed and verified in one tap;
  imports are labelled as user-owned and validated locally.
- `ATTEMPTED` is visually distinct from `VERIFIED`.
- A redesign may change tokens and layout, but never the eight-stage trust pipeline.
