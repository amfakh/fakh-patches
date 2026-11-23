/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.patches.mazii

import app.fakh.patches.mazii.shared.Constants.COMPATIBILITY_MAZII
import app.fakh.patches.shared.disableProviders
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch

/**
 * Starts the ad SDKs that run from a content provider before Application.onCreate. The app itself skips every ad slot
 * and the SDK init while Premium is on, so this depends on the Premium unlock.
 */
private val adSdkManifestPatch = patch {
    dependsOn(unlockPremiumPatch)

    execute {
        document("AndroidManifest.xml").use { manifest ->
            manifest.disableProviders(
                "com.facebook.ads.AudienceNetworkContentProvider",
                "com.vungle.ads.VungleProvider",
                "com.mbridge.msdk.config.component.status.MBComponentLifecycleProvider",
            )
        }
    }
}

@Suppress("unused")
val hideAdsPatch = patch(
    name = "Hide ads",
    description = "Removes banner, native, app open, interstitial and rewarded ads.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_MAZII)
    category("Ads")
    dependsOn(adSdkManifestPatch)
}
