# FOMO Indonesia

Patches for **FOMO Indonesia** (`id.anon.android`, "FOMO Indonesia" on APKPure),
version `9.0.64`, as an XAPK.

## Patches

| Category | Patch | What it does |
|---|---|---|
| Premium | Unlock FOMO+ | Unlocks FOMO+ on your phone: salary insight summary and role level filters, subscriber reaction styles, and the premium profile perks. Paywall screens stop blocking those features. Also hides promo cards, banners and sponsored posts in the home feed, and silences the "Gunakan logo default" 403 alert. |

## How the unlock works

FOMO is a React Native app: every screen lives in one Hermes JavaScript bundle
inside the APK. Your subscription status arrives from the FOMO server with your
profile (`subscription`: NONE, PLUS or PRO), and one function in the bundle —
`hasFomoPlusSubscription()` — decides for every paywall whether to show the
premium path or block it: the salary insight summary, the role level filter,
subscriber reaction styles, the profile card.

The patch rewrites that one function in the bundle so it always answers "yes".
The unlock happens on your phone only; nothing is bought or faked on the
server. Features where the server itself withholds data from non-subscribers
stay out of reach.

## What is not unlocked

- Salary data the FOMO API only serves to paying accounts.
- FOMO PRO-only perks that are enforced by the server (posting in f/Publik,
  Mynd usage limits).

## Build

See the [repo README](../../README.md).

## Test on desktop

With [morphe-desktop](https://github.com/MorpheApp/morphe-cli/releases) (needs Java 21):

```bash
java -jar morphe-desktop-2.2.0-all.jar patch -p patches/build/libs/patches-<version>.mpp -o fomo-patched.apk <FOMO Indonesia 9.0.64 XAPK>
```

## Getting the APK

Download the `9.0.64` XAPK from APKPure by hand.
