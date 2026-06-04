# RGRemote Fix Plan Evidence

Last updated: 2026-05-21.

## 1. Google TV Remote v2 Session

- Previous error: `GoogleTvAdapter` opened TLS, wrote configure/set-active/one command, and closed without reading server messages or answering pings.
- Upgrade: commands now run through a bounded Remote v2 session that negotiates features, responds to ping requests, waits for `remote_start`, and refuses unsupported command families.
- Changed files:
  - `app/src/main/java/com/rgremote/app/google/GoogleTvAdapter.kt`
  - `app/src/main/java/com/rgremote/app/google/ProtoWire.kt` via existing framing tests
- Evidence:
  - `androidtvremote2` documents Remote v2 as the same protocol used by the Google TV mobile app and exposes URLs/app links as commands: https://github.com/tronikos/androidtvremote2
  - Its protocol notes describe feature negotiation, ping, key, power, volume, app-link features, and server-initiated state updates: https://deepwiki.com/tronikos/androidtvremote2/4.3-remote-control-protocol
  - Its current `remote.py` handles `remote_configure`, `remote_set_active`, `remote_start`, `remote_ping_request`, app links, current app, and volume updates: https://raw.githubusercontent.com/tronikos/androidtvremote2/main/src/androidtvremote2/remote.py
- Why better: one-shot writes can silently fail on devices expecting a live Remote v2 conversation; the new path waits for readiness and handles keep-alive traffic.

## 2. Google TV Credential Hardening

- Previous error: client private keys were stored as PEM files under app files, and app backup was enabled.
- Upgrade: client private keys are generated in Android Keystore, legacy PEM files are deleted on regeneration, app backup is disabled, and paired control verifies the server certificate hash captured during pairing.
- Changed files:
  - `app/src/main/java/com/rgremote/app/google/GoogleTvKeyStore.kt`
  - `app/src/main/java/com/rgremote/app/google/GoogleTvPairingManager.kt`
  - `app/src/main/java/com/rgremote/app/data/db/DeviceEntity.kt`
  - `app/src/main/java/com/rgremote/app/data/db/RGRemoteDatabase.kt`
  - `app/src/main/AndroidManifest.xml`
- Evidence:
  - Android Keystore is intended to make app keys harder to extract: https://developer.android.google.cn/privacy-and-security/keystore
  - Android Auto Backup can include app files unless disabled or excluded: https://developer.android.com/identity/data/autobackup
- Why better: private key material no longer lives as readable PEM app data, and post-pairing control no longer trusts any local TLS peer.

## 3. Re-Pair Recovery

- Previous error: the UI said "Re-pair", but `startPairing()` returned early when a device was already paired.
- Upgrade: starting pairing for an already-paired Google TV now deletes the old DB credential, deletes the Android Keystore entry, cancels any open session, and starts a fresh pairing session.
- Changed files:
  - `app/src/main/java/com/rgremote/app/ui/RGRemoteViewModel.kt`
  - `app/src/main/java/com/rgremote/app/google/GoogleTvPairingManager.kt`
  - `app/src/main/java/com/rgremote/app/data/registry/DeviceRegistry.kt`
  - `app/src/main/java/com/rgremote/app/data/db/DeviceDao.kt`
- Evidence:
  - `androidtvremote2` tells users to clear Android TV Remote Service storage when pairing/control breaks, which implies re-pair recovery is a normal maintenance path: https://raw.githubusercontent.com/tronikos/androidtvremote2/main/src/androidtvremote2/remote.py
- Why better: the in-app recovery action now matches the label and avoids forcing users to reinstall or manually clear app data.

## 4. Roku Launch and Deep Links

- Previous error: Roku launch encoded the entire typed target as `/launch/<encoded>`, breaking documented query-string deep links.
- Upgrade: `RokuLaunchTarget` parses channel IDs, `launch/...` paths, full ECP URLs, and `contentId`/`mediaType` query params while preserving `?`, `&`, and `=`.
- Changed files:
  - `app/src/main/java/com/rgremote/app/roku/RokuLaunchTarget.kt`
  - `app/src/main/java/com/rgremote/app/roku/RokuEcpClient.kt`
  - `app/src/test/java/com/rgremote/app/roku/RokuLaunchTargetTest.kt`
- Evidence:
  - Roku documents ECP launch as `POST /launch/<channelId>?contentId=<content ID>&mediaType=<mediaType>`: https://developer.roku.com/dev/docs/external-control-api
  - Roku deep-link docs require `contentId` and `mediaType` to be passed as query parameters: https://developer.roku.com/dev/docs/implementing-deep-linking
- Why better: channel launches still work, while real deep links now reach Roku apps in the shape Roku expects.

## 5. Roku Power Semantics

- Previous error: Roku code sent undocumented `PowerOn` and `Power` ECP keypresses.
- Upgrade: Roku `PowerOn` is now treated as a Home/wake attempt, Roku power toggle is rejected, and the UI labels Roku actions as "Wake" and "Power off".
- Changed files:
  - `app/src/main/java/com/rgremote/app/roku/RokuEcpClient.kt`
  - `app/src/main/java/com/rgremote/app/ui/RemoteControls.kt`
  - `app/src/main/java/com/rgremote/app/ui/RGRemoteViewModel.kt`
- Evidence:
  - Roku's ECP keypress list documents remote key values and TV power-off behavior, not a guaranteed local-network power-on command: https://developer.roku.com/dev/docs/external-control-api
  - Roku support states wake/control behavior depends on device model, network, CEC, and power settings: https://wwwimg.roku.com/mobile/faq.html
- Why better: the app now describes what it can reliably attempt instead of implying a hard power-on guarantee.

## 6. Watch Flow Failure Handling

- Previous error: `watchGoogleTv()` and `watchRoku()` could overwrite a failed send with a success status.
- Upgrade: `sendTo()` returns `Result<Unit>`, and watch flows stop after a failed Roku input switch, Roku Home, or Google TV Home command.
- Changed files:
  - `app/src/main/java/com/rgremote/app/ui/RGRemoteViewModel.kt`
- Evidence:
  - Local source evidence: previous `sendTo()` only updated status and returned `Unit`, so callers could not branch on failure.
- Why better: UI status now reflects the actual command chain instead of false-positive "Watching..." states.

## 7. Manual Device Fallback

- Previous gap: discovery depended entirely on SSDP/NSD multicast.
- Upgrade: Settings now supports "Add by IP" for Roku and Google TV; Roku manual add validates with `query/device-info`, and Google TV manual add is verified during pairing.
- Changed files:
  - `app/src/main/java/com/rgremote/app/domain/ManualDeviceEndpoint.kt`
  - `app/src/main/java/com/rgremote/app/ui/RGRemoteViewModel.kt`
  - `app/src/main/java/com/rgremote/app/ui/RGRemoteApp.kt`
  - `app/src/test/java/com/rgremote/app/domain/ManualDeviceEndpointTest.kt`
- Evidence:
  - Android NSD is discovery over DNS-SD on the local network, which depends on devices being visible to service discovery: https://developer.android.com/develop/connectivity/wifi/use-nsd
  - Roku ECP exposes `query/device-info` over port 8060, which is a practical manual-IP validation endpoint: https://developer.roku.com/dev/docs/external-control-api
- Why better: multicast/router failure is no longer a dead end for a known local device address.

## Verification

- `.\gradlew.bat -p .\RGRemote testDebugUnitTest` passed.
- `.\gradlew.bat -p .\RGRemote assembleDebug` passed.
- `.\gradlew.bat -p .\RGRemote assembleRelease` passed, including release minification and lintVital.
