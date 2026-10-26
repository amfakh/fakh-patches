# Fakh Patches

Patches for use with [Morphe](https://github.com/MorpheApp). The only app covered so far is
**GitHub** (`com.github.android`), version `1.282.0`, as the APKM from APKMirror.

## Use

In Morphe Manager, add this URL as a patch source:

```
https://github.com/amfakh/fakh-patches
```

Or download the `.mpp` from [releases](https://github.com/amfakh/fakh-patches/releases/latest) and
load it as a local patch file.

## Patches

| Category | Patch | What it does |
|---|---|---|
| Theme | Material You | Colors the app from your wallpaper (Android 12+): the accent color instead of GitHub blue, and tinted surfaces, cards and borders in light and dark mode. Adds a **Fakh Patches** section to Settings with a **Material You** switch and a **Pitch black in dark mode** switch. |

The app draws its screens with both Views and Jetpack Compose, so the patch recolors both: resource
colors for the Views, and the Compose palettes and schemes when the app starts. Markdown and README
bodies are drawn by a WebView with GitHub's own CSS, so they keep their colors. Semantic colors (red,
green, yellow, purple, the diff lines) stay as they are, so a changed line is still easy to spot.

The switches are saved in the app's own settings. **Pitch black in dark mode** applies right away and
only affects dark mode. **Material You** shows a Restart bar, because the palettes are built once per
launch.

Install over the app you already have only if it was patched by the same Morphe Manager keystore;
otherwise Android asks you to uninstall first.

## Build

The Morphe Gradle plugin needs GitHub credentials for its package registry, even when building
offline from the Gradle cache. The extension module is an Android library, so it also needs an
Android SDK.

```bash
ANDROID_HOME=<android sdk> GITHUB_ACTOR=<github user> GITHUB_TOKEN=$(gh auth token) ./gradlew :patches:buildAndroid
```

The bundle lands in `patches/build/libs/patches-<version>.mpp`.

## Test on desktop

With [morphe-desktop](https://github.com/MorpheApp/morphe-cli/releases) (needs Java 21):

```bash
java -jar morphe-desktop-1.19.1-all.jar patch -p patches/build/libs/patches-<version>.mpp -o github-patched.apk com.github.android_1.282.0-953_4arch_6dpi_apkmirror.com.apkm
```
