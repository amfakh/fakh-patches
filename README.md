# Fakh Patches

Patches for use with [Morphe](https://github.com/MorpheApp). Apps covered so far:

| App | Package | Version | Format |
|---|---|---|---|
| GitHub | `com.github.android` | `1.282.0` | APKM from APKMirror |
| LinkedIn | `com.linkedin.android` | `4.1.1264` | APKM from APKMirror |

## Use

In Morphe Manager, add this URL as a patch source:

```
https://github.com/amfakh/fakh-patches
```

Or download the `.mpp` from [releases](https://github.com/amfakh/fakh-patches/releases/latest) and
load it as a local patch file.

## Patches

### GitHub

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

### LinkedIn

Based on [Michii Patches](https://github.com/heyymichii/michii-patches) by heyymichii (GPLv3).

| Category | Patch | What it does |
|---|---|---|
| Ads | Hide ads, Hide promoted jobs, Hide suggested posts, Hide Premium upsells | Removes promoted posts and jobs, suggested posts, and Premium/AI upsells, including the profile menu card. |
| Feed | Feed filters, Disable double-tap like | Optional filters (focus mode, celebrations, job cards, network activity, videos, "New posts" pill, "Show translation"), and no like on double tap. |
| Media | Download media | A download button on full screen photos, videos, profile photos and banners. |
| Chat | Messaging | Hides sponsored messages, with an optional ghost mode. |
| Navigation | More tab | Hides items of the More tab (Puzzle games, Groups, Events, Newsletters), or moves them to the bottom of the profile menu and removes the tab. Adds a Puzzle games shortcut to the LinkedIn app icon, which has its own switch. |
| Links | Open links directly, Sanitize share links | Skips the warning page, resolves `lnkd.in` links, and strips tracking from shared links. |
| Privacy | Block tracking | Optional block of most analytics events. |
| Theme | Material You, Remove gaps and dividers | Wallpaper colors for accents, backgrounds, cards, bars and borders, and flat lists without gaps or dividers. |
| Sign-in | GmsCore support | Sign in with Google through [microG RE](https://github.com/MorpheApp/microg-RE). |

Settings are in **Fakh Patches** on LinkedIn's Settings screen (profile menu → Settings & Privacy), or long press
the Home tab.

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
