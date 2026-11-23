# Mazii: Japanese Dictionary

Patches for **Mazii: Japanese Dictionary** (`com.mazii.dictionary`), version `6.8.64`, as an XAPK from APKPure.

## Patches

Every patch is on by default. Morphe Manager groups them by category.

| Category | Patch | Default | What it does |
|---|---|---|---|
| Ads | Hide ads | On | Removes banner, native, app open, interstitial and rewarded ads. |
| Feature | Disable Play license check | On | Stops Pairip's Google Play license check from blocking the patched app. |
| Premium | Premium unlock | On | Unlocks the client-side Premium checks. |
| Privacy | Disable analytics | On | Stops Firebase Analytics, Crashlytics, Performance and Sessions, and Facebook App Events. |

## How the patches work

**Premium unlock** forces `PreferencesHelper.isPremium`, the account-based premium check and `Account.Result.getPremium` to return true. The app skips its own ad slots and ad SDK startup while Premium is on, so **Hide ads** depends on it and also disables the Audience Network, Vungle and Mintegral content providers.

**Disable analytics** uses Firebase's manifest switches, disables `TransportBackendDiscovery` and turns off Facebook App Events and advertiser ID collection.

## Known limitations

Content that the server gates by account (for example AI features) cannot be unlocked from the client.

## Build

See the [repo README](../../README.md).
