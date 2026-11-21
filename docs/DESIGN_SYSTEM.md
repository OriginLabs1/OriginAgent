# Origin Agent design system — Aurora

> Introduced in v0.0.3. Aurora is the visual language of Origin Agent; the Constitution
> remains the source of truth for behavior.

## 1. Why a new look

The v0.0.2 interface was correct but generic: a conventional Material screen with a chip
and a list. Origin Agent is not a generic chat client. It is a sovereignty-first
intelligence environment, and its interface should communicate that before a word is typed.

Aurora makes four ideas visible:

1. **Conversation is the center.** A calm, deep canvas with one bright action — asking.
2. **Truth is attributable.** The serving model and verification state are always in view.
3. **Power is translucent, not hidden.** Glass cards expose provenance, tools and consent
   without a separate “developer mode”.
4. **Sovereignty is quiet.** No neon dashboard theater, no novelty that costs battery or
   attention on a 4 GB device.

## 2. Palette

### Aurora Night (default dark)
| Token | Value | Role |
|---|---|---|
| Canvas | `#070A12` | Deep obsidian background |
| Surface | `#101728` @ 68–88% | Glass cards and dialogs |
| Violet | `#9B7BFF` | Primary identity, active state |
| Indigo | `#5B6CFF` | Gradient partner |
| Cyan | `#49D6E8` | Connection / secondary accent |
| Mint | `#5EEAD4` | Verified / local / trust accent |
| Amber | `#FFC978` | Attention |
| Rose | `#FF8FA3` | Blocked / failed |

### Aurora Day (light companion)
A soft `#F6F8FF` canvas with `#5B3FD6` violet, `#0B8578` teal and the same accents.
Both schemes are unit-tested for WCAG AA contrast on primary text surfaces
(`AuroraContrastTest`).

Material You dynamic color remains available and is still user-controlled. The Aurora
accent constants stay constant so the product identity survives wallpaper-derived schemes.

## 3. Shape and depth

- Cards: 24 dp radius, hairline border, no heavy elevation.
- Dialogs: 28 dp radius, tonal surface, explicit kicker line.
- Bottom dock: a single floating pill with a gradient-selected item.
- Message bubbles: 22 dp with a tight tail; user turns use the conversation gradient,
  Origin turns use glass plus a micro-attribution header.
- Depth comes from translucent layers and three slow radial light wells, not from shadows.

## 4. Typography

- Display: system sans, ExtraBold, tight tracking (`-1.8.sp` at displayLarge).
- Titles: SemiBold, restrained line height.
- Micro-labels: monospace, medium weight, positive tracking. Used for `ORIGIN AGENT`,
  `CHAIN VERIFIED`, section kickers and capability kinds.
- No remote fonts, no bundled font files: readability is independent of network and storage.

## 5. Motion

- Ambient light wells drift over nine seconds and reverse; Reduce motion sets drift to zero.
- List scroll, dialog appearance and selection follow platform motion.
- The mark breathes only when motion is allowed.
- No decorative animation blocks input or runs while the screen is idle in the background.

## 6. Component inventory

| Component | Purpose |
|---|---|
| `AuroraBackground` | Three radial light wells over the canvas |
| `AuroraScreen` | Standard secondary-screen wrapper |
| `AuroraMark` | Origin mark drawn from circles and gradients |
| `GlassCard` | Translucent bordered container |
| `AuroraPill` | Status / risk / provenance capsule |
| `ModelChip` | Always-truthful active model control |
| Download controls | Picker rows for verified one-tap weight install with progress |
| `GradientButton` | Primary action with brand gradient |
| `SectionHeading` | Kicker + title hierarchy |
| `AuroraRule` | Quiet divider |
| `MessageBubble` | Attribution-aware conversational turn |
| `SuggestionRow` / starter grid | Low-friction ways into conversation |

## 7. Accessibility and resource discipline

- Semantic labels on icon controls; text never relies on color alone.
- Contrast is tested for light and dark palettes.
- Reduce motion is a single shared preference honored by every animated component.
- All visuals are drawn with Compose primitives and CSS gradients. No images, fonts,
  analytics, CDN requests or new dependencies were added.
- Target remains Android 8+ / 4 GB RAM; effects are static gradients and cheap draw calls.

## 8. Invariants

- The UI never invents a model identity; `ModelChip` and `MessageBubble` only render state
  supplied by the harness.
- No local model is offered unless its weights can be installed and verified in one tap.
- `ATTEMPTED` is visually distinct from `VERIFIED`.
- A redesign may change tokens and layout, but never the eight-stage trust pipeline.
