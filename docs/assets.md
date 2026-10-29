# Asset provenance

No icon, font, sound, image or service credential was copied from either comparison APK or the iOS reference.

| Resource | Source | Terms / status |
|---|---|---|
| Launcher and notification plane vectors | Original geometry authored for this project | Covered by repository software license; provisional artwork |
| Airport catalog | [OurAirports](https://ourairports.com/data/) | Public Domain. Reproducible import in tools/import_airports.py; source hash saved beside catalog |
| Temporary ready-screen fonts | Android system sans/monospace | Supplied by the OS; the flight UI uses Inter below |
| Reference images in docs/references | Sources in references/index.json | Research evidence only, excluded from app resources and never shipped |
| Existing web textures/models/HDRs | Pre-existing repository | Provenance unknown; not imported into native app |
| Former window-frame prototype | Original image_gen generation, 2026-10-27 | Retired and removed from runtime resources after device feedback; historical prompt in window-asset.json |
| 3D aircraft marker | A320_nologo.glb by amvlab | CC BY 4.0. Exact revision/hash below; source model kept unmodified, display scale/orientation configured in the app. Replaces the procedural prototype |
| Inter variable font | Google Fonts distribution of the Inter project | SIL Open Font License 1.1, bundled notice in native assets/notices/inter-ofl.txt |
| Former cloud-image prototype | Original image_gen generation, 2026-10-27 | Retired and removed from runtime resources; historical prompt in sky-asset.json |
| Audio library | Pending | Must be original or independently licensed before shipping |

Map provider attribution and licensing remain visible when the renderer is enabled. SDK imagery is not copied into bundled art.

UI icons now use licensed Lucide geometry converted to Android VectorDrawable. The exact upstream revision and source SVG hashes are in [icon-provenance.json](icon-provenance.json). The full ISC/MIT notice ships in `assets/notices/lucide-license.txt`. No source-app icon assets were reused. The small window-outline glyph remains original.

The boarding-pass barcode is now generated with ZXing core (Apache 2.0). It encodes only `FOCUS FLIGHT: <local session id>`, preserving random destinations. It does not encode an airline credential or a URL. The former decorative random-square pattern is removed.

AirFlow package guide images were inspected locally on 2026-10-29 as comparison evidence for paper-pass spacing and cabin shapes, using REA archive inspection followed by bounded resource extraction. No extracted imagery is shipped or committed. The second comparison package has Compose-generated ticket UI rather than a ticket bitmap; its archive inventory alone does not verify that screen's live appearance.

## Second device-feedback revision (2026-10-29)

- Aircraft: the unbranded A320 from [amvlab aircraft models](https://github.com/amvlab/aircraft-models), revision `91d835e8e851b2317fe79af291c9fed6153fd525`, source `models/A320_nologo.glb`. Licensed [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). A visible Asset credits panel links the creator/license; the bundled notice also records origin and display-only adaptations.
- Window: an original native frame and a single native blind replace the generated photographic frame. A bundled Filament scene replaces all window Mapbox rendering and cloud-image overlays. The scene contains Kenney City Kit buildings (CC0), with original airport, runway and 3D cloud geometry. It is fictional scenery, not a geographic reconstruction.
- City assets: creator/license verified on [Kenney's City Kit page](https://kenney.nl/assets/city-kit-commercial). The official archive endpoint did not return a usable ZIP, so the six GLBs were acquired from a pinned public distribution mirror. Exact distribution revision and original-file hashes are in [scenery-provenance.json](scenery-provenance.json). `tools/create_window_scene.py` assembles the scene reproducibly from the local source cache.
- The prototype aircraft generator now writes only to the ignored build-validation folder and cannot overwrite the shipping A320.
- The previous generated frame/cloud plate and their prompt documents are historical prototypes. They are not referenced by the new window implementation.

Filament is pinned to 1.72.0, whose Android metadata is compatible with this project's SDK. The newer 1.77.2 requires compile SDK 37. The exact 1.72.0 ModelViewer source/API was inspected for lifecycle, camera, loading and rendering behavior.
