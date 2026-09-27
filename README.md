# CastBridge TV

A clean Android screen-mirroring prototype inspired by modern Cast-to-TV apps.

## What is included

- Mirror Screen using Android MediaProjection
- Local Wi-Fi TV receiver served directly by the phone
- Browser-based TV receiver at port `8765`
- JPEG frame streaming over a local WebSocket connection
- Shareable receiver URL
- Cast Tools UI for Media / Web / Remote placeholders
- Android 8.0+ support, target SDK 35
- GitHub Actions workflow that builds a debug APK automatically

## Important architecture note

This version uses a **local browser receiver** rather than pretending to implement proprietary Chromecast/DLNA protocols. This makes the screen-mirroring flow deployable without a vendor SDK or cloud service:

1. Install the APK on the Android phone.
2. Put phone and TV on the same Wi-Fi network.
3. Tap **Mirror Screen** and approve Android's screen-capture permission.
4. Open `http://PHONE-IP:8765` in the TV's browser.
5. The TV browser displays the phone screen.

For best results, use a modern TV browser and a 5 GHz Wi-Fi network.

## Build on GitHub

1. Create/open a repository named `CastBridge`.
2. Upload the contents of this ZIP to the repository root.
3. Commit to `main`.
4. Open **Actions → Build CastBridge TV APK**.
5. Run the workflow or wait for the push build.
6. Open the completed workflow run and download the artifact **CastBridge-TV-debug-apk**.
7. Inside the artifact ZIP is `app-debug.apk`.

No Android Studio is required to build the APK on GitHub Actions.

## Next production upgrades

- Google Cast SDK support for compatible media casting
- DLNA/UPnP device discovery and media rendering
- QR code pairing
- Automatic TV discovery via mDNS/NSD
- Full-screen immersive receiver UI
- Photo/video playlist casting
- Brand-specific TV remote protocols
- Release signing / Play Store bundle
- Adaptive app icon and splash screen

## Project structure

```text
CastBridgeTV/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/castbridge/tv/
│       │   ├── MainActivity.java
│       │   ├── MirrorService.java
│       │   └── CastServer.java
│       └── res/values/
├── .github/workflows/android.yml
├── build.gradle
├── gradle.properties
└── settings.gradle
```
