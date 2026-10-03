# RGRemote QA Checklist

## Build

- Run from the repository root.
- `.\gradlew.bat assembleDebug` succeeds.
- `.\gradlew.bat assembleRelease` succeeds when release testing is in scope.
- No build file declares `kotlin.android`.
- `local.properties` and keystore secrets are not documented or staged.

## Release APK Install

- `app/build\outputs\apk\release\app-release.apk` exists after `assembleRelease`.
- `apksigner verify --print-certs` passes.
- Temporary local release-testing builds show signer `C=US, O=Android, CN=Android Debug`.
- Debug signer is treated as a blocker for production/public distribution.
- `adb install -r .\app\build\outputs\apk\release\app-release.apk` succeeds on a test device.
- App launches after install without clearing required paired-device state unexpectedly.

## Roku MVP

- Scan discovers the TCL Roku TV on the same subnet.
- Scan does not show **Online** until `query/device-info` succeeds (SSDP alone is not enough).
- Status line shows power mode when online (for example `Online · Standby`).
- Wake is disabled when ECP reports `PowerOff`; standby/off hints appear under transport buttons.
- Failed Roku commands retry with backoff, then trigger a rescan to pick up a changed IP (except HTTP 403 network-access errors).
- `query/device-info` succeeds after Roku Network Access is Enabled or Permissive.
- D-pad, OK, Home, Back, Play, volume, mute, and power commands send as ECP keypresses.
- HDMI buttons use `InputHDMI1` through `InputHDMI4`.
- Status refresh reads `query/active-app` and displays HDMI inference only as a hint.

## Roku Connection Regression (manual)

Run on the same Wi-Fi as the TCL Roku TV after `testDebugUnitTest` passes.

1. **ECP gate:** With TV on, tap Scan. Status should move `Checking` → `Online · On` (or `Standby`), not `Online` immediately on SSDP alone.
2. **Live control:** Send D-pad, Home, volume, and Power off; confirm TV responds.
3. **Wake / standby:** Put TV in standby (not full power off). Tap Wake; status should recover to online when ECP answers.
4. **Full off:** Power TV fully off. Wake should stay disabled with Fast TV Start hint; `curl http://<roku-ip>:8060/query/device-info` from a PC should fail.
5. **Stale IP recovery:** Change the saved IP in Settings diagnostics to a wrong address, send Home, confirm app rescans and commands work again without manual re-add.
6. **Network access denied:** Set Roku mobile control to Default/Deny; confirm failure message mentions Network access and rescan does not loop endlessly.

## Google TV Phase 2

- mDNS discovery listens for `_androidtvremote2._tcp.`.
- Pairing generates an Android Keystore RSA client credential.
- Start pairing opens TLS on port 6467 and sends the v2 pairing handshake.
- Finish pairing accepts a 6-character hex PIN and stores the credential alias in Room.
- Remote commands use TLS on port 6466 and stay isolated from Roku code.

## Watch Flows

- **Watch Google TV** requires a saved HDMI mapping, sends the Roku input command, waits, then sends Google TV Home.
- **Watch Roku** sends Roku Home and selects the Roku tile.
- App never claims automatic panel visibility detection.

## Visual QA

- Normal phone layout matches the Roku neon/glass direction in `docs/ui-target.md`.
- Small phone layout keeps the header, remote controls, quick actions, and bottom nav usable.
- Font scale around 1.3x does not clip labels or resize fixed controls awkwardly.
- Roku selected state and Google TV paired state remain visually distinct.
- Quick actions are usable: HDMI tiles, Roku shortcut tiles, manual channel/deep-link launch, and customize affordance.
- Disabled, unpaired, loading, and offline states are readable and do not look like successful online states.
- Bottom navigation remains slim, segmented, and non-overlapping.

## Pairing And Persistence

- Roku discovery and saved Roku device survive app restart.
- Google TV pairing credential alias is stored without exposing private key material.
- Paired Google TV commands work after app restart.
- Removing or replacing a device does not corrupt the other device lane.
- Room schema changes include a migration plan before release.

## Known Limitations

- Google TV Remote v2 is reverse-engineered and may break after OS updates.
- Roku active-app HDMI values are hints, not proof of visible panel input.
- Visual QA is currently manual/screenshot based.
- Release APK signing currently uses temporary Android debug credentials for release testing only.
- Hardware verification requires devices on the same local network with multicast/NSD allowed.
