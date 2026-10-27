# LinkedIn

Patches for the LinkedIn app (`com.linkedin.android`, version `4.1.1264`, APKM from APKMirror).

Based on [Michii Patches](https://github.com/heyymichii/michii-patches) by heyymichii (GPLv3).

## Patches

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
