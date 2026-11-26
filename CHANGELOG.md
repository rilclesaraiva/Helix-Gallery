# Changelog

All notable changes to **Helix Gallery** are recorded here, by version. This file is also the text of each GitHub Release, so it is written in English, for people who use the app.

Format inspired by [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning: [SemVer](https://semver.org/) — `MAJOR.MINOR.PATCH`.

## [Unreleased]

### Changed
- **Lock a photo without leaving it:** Add to → Vault (and "Move to vault" in the selection menu) now locks the photo right away with no password and no jump into the vault; a small message confirms it. The vault password is remembered by the app only for adding things; viewing the vault still needs the password or your fingerprint/PIN. (Until you open the vault once after updating, the old flow is used.)
- **Trash selection** shows Restore and Delete at the bottom of the screen; they are no longer in the ⋮ menu.
- **No reloading when you come back:** the gallery list stays in memory and keeps watching for changes while the app is open, so returning from another app no longer reloads everything.
- **Edit videos:** the Edit button now also works on videos — trim the clip, rotate, mute — and saves a copy.
- **New photos show up right away:** the gallery now watches images and videos live, so a new picture from another app appears without restarting.
- **Background music comes back:** playing a video pauses other media, and when the video pauses, ends or you leave it, the other player resumes.
- **Drag to select** now selects the whole range between where you started and where your finger is, like Google Photos; sliding back unselects.
- **Favorite heart** updates instantly and beats like BoomListener's.
- **Add to…** is a small panel: Album, a divider, then Vault.
- **Albums tools are editable:** tap the dotted "+" tile (Edit tools) to unpin, pin or reorder the shortcuts at the top.
- **Tabs are a swipeable carousel:** the page follows your finger and settles with the same motion as BoomListener's carousels.
- **Viewer bottom bar** has no grey background anymore: just the icons over the photo with a faint, transparent darkening behind them, like Google Photos. The ⋮ menu only lists what isn't already on the screen, and "Move to trash" is now just "Trash".
- **Swipe up on a photo** opens its details; double-tap zoom now glides toward the tapped point instead of jumping.
- **Add to…** (＋) button in the viewer, between Edit and Trash: add the item to the vault or copy it to another album.
- **Selection bar** works like BoomListener's: a floating bar with close, count, select all and a ⋮ menu with the actions, and it replaces the bottom navigation while you select (both animate in and out).
- **Confirmation panel** before moving to the trash or deleting, instead of the system's dialog.
- **Vault lock screen** redesigned (round password field with a show/hide eye button).
- **Switch tabs by swiping** sideways on Photos, Albums and Search.
- **Drag to select:** long-press a photo and, without lifting your finger, slide over others to select them too.
- **Smooth transitions everywhere:** any sudden change on screen now animates.
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

### Fixed
- Videos stop as soon as you leave them (they kept playing for a few seconds during the closing animation).
- Drag to select works again.
- Vault: swipe sideways inside a photo or video to move to the next one, like in the gallery.
- Long-pressing a single photo no longer unselects it when you lift your finger.
- Playing a video now pauses music and other media playing on the phone.
- Videos: pressing play after the video ended restarts it, and rotating the screen no longer sends the video back to the start.
- Photos: layout changes happen only by pinching (the ⋮ button is gone), and the photos now slide into their new places instead of jumping.
- Viewer: the bottom bar now spans the screen with labeled actions (Share, Edit, Trash) like Google Photos, and Favorite moved to the top.
- Vault: opening a photo or video inside the vault no longer locks it (it showed a black screen and asked for the password again). The vault now locks only when you leave the app or the screen.

### Added
- **Empty trash** is available from the ⋮ menu inside the Trash.
- **Delete without asking is requested on first launch**, right after photo access (Settings → Gallery → Deleting lets you turn it off). The video progress bar now glides smoothly like BoomListener's, and the Albums screen has compact shortcuts above a grid of album cards.
- Settings → Gallery → Deleting: shortcut to allow "Delete without asking", so moving to trash and deleting no longer show Android's confirmation panel.
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
