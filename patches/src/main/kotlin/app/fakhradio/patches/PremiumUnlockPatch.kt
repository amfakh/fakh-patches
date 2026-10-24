package app.fakhradio.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.fakhradio.patches.shared.Constants.COMPATIBILITY_RADIO_ID

/**
 * Every premium check in the app (ads manager, mini player EQ button, paywall)
 * funnels through one getter:
 *
 * ```
 * // com.appgeneration.ituner.appunlock.b implements appunlock.a
 * boolean a() { return prefs.getBoolean(<unlock key>, false); }
 * ```
 *
 * `b()` writes `true` after a purchase, `c()` writes `false` when billing
 * reports nothing owned. R8 renames the class and its methods, but keeps the
 * `appunlock` package, and that package holds only the interface and its single
 * implementation, so the getter is the one concrete `()Z` method in it.
 */
private val isUnlockedFingerprint = Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type.startsWith("Lcom/appgeneration/ituner/appunlock/") &&
            method.implementation != null
    },
)

@Suppress("unused")
val premiumUnlockPatch = bytecodePatch(
    name = "Premium unlock",
    description = "Unlocks the lifetime premium client-side (no visual ads, music equalizer).",
    default = true,
) {
    compatibleWith(COMPATIBILITY_RADIO_ID)

    execute {
        isUnlockedFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )
    }
}
