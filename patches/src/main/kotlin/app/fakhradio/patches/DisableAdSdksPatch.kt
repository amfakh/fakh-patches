package app.fakhradio.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.fakhradio.patches.shared.Constants.COMPATIBILITY_RADIO_ID
import app.fakhradio.patches.shared.disableProviders
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import org.w3c.dom.Element

/** Whether this method references anything whose descriptor starts with [prefix]. */
private fun Method.references(prefix: String): Boolean =
    implementation?.instructions?.any {
        (it as? ReferenceInstruction)?.reference?.toString()?.startsWith(prefix) == true
    } == true

/**
 * The ad manager's `Z(Activity)`, run by `Y(Activity)` when the main or player
 * screen starts. Its first call initializes every ad network and LevelPlay
 * (`usecase.a.c()`), with no premium check; the rest builds the banner,
 * interstitial, native and app open managers. With premium every display path
 * already checks `shouldShowAds()`, so skipping the method only drops the SDK
 * init.
 */
private val initAdNetworksFingerprint = Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
    custom = { method, classDef ->
        classDef.type.startsWith("Lcom/appgeneration/ituner/ad/") &&
            method.references("Lcom/appgeneration/ituner/ad/usecase/a;->c()V")
    },
)

/** `usecase.n.e()`: starts the AppHarbr ad verification SDK from the ad manager's `I()` at app start. */
private val initAppHarbrFingerprint = Fingerprint(
    returnType = "V",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type.startsWith("Lcom/appgeneration/ituner/ad/usecase/") &&
            method.references("Lcom/appgeneration/coreprovider/ads/appharbr/c;->")
    },
)

/** Ad SDKs that start themselves from a content provider before `Application.onCreate`. */
private val adSdksManifestPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { manifest ->
            manifest.disableProviders(
                "com.google.android.gms.ads.MobileAdsInitProvider",
                "io.bidmachine.BidMachineInitProvider",
                "com.magnite.sdk.adsbase.MagniteInitProvider",
                "com.mobilefuse.sdk.MobileFuseSdkInitProvider",
                "com.applovin.sdk.AppLovinInitProvider",
                "com.vungle.ads.VungleProvider",
                "com.facebook.ads.AudienceNetworkContentProvider",
                "com.mbridge.msdk.config.component.status.MBComponentLifecycleProvider",
                "com.smaato.sdk.core.lifecycle.ProcessLifecycleOwnerInitializer",
                "sg.bigo.ads.controller.provider.BigoAdsProvider",
            )
            // Unity runs through androidx.startup, which also hosts WorkManager
            // and lifecycle, so only its initializer entry goes.
            val metaData = manifest.getElementsByTagName("meta-data")
            (0 until metaData.length).map { metaData.item(it) as Element }
                .filter { it.getAttribute("android:name") == "com.unity3d.services.core.configuration.AdsSdkInitializer" }
                .forEach { it.parentNode.removeChild(it) }
        }
    }
}

@Suppress("unused")
val disableAdSdksPatch = bytecodePatch(
    name = "Disable ad SDKs",
    description = "Keeps the ad networks, LevelPlay and AppHarbr from starting at all, so they cannot track in the background. Requires Premium unlock.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_RADIO_ID)
    category("Privacy")
    dependsOn(premiumUnlockPatch, adSdksManifestPatch)

    execute {
        listOf(initAdNetworksFingerprint, initAppHarbrFingerprint).forEach {
            it.method.addInstructions(0, "return-void")
        }
    }
}
