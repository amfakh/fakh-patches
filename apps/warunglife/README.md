# Warung Life Simulator

Patches for **Warung Life Simulator** (`com.neopawstudio.warunglife`), version `1.2`, as an APKS bundle (APKCube)
or merged APK.

## Patches

Every patch except Clone and GmsCore support is on by default. Morphe Manager groups them by category.

| Category | Patch | What it does |
|---|---|---|
| Ads | Hide ads | Removes the ads shown when you level up and gives the rewards of rewarded ads (full energy, extra side job, double pay, upgrade discount) without watching them. The patched app also opens normally instead of closing on Google Play's license check. |
| Feature | Clone | Installs the game as a separate app next to the Play Store version. Progress is not shared between the two. Options: **App name** (default "Warung Life Simulator (Clone)") and **Package suffix** (default `fakh`, giving `com.neopawstudio.warunglife.fakh`); a different suffix installs another clone. |
| Feature | GmsCore support | Makes "Tautkan akun" with Google work by signing in through microG RE (GmsCore) instead of Google Play services, so the warung saved to your Google account comes back. Needs microG RE installed and signed in to a Google account. |

## How it works

The game is built with Godot. Its scripts reach Unity Ads only through one Android plugin, `GodotUnityAds`, which
loads and shows an ad and reports back when it finished. The patch makes that plugin answer every request at once as
if the ad had played to the end, so the game pays the reward and moves on. The Unity Ads SDK is never started.

Daily limits the game sets for rewarded ads (energy refills per day, for one) still apply. The "Tanpa Iklan" purchase
is not unlocked.

Linking a Google account ("Tautkan akun" > Google) only works with GmsCore support. Google hands the game its sign-in
token only when the request comes from the developer's own signing key, which a patched app never has. microG RE
signs in on the game's behalf with the original package and key, and that works with Clone too. On Android 14 and
newer the game's first sign-in method always goes to Google Play services and fails, so the patch sends it straight to
the older Google sign-in that microG RE handles.

The app also ships Google's PairIP protection. At startup it asks Google Play whether the app was signed by the
developer, and closes itself when it was not. A patched app is always signed with another key, so the patch skips
that check.

## Install

Without Clone, the Play Store app must be uninstalled first: it is signed with another key. Uninstalling deletes the
save on the phone, so link the game to your Google account first if you want to keep your warung. With Clone, the
patched game installs next to the Play Store app and starts a new save.

## Build

See the [repo README](../../README.md).

## Test on desktop

With [morphe-desktop](https://github.com/MorpheApp/morphe-desktop/releases) 2.2.0 or newer (needs Java 21):

```bash
java -jar morphe-desktop-2.2.0-all.jar patch -p patches/build/libs/patches-<version>.mpp -o warunglife-patched.apk <Warung Life 1.2 APKS>
```

## Getting the APK

Download the `1.2` APKS from APKCube by hand. Morphe Manager's own lookup only knows APKMirror and Uptodown pages.
