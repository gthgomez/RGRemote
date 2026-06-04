# RGRemote UI Target

Last updated: 2026-05-15.

This document captures the active visual direction for RGRemote. The reference image is the May 13, 2026 neon/glass Roku remote mockup supplied by Jonathan. If the image file is unavailable, use the criteria below as the durable target.

## Product Boundary

Roku-side UI and Google TV UI are different modes.

- Roku mode should feel like the primary premium remote surface: neon purple, black glass, dense control grouping, etched circuitry, quick Roku shortcuts, HDMI tiles, and a launch panel.
- Google TV mode should stay paired-device focused. It can share shell language and polish, but it should not inherit Roku-only shortcuts or imply Roku HDMI control from the Google adapter.

## Target Feel

- Dark glass base with thin violet outlines, small internal highlights, and restrained glow.
- Dense, functional controls rather than oversized marketing panels.
- Clear Roku selected state in the device header and remote card.
- Main remote surface visible early on the screen, with quick actions reachable below it.
- Bottom nav is slim, segmented, and visibly connected to the app shell.
- Use accent color sparingly: violet for active remote state, green for online/power success, red only for destructive/power-off emphasis.

## Current Reference Assets

Existing local visual pass screenshots:

- `visual-pass-rgremote-roku-s25.png`
- `visual-pass-rgremote-small-roku.png`
- `visual-pass-rgremote-roku-font-1_3.png`
- `visual-pass-rgremote-font-1_3.png`

These screenshots are evidence of previous passes, not the final target. Re-check them after UI changes instead of assuming they still match the code.

## Layout Requirements

- Small phones must fit the header, Roku remote surface, and bottom nav without text overlap.
- Normal phones should show the header, remote surface, first quick-action row, and bottom nav in a coherent vertical flow.
- Controls must keep stable dimensions during press, disabled, loading, and font-scale states.
- Quick actions should include HDMI tiles, Roku app shortcuts, manual channel/deep-link launch, and customization affordance.
- Cards should use tight radii and slim strokes. Do not stack decorative cards inside decorative cards.

## Visual QA

Before calling a UI pass done:

- Capture or inspect normal phone and small phone layouts.
- Check font scale around 1.3x.
- Confirm buttons and labels do not overlap or resize their containers.
- Confirm Roku and Google TV selected states remain visually distinct.
- Confirm quick actions are usable, not just decorative.
- Confirm disabled/unpaired Google TV states do not look like successful Roku states.

## Known Gaps To Watch

- The reference target has richer micro-detail, etched texture, and ambient glows than the current Compose implementation.
- Current visual QA is screenshot/manual driven; there is no automated screenshot test suite yet.
- Hardware state can affect available controls, so visual states should be checked with both paired and unpaired device data.
