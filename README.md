# RGRemote

Personal-use Android remote for a TCL Roku TV plus an Onn Google TV box.

## Docs Map

- `PROJECT_CONTEXT.md` - repo scope, architecture, file map, and invariants.
- `AGENTS.md` - cold-start routing, high-risk zones, and local commands for coding agents.
- `STATUS.md` - active project status and evidence-backed capability breakdown.
- `docs/ui-target.md` - Roku-side visual target and Google TV separation rules.
- `docs/release-testing.md` - debug-signed release testing workflow and production signing warning.
- `QA_CHECKLIST.md` - build, visual, pairing, install, and regression checklist.

## Scope

- Roku is the stable control plane: SSDP discovery, ECP HTTP commands on port 8060, app launch, active-app status, volume, power, and HDMI input switching.
- Google TV is isolated behind the experimental Android TV Remote Protocol v2 adapter: mDNS discovery, Android Keystore-backed client certificate, PIN pairing flow, TLS socket framing, navigation keys, volume, power, and deep-link launch.
- Input visibility is not automatic. The app only infers the likely TV input from Roku `query/active-app` when it returns a `tvinput.hdmiX` app id.

## First-run checklist

1. Roku TV: set **Settings > System > Advanced system settings > Control by mobile apps > Network access** to Enabled or Permissive.
2. Phone and TVs must be on the same subnet with multicast allowed.
3. Use the app scan button to discover Roku via SSDP.
4. Keep Google TV discovery running and pair with the 6-character PIN shown on the TV.
5. Map the Onn box to its Roku HDMI port before using **Watch Google TV**.

## Reality notes

- Roku ECP is documented by Roku, but Roku OS upgrades can restrict local control.
- Google TV Remote v2 is reverse-engineered and undocumented by Google. It is intentionally contained in `google/` so a protocol break does not affect Roku control.
- Neither Roku ECP nor Google TV Remote v2 can prove which HDMI source is visible on the physical panel.
- The release APK is temporarily signed with Android debug credentials for local release testing only. See `docs/release-testing.md` before sharing or production use.

## Verification

Run from the repository root:

```powershell
.\gradlew.bat assembleDebug
```

## Privacy

RGRemote collects no data: no analytics, no ads, no cloud backend. See
[PRIVACY.md](PRIVACY.md).

## License

Licensed under the [Apache License 2.0](LICENSE). Copyright 2026
Jonathan Gomez Aguilar.
