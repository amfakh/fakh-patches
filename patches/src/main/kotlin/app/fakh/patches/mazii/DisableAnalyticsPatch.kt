/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.patches.mazii

import app.fakh.patches.mazii.shared.Constants.COMPATIBILITY_MAZII
import app.fakh.patches.shared.firebase.firebaseManifestPatch
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import org.w3c.dom.Element

/** Turns off Facebook App Events and advertiser ID collection. */
private val analyticsManifestPatch = patch {
    execute {
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0) as Element
            val metaData = manifest.getElementsByTagName("meta-data")
            (0 until metaData.length).map { metaData.item(it) as Element }
                .filter {
                    it.getAttribute("android:name") in setOf(
                        "com.facebook.sdk.AutoLogAppEventsEnabled",
                        "com.facebook.sdk.AdvertiserIDCollectionEnabled",
                    ) && it.parentNode == application
                }
                .forEach { it.setAttribute("android:value", "false") }
        }
    }
}

@Suppress("unused")
val disableAnalyticsPatch = patch(
    name = "Disable analytics",
    description = "Turns off analytics and tracking.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_MAZII)
    category("Privacy")
    dependsOn(firebaseManifestPatch(), analyticsManifestPatch)
}
