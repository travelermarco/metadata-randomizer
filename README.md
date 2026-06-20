# Metadata Randomizer

An Android app that **strips and replaces photo/video metadata with randomized fake data** before sharing — so your real GPS location, device identity, and timestamps never leave your phone.

## How it works

When you share a photo or video, instead of sending it directly to WhatsApp, Telegram, or any other app, you choose **Metadata Randomizer** first. The app processes the file in under a second and then opens the normal share sheet so you can pick your destination.

What gets replaced on every file:

| Field | What you get instead |
|-------|---------------------|
| GPS coordinates | Random location near a real city (London, Tokyo, Dubai…) |
| Device make/model | Random Android device from a pool of 30+ real models |
| Timestamps | Random date between 6 months and 3 years ago |
| Filename | Random `IMG_XXXXXXXX.jpg` / `VID_XXXXXXXX.mp4` |
| Software / firmware | Matching the fake device profile |
| Serial numbers, comments, copyright | Removed |

For videos, the file is fully remuxed — only the audio and video tracks are kept, all metadata tracks are dropped.

## Usage

1. Open your Gallery and select a photo or video
2. Tap **Share**
3. Choose **Metadata Randomizer** from the share sheet
4. Done — a new share sheet opens with the anonymized file ready to send

No configuration needed. No accounts. No internet access required.

## Installation

Download the latest APK from the [Releases](../../releases) page and install it on your Android device (Android 7.0+).

> You may need to allow installation from unknown sources in your device settings.

## Build from source

Requirements: JDK 17, Android SDK (API 35), Gradle 8.6

```bash
git clone https://github.com/travelermarco/metadata-randomizer.git
cd metadata-randomizer
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

## Technical details

- **Images**: decoded via `BitmapFactory` (all original EXIF discarded), re-encoded as JPEG 95%, fake EXIF injected via `androidx.exifinterface`
- **Videos**: remuxed with `MediaExtractor` + `MediaMuxer` (metadata/subtitle tracks excluded)
- **Sharing**: processed files served via `FileProvider`, original URIs never forwarded
- Language: Kotlin · Min SDK: 24 (Android 7.0) · No third-party dependencies beyond AndroidX

## License

MIT
