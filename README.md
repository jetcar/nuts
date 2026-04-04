# nuts

A small Android wood nuts-and-bolts puzzle game with two playable levels.

## Gameplay
- Tap a bolt to select it.
- Tap an empty socket to move the bolt.
- A plank disappears when both of its bolts have been moved away.
- Clear every plank to finish the level.

## Levels
- Level 1: Timber Trail
- Level 2: Sawmill Switchback

## Build
1. Ensure the Android SDK is available.
2. Run `./gradlew test` for unit tests.
3. Run `./gradlew assembleDebug` to build the debug APK.

## Releases
- Signed release builds are produced by GitHub Actions and attached to GitHub Releases.
- The workflow expects these repository secrets:
  - `ANDROID_KEYSTORE_BASE64`
  - `ANDROID_KEYSTORE_PASSWORD`
  - `ANDROID_KEY_ALIAS`
  - `ANDROID_KEY_PASSWORD`

### Create a signing key
Run this locally and keep the generated keystore private:

```bash
keytool -genkeypair \
  -v \
  -storetype PKCS12 \
  -keystore nuts-release.jks \
  -alias nuts-release \
  -keyalg RSA \
  -keysize 4096 \
  -validity 3650
base64 -w 0 nuts-release.jks
```

Add the base64 output and passwords to the repository secrets listed above.

## Updates
- On launch, the app checks the latest GitHub Release for `jetcar/nuts`.
- If a newer APK release is available, the app prompts the user to download it.
- Users can skip a specific release version from the in-app prompt.
