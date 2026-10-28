# iOS reference specification

Research date: 2026-10-27. Phase 1 only. This is a native rebuild of the existing project. The iOS app by Cement Planet LTD, App Store 6648771147, is the design authority. Neither supplied Android package nor the current web prototype defines the target UI.

## Evidence and interpretation

- [App Store listing and version history](https://apps.apple.com/us/app/focusflight-deepfocus-timer/id6648771147): primary feature and language evidence; six promotional images inspected.
- [Public iOS capture](https://screensdesign.com/apps/focusflight-deepfocus-timer/): 14:57.985, 480 x 1038, 30 fps. Downloaded from the page's exposed player, extracted at 1 fps with ffmpeg. All four overview sequences and detailed frames were inspected. Settings at [14:57](references/ios-14-57.jpg) visibly identify version 2.0; the compressed build suffix is unknown. This is a third-party recording, not direct execution on an iPhone.
- [Seven public showcase screens](https://screensdesign.com/showcase/focusflight-deepfocus-timer): higher resolution supplements. See [reference overview](references/overview.jpg), [reference index with source URLs and hashes](references/index.json).
- There are no user recordings or screenshots. The user confirmed the inputs-folder reference was a typo. No iOS binary was sought or analyzed.

Labels: **observed** means visible in inspected imagery; **documented** means stated by a primary source; **unknown** means insufficient evidence. Proposed Android behavior is called out separately. Promotional cards are not proof of operational features. References are research material only, never production assets.

## Visual system

Observed: full-screen geographical imagery, sparse floating controls, translucent rounded panels, compact airport-code labels, ticket and membership-card objects. Dark surfaces predominate in session utilities; onboarding includes both white and black backgrounds. Primary actions are broad capsules. Yellow/black airport chips contrast with white route lines. Selected scenarios carry distinct colors and pictograms. Paper boarding passes contrast against dark backgrounds; the live ticket can also be dark.

Exact source fonts, font weights, hex colors, corner radii, blur kernels, shadows, spacing, icon dimensions, safe-area measurements and spring constants are **unknown**. Do not convert screenshot pixels directly to Android dp. Compression, map content and device scale affect sampled colors. Font category is visibly sans serif for operational UI, serif for some editorial/card headings, and barcode-like graphics on tickets. The user's own font and icon families must be selected before final fidelity review. No Apple fonts or original artwork will be extracted for shipping.

The initial implementation must measure bounds from the linked individual references at a fixed reference viewport, then compare scaled screenshots. Those measurements are implementation approximations, not recovered original tokens. Preserve map attribution, Android system insets, accessibility semantics and usable touch targets. Allow font scaling without clipping. These are platform adaptations, not license to replace the composition with stock cards or bottom navigation.

## Screens and states

### 1. Introduction and setup

References: [scenario selection](references/ios-00-40.jpg), [reminder](references/ios-01-00.jpg), [blocking explanation](references/ios-01-10.jpg), [map selection](references/ios-01-30.jpg), [higher resolution scenario](references/reference-01.webp).

Observed: opening dark typographic panels lead into light setup pages. Segmented progress marks sit near the top. Headings are centered above the main interaction. The lower edge holds a broad Continue capsule, sometimes with Skip beneath. Scenario choices appear as colored rounded tags in a clustered arrangement with icons; selection and motion are visible across successive frames. Reminder setup uses an hour/minute wheel and notification preview. Blocking explanation uses a central hourglass/lock motif. Map setup uses four visual tiles in two columns with a labels row beneath.

Unknown: complete conditional branch order, first-launch persistence, exact permission-denial branches, tag collision physics and reminder recurrence semantics. Android proposal: explain each optional permission just before its system Settings flow, allow denial, and resume setup. Replace all illustrations and brand text with owned material while preserving hierarchy.

### 2. Membership invitation and initial reward

References: [invitation](references/reference-03.webp), [window feature page](references/reference-04.webp), [card reveal](references/ios-03-05.jpg), [reward replay](references/ios-14-30.jpg).

Observed: editorial feature carousel with large hero artwork, serif titles and dark atmospheric backgrounds; original purchase controls remain near the bottom. Reward presentation centers a physical-looking card with an envelope/packaging metaphor. A pale blue initial card is visible.

Unknown: all upgrade tiers, thresholds, card materials, reveal easing and whether purchase is required for each progression path. Proposed substitute requiring approval: omit subscriptions, store transactions and restore purchases in a private build; preserve a local membership collection and original reward choreography using owned cards. Do not invent thresholds and present them as cloned behavior.

### 3. Starting airport

Reference: [03:20](references/ios-03-20.jpg).

Observed: globe in the upper area; a dark rounded selection panel below with search, current-location and random-location actions. The current-location action is a light capsule; random location is secondary. Unknown: search results, loading, unavailable-location behavior and airport ranking. Android proposal: manual selection must work without location permission; no background location is needed for a simulated route.

### 4. Home

Reference: [03:45](references/ios-03-45.jpg).

Observed: edge-to-edge map, small greeting then larger city name at upper left, departure marker on the map. A broad Start Journey capsule sits above a translucent lower panel containing Mine, Trends and World rows. This is not a conventional tab bar. Map colors and label visibility follow the chosen style. Unknown: greeting rules, return-to-last-airport rules and all empty/error states.

### 5. Duration and destination booking

References: [03:50](references/ios-03-50.jpg), [higher resolution booking](references/reference-05.webp), [App Store composition](references/reference-07.webp).

Observed: the map remains the canvas. Back control at upper left; several circular controls across the upper right. A narrow curved/ticked duration ruler sits above a horizontal row of destination cards. Each card shows an airport-code chip, city and minute estimate. The selected card is pale, neighboring cards are dark. A light Book My Flight capsule spans most of the bottom content width. A white line links the departure marker to the selected destination; airport chips use yellow outlines/fills over dark ground.

Unknown: exact time range, increment, rounding, drag resistance, search/filter ordering and random-destination concealment. Do not inherit the web prototype's 850 km/h plus 30-minute formula as iOS truth. The source appears to offer routes near a selected duration; final duration policy needs approval. Proposed domain model stores both requested and planned duration so UI rounding cannot silently change elapsed focus credit.

### 6. Seat and focus scenario

Reference: [04:05](references/ios-04-05.jpg).

Observed: dark aircraft-cabin silhouette with a central aisle, row numbers and seat columns A/C and D/F. A selected seat becomes a colored square bearing its scenario pictogram. Confirm appears as a broad bottom capsule. Back returns to booking. The scenario can differ between seats. Unknown: mapping of every seat to scenario, disabled seats, selection customization flow and any seat-class rules. App Store documentation confirms studying, working, reading and creative focus types. Those four are required even when capture labels differ.

### 7. Boarding pass and boarding interaction

References: [ticket over map](references/ios-04-10.jpg), [ticket detail](references/ios-04-18.jpg), [boarding interaction](references/ios-04-41.jpg).

Observed: large departure and arrival codes at top, cities and duration below, paired metadata rows for seat, distance, boarding time and date. The lower ticket portion is separated by a perforation-like line and carries a barcode graphic. Map placement transitions to a dark detail view. A lower Airplane Mode row has an orange icon and Edit action. A broad Boarding button anchors the detail view. The barcode stub visibly rotates/separates during the interaction before boarding.

Unknown: gesture threshold, whether dragging and tapping are equivalent, actual barcode payload, haptic pattern, exact tear timing and ticket detail editing. Barcode is decorative unless a verified function requires encoded data. Android proposal: accessible tap alternative for any drag gesture and an owned ticket graphic.

### 8. Airplane Mode configuration

Reference: [04:40](references/ios-04-40.jpg). Detailed blocking selection is visible in the source video around 04:26-04:44.

Observed: dark scrolling page with heading, explanatory copy, default blocking card and colored scenario cards. Each row can show configured counts and selection marks. Native iOS app/site selection is a system flow. Unknown: exact category and domain semantics, strictness, shield content and permission-revocation presentation. Android substitute must state its actual permissions and enforcement limits, described in limits.md. The term does not authorize switching the device's radio airplane mode.

### 9. Takeoff interstitial

Reference: [04:46](references/ios-04-46.jpg).

Observed: luminous pale background, centered takeoff icon above a short two-line departure message, compact dark GO capsule near the lower portion. Unknown: exact interstitial duration, camera synchronization and whether action can be skipped. Do not let a visual animation define elapsed focus time. The session-start boundary must be chosen explicitly and tested.

### 10. In-flight map

References: [04:50](references/ios-04-50.jpg), [06:09](references/ios-06-09.jpg), [App Store view](references/reference-13.webp).

Observed: map dominates the entire screen. White aircraft and route line with endpoint markers; circular utilities form short vertical groups on the left and right. Time Remaining is anchored bottom left; Distance Remaining bottom right. Control glyphs include pause, audio, route/view and window/map tools. Keep these functions stable while matching their verified visual placement. Unknown: all glyph meanings, altitude model, camera follow constraints, precise route curvature and interaction lockouts. Current capture proves movement and map switching, not a physically accurate flight simulation.

### 11. Airplane-window focus view

References: [05:00](references/ios-05-00.jpg), [high resolution window](references/reference-06.webp).

Observed: almost black cabin, realistically shaded vertical rounded window in the center, illuminated inner rim, calm cloud scene. Map-style widgets are replaced by the window while utility groups and lower time/distance remain. Window-scene selection is a compact modal with two image tiles, Cloud and Satellite Map. Unknown: 3D geometry, parallax, scene loop length, lighting changes and whether geography always matches the flight. Use owned window and sky assets; do not reuse downloaded reference art.

### 12. Map and window selectors

References: [window selector](references/ios-05-10.jpg), [map selector](references/ios-05-20.jpg), [higher resolution setup](references/reference-02.webp).

Observed: rounded translucent floating panels over the retained scene, title at upper left, close control upper right. Four map tiles: Monochrome, Terra, Standard, Satellite. Selection is outlined; Labels has a dedicated on/off row. Source description calls map modes 3D, satellite and classic; retain this documented capability, with final naming resolved from the actual menu evidence. Unknown: whether Terra is a material style or projection, exact tile sizes and blur implementation.

### 13. Sound panel

References: [05:40](references/ios-05-40.jpg), [05:50](references/ios-05-50.jpg).

Observed: large translucent overlay; Sound heading, master switch, volume control, then White Noise and Focus Music groups of rounded tiles. The chosen tile has saturated color and an ON state; unchosen tiles are dark. Airplane ambience and waterfall choices are demonstrated. Unknown: exact mix behavior, concurrent tracks, fade lengths and audio loop boundaries. Bundle only owned or suitably licensed replacements, maintain a license manifest, and handle audio focus/headphones. No original recording is an audio source for the rebuild.

### 14. Pause and flight options

References: [paused](references/ios-06-13.jpg), [options](references/ios-06-17.jpg).

Observed: map dims/blurs substantially, pause indicator and clock label appear toward the lower center, utility control becomes play. Options include Give up, Airplane Mode and Keep Screen Awake. The latter exposes Automatic/Disable/Enable choices. Unknown: cancellation confirmation, pause-time credit policy and automatic screen-awake rules. Proposed behavior: paused time earns no focus or distance; cancellation is explicit and recorded without award. These are proposals until approved.

### 15. Scenario editor and app icons

Reference: [07:00 region](references/ios-07-00.jpg), [scenario editor](references/ios-07-22.jpg).

Observed: scenario editing has a large colored icon disc, editable label, icon grid and color choices. App-icon customization is demonstrated through an iOS home-screen preview. Unknown: icon catalog, custom-scenario limits and deletion behavior. Android substitute: owned scenario pictograms; launcher icon variants only if approved, using activity aliases with launcher-specific behavior documented.

### 16. FlightLog and records

References: [filters](references/ios-07-28.jpg), [status](references/ios-07-18.jpg), [scenario filter](references/ios-07-35.jpg), [sort control](references/ios-07-45.jpg).

Observed: dark full-screen record area with a compact horizontal filter bar for Status, Scenario and Sort By. Status menu includes planning, in-progress, landed and canceled concepts. Scenario filter displays colored chips and Clear All. Flight cards show date, endpoint codes/cities, duration, distance and state. A large dark folder graphic occupies the lower area. Some filtered states are empty. Unknown: exact sorting options, record deletion, note editing, long-press gestures and populated route-detail hierarchy. App Store paper-pass imagery also shows a companion Notes card. Preserve record data rather than enforcing the prototype's 50-row limit.

### 17. Trends

References: [08:10](references/ios-08-10.jpg), [settings](references/ios-08-29.jpg), [empty day](references/ios-08-41.jpg).

Observed: expandable translucent sheet over the map, Total/Year/Month/Week/Day interval selector, previous/next controls and an illustrated No Flight Data state. Edit opens settings with delay threshold, chart visibility and reordering. Flight streak, flight count, weekday averages and scenarios are visible categories. Unknown: populated chart geometry, aggregation formulas, streak rules, threshold meaning and time-zone handling. Do not fabricate those rules as parity; propose them separately before implementation.

### 18. World and route history

References: [globe](references/ios-09-00.jpg), [world cards](references/ios-09-50.jpg), [airport list](references/ios-10-00.jpg).

Observed: whole-earth view supports rotation; a lower control group switches displayed route/scenario content. World sheet includes location, around-earth progress and airport-visited cards. Unknown: route altitude, visited-airport credit rules, earth-circumference normalization, route clustering and card share output. No actual GPS travel should be inferred from simulated sessions.

### 19. Mine and membership collection

References: [Mine](references/ios-12-00.jpg), [card reveal](references/ios-14-30.jpg).

Observed: translucent lower sheet over home map, membership-card hero, Settings shortcut and configuration rows. Card reveal has its own full-screen ceremony. Unknown: all membership progression rules, tier count, downgrade behavior and persistent membership identity. Room stores derived progress, but rules remain approval-gated.

### 20. Settings, notification and widgets

References: [settings](references/ios-12-15.jpg), [reminder](references/ios-12-30.jpg), [awake](references/ios-12-40.jpg), [units/language rows](references/ios-12-55.jpg), [widget guide](references/ios-09-30.jpg), [bottom settings](references/ios-14-57.jpg).

Observed: rounded grouped rows in a dark sheet, current value and chevron at right. General rows cover blocking, scenarios, icon, notification, sound/haptics, in-flight settings, trends, units and language. Guide rows mention iCloud, Live Activity, StandBy and Widgets. Lower sections include acknowledgements, support, database backups and account deletion. Notification configuration uses a time wheel and preview. Widget guide shows different widget shapes. Unknown: backup format, account semantics, language-picker UI, all widget variants and their operational refresh behavior.

Android proposals needing approval: local backup/import instead of iCloud; ongoing notification instead of Live Activity; Android widgets instead of Apple widget installation guidance; optional landscape focus view instead of system StandBy integration. Remove original store, subscription, support and promotional links, retain a useful local settings structure. English, German, Japanese, Korean, Simplified Chinese and Traditional Chinese are documented languages. Metric default and optional nautical miles are required; the captured imperial setting is not the desired default.

### 21. Landing and unverified extras

Landing is documented by the App Store, but a full completed-flight landing sequence is **unknown**. The capture cancels or leaves flights active. Success animation, sound, summary layout, reward award moment and next-flight transition must remain unresolved until evidence or an approved substitute is available.

The v2.0 introduction also advertises random mode, new widgets, routes and Shared Flights; Magazine appears as an upcoming feature. Shared Flights operation and Magazine content are unknown. They must not silently become required online services or be claimed implemented based on promotional screens.

## Approved implementation deviations

On 2026-10-28 the user removed satellite imagery entirely. Preserve the source evidence above as research, but implement only Monochrome, Terra and Standard in the map picker. The cloud-window scene remains; the satellite-window scene is omitted. Standard is the default, with readable dark greeting/ruler text on light vector styles. Terrain and buildings remain separate capabilities.

## Motion and state contract

Observed motion vocabulary: floating scenario tags, map camera travel, sheet expansion, seat highlight, ticket-stub rotation/separation, card reveal and aircraft progression. Exact durations, easing curves, touch latency and haptics are unknown. One-frame-per-second extraction is a navigation aid, not sufficient timing evidence; revisit the original 30 fps clip for each implemented transition.

For every verified screen, Phase 2 must cover loading, empty, active, disabled, selected, permission denied/revoked, background/return and interrupted states where relevant. These are Android completeness requirements; source appearance for unavailable states remains unknown. No invented state should be described as copied. Validate light/dark content, portrait, landscape focus, font scaling, reduced animation and offline map failure. Review still-image similarity and short motion clips separately.
