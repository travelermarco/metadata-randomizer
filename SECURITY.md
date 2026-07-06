# Security

Metadata Randomizer's entire point is privacy: photo/video processing (stripping and replacing GPS/device/timestamp metadata) happens **entirely on-device**, nothing is uploaded anywhere.

The app requests two permissions:
- `INTERNET` — used only by the built-in update checker, which calls the GitHub Releases API to check for and download new versions (see `UpdateChecker.kt`). It does not verify a checksum/signature of the downloaded APK beyond HTTPS, but Android's installer independently requires an update's signing certificate to match the currently installed app.
- `REQUEST_INSTALL_PACKAGES` — needed to launch the system install prompt for an updated APK downloaded via the above.

No other network access, analytics, or telemetry exists in this app.

## Reporting a vulnerability

If you find a security issue (e.g. a way for original metadata to leak instead of being replaced), please open a GitHub issue or contact the maintainer directly rather than disclosing it publicly first.
