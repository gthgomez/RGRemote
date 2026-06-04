# RGRemote Agent Guide

This is the repo-local cold-start guide for coding agents working in `C:\Workspace\Project_Android\RGRemote`. It inherits the root workspace policy and `C:\Workspace\Project_Android\PROJECT_CONTEXT.md`; those higher-level files win on safety, approvals, verification, and truthfulness.

## Read Order

1. Root startup files required by `C:\Workspace\AGENTS.md`.
2. `C:\Workspace\Project_Android\PROJECT_CONTEXT.md`.
3. This file.
4. `PROJECT_CONTEXT.md`.
5. `README.md`.
6. `docs/ui-target.md` for visual/UI work.
7. `docs/release-testing.md` for release build or install work.
8. `QA_CHECKLIST.md` before final verification or handoff.

## Commands

Run from `C:\Workspace\Project_Android` unless a task says otherwise.

```powershell
.\gradlew.bat -p .\RGRemote assembleDebug
.\gradlew.bat -p .\RGRemote assembleRelease
```

Useful release-test checks:

```powershell
$env:ANDROID_HOME\build-tools\36.0.0\apksigner.bat verify --print-certs .\RGRemote\app\build\outputs\apk\release\app-release.apk
adb install -r .\RGRemote\app\build\outputs\apk\release\app-release.apk
```

If SDK paths differ, discover the installed build-tools version locally instead of guessing.

## High-Risk Zones

- `app/build.gradle.kts`: release signing, SDK levels, plugin declarations, and minification settings.
- `google/`: reverse-engineered Google TV Remote v2 protocol, TLS framing, pairing, and Android Keystore aliases.
- `roku/RokuEcpClient.kt`: physical TV commands, power, volume, HDMI switching, and app launch.
- `data/db/`: Room schema and persisted pairing/device data.
- `network_security_config.xml`: local network cleartext policy for Roku ECP.
- `docs/release-testing.md`: must not imply debug signing is production-safe.

## Implementation Rules

- Use existing Compose patterns in `ui/`; do not introduce a navigation framework unless the app flow genuinely needs one.
- Keep Roku and Google TV behavior separated. Shared UI/domain types are okay when they are neutral.
- Keep hardware claims honest. The app can infer likely Roku HDMI state, but it cannot prove panel visibility.
- Use `collectAsStateWithLifecycle` for Flow-backed UI state.
- Preserve compact phone layouts. Text must fit on small devices and font-scale QA matters.
- Use Room migrations deliberately. Avoid destructive migration changes unless the user explicitly approves data loss.
- Never commit or document local secrets from `local.properties`, keystores, or machine-specific paths except as examples.

## Handoff Format

For implementation work, final handoff should include:

- Risk/status.
- Changed files.
- Verification commands and results.
- Regression checks completed.
- Open risks, especially live-device or release-signing gaps.
