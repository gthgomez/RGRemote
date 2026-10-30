# Showcase capture status

Source inspected: [`d905c76d9bce13ee344cf2286fe4041d0e980e8e`](https://github.com/gthgomez/RGRemote/tree/d905c76d9bce13ee344cf2286fe4041d0e980e8e).
Inspection date: 2026-10-30. This is a source/documentation pass, not a recorded app run.

## Evidence available

No verified runtime screenshots or clips are checked into this repository at the
inspected revision. No new runtime media was created for this pass.
Images under `docs/reference/` are UI design targets only. They do not prove that
the app rendered those screens or controlled a TV.

## Execution blocker

The selected cloud environment has JDK 21 but no Android SDK installation,
`ANDROID_HOME`/`ANDROID_SDK_ROOT`, `adb`, `emulator`, `sdkmanager`, `/dev/kvm`,
or attached Android device. This repository’s existing JDK 17 setup and Android
prerequisites were inspected rather than replaced with a large toolchain download.

Required next setup: JDK 17, Android SDK platform 36, an Android 8.0+ phone, and a Roku/Google TV device on the same LAN with multicast allowed. Hardware testing also requires the TV’s local-control setting and explicit pairing.

No Android build, discovery, pairing, command delivery, HDMI handoff, or physical-TV check was executed for this showcase.

## Capture a genuine demonstration

Capture the unpaired/offline UI first. For end-to-end evidence, use an authorized test TV, pair it on the same LAN, record a navigation action on both phone and physical TV, and label HDMI state as an inference. Redact IP addresses, pairing PINs, and device identifiers.

After installing and manually opening the debug build on the test device, record
screens using Android's actual display output (requires Android platform tools):

```sh
mkdir -p docs/showcase
adb exec-out screencap -p > docs/showcase/screen.png
# Stop recording with Ctrl-C after the short walkthrough.
adb shell screenrecord --time-limit 20 /sdcard/showcase.mp4
adb pull /sdcard/showcase.mp4 docs/showcase/demo.mp4
adb shell rm /sdcard/showcase.mp4
```

Inspect every frame before publication. Keep the clip short and compress it for
GitHub; do not add large model/APK/build/ROM/save files. Add captions identifying
source commit, build variant, device/Android version, fixture origin, and which
steps were really exercised. Scripted UI states and design references must stay
labeled separately from device and end-to-end evidence.

## Source and verification references

[Project context](../../PROJECT_CONTEXT.md), [UI design references](../ui-target.md), and [hardware QA checklist](../../QA_CHECKLIST.md).

This pass changes README/capture documentation only. Local paths and links are
checked; Android Gradle, connected, release, and hardware gates remain unrun.
