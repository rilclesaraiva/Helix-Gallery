# Changelog

All notable changes to **Helix Gallery** are recorded here, by version. This file is also the text of each GitHub Release, so it is written in English, for people who use the app.

Format inspired by [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning: [SemVer](https://semver.org/) — `MAJOR.MINOR.PATCH`.

## [Unreleased]

### Changed
- **New app icon:** the Helix Nebula with its full glow and texture.
- **Top buttons float:** Settings and the ⋮ menu now sit in translucent circles over the page, like the back button, instead of scrolling with the title.
- **Readable text and icons:** text and icons that were drawn in pure black (for example the top buttons) now use the theme colors.
- **No more waiting on screen changes:** tapping or going back right after opening a screen responds immediately.
- **Settings reorganized:** a home with categories (Appearance, Gallery, System, About) that open their own pages, with choices as radio buttons instead of pop-up menus. Icons follow the same drawing style as the author's other apps.
- **Update notice** is now a bottom panel (download, remind later or skip this version), and Settings → System → Updates lets you pause or resume these notices.
- **About** shows the developer's GitHub photo, the version and build, project links and legal pages.
- **Tabs remember where you were:** Photos, Albums and Search keep their scroll position when you switch tabs or open a photo and come back. Tapping the open tab again scrolls it to the top.
- **Screen animations:** screens slide in and out following the direction (open or back), the previous screen steps back and fades, and the back gesture follows your finger with the same animation.
- **Fast-scroll handle no longer jumps:** its position is now measured in pixels per row, so dragging stays steady even with day/month headers between photo rows.
- **No bars stuck to the top:** screen titles now scroll away with the content and the buttons float over the page with a translucent background. The back button no longer covers titles in Settings.
- **Fast-scroll handle:** only the handle itself reacts to your finger; sliding along the track just scrolls the list, and the handle follows your finger from where it is, without jumping.

### Added
- **Photo editor:** open a photo, tap ⋮ → Edit to rotate, flip, crop (1:1, 4:3, 3:4, 16:9 or the original shape, with zoom), adjust brightness, contrast and color, or apply a filter. It saves a copy next to the original; the original never changes.
- **Backup:** in Settings → System, save all your photos and videos into one ZIP file in a place you pick (phone folder, SD card or a cloud app's folder), and restore from a ZIP later. Photos already in the gallery are skipped. Nothing is sent by the app.
- **Clean up:** in the Albums tab, review screenshots, large videos (100 MB or more) and exact duplicate photos, and move the ones you pick to the trash in one go. Duplicates are found by comparing the files' content on your device (the oldest copy of each is kept).
- **Vault:** keep private photos and videos in a password-protected ZIP file (AES-256) in the Documents/HelixGallery folder. You create the password once and it is shown a single time (there is no recovery). Open it with the password or with your fingerprint, face or phone PIN. Select photos in the gallery and tap the lock to move them in; originals are only deleted after they are safely stored. You can restore items to the gallery or delete them. After uninstalling the app, any ZIP app can open the file with your password.
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
