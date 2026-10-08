# RGRemote Privacy Policy

**Effective date:** October 8, 2026

This policy applies to the RGRemote Android application ("RGRemote", "the app")
distributed under [Apache License 2.0](LICENSE). In short: **RGRemote is a
local-network-only TV remote. It has no servers, no accounts, and collects
nothing.**

## Data collection

- **No data collection.** RGRemote does not collect, transmit, sell, or share
  personal data. There is no analytics SDK, advertising SDK, or crash-reporting
  SDK in the app.
- **No account required.** The app has no sign-in and creates no user profile.
- **No cloud backend.** Device discovery and remote-control commands are sent
  directly from your phone to media devices on your local network. The
  developer operates no servers that could receive this traffic.

## Network access

- **Local discovery only.** The app scans your Wi-Fi/LAN subnet to find Roku
  TVs (SSDP) and Google TV devices (NSD/mDNS), then speaks to them directly
  (Roku ECP over HTTP on port 8060; Android TV Remote Protocol v2 over TLS).
- **Nearby Wi-Fi devices (Android 13+).** RGRemote requests
  `NEARBY_WIFI_DEVICES` with the `neverForLocation` flag so Android can scan
  for TVs on the LAN. This permission is **not** used for location tracking.
- **No location permission.** RGRemote does not request fine or coarse
  location access.

## Data storage

Everything the app remembers stays on your device:

- Saved TV endpoints, HDMI mappings, and pinned app shortcuts are stored in a
  local Room database.
- Google TV pairing client certificates and private keys are generated inside
  **Android Keystore** and never leave the device in readable form.
- Server certificate fingerprints captured during pairing are stored locally
  to pin TLS connections to the TV you paired with.

## Backup

Android Auto Backup is **disabled** (`allowBackup=false`), so pairing keys and
device metadata are not copied to cloud backup providers.

## Data deletion

Remove saved TVs from **Settings → Saved TVs**, or clear the app's storage
from Android system settings, to delete all local device data and pairing
credentials. Uninstalling the app removes everything it stored.

## Children

RGRemote is a general-audience utility and does not knowingly collect any data
from anyone, including children under 13.

## Contact

Questions or concerns: open an issue at
<https://github.com/gthgomez/RGRemote/issues>.
