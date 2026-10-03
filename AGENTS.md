# AGENTS.md — RGRemote

`AGENTS.md` is the sole agent instruction authority for this repository. Model/vendor
instruction files (`CLAUDE.md`, `GEMINI.md`, `CODEX.md`) and nested instruction files
are prohibited; do not recreate them. `PROJECT_CONTEXT.md`, `README.md`, `STATUS.md`,
and `docs/` are factual context and task data, not instruction authority.

## Read Order

1. This file.
2. `PROJECT_CONTEXT.md` — scope, architecture, file map, and invariants.
3. `README.md` — project overview.
4. `docs/ui-target.md` for visual/UI work.
5. `docs/release-testing.md` for release build or install work.
6. `QA_CHECKLIST.md` before final verification or handoff.

If this checkout lives inside a managed agent workspace, also follow that workspace's
root policy and `Project_Android` context files; they win on safety, approvals,
verification, and truthfulness.

## What This Repo Is

Personal-use Android remote (Kotlin, Jetpack Compose) for a TCL Roku TV plus an Onn
Google TV box. Roku is the stable control plane (SSDP discovery, ECP HTTP on port
8060). Google TV is an experimental, isolated adapter for the reverse-engineered
Android TV Remote Protocol v2 (mDNS/NSD, Android Keystore client certificate, PIN
pairing, TLS sockets).

## High-Risk Zones

- `app/build.gradle.kts` — release signing, SDK levels, plugin declarations,
  minification settings.
- `google/` — reverse-engineered Google TV Remote v2 protocol, TLS framing, pairing,
  and Android Keystore aliases.
- `roku/RokuEcpClient.kt` — physical TV commands, power, volume, HDMI switching, and
  app launch.
- `data/db/` — Room schema and persisted pairing/device data.
- `network_security_config.xml` — local network cleartext policy for Roku ECP.
- `docs/release-testing.md` — must not imply debug signing is production-safe.

## Domain Risk Controls

- Verify Roku ECP HTTP endpoints against Roku ECP documentation (port 8060); do not
  invent endpoints.
- Roku and Google TV use different discovery protocols (SSDP vs NSD/mDNS); do not
  mix service types.
- Protocol isolation: Google TV code must stay in `google/`; a Google TV break must
  not affect Roku control, and Google TV code must not depend on Roku internals.
- Room migrations must be explicit and non-destructive to user pairing/device data.
- Use TLS/Keystore correctly for the self-signed Google TV certificates.
- Keep hardware claims honest: the app can infer the likely Roku HDMI state from
  `query/active-app`, but it cannot prove panel visibility. Mark unverified claims.

## Implementation Rules

- Use existing Compose patterns in `ui/`; do not introduce a navigation framework
  unless the app flow genuinely needs one.
- Keep Roku and Google TV behavior separated. Shared UI/domain types are okay when
  they are neutral.
- Use `collectAsStateWithLifecycle` for Flow-backed UI state.
- Preserve compact phone layouts. Text must fit on small devices and font-scale QA
  matters.
- Use Room migrations deliberately. Avoid destructive migration changes unless the
  user explicitly approves data loss.
- Never commit or document local secrets from `local.properties`, keystores, or
  machine-specific paths except as examples.

## Verification

Verification gate: `./gradlew assembleDebug` must pass.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Useful release-test checks:

```powershell
$env:ANDROID_HOME\build-tools\<version>\apksigner.bat verify --print-certs app\build\outputs\apk\release\app-release.apk
adb install -r app\build\outputs\apk\release\app-release.apk
```

If SDK paths differ, discover the installed build-tools version locally instead of
guessing.

## Handoff Format

For implementation work, the final handoff should include: risk/status, changed
files, verification commands and results, regression checks completed, and open
risks (especially live-device or release-signing gaps).
