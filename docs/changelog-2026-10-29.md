# Android UI and flight-view rework, 2026-10-29

Uncommitted application changes from both device-feedback rounds. Existing user commits and repository-cleanup changes are excluded.

## Home and settings

- Moved Start Journey to the bottom; removed the Mine/Trends/World home panel.
- Added a top-right profile/settings entry; retained FlightLog, departure selection, units and keep-awake controls.
- Removed Renderer Check from settings. Debug preview intents remain for development tests only.
- Added visible asset credits and bundled license notices.

## Boarding and ticket

- Added a contrasting duration-ruler surface, translating ticks/numeric labels, continuous dragging and five-minute settling; retained accessibility progress/actions.
- Preserved destination search and snapping airport cards; fitted route cameras to keep both endpoints visible.
- Added a directional marker moving from departure toward arrival.
- Rebuilt the cabin as one scrollable object: cockpit, fuselage, internal windows and seats. Clipped the viewport and moved the selection summary/Confirm into an opaque footer.
- Kept A/Study, C/Reading, D/Creative and F/Work.
- Replaced provisional UI icons with 26 licensed Lucide vectors; normalized SVG arcs for correct Android rendering.
- Rebuilt the boarding pass on a clean paper surface with stronger typography, clearer fields and no decorative watermark lines.
- Replaced random-square artwork with a real PDF417 barcode encoding only a local session reference. Random destinations are not included in its payload.
- Added a paper-feed reveal, retaining the stub tear and takeoff interstitial.

## Map and aircraft

- Moved required Mapbox logo/attribution to the bottom-right map edge.
- Used dark routes on light styles and illuminated pale routes on Terra; added a contrasting route casing.
- Displayed code labels for both departure and arrival, with illumination in the night style.
- Map gestures release follow before camera updates can recenter; explicit follow restores tracking.
- Disabled rotation, pitch, quick-zoom and pinch-pan combinations; retained one-finger pan/two-finger zoom and level north-up at globe zooms.
- Replaced the procedural marker with the unbranded amvlab A320 (CC BY 4.0), corrected its heading axis and improved night visibility.
- Aircraft display size now grows when zooming in instead of being held constant.
- Switched styles without reloading the entire map; cached repeated updates and reduced foreground clock polling.
- Added bounded provider-error handling, a limited-map retry state and credential-redacted debug diagnostics.

## Flight controls and window

- Removed the duplicate central Map/Window toggle.
- The existing return button now uses a map glyph; appearance uses a separate palette glyph.
- Replaced the blur-heavy pause presentation with a nearly black screen, centred remaining time and Resume flight.
- Removed the photographic window and cloud plate from runtime resources.
- Built one native frame and one draggable blind, with open/close accessibility actions.
- Replaced the window Mapbox renderer and cloud overlay with an offline Filament scene: airport terminal, runway, city buildings and actual 3D cloud banks.
- Added simulated climb/cruise/descent camera movement, daylight lighting and rounded cloud geometry.
- Pausing freezes scenery; covering/backgrounding stops scheduled rendering. Added native-view redraw/lifecycle handling for Compose integration.

## Assets, dependencies and tooling

- Added ZXing core 3.5.4 and Filament 1.72.0. Filament 1.77.2 was evaluated but requires compile SDK 37; the compatible version keeps the current toolchain.
- Added exact asset revisions/hashes and license records for the airliner, Lucide icons and Kenney CC0 city models.
- Added a deterministic scene generator and SVG-path normalization tool.
- Retained the old aircraft generator as a prototype tool writing only into ignored build output.
- Updated UI specification and asset/implementation documentation to reflect the user's revised direction.

## Validation

- Debug app and instrumentation APK builds passed.
- All 11 unit tests passed, including PDF417 round-trip decoding.
- API 36 emulator: all four final instrumentation tests passed. The full flow covers ruler interaction, seat scrolling, pass recreation, displayed-barcode decoding, boarding, map detachment/re-follow, three styles, blind dragging, pause/resume, cancellation and persisted FlightLog notes. Separate checks cover SDK map configuration and native scenery detail, movement, pause and absence of a Mapbox view inside the window.
- S24 Ultra: all three final map/window instrumentation tests passed. Flight records and preferences were preserved.
- Investigated emulator graphics interruptions and an airplane-mode/network issue; final validation ran after restoring connectivity. Texture/window pixel capture and explicit redraw handling replaced misleading blank captures.

## Remaining limits

This is a UI/renderer milestone, not completion of the entire rebuild. Battery/long-session frame-rate measurements and further visual polish remain. The window is fictional stylized scenery, not actual geography or weather. Audio, blocking, widgets, full progression/analytics, backup/localization and the remaining feature-map work are still pending.
