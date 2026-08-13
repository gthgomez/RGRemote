# RGRemote Status

**Last verified:** 2026-08-01
**Status:** usable
**Confidence:** high

## Purpose

Personal-use Android remote control application for TCL Roku TVs (stable control plane) and Onn Google TV boxes (isolated experimental Android TV Remote Protocol v2 adapter).

## Current State

The app is built as a single-Activity Compose remote with dual control lanes. Roku control handles SSDP discovery and ECP HTTP commands (port 8060). Google TV control is isolated in `google/` using mDNS (`_androidtvremote2._tcp.`), Android Keystore RSA client credentials, 6-character PIN pairing, and TLS sockets (ports 6466/6467). Room DB stores device pairings.

## Verified Capabilities

- Roku ECP HTTP control (D-pad, OK, Home, Back, Play, volume, power, HDMI input switching).
- Roku SSDP discovery with `query/device-info` validation and stale IP auto-rescan recovery.
- Google TV Remote v2 isolated adapter (mDNS discovery, Keystore client certificate generation, PIN pairing, TLS socket framing).
- Room database device state persistence with explicit migration invariants.
- Quick actions and HDMI input mapping ("Watch Google TV" / "Watch Roku" workflows).

## Recent Evidence

- `handoff-20260710-221406.md` records verified Roku ECP gate and Google TV v2 TLS framing.
- `QA_CHECKLIST.md` specifies 7 visual/connection QA categories and release APK verification.

## In Progress

- Refinement of Google TV PIN re-pairing edge case handling after TV firmware updates.
- Visual layout testing across small screen phones and large font scaling (1.3x).

## Blockers

- Release APK currently uses temporary debug signing credentials (production release signing material required for public release).

## Risks and Unknowns

- Google TV Remote v2 protocol is reverse-engineered; Google TV OS updates may break socket framing.
- Physical panel HDMI active source state cannot be hard-verified from API responses alone (treated as hints).

## Verification

- `.\gradlew.bat -p .\RGRemote assembleDebug` verified command in `README.md`.

## Next Actions

1. Run physical device network connection regression test against TCL Roku TV.
2. Verify Google TV TLS socket reconnection and PIN re-entry flow.
3. Configure production release keystore signing.

## Evidence Sources

- [README.md](file:///C:/Workspace/Project_Android/RGRemote/README.md)
- [QA_CHECKLIST.md](file:///C:/Workspace/Project_Android/RGRemote/QA_CHECKLIST.md)
- [handoff-20260710-221406.md](file:///C:/Workspace/Project_Android/RGRemote/handoff-20260710-221406.md)
