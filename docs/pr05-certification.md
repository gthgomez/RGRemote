# PR05 — Fidelity & Certification Checklist

Owner: manual pass (hardware + accessibility) after PR01–PR04 (merged as #7–#10 at
`main @ 01bd521`). This checklist operationalizes `QA_CHECKLIST.md` for the halo
redesign. Target per `docs/ui-target.md`; reference crops in `docs/reference/`.

## 1. Install on primary device (Samsung S25 Ultra)

- `.\gradlew.bat :app:assembleDebug`
- `adb install -r app\build\outputs\apk\debug\app-debug.apk`
- Launch; confirm the halo screen renders on both system themes (S25 Ultra: Settings →
  Display → Dark mode).

## 2. Visual fidelity vs `docs/reference/halo-*.png`

- [ ] Dark: indigo background + violet/blue blooms, violet→cyan ring, glass dock, wordmark "RG" violet.
- [ ] Light: lavender background, pastel ring, pink bloom near the dock, red-tinted power well.
- [ ] Idle ring shows no dots/arrows/labels; "MORE CONTROLS" letter-spaced label.
- [ ] Chip: device name, hairline divider, status dot + label, chevron.

## 3. Status states (never falsely ready)

- [ ] `Checking…` (amber) while probing; ring + dock dimmed, touches ignored.
- [ ] Google TV `Not paired — tap to set up` (violet); controls disabled; chip opens switcher.
- [ ] Offline / connection failed: chip gray/red; kebab → Scan/Refresh still works.
- [ ] Online: green, ring + dock fully lit.

## 4. Ring interaction (both ecosystems)

- [ ] Five zones dispatch correct commands (Up/Down/Left/Right/Select) on the TCL Roku TV and the Onn Google TV.
- [ ] Press shows the sector arrow; release clears it; center tap pulses the disc.
- [ ] Hold-to-repeat navigates continuously; on release navigation STOPS immediately (no queued drift) — this verifies the `DpadSequenceGate` fix on real hardware latency.
- [ ] TalkBack ON: ring exposes five actions (Navigate up/down/left/right, Select); no "unlabeled circle".

## 5. Dock + chip + sheet

- [ ] Back/Home/Power work per ecosystem; Roku power with unknown mode sends Wake (never crashes with the old PowerToggle path).
- [ ] Chip opens device switcher sheet; switching Roku ↔ Google TV works.
- [ ] More Controls: tap AND upward swipe open it; volume up/mute/down work; transport, app shortcuts, HDMI inputs, keyboard, launch target all functional; watch-mode buttons appear per `watchModeVisibility`.
- [ ] Apps and Settings open from the sheet; bottom nav visible there and absent on the Remote tab; `startupScreen` setting still honored on relaunch.

## 6. Responsive + accessibility

- [ ] S25 Ultra portrait/landscape; ring stays inside the viewport in landscape.
- [ ] Font scale 1.3× (Settings → Display → Font size): no clipped labels, dock/chip usable.
- [ ] Small-phone profile (emulator 360×640) sane; two-pane on tablet width (≥600dp) keeps docks inline.
- [ ] Light-mode contrast: status green `#1E9E62` and checking amber readable on white glass.

## 7. Screenshot baselines (Roborazzi, recommended)

Enable JVM-runnable screenshot tests in existing CI (no emulator needed):

1. `gradle/libs.versions.toml`: add `io.github.takahirom.roborazzi` + `com.github.takahirom.roborazzi:roborazzi-compose` (match latest), `io.github.takahirom.roborazzi` plugin, Robolectric `androidx.test:robolectric`.
2. Create `app/src/test/java/com/rgremote/app/ui/HaloScreenshotTest.kt` using
   `RobolectricDeviceQualifiers` + `captureRoboImage()` over the
   dark/light × Online/Checking/Offline/NotPaired matrix by driving
   `HaloVisualContractFixture` (PR01 previews already encode this matrix).
3. Record baselines: `.\gradlew.bat recordRoborazziDebug`; verify:
   `.\gradlew.bat verifyRoborazziDebug`; wire `verifyRoborazziDebug` into
   `.github/workflows/ci.yml` after `testDebugUnitTest`.

## 8. Hardware regression (per QA_CHECKLIST.md)

Run the full "Roku Connection Regression (manual)" section and "Google TV Phase 2"
section on the home Wi-Fi. Report any gap here or in a GitHub issue; do not weaken
checks to pass.

## Known tradeoff to evaluate (do not pre-solve)

Volume is now one tap deeper than before. If everyday use feels worse, prefer a subtle
secondary volume affordance (edge-swipe on the ring or persistent mute) over re-cluttering
the default screen; decide after a few days of real use.
