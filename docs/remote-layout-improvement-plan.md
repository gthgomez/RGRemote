# RGRemote Remote Layout Improvement Plan

Last updated: 2026-05-31.

This plan turns the layout critique into **subagent-ready work packages**. Use it to improve the Remote tab hierarchy, cross-tab consistency, and daily-use thumb zone without another styling-only pass.

## Goals

1. **Remote-first viewport** — D-pad and volume stay visible without scrolling past setup chrome.
2. **Clear dual-TV workflow** — Switch and watch flows stay obvious; secondary actions don’t compete with the pad.
3. **Symmetric ecosystems** — Roku and Google TV Remote tab layouts feel equivalent when utilities are hidden.
4. **Cross-tab context** — Apps/Settings show which TV is active without restoring the full device bar.
5. **Setup vs daily modes** — Incomplete setup shows guidance; complete setup defaults to minimal chrome.

## Non-goals (this pass)

- Pixel-perfect match to the May 2026 mockup image.
- New protocol features or discovery rewrites.
- Automated screenshot/UI tests (manual visual QA only).

## Architecture constraints (do not break)

- `roku/` and `google/` protocol isolation.
- `RemoteUiPreferences` for hide/show guide and utilities.
- `canonicalDevicesPerType()` and `DeviceRegistry.dedupeStoredDevices()` for duplicate TVs.
- Snackbar `userFeedback` vs `connectionStatus` split in `RGRemoteViewModel`.

---

## Phase overview

| Phase | Focus | Parallel? |
|-------|--------|-----------|
| **P0** | Scroll shell + pinned remote | 1 agent (foundation) |
| **P1** | Header merge + power semantics | 1 agent after P0, or parallel if P0 only touches scroll |
| **P2** | Cross-tab context + Google symmetry | 2 agents in parallel |
| **P3** | Setup vs daily mode polish | 1 agent |
| **P4** | Visual parity (Apps/Settings shell) | 1 agent (optional) |

**Recommended execution order:** P0 → (P1 + P2 parallel) → P3 → P4.

---

## P0 — Pinned remote scroll shell (foundation)

**Owner files:** `RGRemoteApp.kt`, `RemoteControls.kt` (layout only), possibly new `RemoteTabLayout.kt`.

### Problem

The entire Remote tab scrolls as one column: header → banner → hero card → utilities. The D-pad leaves the thumb zone on small phones.

### Implementation

1. Replace Remote tab root `Column(verticalScroll)` with a **non-scrolling top region** + **scrollable bottom region**:
   - **Fixed (or weighted) top:** `ActiveDeviceHeader` + optional `SetupGuideBanner` + **RemoteSurface** (D-pad block only, or full card without shortcuts).
   - **Scrollable bottom:** `RemoteShortcutDock`, `RemoteUtilitiesDock`, Google `LaunchPanel` (if not moved in P2).
2. Use `Column(Modifier.fillMaxSize())` with `RemoteSurface(Modifier.weight(1f, fill = false))` or `BoxWithConstraints` to cap hero height on short screens (e.g. `maxHeight = 58%` of viewport minus header/nav).
3. Ensure `defaultMinSize(minHeight = 360.dp)` on hero does not force overflow — prefer **flexible cluster size** from `BoxWithConstraints` in `RemoteControls.kt`.
4. Bottom nav remains in `Scaffold.bottomBar`; respect `padding` from scaffold.

### Acceptance criteria

- On ~640dp height device, D-pad and volume visible without scrolling when guide hidden and utilities collapsed.
- Shortcuts/utilities still reachable via scroll below the card.
- No regression: watch chips, transport row, snackbar, segment bar still work.

### Verify

```powershell
.\gradlew.bat assembleDebug
```

Manual: Remote tab, utilities off, guide off, font scale 1.0 and 1.3.

---

## P1 — Merge header into remote card + power clarity

**Owner files:** `DeviceHeader.kt`, `RemoteControls.kt`, `RGRemoteApp.kt`.

**Depends on:** P0 layout structure (coordinate who owns segment bar).

### Tasks

1. **Remote-only compact chrome**
   - Move `EcosystemSegmentBar` (Roku | Google TV) into the **top of `RemoteSurface`** (below optional 1-line status).
   - Shrink or remove outer `ActiveDeviceHeader` on Remote tab; keep ⋮ menu (scan, refresh, guide) on card top-right.
   - Retain device name + online dot as **one line** inside card (ellipsis).

2. **Power semantics (UX copy + layout)**
   - Card top: red icon = Power off / toggle (ecosystem-specific).
   - Transport row green **POWER**: label **Wake** on Roku, **Power** on Google TV; avoid two identical “power” affordances without labels.
   - Document in UI strings or `contentDescription` only; no ECP behavior change unless bug found.

3. **Remove duplicate “ecosystem pill”** if any remains inside card after segment move.

### Acceptance criteria

- Remote tab saves ~48–64dp vs current header + card top row.
- User can identify wake vs power-off without reading Settings.

### Verify

Same as P0 + manual power press on Roku TV.

---

## P2a — Apps/Settings active-TV context (parallel)

**Owner files:** `RGRemoteApp.kt` (`AppsPanel`, `SettingsPanel`), optionally `DeviceHeader.kt` (shared `ActiveTvSubtitle` composable).

### Tasks

1. Add **`ActiveTvSubtitle`** below tab title on Apps and Settings:
   - Text: `Controlling: {friendlyName}` or `Controlling: Roku` / `Google TV` when name missing.
   - Tint with ecosystem accent; tap opens Remote tab (`onSelectTab(REMOTE)`).
2. If `selectedDevice == null`, show `No device selected — open Remote` with same tap action.
3. Do **not** re-add full device chip grid on Apps/Settings.

### Acceptance criteria

- User on Apps knows which ecosystem pins/refresh apply.
- Tap subtitle navigates to Remote tab.

---

## P2b — Google / Roku Remote symmetry (parallel)

**Owner files:** `QuickActions.kt`, `RGRemoteApp.kt`, `RemoteControls.kt`.

### Tasks

1. **Google TV launch panel**
   - Move standalone Google `LaunchPanel` below card into **`RemoteUtilitiesDock`** (new section “Launch app”) OR hide behind same Utilities expander pattern as Roku HDMI.
   - When `showUtilitiesDock == false`, Google users launch only via Apps pins / typed launch in Apps tab (add one line in Apps if needed).

2. **Shortcut dock**
   - Keep inside hero card for both ecosystems OR move to scroll region below pad for both (match P0 split).
   - Same row height and chip count (4) for Roku and Google.

3. **Watch row**
   - Unchanged logic (`watchModeVisibility`); ensure visible only in card footer for both when applicable.

### Acceptance criteria

- With utilities hidden, Roku and Google Remote tabs have similar vertical structure (card only + shortcuts in scroll).
- No Google-only full-width launch block below utilities unless expander open.

---

## P3 — Setup mode vs daily mode

**Owner files:** `RemoteDesign.kt` (`setupGuideNeeded`), `RGRemoteApp.kt`, `RemoteUiPreferences.kt`, `SettingsPanel`.

### Tasks

1. **`setupComplete` derived state** (already partially present):
   - Roku present, Google paired, HDMI mapped.
2. **Daily mode defaults** (when complete):
   - `showConnectionGuide` default stays user preference; on first completion optionally set banner hidden once (preference write — discuss, default **off** for banner auto-hide to avoid surprising users).
3. **Setup mode UI**
   - When `!setupComplete`: allow banner + expanded utilities by default (`utilitiesExpanded = true` already keyed off this).
   - When `setupComplete`: utilities collapsed; banner only if user left guide enabled.
4. **Optional:** Segment-only header strip outside card during setup only (if P1 merged header, show slim “Step 3 of 5” in banner only).

### Acceptance criteria

- Fresh install: guide visible, utilities expanded.
- Completed setup: Remote opens to pad-first layout with minimal chrome (prefs respected).

---

## P4 — Visual shell parity (optional)

**Owner files:** `RGRemoteApp.kt` (`AppsPanel`, `SettingsPanel`), `RemoteDesign.kt`.

### Tasks

1. Shared **`NeoPanel`** wrapper: same corner radius, border, light `cyberEtch` as hero card (subtle).
2. Slim top padding on Apps/Settings to align with Remote horizontal padding.
3. No new features; styling only.

### Acceptance criteria

- Apps/Settings feel like same app as Remote, not default Material cards only.

---

## Subagent dispatch guide

### Agent A — P0 Scroll shell (run first)

```
Implement P0 from RGRemote/docs/remote-layout-improvement-plan.md.
Split Remote tab: fixed hero (D-pad) + scrollable shortcuts/utilities.
Files: RGRemoteApp.kt, RemoteControls.kt, optional RemoteTabLayout.kt.
Do not change ViewModel or discovery. Run assembleDebug.
Return: summary of layout structure and screenshot notes for small phone.
```

### Agent B — P1 Header merge (after A)

```
Implement P1 from RGRemote/docs/remote-layout-improvement-plan.md.
Merge segment + status into RemoteSurface; clarify wake vs power-off labels.
Coordinate with P0 scroll regions. Run assembleDebug.
```

### Agent C — P2a Apps/Settings subtitle (parallel with D)

```
Implement P2a from RGRemote/docs/remote-layout-improvement-plan.md.
Add ActiveTvSubtitle on Apps and Settings tabs.
```

### Agent D — P2b Google/Roku symmetry (parallel with C)

```
Implement P2b from RGRemote/docs/remote-layout-improvement-plan.md.
Move Google launch into utilities expander; align shortcut placement with P0 if merged.
```

### Agent E — P3 Setup vs daily (after B–D)

```
Implement P3 from RGRemote/docs/remote-layout-improvement-plan.md.
Setup-complete defaults for banner/utilities expansion.
```

### Agent F — P4 Visual parity (optional, last)

```
Implement P4 from RGRemote/docs/remote-layout-improvement-plan.md.
Shared panel styling for Apps/Settings only.
```

### Parallelization matrix

| Wave | Agents | Notes |
|------|--------|-------|
| 1 | A | Blocks layout contract for B and D |
| 2 | B, C, D | B touches RemoteControls; C/D mostly separate files |
| 3 | E | Preferences + banner behavior |
| 4 | F | Cosmetic only |

**Merge conflict hotspots:** `RGRemoteApp.kt` (all phases), `RemoteControls.kt` (P0, P1, P2b), `QuickActions.kt` (P2b).

---

## QA checklist (full pass)

- [ ] Remote: D-pad visible without scroll (guide off, utilities collapsed, setup complete).
- [ ] Remote: segment switch Roku ↔ Google TV; commands route correctly.
- [ ] Watch Google TV / Watch Roku still work when prerequisites met.
- [ ] Apps: subtitle matches selected device; pins launch correct ecosystem.
- [ ] Settings: toggles hide guide and utilities; merge duplicates still works.
- [ ] Font scale 1.3x: no overlap on segment, OK button, HDMI row (utilities expanded).
- [ ] `testDebugUnitTest` + `assembleDebug` pass.

## Reference

- Layout critique: conversation 2026-05-31 (pinned remote, header merge, cross-tab context).
- UI target: `docs/ui-target.md`
- Dedupe / prefs: `DeviceRegistry.kt`, `RemoteUiPreferences.kt`
