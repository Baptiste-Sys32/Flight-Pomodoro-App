# Asset provenance

No icon, font, sound, image or service credential was copied from either comparison APK or the iOS reference.

| Resource | Source | Terms / status |
|---|---|---|
| Launcher and notification plane vectors | Original geometry authored for this project | Covered by repository software license; provisional artwork |
| Airport catalog | [OurAirports](https://ourairports.com/data/) | Public Domain. Reproducible import in tools/import_airports.py; source hash saved beside catalog |
| Temporary ready-screen fonts | Android system sans/monospace | Supplied by the OS; the flight UI uses Inter below |
| Reference images in docs/references | Sources in references/index.json | Research evidence only, excluded from app resources and never shipped |
| Existing web textures/models/HDRs | Pre-existing repository | Provenance unknown; not imported into native app |
| Cabin window frame | Built-in image_gen, original generation on 2026-10-27 | RGBA transparency checked. Saved at android/app/src/main/res/drawable-nodpi/window_frame.png; exact prompt in window-asset.json |
| 3D aircraft marker | Original procedural geometry in tools/create_aircraft.py | glTF 2.0, 560 triangles. Not derived from the existing web models or either APK |
| Inter variable font | Google Fonts distribution of the Inter project | SIL Open Font License 1.1, bundled notice in native assets/notices/inter-ofl.txt |
| Cloud-sky plate | Built-in image_gen, original generation on 2026-10-27 | Saved at android/app/src/main/res/drawable-nodpi/cloud_sky.png; exact prompt in sky-asset.json |
| Audio library | Pending | Must be original or independently licensed before shipping |

Map provider attribution and licensing remain visible when the renderer is enabled. SDK imagery is not copied into bundled art.
