package app.fakh.patches.smartlife

import app.fakh.patches.shared.gms.gmsCoreSupportResourcePatch
import app.fakh.patches.shared.gms.redirectToGmsCore
import app.fakh.patches.smartlife.shared.Constants.COMPATIBILITY_SMARTLIFE
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val SMARTLIFE_PACKAGE = "com.tuya.smartlife"

/** SHA-1 of Smart Life's signing certificate, which GmsCore presents to Google in place of the patched app's. */
private const val SMARTLIFE_SIGNATURE = "94dbfb934543ca1477b440add47566d757fd25ef"

/**
 * androidx.credentials' CredentialProviderFrameworkImpl.isAvailableOnDevice (Android 14+): the Android Credential
 * Manager routes the Google sign-in to Google's own Play services, which a GmsCore redirect cannot reach.
 */
private object FrameworkCredentialProviderFingerprint : Fingerprint(
    definingClass = "Luh8;",
    name = "isAvailableOnDevice",
    returnType = "Z",
    parameters = emptyList(),
)

@Suppress("unused")
val gmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "Makes \"Sign in with Google\" work. Signs in through microG RE (GmsCore) instead of Google " +
        "Play services, so it needs microG RE installed and signed in to a Google account.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SMARTLIFE)
    category("Feature")
    dependsOn(signatureSpoofPatch, gmsCoreSupportResourcePatch(SMARTLIFE_PACKAGE, SMARTLIFE_SIGNATURE))

    execute {
        redirectToGmsCore()

        // Without the framework provider, Credential Manager falls back to its Play services one, which
        // redirectToGmsCore already points at GmsCore.
        FrameworkCredentialProviderFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )
    }
}
