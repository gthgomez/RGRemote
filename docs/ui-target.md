# RGRemote UI Target — "Halo" Remote

Last updated: 2026-10-08. This document is the authoritative visual contract for the
Remote tab redesign. It supersedes the May 13, 2026 dense neon/glass direction, which is
retired. Any agent implementing UI must match this contract and the reference images.

## Reference assets (in-repo)

- `docs/reference/halo-mockup-dark-light.png` — authoritative dual-theme mockup.
- `docs/reference/halo-dark-phone.png` / `halo-light-phone.png` — per-theme full screens.
- `docs/reference/halo-{dark,light}-{header,ring,dock}.png` — zoomed regions.

The mockup's phone hardware chrome (status bar, clock, Dynamic Island, home indicator)
and the promotional backdrop are NOT part of the target. Clone the app screen only.

## Product boundary

One shared minimal remote design for both ecosystems. Ecosystem differences live in
command semantics and capability gating, not in two different screen languages. Roku and
Google TV states must remain visually distinct through the device chip and status text.

## Screen composition (single screen, top to bottom)

| Zone | Vertical span | Content |
|---|---|---|
| Header | 0–13% | `RGRemote` wordmark ("RG" violet, remainder primary text) + kebab menu top-right |
| Device chip | 13–26% | Full-width glass pill: violet rounded-square TV glyph well, device name, hairline divider, status dot + label, chevron-down; opens device switcher |
| Halo ring | 26–78% | One continuous glowing circle; blank center; nothing visible while idle |
| Glass dock | 78–90% | Floating capsule, three equal segments: Back, Home, Power |
| More Controls | 90–100% | Small circular chevron-up well + letter-spaced "MORE CONTROLS" label; opens secondary-control sheet |

No bottom navigation bar, volume rail, or transport controls are visible on the default
screen. All secondary controls (volume, mute, transport, app shortcuts, HDMI, keyboard,
watch mode) live in the More Controls sheet. Apps and Settings remain reachable from that
sheet; bottom navigation may be hidden in the default remote view.

## Measured geometry

Values measured from the reference PNG. They are the contract; tune only ±small margins
against real-device screenshots.

- Ring outer diameter: ~72% of screen width (~290dp at 412dp width). Ring center at ~47%
  of screen height.
- Center (select) disc: ~54% of ring diameter.
- Ring bright core stroke: ~14% of ring diameter (~40dp), with soft glow shoulders
  roughly doubling the perceived band.
- Idle ring shows NO dots, arrows, or "OK" label. Direction arrows appear only while a
  zone is pressed; center pulses briefly on Select.
- Screen padding: 18–22dp horizontal. Header 48–52dp tall. Chip 48–54dp tall.
- Dock capsule ~90dp tall, ~70% screen width, three equal segments with hairline
  separators; each icon in a circular well ≥58dp.
- More Controls affordance: minimum 48dp touch target.

## Touch contract (ring)

Five zones: center Select (distance ≤ 0.49 × radius) and four directional sectors split
by `|dx| >= |dy|`. Outside the outer radius = no hit. Drawing is independent of command
dispatch; the ring is ecosystem-agnostic. Press-and-hold repeats using the existing
400ms initial delay / 110ms interval; release must cancel and leave no queued commands.
TalkBack must see five independently addressable actions (Up/Down/Left/Right/Select),
never one unlabeled circle.

## Device chip states (must never falsely imply readiness)

| ConnectionStatus | Dot | Label | Interaction |
|---|---|---|---|
| `ONLINE` | green | "Online" | normal |
| `CHECKING` | amber | "Checking…" | controls disabled |
| `OFFLINE` / `CONNECTION_FAILED` | gray/red | "Offline" / "Connection failed" | controls disabled, overlay hint |
| `NOT_PAIRED` / `PAIRED` (Google unpaired) | violet | "Not paired — tap to set up" | opens pairing flow |
| no device | neutral | "No TV — add one" | opens add-device flow |

Ring and dock render at reduced opacity and ignore touches whenever controls are
disabled (`CHECKING`, `NOT_PAIRED`, `OFFLINE`, `CONNECTION_FAILED`, no device).

## Color tokens

Canonical values live in `ui/theme/HaloColors.kt` (theme-flippable) and `Palette.kt`
(theme-invariant accents). Sampled from the reference; re-sample at 1× if a token must
change, and never inline hex literals in composables.

Summary (dark → light):

- Background: `#0B0F27` → `#070A1E` vignette; `#F4F7FF` → `#D8E8F8`.
- Ambient blooms: violet/blue lower-left + right (dark); lavender/cyan plus a pink bloom
  near the dock (light — the pink bloom is required, do not drop it).
- Wordmark accent "RG": `#A26DF6` / `#6D3FD6`.
- Ring gradient (revised mockup): light blue at top `#4FB8FF` → violet `#6D7CFF`/`#9B5CF6`
  → pink at bottom `#D86DDF` (dark); pastel equivalents in light. A darker band sits
  between the glowing ring and the disc; disc is ~60% of ring diameter.
- Ring-adjacent volume: small +/− controls flank the ring's right edge and fade in only
  while the ring is touched (idle ring stays clean); volume also lives in More Controls.
- Ring center: `#101543` + white 8% border / white + `#C9C9F5` border.
- Dock: translucent indigo glass + white hairlines / white glass + `#E1E6F5` borders.
- Power accent: existing `RgDanger` red with a red-tinted well (dark); `#E24A5E` + pink
  tint (light).
- Status green: `RgSuccess` (dark); `#1E9E62` (light, contrast-boosted).

## Typography

Wordmark and labels use the app's existing sans (no new font). "MORE CONTROLS" is
uppercase with wide letter spacing (~2sp). Device chip text is single-line, ellipsized.

## Verification expectations

- Deterministic Compose previews must exist for dark and light × Online / Checking /
  Offline / Not paired (see `ui/preview/VisualContractPreviews.kt`).
- Screenshot baselines (Roborazzi, added in PR05) cover the same matrix.
- Manual visual QA per `QA_CHECKLIST.md` includes S25 Ultra (primary device), a small
  phone profile, font scale 1.3×, and landscape.
