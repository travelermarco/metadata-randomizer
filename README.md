# Metadata Randomizer

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?logo=open-source-initiative&logoColor=white)](LICENSE)

An Android app that **strips and replaces photo/video metadata with randomized fake data** before sharing — so your real GPS location, device identity, and timestamps never leave your phone in the shared file. All processing happens on-device.

## How it works

When you share a photo or video, instead of sending it directly to WhatsApp, Telegram, or any other app, you choose **Metadata Randomizer** first. The app processes the file (or files — single or multiple photos/videos at once are supported) in under a second and then reopens the normal share sheet so you can pick your destination.

What gets replaced on every photo:

| Field | What you get instead |
|-------|---------------------|
| GPS coordinates | Random location near a real city (London, Tokyo, Dubai…) from a pool of 30 cities worldwide, with small random jitter |
| Device make/model | Random Android device from a pool of 30+ real device profiles (Samsung, Xiaomi, Pixel, OnePlus, Sony, etc.) |
| Timestamps | Random date/time between 6 months and 3 years ago |
| Filename | Random `IMG_XXXXXXXX.jpg` / `VID_XXXXXXXX.mp4` |
| Software / firmware | Matching the fake device profile |
| Serial number, user comment, description, artist, copyright, camera owner | Removed |

For videos, the file is fully remuxed — only the audio and video tracks are kept, all metadata and subtitle tracks are dropped. Video files don't get a fake device/GPS profile injected (there's no equivalent metadata to write in a bare MP4 remux); the filename is still randomized.

## Usage

1. Open your Gallery and select one or more photos/videos
2. Tap **Share**
3. Choose **Metadata Randomizer** from the share sheet
4. The app shows a brief confirmation of what was changed for each file, then automatically reopens the share sheet with the anonymized file(s) ready to send

You can also launch the app directly from the home screen, which just shows usage instructions (and checks for updates — see below).

No configuration or accounts needed. The metadata processing itself works fully offline.

> **Note:** if you send as a "Photo" in Telegram or WhatsApp, those apps re-compress and strip all metadata themselves regardless of this app. To verify Metadata Randomizer actually did its job, send as a "File" instead.

## Built-in update checker

The app checks the GitHub Releases API (`api.github.com/repos/travelermarco/metadata-randomizer/releases/latest`) for a newer version — on launch, and after processing a share. If one is found, an "Update available" banner appears:

- If the release has a `.apk` asset attached, tapping the banner downloads it and launches the system install prompt directly.
- Otherwise, it opens the GitHub release page in the browser.

This is the **only** network activity in the app — nothing related to your photos, videos, or their metadata is ever sent anywhere. No analytics or telemetry.

## Permissions

- `INTERNET` — used only by the update checker described above.
- `REQUEST_INSTALL_PACKAGES` — needed to launch the system install prompt for an update APK downloaded via the update checker.

## Installation

Download the latest APK from the [Releases](../../releases) page and install it on your Android device (Android 7.0+).

> You may need to allow installation from unknown sources in your device settings.

## Build from source

Requirements: JDK 17, Android SDK (compileSdk/targetSdk 35), Gradle 8.6 (via wrapper)

```bash
git clone https://github.com/travelermarco/metadata-randomizer.git
cd metadata-randomizer
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

A GitHub Actions workflow (`.github/workflows/ci.yml`) builds the debug APK on every push/PR to `main`.

## Technical details

- **Images**: decoded via `BitmapFactory` (all original EXIF discarded in the process); visual orientation is corrected from the original EXIF orientation tag before re-encoding, then re-encoded as JPEG at quality 95; fake EXIF (device, timestamps, GPS) is written back via `androidx.exifinterface`, and identifying tags (user comment, description, artist, copyright, camera owner, serial number) are cleared
- **Videos**: remuxed with `MediaExtractor` + `MediaMuxer`, copying only audio/video tracks (metadata/subtitle tracks excluded)
- **Sharing**: processed files are served via `FileProvider`; original content URIs are never forwarded to the destination app; both single (`ACTION_SEND`) and multiple (`ACTION_SEND_MULTIPLE`) shares are supported
- **Update checker**: separate from the metadata pipeline, see above
- Language: Kotlin · Min SDK: 24 (Android 7.0) · Target/compile SDK: 35
- Dependencies: AndroidX (`core-ktx`, `appcompat`, `lifecycle-runtime-ktx`, `exifinterface`), Material Components, and `kotlinx-coroutines-android`

## Security

See [SECURITY.md](SECURITY.md) for a fuller breakdown of what data leaves the device (short version: nothing except the update check above) and how to report issues.

## License

MIT
