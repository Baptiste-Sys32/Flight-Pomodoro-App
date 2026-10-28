# Focus Flight - A timer for geeks

A private native Android rebuild of the existing flight-themed study timer. Kotlin, Jetpack Compose, Room and a foreground timer service. The previous Three.js/Globe.gl web prototype remains in `index.html` while migration is underway.

The current native milestone implements a persisted countdown, pause/resume/cancel, arrival records and ongoing notifications. The temporary timer screen is not the final iOS-matched interface. See [implementation status](docs/implementation-status.md), [UI specification](docs/ui-spec.md) and [feature map](docs/feature-map.md) for scope and remaining work.

## Build and test

Requires JDK 17 or newer and Android SDK 36. Configure `sdk.dir` in the ignored `android/local.properties` file.

```sh
cd android
./gradlew assembleDebug testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

Device tests require one explicitly selected device, or configure `ANDROID_SERIAL` for the desired ADB target. The UI test currently runs on API 36 and below because the installed Espresso version uses an input API removed on API 37. Persistence and service tests run on both.

Phone debug APK: `android/app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`. Emulator APK: `app-x86_64-debug.apk` in the same directory. Both are ignored by Git. Do not publish this personal build. Map configuration instructions: [map-setup.md](docs/map-setup.md).

## Assets

Only original or independently licensed native resources are used. See [asset provenance](docs/assets.md). iOS reference images under `docs/references` are research evidence and are never bundled as application assets.

## License

This project is licensed under the MIT License. See the [LICENSE.md](LICENSE.md) file for details.
