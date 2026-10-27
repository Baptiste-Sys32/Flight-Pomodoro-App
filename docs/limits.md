# Android feasibility and limits

Research date: 2026-10-27. These are API-based findings and proposed mitigations, not device-tested guarantees. The app is private and sideloaded; that does not remove Android permission, lifecycle or provider restrictions.

## Timer and background operation

A correct countdown must use a persisted session deadline rather than decrementing a value every second. Within one boot, [SystemClock.elapsedRealtime](https://developer.android.com/reference/android/os/SystemClock) is monotonic and includes deep sleep. Save a boot identifier, monotonic deadline and UTC recovery timestamps. [BOOT_COUNT](https://developer.android.com/reference/android/provider/Settings.Global) can distinguish boots; elapsedRealtime does not survive reboot. Reboot reconciliation policy remains a product decision, especially if the wall clock changed while off.

A visible, user-started foreground service supports session status, but it is not immortal. Start it from the session-start UI, respect [background start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start), and persist state before relying on it. For a long silent timer, `specialUse` is a candidate with a manifest subtype explanation. `shortService` has an approximately three-minute limit and is unsuitable. Do not misdeclare a silent timer as media playback. Audio uses its legitimate `mediaPlayback` service. [Service types](https://developer.android.com/develop/background-work/services/fgs/service-types) define these distinctions; Play review would matter only if distribution changes.

Schedule one completion alarm. [Exact alarms](https://developer.android.com/develop/background-work/services/alarms) need an appropriate permission/access path. Prefer user-granted `SCHEDULE_EXACT_ALARM`, check `canScheduleExactAlarms`, and handle denial/revocation. A precise interval displayed after reopening remains possible without access; punctual background completion cannot then be guaranteed. WorkManager is not a one-second clock or an exact alarm substitute. Avoid a continuous wake lock or continuous map rendering to keep time.

Android 13's [Active apps Stop action](https://developer.android.com/develop/background-work/services/fgs/handle-user-stopping) removes the whole application without callbacks; scheduled jobs/alarms remain. Settings Force stop is different and prevents normal unattended recovery until the user relaunches. OEM battery policies, restart, permission revocation and process death require real-device tests. Do not claim guaranteed completion notifications under every condition.

Make session completion idempotent in a Room transaction. Alarm, UI and service may race; only one transition may award time/distance or upgrade membership. A terminated app can reconcile logical completion later even when it could not deliver the celebration on time.

## App blocking

[UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager) observes usage/events after user-granted usage access. It does not suspend apps, close them or prevent launch. Polling introduces delay and unavailable data before unlock must be handled.

An application overlay requires special [Settings permission](https://developer.android.com/reference/android/provider/Settings) and `canDrawOverlays`. It can cover a distracting app, but that app continues running. Android allows sensitive apps to [hide overlays](https://developer.android.com/security/fraud-prevention/activities); system surfaces, split screen and OEM behavior create further coverage gaps. Background activity launch restrictions also matter. This is a deterrent, not OS-level enforcement.

An [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) can react to permitted window events and present an intervention. The user must enable it; it can be revoked. Sideloading can introduce a [restricted settings gate](https://support.google.com/android/answer/12623953). Prototype on the actual device before choosing it. Do not request broad node-content access when package/window events suffice. Accessibility and usage monitoring should be optional and isolated from the timer.

Neither solution provides Screen Time parity or tamper resistance. Users can revoke access, uninstall or stop the app. A normal personal app cannot gain device-owner suspension powers silently. [Android 14](https://developer.android.com/about/versions/14/behavior-changes-all) prevents using killBackgroundProcesses to kill other apps. Root or device-owner enrollment is outside the proposed scope.

Proposed sequence: optional DND first; compare Accessibility intervention against usage/overlay on the intended phone; choose only after tests. Essential system/navigation/permission surfaces and emergency access must stay usable. Do not mistake the comparison APK's implementation for proof of reliability.

## Websites and Do Not Disturb

App blocking does not block domains. Browser-specific Accessibility inspection can support a bounded set of browsers but is brittle and cannot establish universal URL filtering. A local [VpnService](https://developer.android.com/develop/connectivity/vpn) is another approach for network/domain filtering, with consent and only one VPN per user/profile. It conflicts with an existing VPN. Encrypted DNS, cached addresses, protocols and shared hosting complicate domain enforcement; URL paths inside HTTPS are not exposed as a generic Android filtering API. No TLS interception is proposed. Select supported-browser coverage or a separately designed VPN solution before claiming website parity.

[NotificationManager](https://developer.android.com/reference/android/app/NotificationManager) requires notification policy access for DND control. For apps targeting Android 15+, policy/filter setters use app-associated AutomaticZenRule behavior rather than directly replacing global policy. Activate/deactivate the app's own rule and respect user overrides. DND silences permitted interruptions; it neither blocks launches nor cuts internet access. “Airplane Mode” is the app's metaphor, not automatic control of the phone's radio mode.

## Live status, widgets and screen awake

The reliable UI substitute is an ongoing countdown/progress notification. Android 13+ [notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission) affects visibility. An FGS still supplies a notification even when permission is denied; denial does not make invisible status parity acceptable. Show a clear degraded state.

API 36 [ProgressStyle](https://developer.android.com/reference/android/app/Notification.ProgressStyle) can show route stages and a custom tracker. [Promoted Live Updates](https://developer.android.com/develop/ui/views/notifications/live-update) are a separate eligibility question. The docs do not explicitly endorse this simulated-flight timer; promotion is unknown and cannot be promised. Do not misrepresent it as real transport tracking. Lock-screen visibility depends on user/OEM privacy settings.

[Glance](https://developer.android.com/develop/ui/compose/glance) uses widget-specific composables, not arbitrary app Compose UI. Standard [periodic updates](https://developer.android.com/develop/ui/compose/glance/glance-app-widget) have a 30-minute minimum; event updates are possible. Do not animate a plane at 60 fps or refresh the whole widget every second. Use snapshots and supported timer presentation, then test launcher rendering and resize. Android home-screen widgets do not guarantee Apple lock-screen or StandBy equivalence.

Keep-awake applies only to the visible focus Activity. Turn it off on exit/pause according to the approved policy. It does not justify background CPU rendering. Proposed StandBy substitute is an optional landscape/dim focus view, not replacement of the Android lock screen.

## Maps, assets, storage and audio

MapLibre Native currently lacks globe support. Google Maps 3D and Mapbox need accounts, owned configuration, attribution and evaluated cost. Neither SDK has passed a project test. Mapbox's current elevated line API does not support globe projection. Google 3D's roadmap mode remains marked experimental. Full evaluation and sources are in feature-map.md. Do not hide attribution to match the iOS screenshot.

Satellite imagery is not automatically licensed for bundling. Mapbox [offline downloads](https://docs.mapbox.com/android/maps/guides/offline/concepts/) use explicit style/tile regions, have tile-pack constraints, and downloaded data cannot be redistributed inside the APK. An SDK cache is not guaranteed offline coverage. Core timer/history should work offline even when geographic detail cannot load.

No user-owned production fonts, icons, plane model, sky/window textures or audio collection have been supplied. Existing repository assets have unknown provenance despite the code's MIT license. Research images and APK resources will never become shipped assets. Keep a provenance/license manifest for replacements. Visual fidelity with different artwork can preserve composition/material/motion, but cannot mean pixel identity.

[Media3 background playback](https://developer.android.com/media/media3/session/background-playback) supports an ExoPlayer and MediaSession inside MediaSessionService. Handle focus loss, calls, noisy/headphone disconnection and user stop. Exact original mixes are unknown; bundled licensed loops are feasible. Audio interruption cannot alter the session clock.

[Room](https://developer.android.com/training/data-storage/room) suits relational records and transactional awards. [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) suits small preferences. Both require explicit backup/data-loss policy. iCloud is unavailable as a native Android equivalent in this scope. Local export/import is proposed; original database compatibility is unknown. No server, login, analytics or subscription is assumed.

[Per-app language support](https://developer.android.com/guide/topics/resources/app-languages) is feasible; Android 12 and earlier Compose compatibility may use AppCompatActivity/AppCompatDelegate. Locale configuration does not supply translations. Six-language quality needs actual translated strings, CJK fonts, number/date formatting and review.

## Current validation limits

No Android build, emulator session or package runtime was performed in Phase 1. Both XAPKs are split bundles: AirFlow supplies armeabi-v7a, the other arm64-v8a. Their base APKs alone omit required native splits; an x86_64 emulator is not guaranteed compatible. Runtime installation of these comparison apps is not a prerequisite for copying iOS appearance, and no license checks will be bypassed.

REA 6.4.0 executes. Archive inspection and resource decoding succeeded on both packages. REA Android method inspection is blocked by missing `jadx-headless-mcp` 0.7.1; Apktool supplied separately labeled static method evidence. Static declarations cannot establish a complete screen flow or background reliability. Local Linux Mint is also flagged outside REA's declared supported host list; this did not prevent the successful archive/resource operations. No unrelated engines or configuration changes were made.
