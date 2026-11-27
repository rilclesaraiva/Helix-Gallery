# Helix Gallery

A free, open-source photo and video gallery for Android, in Material 3 Expressive and with the colors of the Helix Nebula. Local first: your photos stay on your device.

> Status: early development (first build for testing). Android 12 (API 31) or newer.

## Features
- All your photos and videos, newest first, with **regular, by-day and by-month layouts**, 3–5 columns and a **fast-scroll handle by date**.
- **Albums** from your device folders, plus favorites, videos, screenshots and trash (the Android system ones, shared with other gallery apps).
- **Viewer** with swipe, pinch/double-tap zoom, video playback and details (EXIF).
- **Select, share, favorite, trash and restore.**
- Light, dark and AMOLED themes, optional wallpaper colors. English, Portuguese and Spanish.

### Planned
Locked vault (password-protected ZIP in a public folder, unlocked with your device biometrics/PIN), basic editor, cleanup (duplicates, screenshots, large videos), places and documents, and backup.

## Install
Download the latest APK from the [Releases](https://github.com/rilclesaraiva/Helix-Gallery/releases) page. The app checks for new versions in Settings → System → Updates.

## Build
```
./gradlew assembleRelease
./gradlew testDebugUnitTest
```
JDK 21, Android SDK 37. Release signing reads `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` from the environment (CI only; never commit keys).

## Built with AI
This project is developed with the help of an AI coding assistant (Claude) under the maintainer's direction, following a written project structure ([`CLAUDE.md`](CLAUDE.md), [`ARCHITECTURE.md`](ARCHITECTURE.md)). Every change goes through CI before it reaches a build.

## Legal
[Privacy policy](https://rilclesaraiva.github.io/Helix-Gallery/privacy) · [Terms](https://rilclesaraiva.github.io/Helix-Gallery/terms) · [Open-source licenses](https://rilclesaraiva.github.io/Helix-Gallery/licenses) · Licensed under the [GNU GPL v3.0](LICENSE).

## Support and contact
If you like the project, you can [buy me a coffee](https://buymeacoffee.com/saraivarilcle). Questions or ideas: saraivarilcle@gmail.com or open an [issue](https://github.com/rilclesaraiva/Helix-Gallery/issues).
