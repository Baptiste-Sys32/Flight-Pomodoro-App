# Map setup

Use your own free Mapbox account. No credential from either comparison APK is usable for this project.

1. Create a public access token in the [Mapbox console](https://console.mapbox.com/).
2. In the existing ignored file `android/local.properties`, preserve the `sdk.dir` line and add `MAPBOX_PUBLIC_TOKEN=` followed by that public token.
3. Rebuild the debug application. Do not paste tokens into chat, commit the local file or put a secret download token into the app.

Current [Maps SDK for Mobile pricing](https://www.mapbox.com/pricing#maps) provides up to 25,000 monthly active users free. Reinstalls can count as additional users. This app is intended for one private user and only Maps SDK functionality; no paid Directions, Search or Navigation API is planned. Verify account terms and usage if that scope changes. No billing account or payment settings are changed by this project.

Vector maps, globe and detailed buildings have been inspected on the connected phone. Complete terrain, offline failure and battery validation remain pending. Satellite is omitted at the user's request. Adding a token does not establish visual fidelity. Before a token exists, the app must remain usable for timing and records and must not instantiate an SDK map that would crash from missing configuration.
