# Smart Life

Patches for **Smart Life** (`com.tuya.smartlife`, "SmartLife - Smart Living" on APKPure), version
`7.12.3`, as an XAPK.

## Patches

| Category | Patch | What it does |
|---|---|---|
| Layout | Hide Plus button | Removes the "Plus" pill from the home header. |
| Layout | Hide Recommended for you | Removes the "Recommended for You" section and its cards from the home screen. |
| Layout | Hide AI ask bar | Removes the "How can I help?" Tuya AI bar above the bottom navigation. |
| Layout | Hide Explore tab | Removes the Explore tab from the bottom navigation. |
| Layout | Hide Rewards Hub | Removes the "Rewards Hub" row and its banner from the Me tab. |
| Layout | Hide My Orders | Removes the "My Orders" row from the Me tab. |
| Layout | Hide Tuya AI Web Version | Removes the "Tuya AI Web Version" row from the Me tab. |
| Privacy | Disable analytics | Stops AppsFlyer from tracking installs and in-app events, and turns off Firebase Analytics, Crashlytics and Sessions. Tuya's own usage statistics stay on. |
| Feature | GmsCore support | Makes "Sign in with Google" work through [microG RE](https://github.com/MorpheApp/microg-RE) instead of Google Play services, so it needs microG RE installed and signed in. Off by default. |

The layout patches and Disable analytics are on by default.

## Signature check

Smart Life's native security library reads the app's signing certificate and kills the process when it
is not Tuya's, so a resigned app closes a few seconds after it starts. Every patch here pulls in a
hidden helper that makes the app see the original certificate again, through a PackageManager proxy
(the same idea as [ApkSignatureKillerEx](https://github.com/L-JINBIN/ApkSignatureKillerEx)).

The installed app must be replaced: the Play Store build is signed with another key, so uninstall it
first (local data is lost, devices stay in your Tuya account).

## Google sign-in

The app signs in through Android's Credential Manager, which on Android 14+ goes to Google's own
Play services and skips GmsCore. **GmsCore support** turns that framework provider off so the app
falls back to the Play services one, which is pointed at microG RE.

## Build

See the [repo README](../../README.md).

## Test on desktop

With [morphe-desktop](https://github.com/MorpheApp/morphe-cli/releases) (needs Java 21):

```bash
java -jar morphe-desktop-1.19.1-all.jar patch -p patches/build/libs/patches-<version>.mpp -o smartlife-patched.apk <Smart Life 7.12.3 XAPK>
```

## Getting the APK

Download the `7.12.3` XAPK from APKPure by hand. Morphe Manager's own lookup only knows APKMirror
and Uptodown pages.
