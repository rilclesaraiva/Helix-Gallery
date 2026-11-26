# Changelog

All notable changes to **Helix Gallery** are recorded here, by version. This file is also the text of each GitHub Release, so it is written in English, for people who use the app.

Format inspired by [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning: [SemVer](https://semver.org/) — `MAJOR.MINOR.PATCH`.

## [Unreleased]

### Changed
- **Tabs remember where you were:** Photos, Albums and Search keep their scroll position when you switch tabs or open a photo and come back. Tapping the open tab again scrolls it to the top.
- **Screen animations:** screens slide in and out following the direction (open or back), the previous screen steps back and fades, and the back gesture follows your finger with the same animation.
- **Fast-scroll handle no longer jumps:** its position is now measured in pixels per row, so dragging stays steady even with day/month headers between photo rows.
- **No bars stuck to the top:** screen titles now scroll away with the content and the buttons float over the page with a translucent background. The back button no longer covers titles in Settings.
- **Fast-scroll handle:** only the handle itself reacts to your finger; sliding along the track just scrolls the list, and the handle follows your finger from where it is, without jumping.

### Added
- **Open with:** Helix Gallery now appears in the "Open with" list for photos and videos from other apps and opens them right away, with zoom for photos and controls for videos. Closing it takes you back to the app you came from.
- **First version of Helix Gallery:** a photo and video gallery for Android 12 and newer, in Material 3 Expressive, with the colors of the Helix Nebula. Everything stays on your device.
- **Photos tab:** all your photos and videos, newest first, with three layouts (regular, by day, by month), 3 to 5 columns and a fast-scroll handle that shows the month and year while you drag.
- **Albums tab:** your device folders, plus ready-made collections for favorites, videos, screenshots and the trash.
- **Viewer:** swipe between photos and videos, pinch or double-tap to zoom, play videos with the Media3 player, and see details (name, date, size, resolution, folder and camera data).
- **Select, share and clean up:** hold a photo to select several; share, favorite, move to the trash or restore them. Favorites and the trash are the Android system ones, so other gallery apps see them too.
- **Search:** find photos by file name, folder, type (photo, video, gif), year or month.
- **Settings:** theme (system, light, dark, AMOLED), optional wallpaper colors, layout and columns, update check and About (source code, license, privacy policy, terms).
- **Languages:** English, Portuguese and Spanish.
- **Pinch to resize the grid:** pinch on the Photos tab to change how many photos fit on screen, from by-day with 3 columns up to by-month.
- **Three-dots menus open as a bottom panel** (layouts on the photo lists, and share, favorite, details and trash in the viewer).
- **Video controls with the wavy progress bar:** play/pause, a progress bar that shows the time while you drag, and mute.
