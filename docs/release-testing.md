# RGRemote Release Testing

Last updated: 2026-05-15.

RGRemote currently allows release APK builds to be signed with the Android debug keystore for local release testing. This is temporary and must not be used for production distribution.

## Current State

`app/build.gradle.kts` sets the `release` build type to use `signingConfigs.getByName("debug")`.

This makes `assembleRelease` produce an installable release-variant APK without a production keystore. It is useful for local smoke testing only.

## Build Commands

Run from `C:\Workspace\Project_Android`:

```powershell
.\gradlew.bat -p .\RGRemote assembleDebug
.\gradlew.bat -p .\RGRemote assembleRelease
```

Release APK output:

```text
C:\Workspace\Project_Android\RGRemote\app\build\outputs\apk\release\app-release.apk
```

## Signature Verification

Example with Android SDK build-tools 36.0.0:

```powershell
$env:ANDROID_HOME\build-tools\36.0.0\apksigner.bat verify --print-certs .\RGRemote\app\build\outputs\apk\release\app-release.apk
```

Expected for the temporary test build:

```text
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Signer #1 certificate DN: C=US, O=Android, CN=Android Debug
```

If the signer is not Android Debug, verify whether production signing has intentionally replaced the temporary config.

## Install Command

```powershell
adb install -r .\RGRemote\app\build\outputs\apk\release\app-release.apk
```

If install fails because a debug build is already installed with a different variant/signature, uninstall the local app only after confirming there is no data you need to preserve:

```powershell
adb uninstall com.rgremote.app
adb install .\RGRemote\app\build\outputs\apk\release\app-release.apk
```

## Production Signing Warning

Before any production, Play, public, or shared release:

1. Remove `signingConfig = signingConfigs.getByName("debug")` from the `release` build type.
2. Add a real release signing config using secure local or CI-provided credentials.
3. Keep keystore files, passwords, and `local.properties` out of git and docs.
4. Re-run `assembleRelease`.
5. Re-run `apksigner verify --print-certs` and confirm the signer is the intended production certificate.
6. Install on a clean device and run the release section of `QA_CHECKLIST.md`.

Do not ship an APK signed with `C=US, O=Android, CN=Android Debug`.
