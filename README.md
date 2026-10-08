# RGRemote

[![CI](https://github.com/gthgomez/RGRemote/actions/workflows/ci.yml/badge.svg)](https://github.com/gthgomez/RGRemote/actions/workflows/ci.yml)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
![Android](https://img.shields.io/badge/platform-Android-3ddc84)
![minSdk](https://img.shields.io/badge/minSdk-26-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7f52ff)

A local-network-only Android remote for Roku TVs and Google TV devices. No
account, no cloud, no analytics — control traffic goes straight from your
phone to your TV over your home LAN.

RGRemote was built and tested against a TCL Roku TV and an Onn Google TV box,
but it speaks standard protocols (Roku ECP and the Android TV Remote Protocol
v2), so other Roku TVs and Android/Google TV devices should work.

## Features

- **Roku control (stable lane):** SSDP discovery, ECP commands on port 8060 —
  D-pad, volume, power, HDMI input switching, channel browsing with search,
  and app launch.
- **Google TV control (experimental lane):** mDNS discovery, PIN pairing with
  an Android Keystore-backed client certificate, TLS-encrypted key injection,
  volume, power, and deep-link launch.
- **Keyboard text entry:** type on your phone and send it to the TV's focused
  text field (Enter/Backspace included) on either ecosystem.
- **Watch Google TV:** one tap switches the paired Roku TV to the HDMI input
  the Onn box sits on, then hands control to the Google TV lane.
- **Light & dark themes** that follow the system, an adaptive two-pane layout
  on tablets and landscape, saved devices with duplicate merging, and offline
  recovery actions.

## Requirements

- Android 8.0+ (minSdk 26) phone.
- Phone and TV on the same subnet with multicast allowed.
- Roku TV: **Settings > System > Advanced system settings > Control by mobile
  apps > Network access** set to Enabled or Permissive.

## Getting the app

A Google Play release is planned. Until then, build from source (below) with
Android Studio or the command line.

## Building from source

- JDK 17, Android SDK with platform 36 (AGP 9.2, Kotlin 2.3).

```bash
./gradlew :app:assembleDebug        # Windows: gradlew.bat
./gradlew :app:testDebugUnitTest
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## First-run checklist

1. Set the Roku network-access setting above.
2. Scan with the app's scan button to discover the Roku via SSDP.
3. Keep Google TV discovery running and pair with the 6-character PIN shown
   on the TV.
4. Map the Google TV box to its Roku HDMI port before using
   **Watch Google TV**.

## How it works

- Roku is the stable control plane: SSDP discovery, ECP HTTP commands on port
  8060, app launch, active-app status, volume, power, and HDMI input
  switching.
- Google TV is isolated behind the experimental Android TV Remote Protocol v2
  adapter: mDNS discovery, Android Keystore-backed client certificate, PIN
  pairing flow, TLS socket framing, navigation keys, volume, power, and
  deep-link launch. All of it lives in `google/` so a protocol break cannot
  affect Roku control.
- Input visibility is not automatic. The app only infers the likely TV input
  from Roku `query/active-app` when it returns a `tvinput.hdmiX` app id.

### Honest limitations

- Roku ECP is documented by Roku, but Roku OS upgrades can restrict local
  control.
- The Google TV Remote v2 protocol is reverse-engineered and undocumented by
  Google; pairing and key injection may break on firmware updates.
- Neither protocol can prove which HDMI source is visible on the physical
  panel — the app infers and labels, it does not assert.
- Text entry to Google TV devices has no shift support, so letters arrive
  lowercase and some shifted punctuation is skipped.
- The release APK in this repo's own testing is signed with debug
  credentials; see `docs/release-testing.md` before distributing builds.

## Privacy

RGRemote collects no data: no analytics, no ads, no cloud backend. Everything
it remembers (saved TVs, pairings, pins) stays on your device. See
[PRIVACY.md](PRIVACY.md).

## Contributing

Issues and PRs are welcome. Agent-authored contributions are fine too —
[`AGENTS.md`](AGENTS.md) documents the repo's engineering rules (read order,
high-risk zones, verification gate) that changes are expected to respect.

## Docs Map

- `PROJECT_CONTEXT.md` — repo scope, architecture, file map, and invariants.
- `AGENTS.md` — cold-start routing, high-risk zones, and local commands for
  coding agents.
- `STATUS.md` — active project status and evidence-backed capability
  breakdown.
- `docs/ui-target.md` — Roku-side visual target and Google TV separation
  rules.
- `docs/release-testing.md` — debug-signed release testing workflow and
  production signing warning.
- `QA_CHECKLIST.md` — build, visual, pairing, install, and regression
  checklist.

## License

Licensed under the [Apache License 2.0](LICENSE). Copyright 2026
Jonathan Gomez Aguilar.
