package app.fakh.patches.coverscreen

import app.fakh.patches.coverscreen.shared.Constants.COMPATIBILITY_COVERSCREEN
import app.fakh.patches.shared.disableProviders
import app.fakh.patches.shared.pairip.pairipLicenseCheckPatch
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

/**
 * The Pro state lives on the Room entity `apps.ijp.coverscreenos.db.OSProperties`
 * (SQLCipher database, written by the native `sabtopw` method after a Firebase
 * license validation). Every Java-side reader goes through its getters:
 *
 * - `A()` returns the `vanisb` field, the Pro flag. The settings UI reads it at
 *   every feature gate (15+ call sites in CoverOSSettings alone) and the cover
 *   engine gates notification actions on it.
 * - `B()` returns `varMirSam`, the trial expiry timestamp. The engine's lock
 *   check is `licensed = I() || B() > now`, so a far-future value keeps the
 *   trial branch open forever.
 * - `z()` returns `valer`, the license email (sentinel `-1` when none). The
 *   engines' `I()` and the "has license email" flows (`H1`, `i2`) all compare
 *   it against the sentinel, and the paywall card keys off those flows.
 *
 * The license itself is validated server-side (Firestore + App Check, device
 * model bound), but forcing the getters makes every read see a licensed state
 * regardless of what the server wrote.
 */
private val isProFingerprint = Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type == "Lapps/ijp/coverscreenos/db/OSProperties;" &&
            method.name == "A" &&
            method.implementation != null
    },
)

private val trialExpiryFingerprint = Fingerprint(
    returnType = "J",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type == "Lapps/ijp/coverscreenos/db/OSProperties;" &&
            method.name == "B" &&
            method.implementation != null
    },
)

private val licenseEmailFingerprint = Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type == "Lapps/ijp/coverscreenos/db/OSProperties;" &&
            method.name == "z" &&
            method.implementation != null
    },
)

/**
 * `CoverScreenOSApplication.n()` exists only to launch the coroutine that
 * initializes AdMob (`ze3` case 0, all it does is the `MobileAds` init chain).
 * The disabled init provider alone still leaves this eager init running and
 * fetching config on every launch. Premium hides every ad slot, so nothing
 * waits on it.
 */
private val adsInitFingerprint = Fingerprint(
    returnType = "V",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type == "Lapps/ijp/coverscreenos/CoverScreenOSApplication;" &&
            method.name == "n" &&
            method.implementation != null
    },
)

/**
 * The two-boolean state holder (`a`, `b`) behind the timeout dialog: its text
 * and second button depend on `a`. See the execute block note.
 */
private val az5HolderFingerprint = Fingerprint(
    returnType = "V",
    parameters = listOf("Z", "Z"),
    custom = { method, classDef ->
        classDef.type == "Laz5;" && method.name == "<init>" && method.implementation != null
    },
)

/**
 * The billing/license snapshot (`ni5`) that `vw` builds from the Firebase
 * `app_bundle` claims and the Play purchases. Its second boolean is the Pro
 * verdict: `cx.g` (the flow `CoverOSSettings.a()` reads first, before falling
 * back to the license email) maps it straight to `Boolean`, and the settings
 * ad rows and the paywall card key off that. It stays false with no real
 * license, whatever the OSProperties getters say.
 */
private val billingStateFingerprint = Fingerprint(
    returnType = "V",
    custom = { method, _ ->
        // R8 renames the class every release, so match the constructor shape.
        method.name == "<init>" &&
            method.parameterTypes.map { it.toString() }.let { types ->
                types.size == 16 &&
                    types.slice(listOf(0, 1, 3, 4, 5, 6, 9, 14)) == listOf("Z", "Z", "Z", "Z", "Z", "Z", "Z", "I") &&
                    types.slice(listOf(7, 8, 10, 11, 12)).all { it == "Ljava/util/List;" } &&
                    types[13] == "Ljava/lang/String;"
            }
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
                "com.facebook.ads.AudienceNetworkContentProvider",
            )
        }
    }
}

@Suppress("unused")
val premiumUnlockPatch = bytecodePatch(
    name = "Premium unlock",
    description = "Unlocks Pro on your phone (every premium feature, no ads), keeps the trial from expiring, " +
        "stops the Play license check from closing the app, and keeps the ad SDK from starting so it cannot " +
        "load ads or track in the background.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_COVERSCREEN)
    category("Premium")
    dependsOn(adSdksManifestPatch, pairipLicenseCheckPatch)

    execute {
        isProFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )
        // ~256e9 ms, year ~9780. The lock check only compares it against the current time.
        trialExpiryFingerprint.method.addInstructions(
            0,
            """
                const-wide v0, 0x3b9aca0000L
                return-wide v0
            """,
        )
        licenseEmailFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "csospro@device.local"
                return-object v0
            """,
        )
        adsInitFingerprint.method.addInstructions(0, "return-void")
        // p2 is the Pro verdict (`b`), p6 is `f`, which the subscription sheet reads
        // (`t7b`) to pick "Manage Subscription" over the purchase options. The
        // registers sit above v15, so const/4 cannot encode them.
        billingStateFingerprint.method.addInstructions(
            0,
            """
                const/16 p2, 0x1
                const/16 p6, 0x1
            """,
        )

        // `CoverOSEngine.d3` is the engine's "cover engaged" flag (`a0` sets it,
        // `l2` clears it), not a license flag. Two byte-identical getters read it
        // through a StateFlow: the manager getter (`vr2.x()` in 244, a different
        // class name in other builds, so match the shape) and
        // `CoverFirstRunSetup.e()`. Both are forced true; a single fingerprint
        // cannot tell them apart (same body, different classes). Their effect on
        // the ads and the timeout picker has not been isolated on a device.
        var licensedGetters = 0
        // Only classes that read `d3`, found through the patcher's index.
        Fingerprint(
            filters = listOf(fieldAccess(definingClass = "Lapps/ijp/coverscreenos/CoverOSEngine;", name = "d3")),
        ).matchAllOrNull().orEmpty().map { it.originalClassDef }.distinctBy { it.type }.forEach { classDef ->
            val method = classDef.methods.singleOrNull { candidate ->
                val impl = candidate.implementation ?: return@singleOrNull false
                candidate.returnType == "Z" && candidate.parameters.isEmpty() &&
                    run {
                        val refs = impl.instructions
                            .mapNotNull { ins -> (ins as? ReferenceInstruction)?.reference?.toString() }
                            .toList()
                        refs.any { r -> r.startsWith("Lkotlinx/coroutines/flow/StateFlow;->getValue") } &&
                            refs.any { r -> r.startsWith("Lapps/ijp/coverscreenos/CoverOSEngine;->d3:Z") } &&
                            impl.instructions.count() < 20
                    }
            }
            if (method != null) {
                mutableClassDefBy(classDef).methods.single {
                    it.name == method.name && it.returnType == "Z" && it.parameters.isEmpty()
                }.addInstructions(
                    0,
                    """
                        const/4 v0, 0x1
                        return v0
                    """,
                )
                licensedGetters++
            }
        }
        if (licensedGetters != 2) throw PatchException("Expected 2 licensed getters (manager + CoverFirstRunSetup.e), found $licensedGetters")

        // The `pq2` holder keeps two "cover engaged" flows, recomputed from
        // `u || x() || engine.M()` and `engine.S0()`-style checks, which `ny3`
        // and the engine consumers read. Seed the two `StateFlowKt.a(Boolean.FALSE)`
        // constructions that back them with TRUE. The same FALSE register also
        // seeds a third flow that must stay FALSE, so the first seed is skipped.
        var licensedFlowHolder: ClassDef? = null
        // Only classes whose static initializer builds a StateFlow, found through the patcher's index.
        Fingerprint(
            name = "<clinit>",
            filters = listOf(methodCall(definingClass = "Lkotlinx/coroutines/flow/StateFlowKt;", name = "a")),
        ).matchAllOrNull().orEmpty().map { it.originalClassDef }.distinctBy { it.type }.forEach { classDef ->
            val clinit = classDef.methods.firstOrNull { it.name == "<clinit>" } ?: return@forEach
            val refs = clinit.implementation?.instructions
                ?.mapNotNull { ins -> (ins as? ReferenceInstruction)?.reference?.toString() }
                ?.toList() ?: return@forEach
            val mutableFlowFields = classDef.fields.count { it.type == "Lkotlinx/coroutines/flow/MutableStateFlow;" }
            val readOnlyFlowFields = classDef.fields.count { it.type == "Lkotlinx/coroutines/flow/StateFlow;" }
            if (mutableFlowFields >= 4 && readOnlyFlowFields >= 4 &&
                refs.count { it.startsWith("Lkotlinx/coroutines/flow/StateFlowKt;->a(") } >= 3 &&
                refs.any { it.startsWith("Lkotlinx/coroutines/flow/FlowKt;->b(") }
            ) {
                licensedFlowHolder = classDef
            }
        }
        val flowHolder = licensedFlowHolder ?: throw PatchException("Engine-state flow holder class not found")
        val flowClinit = mutableClassDefBy(flowHolder).methods.first { it.name == "<clinit>" }
        val flowInsns = flowClinit.implementation!!.instructions.toList()
        val falseSince = HashMap<Int, Int>()
        val flowSeeds = mutableListOf<Pair<Int, Int>>() // invoke index to seed register
        flowInsns.forEachIndexed { idx, ins ->
            when (ins.opcode) {
                Opcode.SGET_OBJECT -> {
                    val ref = (ins as ReferenceInstruction).reference.toString()
                    if (ref.startsWith("Ljava/lang/Boolean;->FALSE")) {
                        falseSince[(ins as OneRegisterInstruction).registerA] = idx
                    }
                }
                Opcode.INVOKE_STATIC -> {
                    val ref = (ins as ReferenceInstruction).reference.toString()
                    if (ref.startsWith("Lkotlinx/coroutines/flow/StateFlowKt;->a(")) {
                        val reg = (ins as FiveRegisterInstruction).registerC
                        if (falseSince.containsKey(reg)) {
                            flowSeeds += idx to reg
                        }
                    }
                }
                else -> {}
            }
        }
        if (flowSeeds.size != 3) throw PatchException("Expected 3 Boolean-seeded StateFlow constructions in clinit, found ${flowSeeds.size}")
        // Skip the first seed (engine liveness, must stay FALSE); flip the rest.
        flowSeeds.drop(1).sortedByDescending { it.first }.forEach { (idx, reg) ->
            flowClinit.addInstruction(idx, "sget-object v$reg, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;")
        }

        // The timeout dialog (`lz3`) reads `az5.a` off a StateFlow to pick its
        // text and to show or hide its second button. Force `a` true for every
        // instance; the class is a plain data holder with exactly three
        // consumers (the dialog, one flow mapper and one flow producer).
        val az5Init = az5HolderFingerprint.method
        az5Init.removeInstructions(0, az5Init.implementation!!.instructions.count())
        az5Init.addInstructions(
            0,
            """
                const/4 v0, 0x1
                iput-boolean v0, p0, Laz5;->a:Z
                iput-boolean p2, p0, Laz5;->b:Z
                return-void
            """,
        )
    }
}
