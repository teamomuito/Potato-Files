# 🥔 Potato Files

A cute file manager for Android. Everything runs on your phone. No account, no server, no tracking.

Built on [octo potato](https://github.com/teamomuito/octo-potato), so it also carries screenshot search, swipe cleanup and deep clean.

## What's inside

- **📁 files**: browse, search, sort, copy, move, rename, zip and unzip. Long-press to select.
- **📸 screenshots**: search the text in every screenshot.
- **💛 swipe**: swipe through photos month by month. Right to keep, left to say bye.
- **🧹 clean**: find old caches, big untouched files and duplicates.
- **🧰 tools**: swipe through *everything*, storage analyzer, batch rename, vault, trash, wi-fi server and more.

## Little promises

- 🗑️ **Trash** keeps things for 30 days before it's gone for good.
- 🔐 **Vault** locks files with AES-256 and a key only this phone has. Unlock with your fingerprint.
- 📡 **Wi-Fi server** is read-only and only runs while you start it. It's plain HTTP, so only use it on a network you trust.
- 🛡️ **Permissions** are asked for only when a feature needs them.

## Build it

You need JDK 17 and the Android SDK (Android Studio sets it up).

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Get a release

Releases are built by GitHub Actions and signed with the key in [`signing/`](signing). Grab the APK for your phone from the [releases page](../../releases). Most phones want the `arm64-v8a` one.

## License

MIT, same as octo potato. Sniglet is under the SIL Open Font License, see [`licenses/`](licenses).
