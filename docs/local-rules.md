# Approved local behavior

The user approved local duration/progression rules and a private, local-only scope on 2026-10-28. These rules are substitutes, not recovered iOS behavior.

## Focus duration and routes

- The selected focus duration is the exact countdown. Route estimates never change it after booking.
- The initial duration selector offers 15-180 minutes in five-minute steps. This is a local range, not a verified source limit.
- Candidate routes are ranked near an illustrative travel estimate: distance / 800 km/h plus 15 minutes, rounded up. This is a simulation, not an airline schedule or navigation tool.
- Manual destination selection keeps the chosen focus duration; simulated speed adapts rather than changing the countdown.
- Persist one random destination at booking, conceal it until landing, and do not reroll on pause, restart or recovery.
- The same clock projection drives aircraft progress, remaining distance, UI and notification. Paused time earns nothing.
- Completed sessions earn their full selected duration and route distance once. Canceled sessions remain in the log and earn no progression credit. Diagnostic test flights must be excluded from progression and trends.

## Local membership collection

Both requirements in a row must be reached. Distance is stored in meters; changing display units does not change eligibility. Tier names and artwork are original to this rebuild.

| Card | Completed focus | Simulated distance |
|---|---:|---:|
| Airfield | Initial card | Initial card |
| Horizon | 10 hours | 5,000 km |
| Jetstream | 50 hours | 25,000 km |
| Stratosphere | 150 hours | 100,000 km |
| Orbit | 500 hours | 300,000 km |

No purchases, streak requirement, expiry, downgrade or paid unlock is used. Progress is derived from completed, non-diagnostic records. Imported records use the same rules; award transactions must remain idempotent.

## Map scope

On 2026-10-28 the user explicitly removed satellite imagery entirely. Use Standard, Monochrome and Terra vector styles, retain the globe, and keep the original cloud-window scene. Satellite tiles and a satellite-window scene are not acceptance requirements. Terrain and 3D buildings are separate capabilities.

## Private scope

- Room and DataStore hold local state. Local backup/import replaces iCloud.
- No accounts, purchases, restore-purchase flow, referrals, Shared Flights or Magazine.
- No upload of study records or notes to a project backend. Mapbox SDK requests follow the provider account and privacy controls described in map-setup.md.

## Implementation status

This file defines the approved local policy. It does not claim that all listed behavior is implemented. See implementation-status.md and feature-map.md.
