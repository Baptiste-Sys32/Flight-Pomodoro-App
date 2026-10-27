# Native rebuild plan

Status: Phase 1 research and planning only, 2026-10-27. No Android app code or scaffold created. Phase 2 requires explicit user approval. This is a migration/rebuild of Flight-Pomodoro-App, preserving its Git history and existing files. The iOS app's verified design is the target; both Android APKs are supplementary comparison evidence.

## Findings from the current repository

At inspection, HEAD was `60c668f` and the working tree was clean. The history contains `3168294` (initial implementation/assets/backups), `f4749f7` (README simplification), and `60c668f` (README/license). No native project, Gradle wrapper, dependency lockfile, package manifest or test suite exists.

The current app is a 6,016-line HTML/CSS/JavaScript file. It imports Inter, Globe.gl 2.27.0, Three.js 0.160.0, fflate 0.7.3 and an older Three.js 0.137 FBX loader. Source: `index.html:9`, `index.html:2947`. README describes a browser flight timer; the code confirms that description. The two HTML backups contain alternate rendering experiments, not native implementations or evidence of FocusFlight parity.

| Existing behavior | Evidence | Rebuild decision |
|---|---|---|
| Airport search, nearest airport and suggestions | `index.html:3420`, `index.html:4877` | Retain field inventory/UX intent only; choose a licensed bundled airport dataset |
| Flight duration = distance / 850 km/h + 30 minutes | `index.html:3659` | Do not inherit as iOS truth; approve duration policy |
| Haversine distance and great-circle movement with flight phases | `index.html:3676`, `index.html:4111` | Reuse as mathematical references after edge-case tests; write native domain logic |
| Browser flight clock and separate Pomodoro clock | `index.html:3976`, `index.html:4027` | Replace with one persisted monotonic session clock |
| Completed/canceled history and statistics, capped at 50 | `index.html:4539` | Retain record fields; Room history without arbitrary 50-row loss |
| Notes | `index.html:5384` | Preserve concept; active note state needs durable storage |
| Window view with a separate Three.js scene | `index.html:5538` | Replace rendering; its scene is not geographically tied to route |
| Units and pass styles | `index.html:4673` | Metric default; optional nautical miles. Custom pass skins are not established source features |

Confirmed static problems: the Pomodoro toggle element is absent (`index.html:3940`, `index.html:3950`); speed changes multiply all elapsed time and cause progress jumps (`index.html:4027`, `index.html:5123`); active sessions are not persisted; multi-stop previews do not drive the actual interpolation or layover clock (`index.html:3906`, `index.html:4111`, `index.html:5301`). Multi-route confirmation can retain the prior duration (`index.html:5317`). Viewing history leaves the live check-in listener attached (`index.html:4608`, `index.html:5096`). Duplicate departure IDs exist (`index.html:2665`, `index.html:2827`). The airport filter rejects valid zero coordinates (`index.html:3436`). Imperial conversion uses statute miles (`index.html:5456`), while other history/pass labels remain kilometers. Map data requires network fetches (`index.html:3423`, `index.html:4258`). Some render loops continue outside focus (`index.html:3097`, `index.html:3196`).

Recommendation: replace the implementation for native Android; preserve the repository, useful mathematical references and data concepts. Keep the existing web prototype available for comparison until the native migration is accepted. Do not translate its UI wholesale. No asset provenance manifest exists for the FBX/MAX/C4D models, HDRs, textures or ZIP; the software MIT license does not establish ownership of every asset.

## Local tool inventory

| Tool | Observed state |
|---|---|
| JDK/Javac | OpenJDK 21.0.12.1, both execute |
| Gradle | No standalone PATH command/wrapper in this repo; cached 9.2.0 launcher executed successfully. Cached 8.14.3 and 8.7 also exist |
| Android SDK | `~/Android/Sdk`, platforms 33 and 36; build-tools 34.0.0, 35.0.0, 36.0.0; platform-tools 37.0.0 |
| SDK Manager | Installed off PATH, `cmdline-tools/latest/bin/sdkmanager`, executed version 20.0 |
| ADB | System PATH binary 34.0.4; home SDK also has newer platform-tools. Select one explicitly for tests |
| Native tools | NDK 28.2.13676358 and CMake 3.22.1 installed |
| Emulator | No home SDK emulator binaries/system images; saved API 36.1 AVD pointer targets a missing `.avd` directory. No usable emulator established |
| ffmpeg/ffprobe | Installed, ffmpeg 6.1.1; public recording extraction succeeded |
| REA | Installed CLI 6.4.0 executes; OpenCode registration exists but doctor calls it stale; current Codex session has no REA MCP tools |
| APK tools | Apktool 2.7.0-dirty executes. REA JADX method provider unavailable because its required headless MCP JAR 0.7.1 is absent |

No config repair was necessary to perform the successful CLI operations. OpenCode config was not edited, and unrelated engines were not installed. Before Phase 2 tests, use a connected device or install/configure an emulator/system image. Pin the project's Gradle/AGP/Kotlin/Compose versions together from their then-current compatibility docs; cached Gradle alone does not choose compatible plugin versions. Proposed minimum Android 10/API 29 and compile/target API 36 are provisional until the user's phone is known. Source app SDK values are not requirements for this rebuild.

## Public research coverage

Primary identity: [Cement Planet LTD iOS App Store listing](https://apps.apple.com/us/app/focusflight-deepfocus-timer/id6648771147). Its v2.0 release notes and description verify the requested route, record, card, random-flight and sound features. Version history verifies portrait/window views and nautical-mile support. Language metadata verifies all six requested languages. UI-specific evidence is organized in ui-spec.md.

The developer root [focus.flights](https://focus.flights) returned 404 during research; its [support page](https://focus.flights/support/) exists but supplies no product-flow detail. No contact was made. Search covered screenshots, reviews, videos, YouTube, Instagram and TikTok with the app/developer identity. No authenticated official social recording was established. Similar-name browser sites were excluded. App Store reviews are user opinions and feature requests, not proof of shipped functionality.

The [public ScreensDesign capture](https://screensdesign.com/apps/focusflight-deepfocus-timer/) was accessible from its normal player. A 27 MB, 14:57.985, 480 x 1038, 30 fps file was downloaded. ffmpeg extracted 898 images at 1 fps; broad sequences and detailed frames were visually inspected. Settings identify v2.0. Seven [showcase images](https://screensdesign.com/showcase/focusflight-deepfocus-timer) and six current App Store promotional compositions were also inspected. Selected evidence and its provenance are saved under `docs/references/`. The full temporary recording/analysis remains under `/tmp/flight-research`, not committed or an app asset. One-fps extraction cannot establish precise motion timing. Revisit the 30 fps clip when implementing each animation.

Two independently embedded YouTube demonstrations were located: [iPhone how-to](https://www.youtube.com/watch?v=kc2bQUqxv1U) and [iPad demonstration](https://www.youtube.com/watch?v=bWKTwMBZIl4). The web fetch was throttled; they were not watched and contribute no behavioral claims. Third-party paywalled screen indexes were not unlocked or treated as observed UI.

There are no local recordings. The user explicitly corrected that part of the initial prompt. The source recording ends without a full successful landing, and most analytics are empty. Exact membership thresholds, fully populated analytics, backup formats and strict blocking behavior remain unknown.

## Android comparison package investigation

Both XAPKs were inventoried; only local supplied artifacts were used. No APK was executed, patched or copied into app source. Their own assets, branding and embedded service configuration are excluded from the rebuild.

| Package | AirFlow 1.2.3 | FocusFlight: Focus Timer 2.8.9 |
|---|---|---|
| Application ID | `com.focus.flight.air.timer` | `com.focus.focusflight` |
| Minimum/target SDK | 29/35 | 32/37 |
| Structure | Flutter with Android flight service, alarm receiver and two widgets | Native Compose, notification/audio/usage-monitor services, blocking activity and widget |
| Map evidence | Google Maps Flutter integration | Bundled Mapbox 11.16.4; actual app globe selection unknown |
| Local data | Airport database | Airport CSV; database tables for history, categories and blocked apps |
| Timer static evidence | Native service uses wall-clock Date difference minus pause offset, clamped progress; notification uses elapsedRealtime chronometer | Stored start/pause state; clock-instant-derived progress and repository-driven remaining minutes |
| Blocking | No verified complete blocking implementation | Usage-event polling plus canDrawOverlays check and BlockedAppActivity path; no Accessibility service declared |
| Runtime limit | Supplied ABI armeabi-v7a | Supplied ABI arm64-v8a |

The [second package listing](https://www.appbrain.com/app/com.focus.focusflight) names Live Talk - Phone Number Tracker and Location as publisher. This does not match the iOS publisher; official-port status is not established. [AirFlow's store listing](https://play.google.com/store/apps/details?id=com.focus.flight.air.timer) is a separate comparison product. Neither can establish iOS design rules.

REA archive and resource operations succeeded:

- AirFlow archive: `ev_42da3dbef8e013a72a85137f251789bf84df64e0e2ba11735c29d5bbf053d3b1`.
- AirFlow resources: `ev_5b9c4f67000bd169a6bb3ddc1400373462d9b8c6933f5d9ad578e75fc9e2108a`.
- FocusFlight Android archive: `ev_69de24237f0e2d20a7b961f79debbebcb19f10eff9ca8ab61a19ae044f4cd801`.
- FocusFlight Android resources: `ev_ca7cc6be8e3d8393fcfcf02606919c30cca6f3981b3bb61175c4f883299d36b3`.

REA `inspect-android-package` failed with missing `REA_JADX_MCP_JAR`. Existing Apktool then supplied static smali evidence, explicitly separate from REA method analysis. Temporary references: AirFlow `airflow-smali/smali/net/air/focus/flight/FlightFocusService.smali:281` (progress), `:686` (chronometer), `:767` (callback); other package `focusflight-smali/smali/jr3.smali:20814` (elapsed calculation), `smali/ra.smali:5359` (usage events), `smali/ua.smali:6666` (overlay check), `smali/sy.smali:3685` (database schema). All paths are below `/tmp/flight-research` and may disappear. Raw evidence contains third-party configuration and must not be committed. Only safe conclusions are retained here.

These observations establish architecture and some methods. Full ordered screen flow, timer behavior after reboot/force-stop, map camera behavior and blocking reliability remain unverified. Resource filenames alone were not accepted as proof of those flows.

## Proposed stack and organization

Kotlin, single Activity, custom Jetpack Compose layouts, lifecycle-aware ViewModels/Flow and Navigation Compose. Material 3 provides appropriate primitives and accessibility behavior; it must not impose a generic visual theme. Room stores relational session data; DataStore stores small preferences. Media3 manages audio independently of the timer. Glance renders widgets. A timer foreground service plus completion alarm supports background status, with the persisted state as authority.

Use a renderer boundary to avoid coupling the session engine to map SDK lifecycle. Google Maps 3D and Mapbox Native are candidates; no provider is selected yet. A WebView-based globe would require an explicit change to the native goal. No backend, account, billing or analytics is proposed.

Proposed structure, not created:

```text
android/
  gradle/                         pinned wrapper and version catalog
  app/src/main/
    java/<owned.package>/
      app/                        Activity, navigation, dependency wiring
      domain/session/             state machine, clock, progress, route math
      data/local/                 Room entities, DAOs, migrations, repositories
      data/preferences/           DataStore preferences
      platform/timer/             FGS, alarm, notification, boot reconciliation
      platform/audio/             MediaSessionService and audio catalog
      platform/blocking/          optional chosen intervention and DND
      platform/widgets/           Glance widgets and state projections
      platform/backup/            explicit local import/export
      map/                        renderer interface and selected adapter
      ui/components/              ticket, seat, capsules, panels, icon buttons
      ui/screens/                 setup, booking, flight, log, Mine, trends, settings
      ui/theme/                   measured reference tokens and owned fonts
    res/                          owned resources and six locales
    assets/                       licensed airport catalog, plane and sound assets
  app/src/test/                   clock/state, route and persistence rules
  app/src/androidTest/            recovery, navigation and meaningful UI checks
docs/                             specs, decisions, asset provenance, evidence
index.html and existing assets    preserved web prototype until migration accepted
```

Start with packages inside one app module; split modules only if complexity warrants it. Do not add an elaborate framework before the app exists. Choose a package ID from the user's app identity before scaffolding.

## Session and data design

Proposed states: draft, boarding, takingOff, cruising, paused, landing, completed, canceled. Store a session ID, endpoint snapshot/geometry, category snapshot, requested/planned duration, start/deadline, accumulated pause, boot identifier, status, note and distance credit. Exact takeoff/landing time boundaries require approval. Keep real focus time distinct from decorative flight phases.

Within one boot, derive time and plane position from elapsedRealtime. UTC timestamps exist for log display/reboot recovery. Persist before starting platform work. On resume, recompute from authority rather than restoring an animation counter. Pause freezes both timer progress and route; completion transitions and awards are one transaction. Cancellation never accidentally awards completion. Reboot/force-stop behavior must be explicit. Metric storage uses meters; display conversion to nautical miles uses exactly 1852 meters per nmi.

Membership thresholds and route duration formulas are configurable domain policies awaiting approval, not guessed replicas. Room queries provide filter/sort/aggregate behavior. Widgets and notifications read session projections; they never become independent clocks. Audio focus loss does not pause study unless the user-approved behavior explicitly says so.

## Milestones in proposed value order

Every milestone requires a build and emulator/device validation before its commit. Commit only relevant files after secret-ignore and tracked-content checks. Do not create releases or publish. Existing code remains intact until a concrete migration is accepted.

1. **Approve scope, then establish a runnable native shell.** Choose identity/assets and device/API target. Pin compatible tool versions, configure emulator or device, and run a minimal Activity. Deliver a reproducible debug build and device screenshot. This is Phase 2, not part of this change.
2. **Prove timer reliability.** Room session engine, monotonic clock, pause/resume/cancel, FGS notification and completion alarm. Test real elapsed time, deep sleep/background, process death, duplicate completion, notification/exact-alarm denial, clock changes and reboot policy. No map dependency. Commit after tests run.
3. **Resolve renderer feasibility early.** Compare globe-to-city camera, aircraft, elevated route, antimeridian, style/label switching, attribution, offline failure and battery on device. Record cost/config requirements and select a provider with approval of any tradeoff. Do not postpone a globe limitation until the UI is built.
4. **Match boarding flow.** Starting airport, home, duration ruler, destination cards, seat/scenario grid, ticket and takeoff. Compare screenshots against linked iOS frames using owned assets. Test cancellation/back/denied location. Confirm requested/planned duration semantics.
5. **In-flight feel and maps.** Connect plane, route and camera to session progress; add matched utility groups, map picker, window scenes and pause dimming. Inspect transitions against 30 fps reference. Verify time consistency during rotation, view changes and background/return. Implement approved landing substitute if original remains unknown.
6. **Records and exploration.** FlightLog pass/notes, state/scenario filters, sort, route replay, World and visited-airport summaries. Add trends only with explicit aggregation policies and approved populated-chart design. Test migrations and no duplicate/lost records.
7. **Membership and audio.** Owned card collection/reveal and approved progression, then licensed sound library/panel, volume and audio lifecycle. Validate award idempotency and background/headphone/call interruption.
8. **Blocking.** Optional DND, then actual-device comparison of intervention mechanisms. Choose one app-blocking strategy and disclose limits in settings. Website coverage is its own acceptance gate. Test revocation, split screen, excluded system surfaces and timer independence.
9. **Widgets and remaining settings.** Glance variants, reminders, awake policy, units, backup/import, optional icons and approved StandBy substitute. Use [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider) for user-selected backup files. Verify launch/resize/permission behavior and consistent units in every surface.
10. **Localization and full fidelity pass.** All six languages, CJK font coverage, accessibility/font scaling, supported orientations, offline/degraded states and end-to-end flights. Require each feature-map ID to work or have an approved substitute. Run final build and regression checks, then a final device review. No “done” claim based on compilation alone.

## Validation and primary API references

Meaningful automated tests: deterministic session state/clock and pause logic, exactly-once completion, antimeridian and zero-coordinate routes, unit conversion, Room queries/migrations and permission-state transitions. Device checks: actual service lifecycle and alarms, audio interruptions, blocking, map performance, widgets and end-to-end navigation. Visual checks compare the same state/viewport with reference images, with known asset/system differences recorded. Do not create tests that merely mirror layout implementation.

Current official docs read for planned APIs: [Compose state](https://developer.android.com/develop/ui/compose/state), [architecture](https://developer.android.com/develop/ui/compose/architecture), [View interop](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose), [animations](https://developer.android.com/develop/ui/compose/animation/introduction), [Navigation](https://developer.android.com/develop/ui/compose/navigation), [accessibility](https://developer.android.com/develop/ui/compose/accessibility), [Room](https://developer.android.com/training/data-storage/room), [DataStore](https://developer.android.com/topic/libraries/architecture/datastore), [Glance](https://developer.android.com/develop/ui/compose/glance), [Media3](https://developer.android.com/media/media3/session/background-playback), [app languages](https://developer.android.com/guide/topics/resources/app-languages). Timer, blocking, notification and map references are linked beside their limits in the other docs. Recheck implementation-specific API and dependency compatibility after approval rather than treating a planning document as a lockfile.

## Approval decisions and unresolved evidence

Required before corresponding work:

- App name/package identity; where the user's owned fonts/icons/images/audio are, or approval to create/select replacements. None have been supplied yet.
- Phone model and Android version, or approval to install a suitable emulator. APK ABI constraints are distinct from the rebuilt app's target ABI.
- Mapbox/Google Maps 3D account and cost preference. Current docs do not justify promising free satellite/globe forever.
- Approve private/local substitutes for commerce/account, iCloud, StandBy and original support links. Decide whether launcher icons, backup, trends and newly observed extras are in required scope.
- Choose best-effort app intervention coverage, and website filtering approach/limitations. No OS-level Screen Time parity claim.
- Approve duration/route relationship, session-start boundary, pause/reboot credit and membership rules if more iOS evidence cannot establish them.
- Approve a landing/completion design only after reviewing a concrete proposal against known composition. Do not silently invent a finished screen in Phase 2.
- Decide whether existing browser localStorage history needs migration. No history export has been supplied; the repo itself contains no user session database.

Phase 1 completed with bounded gaps: local repo/tool audit, public research, both supplied package analyses, visual reference capture and four planning docs. REA method provider and Android runtime remain unavailable; these are explicitly reported, not presented as verified. Pause here for approval. No Phase 2 work is authorized by this document.
