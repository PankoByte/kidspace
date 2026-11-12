# Changelog

Notable changes to Kid Space, newest first. Version numbers match `versionName` in `app/build.gradle.kts`.

## 1.0 (2026-11-13)

First public release.

- Kid home screen that shows only the apps a parent chose.
- App protection through an Accessibility service that sends the child home from any other app and closes the notification panel.
- Play timer with 15, 30, 45 and 60 minute presets, a custom time, or no limit, and a "Time's up" screen with an option to add 15 minutes.
- 4-digit parent PIN, stored as a salted PBKDF2 hash, with a growing lockout after 5 wrong tries.
- Emergency link on the PIN screen that opens the dialer.
- Optional Do Not Disturb during Kid mode and an optional backup guard that uses Usage access.
- Takes the home app role only while Kid mode runs, then hands it back to the parent's launcher.
- App data excluded from cloud backup and device-to-device transfer.
- 13 languages.
