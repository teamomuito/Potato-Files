# potato files

A file manager for Android that also carries over everything from [octo potato](https://github.com/teamomuito/octo-potato): screenshot text search, swipe cleanup, deep clean, and the one-week tidy for throwaway screenshots. Everything runs on the phone. There's no account and no server.

## Tabs

- **files**: browse shared storage, search this folder and below, sort by name, date, size or type, multi-select, copy/move/paste, new folders, long press to select, "more" for per-file actions (open, rename, tags and note, zip, unzip, lock in vault, share, trash, secure delete). Text files open in the viewer, which also has a hex mode.
- **screenshots**: the octo potato home, with OCR search of every screenshot.
- **swipe**: the octo potato photo-by-month swipe.
- **clean**: the octo potato deep clean.
- **tools**: swipe through everything, storage analyzer, duplicates, batch rename, vault, timeline, privacy audit, wi-fi server, auto-sort, trash.

## Swipe through everything

Tinder-style cleanup for every file, not just photos. Files are shown oldest first. Swipe right to keep, left to bin, or use the buttons. Binned files wait in a pile you can review and rescue from before anything moves. Undo works on the last card. Decisions are remembered, so you won't see a file again.

It's available from any folder ("swipe this folder") and from Tools ("swipe through everything").

## Trash

Our own trash lives in a hidden `.OctoFilesTrash` folder on the same volume. Files are restored with a rename. Everything is purged after 30 days, checked once a day in the background and every time you open the trash.

## Vault

"lock in vault" encrypts a file with AES-256-GCM. The key lives in the Android Keystore, and the encrypted copy lives in app-private storage, so other apps and the gallery can't see it. Unlocking needs a fingerprint or screen lock. The original is overwritten and deleted after it's encrypted.

The fingerprint prompt guards the UI. The key doesn't itself require authentication, so this protects against casual access, not against someone who can run code as this app.

## Wi-Fi server

Tools → wi-fi server starts a read-only HTTP server on port 8080 for shared storage. Every request needs the six-digit code shown on the phone. It's plain HTTP, so only use it on a network you trust.

## Not built yet

- Cloud accounts (Drive, Dropbox, OneDrive). Each needs its own OAuth setup.
- Wi-Fi Direct and Bluetooth transfer.
- 7z and RAR, and password-protected zips.
- Map view for geotagged photos.
- Scripting and automation beyond auto-sort rules.
- Kids and student modes, and creator mode.
- OCR for non-Latin scripts (inherited from octo potato).
- Uploading through the wi-fi server.

## Permissions

- **All files access** (needed to browse). Requested from the files tab.
- **Media access**, **notifications**, **usage stats** and **media management** (screenshot and deep-clean features, inherited).
- **Internet**: only for the wi-fi server, which listens only while you start it.
- **Biometrics**: unlocks the vault.

## Build

Needs JDK 17 and the Android SDK, which Android Studio sets up.

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Releases

Pushing a tag like `v0.1.0` runs [`.github/workflows/release.yml`](.github/workflows/release.yml). It runs the unit tests, builds a release apk and attaches it to a GitHub release. Without signing secrets the apk is signed with the debug key. To sign with your own key, add the repo secrets `OCTO_KEYSTORE_B64` (base64 of the `.jks`), `OCTO_KEYSTORE_PASSWORD`, `OCTO_KEY_ALIAS` and `OCTO_KEY_PASSWORD`.

## License

MIT, same as octo potato. Sniglet is under the SIL Open Font License, see [`licenses/`](licenses).
