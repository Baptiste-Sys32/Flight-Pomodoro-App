# Asset provenance

No icon, font, sound, image or service credential was copied from either comparison APK or the iOS reference.

| Resource | Source | Terms / status |
|---|---|---|
| Launcher and notification plane vectors | Original geometry authored for this project | Covered by repository software license; provisional artwork |
| Airport catalog | [OurAirports](https://ourairports.com/data/) | Public Domain. Reproducible import in tools/import_airports.py; source hash saved beside catalog |
| UI fonts | Android system sans/monospace initially | Supplied by the OS; production font selection pending |
| Reference images in docs/references | Sources in references/index.json | Research evidence only, excluded from app resources and never shipped |
| Existing web textures/models/HDRs | Pre-existing repository | Provenance unknown; not imported into native app |
| Cabin window frame | Built-in image_gen, original generation on 2026-10-27 | RGBA transparency checked. Saved at android/app/src/main/res/drawable-nodpi/window_frame.png; exact prompt in window-asset.json |
| Sky/aircraft/audio production assets | Pending | Must be original or independently licensed before shipping |

Map provider attribution and licensing remain visible when the renderer is enabled. SDK imagery is not copied into bundled art.
