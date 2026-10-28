# Implementation status

Phase 2 approved by the user on 2026-10-27. Work is in progress. No full-feature completion or design-parity claim is made.

## Confirmed decisions

- App name: Focus Flight - A timer for geeks. Package: personal.focusflight.
- Target device: Galaxy S24 Ultra. User described Android 16; ADB reports Android release 17, SDK 37. Validate the connected system and an API 36 emulator.
- Free map usage is mandatory; API keys are acceptable. Terrain and 3D buildings are preferred.
- Mapbox Native is the first renderer candidate. Its current mobile tier is free up to 25,000 MAUs. Only Maps SDK resources are intended; no paid navigation/search API. Requires the user's own account/public access token.
- No production assets were supplied. Original vector placeholders are used; no source-app assets are shipped.

## Approved local substitutions

Approved by the user on 2026-10-28: the selected focus duration is exact; route candidates suit that duration. Membership progression derives only from completed focus time and simulated distance. Thresholds will be documented as local rules. Local backup/import replaces iCloud. Purchases, accounts, referrals, Shared Flights and Magazine are omitted.

## Timer policies used for the first implementation

- Focus begins when the user starts the session, independent of the rendering frame loop.
- Paused time earns no focus or distance.
- Same-boot time uses elapsedRealtime. Reboot recovery uses UTC, clamped to avoid adding time if the wall clock moved backwards. This fallback cannot defeat deliberate clock manipulation across reboot.
- Only completed sessions earn distance and focus credit. The completion transaction is idempotent.
- If a cancellation races after the deadline, the completed session is preserved.
- Exact-alarm denial falls back to an inexact alarm. Correct elapsed state remains recoverable, but precise background notification timing is degraded.
- A boot receiver reconciles and schedules an alarm; it does not start an unrequested foreground service from boot.
- Ongoing notification progress refreshes every 1-5 seconds depending on flight length; short diagnostic sessions receive intermediate updates. The native chronometer displays the countdown between reposts.

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

User-reported notification progress issue: a fixed 15-second repost interval gave the 15-second diagnostic flight no intermediate update. Replaced with an adaptive 1-5 second interval. The device regression captures the active notification progress and requires a later increase of more than 20/1000 before completion. Passed on API 36 and the S24 Ultra. An earlier phone run refused to create a second flight while the user's session was active; no cancellation or overwrite was performed.

## Map and flight-screen milestone

- Native Mapbox globe, Standard/Monochrome/Terra/Satellite choices, labels, DEM terrain and 3D-building controls. The style names follow the observed menu; map cartography and Terra's night styling are provider substitutes, not pixel-identical Apple Maps.
- Actual session progress drives the great-circle route, aircraft and remaining distance. Follow view and route overview share the persisted clock. Pausing dims/blurs the scene without advancing progress.
- Original procedural glTF aircraft, original generated window and cloud plate, and independently licensed Inter font. Map attribution stays visible.
- A non-indexed mesh triggered a native Mapbox worker crash in the emulator. Explicit 16-bit indices fixed the observed failure. LOCATION_INDICATOR rendering makes the model visible above buildings; placement and orientation were checked in a river scene.
- The renderer test waits for the fully loaded map, not just accepted style JSON. Passed on the S24 Ultra. Globe and detailed Paris 3D buildings were visually inspected there; the original window and aircraft were inspected in the API 36 emulator.
- The revised API 36 UI regression starts a diagnostic flight, pauses, resumes, confirms cancellation and returns to the ready screen. Passed after connecting the flight screen.

The ready screen is still temporary. The boarding flow, source-matched home, audio controls, logs and other feature-map entries remain pending. Elevated globe arcs, complete style/terrain combinations, antimeridian rendering, animation fidelity, offline cache behavior and battery measurements are not yet verified.

## Pending

- Elevated route geometry on globe and the broader renderer acceptance checks above.
- Licensed or original audio.
- Landing choreography; local membership rules are now approved and documented in local-rules.md.
- All remaining feature-map entries and Samsung-specific permission/launcher checks.
