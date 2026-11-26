# Privacy Policy — Helix Gallery

_Last updated: 26 November 2026_

Helix Gallery is a free, open-source gallery app (GPL-3.0). This page says what the app does with your data.

## What the app does
- **Your photos and videos stay on your device.** The app reads them through Android's media library to show them. It does not upload, copy or send them anywhere. They leave your device only when you choose Share and pick another app.
- **Your choices** (theme, layout, columns) are saved on your device only.
- **The vault** is a password-protected ZIP file (AES-256) saved in your Documents/HelixGallery folder on the device. The password is never sent anywhere and the app cannot recover it. If you turn on quick unlock, the password is stored encrypted with a key held by Android's hardware-backed keystore, usable only after you authenticate with your biometrics or screen lock.
- **No account, no ads, no analytics, no trackers, no cookies.**

## Permissions
- **Photos and videos** (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`; on Android 12, storage read access): to list and show your media. The app asks for full access.
- **Media management** (`MANAGE_MEDIA`, optional): if you allow it in Android settings, the app can move items to the trash or delete them without Android asking each time. Without it, Android asks and nothing else changes.
- **Biometrics** (`USE_BIOMETRIC`): lets the vault ask Android to check your fingerprint, face or screen lock. Android does the check; the app never receives your biometric data.
- **Internet:** used only to ask GitHub whether a newer version exists (the latest release of the project's repository). The request carries the app name and version, as any web request carries them, and nothing from your gallery.

## Third parties
Update checks go to GitHub (see GitHub's privacy statement). When you share a photo, the app you pick receives it under its own policy.

## Contact
saraivarilcle@gmail.com or the project's issues on GitHub: https://github.com/rilclesaraiva/Helix-Gallery/issues
