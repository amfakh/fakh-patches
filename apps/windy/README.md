# Windy

Patch for **Windy** (`com.windyty.android`, "Windy.com - Weather Forecast" on APKMirror),
version `51.3.2`, as an APKM.

## Patches

| Category | Patch | What it does |
|---|---|---|
| Premium | Unlock Premium | Unlocks Windy Pro client-side: higher tile zoom, 1-hour forecast steps, the premium calendar view and all premium UI. |

## How the unlock works

Windy is a Capacitor web-hybrid app: every subscription check lives in a single minified
JS bundle shipped in the APK, at `assets/public/v/<version>/mobile.js` (the versioned
directory changes each release, so the patch scans for it). The patch rewrites five
same-length byte spans in that bundle:

- The `subscription` store's default becomes `premium`, so after any cache eviction the
  store still reports a subscription.
- The `xr` premium flag is forced true at module load, before the account API answers.
- `ju()` (hasAny), the UI gate for zoom caps, forecast steps and the paywall, always
  returns true.
- The tier-clearing function can no longer write `null` to the store, so the premium
  default survives the logged-out account response.
- The same function adds the `subs-${tier}` body class instead of removing it, and the
  stylesheet uses `body.subs-premium` to hide the "Go Premium" button.

No server is contacted to fake an entitlement: the app simply treats the subscription
store as if it held `premium`, exactly like the paid tier does.
