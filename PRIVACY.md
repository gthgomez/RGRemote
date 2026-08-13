# RGRemote Privacy Policy

> **Play Store URL placeholder:** `TODO_PRIVACY_URL`

## Summary

RGRemote is a **local-network-only** TV remote. Control traffic stays on your home LAN; the app does not require an account.

## Data collection

- **No account required.** RGRemote does not create user accounts or collect personal identifiers.
- **No cloud backend.** Device discovery and remote commands are sent directly to TVs on your local network. RGRemote does not transmit your data to our servers.
- **No third-party analytics or ads** in the configuration described here.

## Network access

- **Local discovery only.** The app scans your Wi-Fi/LAN subnet to find Roku TVs (SSDP) and Google TV boxes (NSD/mDNS).
- **Nearby Wi-Fi devices (Android 13+).** RGRemote requests `NEARBY_WIFI_DEVICES` with the `neverForLocation` flag so Android can scan for TVs on the LAN. This permission is **not** used for location tracking.
- **No location permission.** RGRemote does not request fine or coarse location access.

## Data storage

- Saved TV endpoints, HDMI mappings, and pinned app shortcuts are stored locally in a Room database on your device.
- Google TV pairing client certificates and private keys are generated in **Android Keystore** on this phone—not as readable PEM files in app storage.
- Server certificate fingerprints captured during pairing are stored locally to pin TLS connections to the TV you paired with.

## Backup

- Android Auto Backup is **disabled** for this app so pairing keys and device metadata are not copied to cloud backup providers by default.

## Erase data

Remove saved TVs from **Settings → Saved TVs**, or clear the app's storage from Android system settings to delete all local device data and pairing credentials.

## Contact

Full policy URL: `TODO_PRIVACY_URL` (to be published before Play Store release)
