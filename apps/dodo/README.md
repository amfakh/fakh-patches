# DoDo Music

Patches for **DoDo Music** (`com.dundunstudio.autoplay`, "DoDo Music - Auto Game Clicker" on APKPure),
version `2.0.14`, as an XAPK.

## Patches

| Category | Patch | What it does |
|---|---|---|
| Premium | Premium unlock | Unlocks premium client-side: no ads and the premium features. Also stops the PairIP license check from closing the app, and keeps the AdMob, Facebook and Adjust SDK providers from starting at all, so they cannot collect or send anything in the background. |

## How the unlock works

Every premium check in the app funnels through one getter on the user entity that the server
fills in: `com.featurestudio.datastore.user.domain.UserEntity.isVip()`. The ad loader
(`loadChayeAd` skips loading AdMob interstitials when it returns true), the UI gates and
`UserDataStore.isPremium()` all read it. The patch makes that getter return `true`, which is
the same thing the mod APK for 2.0.9 does.

The app ships Google's PairIP protection: at startup,
`com.pairip.application.Application.attachBaseContext` calls
`LicenseClient.checkLicense(Context)`, which talks to the Play licensing service and closes
the app when the signature does not match the one Play knows. A repackaged APK always fails
that check, so the patch makes `checkLicense` return immediately.

The AdMob, Facebook and Adjust SDKs each start from a content provider before the app's
`Application.onCreate` runs. Their provider entries are disabled in the manifest, so none of
them loads at all — no ads, no analytics, no background work.
