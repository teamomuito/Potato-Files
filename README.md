# 🥔 Potato Files

A cute file manager for Android. Everything runs on your phone. No account, no server, no tracking.

Built on [octo potato](https://github.com/teamomuito/octo-potato), so it also carries screenshot search, swipe cleanup and deep clean.

## What's inside

- **📁 files**: browse, search, sort, copy, move, rename, zip and unzip. Long-press to select.
- **📸 screenshots**: search the text in every screenshot.
- **💛 swipe**: swipe through photos month by month. Right to keep, left to say bye.
- **🧹 clean**: find old caches, big untouched files and duplicates. With [Shizuku](https://shizuku.rikka.app) (no root needed) it also finds leftovers from uninstalled apps and clears every app's cache at once.
- **🧰 tools**: swipe through *everything*, storage analyzer, batch rename, vault, trash, wi-fi server and more.

## Little promises

- 🗑️ **Trash** keeps things for 30 days before it's gone for good.
- 🔐 **Vault** locks files with AES-256 and a key only this phone has. Unlock with your fingerprint.
- 📡 **Wi-Fi server** is read-only and only runs while you start it. It's plain HTTP, so only use it on a network you trust.
- 🛡️ **Permissions** are asked for only when a feature needs them. Shizuku is optional. potato checks whether it's running each time you open the clean tab, and only binds to it when you open the leftovers or deep cache sections.

## Build it

You need JDK 17 and the Android SDK (Android Studio sets it up).

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Get a release

Every push to the default branch builds the app and publishes a release, `build-N`, with the APKs and the changes since the last one. The newest is always at the [releases page](../../releases/latest). Most phones want the `arm64-v8a` APK. Only the 5 newest releases are kept.

Releases are signed with the key in [`signing/`](signing), which is committed on purpose so each build installs over the last. Anyone with that key can sign an APK that installs as an update, so only download releases from this repo.

## License

MIT, same as octo potato. DotGothic16 is under the SIL Open Font License, see [`licenses/`](licenses). The look is borrowed from [Pictoclip](https://github.com/teamomuito/pictoclip), the DS menu clipboard app.
