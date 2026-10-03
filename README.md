# Retro Pong

An Atari-style Pong game for Android, built with Kotlin and Jetpack Compose.

Features: main menu, pause and game over screens, three difficulty levels, sound, saved best rally and win count, English / Tamil / Hindi, per-app language support (Android 13+), no permissions, no network, no data collected.

## Architecture

```
app/src/main/java/com/example/retropong/
├── MainActivity.kt          App entry, simple screen navigation
├── game/GameEngine.kt       Pure Kotlin game rules (no Android code, fully unit tested)
├── data/SettingsRepository  DataStore: difficulty, sound, best rally, wins
├── audio/SoundManager       Beeps from ToneGenerator (no audio files)
└── ui/
    ├── GameViewModel        Connects engine + settings + sound to the UI
    ├── GameScreen           Canvas drawing, touch input, frame loop, overlays
    ├── MenuScreen
    └── SettingsScreen
```

Data flow: touch input -> `GameViewModel` -> `GameEngine` (returns new `GameState` + events) -> `StateFlow` -> Compose redraw. Events (wall hit, score...) trigger sound and saved stats.

## 1. Copy the files into your project

Copy everything under `app/src/` into your Android Studio project's `app/src/`, overwriting the existing files with the same names. Also copy `.github/` to the project root.

If your old `MainActivity.kt` contains the camera AI code, save it first (for example in a Git branch called `camera-ai`).

## 2. Update `app/build.gradle.kts`

Add inside `dependencies { }` (and remove the old CameraX / ML Kit lines, since this app does not use the camera):

```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
implementation("androidx.datastore:datastore-preferences:1.1.1")
testImplementation("junit:junit:4.13.2")
```

Inside `android { defaultConfig { } }`, set a **unique** application ID and a version. Google Play does not accept IDs starting with `com.example`, so change only `applicationId` (do not rename the Kotlin package):

```kotlin
applicationId = "com.yourname.retropong"   // choose your own, permanent once published
versionCode = 1
versionName = "1.0.0"
// targetSdk must be 36 or higher for new Play submissions
```

Inside `android { }`, add release signing and shrinking (the signing values come from environment variables, so no secrets are stored in the repo):

```kotlin
signingConfigs {
    create("release") {
        val path = System.getenv("KEYSTORE_PATH")
        if (path != null) {
            storeFile = file(path)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
}
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        if (System.getenv("KEYSTORE_PATH") != null) {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

Click **Sync Now**.

## 3. Run the tests

In Android Studio, right-click `GameEngineTest` and choose **Run**, or from a terminal:

```
./gradlew testDebugUnitTest
```

## 4. Git and GitHub (DevOps)

```
git init
git add .
git commit -m "Retro Pong 1.0"
git branch -M main
git remote add origin https://github.com/<you>/retro-pong.git
git push -u origin main
```

The workflow in `.github/workflows/android.yml` then runs lint, unit tests and a debug build on every push.

Never commit your keystore (`.jks`) file. Back it up somewhere safe.

## 5. Release to Google Play

1. Create an upload keystore (Android Studio: Build > Generate Signed App Bundle > Create new, or `keytool`).
2. In your GitHub repo, add these secrets: `KEYSTORE_BASE64` (the keystore encoded with base64), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Tag a release: `git tag v1.0.0 && git push origin v1.0.0`. The pipeline builds a signed `.aab` and attaches it as a build artifact.
4. In Google Play Console: create the app, upload the `.aab`, and fill in the store listing, screenshots, privacy policy URL (see `docs/PRIVACY_POLICY.md`), content rating, target audience, and Data safety form (this app collects no data).
5. Requirements to re-check in Play Console before every release, because they change:
   - New apps and updates must target Android 16 (API 36) or higher from 31 August 2026.
   - A personal developer account created after 13 November 2023 must run a closed test with at least 12 testers opted in for 14 continuous days before applying for production access.
6. Promote from Internal testing, to Closed testing, to Production.

## Quality checklist

- [x] Game logic separated from UI and unit tested
- [x] State survives configuration changes (ViewModel)
- [x] Auto-pause when the app goes to the background
- [x] Localization: English, Tamil, Hindi (please have a native speaker review the translations)
- [x] Accessibility: content descriptions, scalable text sizes, high contrast
- [x] No permissions, no network, no tracking
- [x] CI pipeline, release signing from secrets, R8 shrinking
- [ ] Add your own app icon (Android Studio: right-click `res` > New > Image Asset)
- [ ] Take screenshots on a phone for the Play listing
- [ ] Test on at least two different phones
