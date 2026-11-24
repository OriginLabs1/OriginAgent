# Local-first data decisions

## PostgreSQL: not justified in v0.0.4

PostgreSQL is an excellent server database. Origin Agent v0.0.4 is a single-user Android app
whose durable data is small, append-only and meant to remain readable without a service:

- tasks, channels and pages: JSONL under `filesDir/duet`;
- memory and provenance: append-only JSONL under `filesDir/origin`;
- model, adapter and UI preferences: `SharedPreferences`;
- GGUF weights: app-private files verified by size and SHA-256.

Adding PostgreSQL would require a reachable server, credentials or local daemon, schema
migrations, sync/conflict resolution and a new backup story. None of that improves the
offline chat, model import or trust ledger in this release. It would also weaken the claim
that Origin works with no network and no account. The decision is therefore **no PostgreSQL
in v0.0.4**.

A future opt-in sync server may use PostgreSQL if multiple devices genuinely need shared
state. That would be a separate, consent-gated feature with local data still authoritative.

## Full OpenDuet/OpenBot server: deferred

Upstream OpenDuet is no longer maintained as a standalone app and points users to OpenBot's
`Settings → Skin → OpenDuet`. Reproducing the whole OpenBot server stack (Bun/Hono, MCP,
connectors, computer use, cloud persistence and browser control) would turn a private mobile
app into an always-on infrastructure product.

v0.0.4 implements the part that fits the product promise: a native, offline-first
workspace inspired by OpenDuet — Chat, Tasks, Make, Channels, short paragraph replies,
local drafts and dictation. It does **not** claim server, connector or computer-use parity.
Those capabilities remain research candidates and should only arrive behind explicit user
configuration after the local core is verified.

## Model data stays on the device

Downloaded weights and imported GGUF files remain in app-private storage. Several models can
be kept at once; only one is resident in the native runtime at a time. Switching is a local
preference change and never uploads a file or asks a remote service which model to use.
