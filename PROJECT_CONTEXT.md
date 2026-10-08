# RGRemote Project Context

Last verified against local source: 2026-05-15.

RGRemote is a personal Android remote app for a TCL Roku TV and an Onn Google TV box. This file is repo-local context only; it does not override workspace or repository instruction authority (`AGENTS.md`).

## Scope

- Android app written in Kotlin and Jetpack Compose.
- Primary device lane: Roku TV local control through SSDP discovery and Roku ECP HTTP commands on port 8060.
- Secondary device lane: Google TV / Android TV Remote Protocol v2 through mDNS/NSD discovery, Android Keystore client credentials, PIN pairing, and TLS sockets.
- Product goal: a polished phone remote that can switch between Roku-native control and a paired Google TV box while keeping protocol boundaries clear.

## Architecture

- `app/src/main/java/com/rgremote/app/MainActivity.kt` starts the Compose app.
- `ui/` owns app state presentation, remote controls, quick actions, bottom navigation, theming helpers, and `RGRemoteViewModel`.
- `domain/DeviceModels.kt` defines device types, command models, and UI-facing device state.
- `roku/RokuEcpClient.kt` owns Roku ECP calls such as `query/device-info`, `query/active-app`, `keypress/...`, and `launch/{id}`.
- `discovery/RokuSsdpDiscovery.kt` and `discovery/GoogleTvNsdDiscovery.kt` own local network discovery.
- `google/` contains the experimental Google TV Remote v2 adapter, pairing manager, keystore wrapper, and protobuf-ish wire helpers.
- `data/db/` contains Room persistence for devices and pairing credential metadata.
- `data/apps/AppPinStore.kt` stores pinned app/shortcut preferences.

## Roku / Google TV Boundary

Roku is the stable control plane. It handles TV power, volume, app launch, HDMI switching, active-app hints, and Roku remote commands through ECP.

Google TV is intentionally isolated. It handles Android TV pairing, navigation, power, volume, and deep-link launch through the reverse-engineered Remote v2 protocol. Google TV code must stay in `google/` unless a shared domain type is required.

Do not make Google TV code depend on Roku internals, and do not make Roku control depend on Google TV availability. A Google protocol break must not take down Roku remote behavior.

## Input Visibility Invariant

The app cannot prove which HDMI source is physically visible on the TV panel. Roku `query/active-app` can return `tvinput.hdmiX`, but that is only a hint. UI copy and docs must say "hint", "likely", or "mapped input" unless hardware verification proves the current claim.

## UI Direction

The Remote tab targets the minimal "Halo" design in `docs/ui-target.md` (reference mockup in `docs/reference/`): wordmark header, device chip, glowing halo ring D-pad, three-button glass dock, and a More Controls sheet. Roku and Google TV states remain visually distinct through the device chip and capability gating, not separate screen languages. The legacy dense neon/glass direction is retired.

See `docs/ui-target.md` for the active visual target.

## Build And Release State

- Build from the repository root with `.\gradlew.bat assembleDebug`.
- Release testing currently uses Android debug signing credentials in `app/build.gradle.kts`.
- Debug-signed release APKs are for local testing only. Remove debug signing and use a real release keystore before production or distribution.
- `local.properties` is local machine state and must not be committed or copied into docs.

## Non-Negotiable Invariants

- Preserve protocol isolation between `roku/` and `google/`.
- Keep Room migrations explicit. Do not casually reset or destructively migrate user device/pairing data.
- Keep Android Keystore credential handling inside the Google TV pairing/control boundary.
- Keep build configuration aligned with Project_Android Gradle rules. Do not double-declare Android or Kotlin plugins.
- Hardware-facing claims require live-device verification or must be marked as unverified.
- Visual polish changes must be checked on both normal and small phone viewports.
