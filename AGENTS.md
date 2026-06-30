# AGENTS.md — RGRemote (Gemini 3 Flash Override)

> Inherits from root [AGENTS.md](file:///C:/Workspace/Project_Android/AGENTS.md). General guidance is in [CLAUDE.md](./CLAUDE.md).

## Gemini-Specific Risks
- Hallucinated Roku ECP HTTP endpoints — verify against Roku ECP documentation (port 8060)
- Incorrect SSDP/NSD discovery service types — Roku and Google TV use different discovery protocols
- Protocol isolation breach — Google TV code must stay in `google/` package; a Google TV break must not affect Roku
- Hallucinated Room migration syntax — migrations must be explicit and non-destructive to user pairing data
- Incorrect TLS/Keystore usage for self-signed Google TV certificates

**Verification gate:** `./gradlew assembleDebug`
