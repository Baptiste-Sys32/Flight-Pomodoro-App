# Feature evidence and Android mapping

Phase 1, researched 2026-10-27. This retains the Phase 1 evidence baseline. Current build/test status is in [implementation-status.md](implementation-status.md); approved substitutions are in [local-rules.md](local-rules.md). D = documented by the [iOS App Store](https://apps.apple.com/us/app/focusflight-deepfocus-timer/id6648771147); V = visible in the [iOS capture](https://screensdesign.com/apps/focusflight-deepfocus-timer/); U = unknown. See ui-spec.md for individual visual references. APK findings are comparison evidence only. Android mechanisms are proposed architecture, not proof of working parity.

## Required platform mappings

| Feature | Source verification | Proposed Android equivalent | Acceptance and open issue |
|---|---|---|---|
| App blocking | D, V configuration | UsageStatsManager plus overlay is the starting candidate; compare an Accessibility service intervention. Optional DND policy access with app-owned AutomaticZenRule | Demonstrate on the actual phone; permissions reversible; never claim suspension or tamper-proof blocking. Site blocking is separate |
| Live Activity | V settings/guide | Ongoing foreground-service notification with countdown and progress; API 36 ProgressStyle where supported | Notification opens active session and remains consistent after recovery. Promoted Live Update eligibility U |
| Widgets | V guide and v2 announcement | Jetpack Glance with persisted state and event updates | Route/session and membership variants; resize and tap tested. Exact original variant inventory U; no continuous 3D widget |
| 3D map and globe | D, V globe and map | Evaluate standard Google Maps, Google Maps 3D and MapLibre Native; add Mapbox Native candidate | Standard Google Maps tilt is not established globe support. MapLibre Native lacks globe. Mapbox/Google 3D must pass renderer spike before selection |
| Audio | D, V sound panel | Media3 ExoPlayer and MediaSessionService, royalty-free or owned bundled audio | Loop/focus/headphone/background tests; license manifest; mixing behavior U |
| FlightLog and membership | D, V log and initial card | Room sessions/routes/categories/notes plus transactional progression | Completed session credited once; filters work. Tier thresholds and upgrade rules U |

## Complete feature inventory

| ID | Feature | iOS evidence | Native proposal / fidelity requirement | Approval or missing evidence |
|---|---|---|---|---|
| F01 | Boarding, takeoff, cruise, landing | D; V up to active/canceled flight | Explicit persisted state machine, matched pass/seat/takeoff screens | Completed landing layout/motion U |
| F02 | Task and duration on pass | D, V booking | Scenario/task, requested and planned durations, selected route, ticket metadata | Duration-to-route mapping and bounds U |
| F03 | Seat selects focus type | D, V seat map | Custom Compose cabin grid and owned pictograms; four required types: study, work, reading, creative | Full seat mapping U |
| F04 | Starting airport, city search | V starting panel | Bundled licensed airport catalog; manual choice, optional foreground location, random start | Search ranking and data license to select |
| F05 | Route selection by duration | V cards/ruler | Great-circle candidates with explicit duration policy and deterministic selection | No adoption of prototype formula without approval |
| F06 | Airplane Mode | D, V | Permission-led blocking configuration per scenario; optional DND | Android substitute in limits.md |
| F07 | Website blocking | D | Supported-browser Accessibility prototype or local VPN/domain filter, separately scoped | Neither is universal Screen Time parity; choose coverage explicitly |
| F08 | Plane advances across world | D, V | Render position from the same session progress as timer and notification | No background render loop; pause is consistent |
| F09 | Map modes 3D/satellite/classic | D; V four style names | Native Standard, Monochrome and Terra, labels toggle, terrain/buildings where available | Satellite explicitly omitted by the user on 2026-10-28 |
| F10 | Whole-earth and 3D route history | D, V globe | Store route endpoints/geometry and render history on globe | Elevated globe arcs require spike, Mapbox lineZOffset restriction |
| F11 | Random route, destination discovered at landing | D; V introduction only | Persist one selected route but conceal destination until completion | Selection algorithm/reveal layout U; no midflight reroll |
| F12 | FlightLog fields | D, V | Endpoint city/code/coordinates, start/end, duration, distance, status, seat, category, notes | Exact detail/share/delete UI U |
| F13 | History filter and sort | V | Room queries by state/category/date with stable ordering | Full sort list U |
| F14 | Membership unlocks/upgrades | D; V initial card | Local progress and owned collectible cards, derived from completed sessions | Tier names, thresholds and miles formula U; approve substitute rules |
| F15 | White noise, music, airplane ambience | D, V | Licensed local library; panel with switch/volume/selected tile | Original audio and mixing rules unavailable |
| F16 | Boarding-pass record, flight data, route and duration | D, V ticket | Room session snapshot, no dependency on mutable airport labels | Preserve notes and data across schema migrations |
| F17 | Portrait focus view | D version history, V | Custom Compose in-flight view | Behavior on orientation changes tested |
| F18 | Airplane-window view | D, V | Original window/cloud visual and native flight HUD | Satellite-window scene omitted by the user on 2026-10-28 |
| F19 | Kilometer and nautical-mile units | Nautical mile D version 1.5.17; units setting V | Default km; optional nmi; canonical meters in storage, nmi = meters / 1852 | Unit formatting everywhere including cards and widgets |
| F20 | Six languages | D language metadata | Android resources: en, de, ja, ko, zh-Hans, zh-Hant; platform per-app languages | Own translations; CJK font selection and review |
| F21 | Pause/resume and give up | V | Persist pause offset and cancellation; all views share state | Source pause credit and cancellation dialog U |
| F22 | Custom scenarios with icon/color/name | V editor | Room categories; owned icon catalog and accessible color choices | Category limit, editing/deletion semantics U |
| F23 | Notes with record | V promotional Notes card | Room per-session notes | Actual note editing UI U |
| F24 | Trends, date ranges, chart settings | V empty dashboard/settings | Room aggregates, same panel hierarchy and intervals | Populated charts and streak/threshold formulas U |
| F25 | World statistics, visited airports | V World cards | Derived distance/airport aggregates | Counting and circumference rules U |
| F26 | Reminders | V time wheel/settings | AlarmManager notification, optional permission; separate from session alarm | Recurrence rules U |
| F27 | Screen awake and sound/haptics settings | V | Activity window keep-awake during visible focus only; Compose/platform haptics | Automatic mode and original haptics U |
| F28 | Map labels/home style | V | Persist style/labels in DataStore | Ensure attribution remains visible |
| F29 | App-icon choices | V | Optional activity-alias launcher icons with owned artwork | Launcher behavior varies; exact catalog U |
| F30 | Database backups | V settings row | Local Storage Access Framework export/import | Original format U, no compatibility claim |
| F31 | iCloud sync | V settings row only | Proposed local-only storage and explicit backup | Omission/substitute needs approval; no cloud service inferred |
| F32 | StandBy | V guide row only | Optional landscape/dim focus view | Android has no identical system mode; approve substitute |
| F33 | Account, purchases, restore, referrals/support | V settings/paywall | Proposed removal of commerce and original links, local profile/reset instead of account | Private-build scope decision, not implicit parity |
| F34 | Shared Flights | V feature announcement only | No implementation promised | Operational feature U; scope requires decision |
| F35 | Magazine | V upcoming announcement | No implementation promised | Upcoming/content feature U |

Automatic alternating Pomodoro breaks are not established as iOS behavior. An App Store review requests meal breaks; that is not proof of a shipped feature. Do not add break cycles, ambient endless flights, speed controls or multi-stop routes merely because the old prototype contains them. Those are separate product decisions.

## Map decision

| Candidate | Globe and routes | Styles and integration | Limits / decision |
|---|---|---|---|
| Standard Google Maps SDK | Tilt/buildings supported; no verified whole-earth globe API | Native MapView, normal/satellite/hybrid/terrain | Does not satisfy globe requirement alone. [Configuration](https://developers.google.com/maps/documentation/android-sdk/configure-map) |
| Google Maps 3D Android | Global camera range, camera animations, polylines and custom models documented | Native Map3DView via AndroidView; satellite/hybrid and experimental roadmap | SDK GA 16.0.1 released Oct 14 2026; billing/key/Play services required. Usage uncharged only through Nov 13 2026 according to current docs, then Immersive Maps SKU. [Release notes](https://developers.google.com/maps/documentation/maps-3d/android-sdk/release-notes), [camera](https://developers.google.com/maps/documentation/maps-3d/android-sdk/configure-camera-controls), [modes](https://developers.google.com/maps/documentation/maps-3d/android-sdk/map-modes), [billing](https://developers.google.com/maps/documentation/maps-3d/android-sdk/usage-and-billing) |
| MapLibre Native | Official roadmap says globe not available for Native; Mercator maps remain useful | Custom style and independently licensed vector/raster sources | Free renderer is not free satellite data. GL JS globe in a WebView would be an explicit hybrid substitute requiring approval. [Roadmap](https://maplibre.org/roadmap/maplibre-gl-js/globe-view/) |
| Mapbox Native | True globe; official Compose example animates an aircraft and follow camera | Native View or Compose; custom styles and satellite | Elevated lineZOffset explicitly unsupported on globe. Needs alternative geometry spike. Own token/account, current MAU pricing, attribution and telemetry opt-out requirements. [Projection](https://docs.mapbox.com/android/maps/guides/projections/), [aircraft example](https://docs.mapbox.com/android/ja/maps/examples/compose/animated-3d-model/), [line restriction](https://docs.mapbox.com/android/maps/api/latest/mapbox-maps-android/com.mapbox.maps.extension.style.layers.generated/-line-layer/line-z-offset.html), [pricing](https://docs.mapbox.com/android/maps/guides/pricing/) |

Recommendation: compare Google Maps 3D and Mapbox Native with an owned plane model, a short route, an antimeridian route, global and close-up cameras, satellite/classic switching and elevated route history. Inspect battery, attribution, account setup and unavailable-network behavior. Mapbox Native has since passed the initial globe/buildings/owned-aircraft spike. Satellite returned HTTP 429 and was explicitly removed from scope by the user on 2026-10-28; complete the remaining vector/globe/terrain checks in implementation-status.md. Google Maps 3D has not been tested in this repo. A custom native globe remains a larger fallback requiring a revised estimate.

## Completion contract

Each F-ID must end with evidence from a working device/emulator test or an explicitly approved substitute/omission. Track source fidelity separately from platform correctness. Unknown layouts, assets and rules cannot be silently marked complete. No feature is complete as of Phase 1.
