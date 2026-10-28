# DoDo Music

Patches for **DoDo Music** (`com.dundunstudio.autoplay`, "DoDo Music - Auto Game Clicker" on APKPure),
version `2.0.14`, as an XAPK.

## Patches

Enabled by default.

| Category | Patch | What it does |
|---|---|---|
| Premium | Premium unlock | Unlocks premium on your phone: no ads and every premium feature. The patched app also opens normally instead of closing on Google Play's license check, and the ad, Facebook and Adjust SDKs never start, so they cannot collect or send anything in the background. |

## How the unlock works

The app decides whether you are premium from one flag on your user profile, which the server
fills in. Ads, locked features and every premium check read that flag. The patch makes it always
say "premium", so the unlock happens on your phone only; no purchase is faked on the server.

The app also ships Google's PairIP protection. At startup it asks Google Play whether the app was
signed by the developer, and closes itself when it was not. A patched app is always signed with
another key, so the patch skips that check.

## What the app ships with, in plain terms

| SDK | What it is | What it does on your phone |
|---|---|---|
| **Google AdMob** | Ad network | Loads and shows the full screen ads, and reads the advertising id to target them. |
| **Facebook SDK** (Meta) | Meta's app SDK | Reports app events to Meta. What the app sends with it is not visible from the patch code. |
| **Adjust** | Install attribution | Tells the developer which ad or campaign brought you to the app, and tracks sessions for that. |

**How the patch stops them:** each of these SDKs starts itself from a small component that
Android runs before the app's own code. The patch switches those components off in the manifest,
so the SDKs are never loaded. Premium hides every ad slot, so nothing in the app looks for them
afterwards.

## Test on desktop

With [morphe-desktop](https://github.com/MorpheApp/morphe-cli/releases) (needs Java 21):

```bash
java -jar morphe-desktop-1.19.1-all.jar patch -p patches/build/libs/patches-<version>.mpp -o dodo-patched.apk <DoDo Music 2.0.14 XAPK>
```

## Getting the APK

Download the `2.0.14` XAPK from APKPure by hand. Morphe Manager's own lookup only knows APKMirror
and Uptodown pages, and falls back to a web search for this app.
