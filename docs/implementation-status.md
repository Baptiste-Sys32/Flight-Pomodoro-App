# Implementation status

Phase 2 approved by the user on 2026-10-27. Work is in progress. No full-feature completion or design-parity claim is made.

## Confirmed decisions

- App name: Focus Flight - A timer for geeks. Package: personal.focusflight.
- Target device: Galaxy S24 Ultra. User described Android 16; ADB reports Android release 17, SDK 37. Validate the connected system and an API 36 emulator.
- Free map usage is mandatory; API keys are acceptable. Terrain and 3D buildings are preferred.
- Mapbox Native is the first renderer candidate. Its current mobile tier is free up to 25,000 MAUs. Only Maps SDK resources are intended; no paid navigation/search API. Requires the user's own account/public access token.
- No production assets were supplied. Original vector placeholders are used; no source-app assets are shipped.

## Timer policies used for the first implementation

- Focus begins when the user starts the session, independent of the rendering frame loop.
- Paused time earns no focus or distance.
- Same-boot time uses elapsedRealtime. Reboot recovery uses UTC, clamped to avoid adding time if the wall clock moved backwards. This fallback cannot defeat deliberate clock manipulation across reboot.
- Only completed sessions earn distance and focus credit. The completion transaction is idempotent.
- If a cancellation races after the deadline, the completed session is preserved.
- Exact-alarm denial falls back to an inexact alarm. Correct elapsed state remains recoverable, but precise background notification timing is degraded.
- A boot receiver reconciles and schedules an alarm; it does not start an unrequested foreground service from boot.

## Validation ledger

| Check | Result |
|---|---|
| Gradle debug app and instrumentation APK | Built successfully with AGP 8.13.2, Gradle 8.14.3, Kotlin 2.2.21, SDK 36 |
| Clock/geography unit tests | 10 passing, including wall-clock changes, pause, reboot fallback, landing timestamp recovery, date-line and antipodal routes, zero coordinates and nautical-mile conversion |
| S24 Ultra, Android 17/API 37 | 5 passing persistence/service tests; 15-second flight paused, resumed and completed with correct credit |
| API 36 emulator | 6 passing tests, including visible Compose start/pause/resume/cancel controls |
| Exactly-once completion | Ten concurrent callbacks produced one completed/credited record |
| Local airport catalog | 4,569 records; coordinate range, reference-location and unique-code checks passed |
| Original window artwork | RGBA and transparent aperture checked; prompt/provenance recorded |
| Secrets and ignores | Local map configuration, generated BuildConfig, APKs and build directories are ignored; credential is not committed |

The temporary timer screen was visually inspected on the S24 Ultra. It is a clock/service milestone, not the final iOS-matched UI. The initial Compose test attempt on API 37 failed inside Espresso's removed InputManager.getInstance dependency; the service harness works there, while the Compose smoke test is restricted to API 36 and below. No app behavior is declared verified by that framework failure.

## Pending

- Mapbox credential is configured locally; renderer behavior, including elevated routes on globe, is not yet tested.
- Owned/selected fonts, window/sky imagery, plane model and licensed audio.
- Landing and membership progression policy review, since exact iOS behavior is unknown.
- All remaining feature-map entries and Samsung-specific permission/launcher checks.
