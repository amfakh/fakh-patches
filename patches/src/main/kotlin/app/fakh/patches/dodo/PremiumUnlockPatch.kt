package app.fakh.patches.dodo

import app.fakh.patches.dodo.shared.Constants.COMPATIBILITY_DODO
import app.fakh.patches.dodo.shared.disableProviders
import app.fakh.patches.dodo.shared.references
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

/**
 * Every premium check in the app funnels through one data class getter:
 *
 * ```
 * // com.featurestudio.datastore.user.domain.UserEntity (Kotlin data class)
 * boolean isVip() { return this.isVip; }
 * ```
 *
 * The field is set from the server's user info response, so without a
 * subscription it is false. The obfuscated callers all follow the same shape:
 * `UserDataStore.isPremium()`, the ad loader (`loadChayeAd` skips AdMob
 * interstitial loading when true) and the Compose UI gates read it directly.
 * R8 keeps the `featurestudio.datastore.user.domain` package and the method's
 * name, because it is part of the data layer's public surface.
 */
private val isVipFingerprint = Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type == "Lcom/featurestudio/datastore/user/domain/UserEntity;" &&
            method.name == "isVip" &&
            method.implementation != null
    },
)

/**
 * `com.pairip.licensecheck.LicenseClient.checkLicense(Context)`, called from
 * `com.pairip.application.Application.attachBaseContext`. It binds to the
 * Play licensing service and closes the app when the response fails to verify.
 * A repackaged APK always fails it, so the call goes.
 */
private val checkLicenseFingerprint = Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    custom = { method, classDef ->
        classDef.type.startsWith("Lcom/pairip/licensecheck/") &&
            method.name == "checkLicense"
    },
)

/**
 * Ad and analytics SDKs that start themselves from a content provider before
 * `Application.onCreate`. Facebook also logs app events (its own analytics)
 * from the same init.
 */
private val adSdksManifestPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { manifest ->
            manifest.disableProviders(
                "com.google.android.gms.ads.MobileAdsInitProvider",
                "com.facebook.internal.FacebookInitProvider",
                "com.adjust.sdk.SystemLifecycleContentProvider",
            )
        }
    }
}

@Suppress("unused")
val premiumUnlockPatch = bytecodePatch(
    name = "Premium unlock",
    description = "Unlocks premium client-side (no ads, unlimited features) and stops the PairIP license check from closing the app. Keeps the AdMob, Facebook and Adjust SDK providers from starting at all, so they cannot collect or send anything in the background.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_DODO)
    category("Premium")
    dependsOn(adSdksManifestPatch)

    execute {
        isVipFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )
        checkLicenseFingerprint.method.addInstructions(0, "return-void")
    }
}
