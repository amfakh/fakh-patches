/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.patches.facer

import app.fakh.patches.facer.shared.Constants.COMPATIBILITY_FACER
import app.fakh.patches.shared.firebase.firebaseManifestPatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefForEach
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch

@Suppress("unused")
val disableAnalyticsPatch = patch(
    name = "Disable analytics",
    description = "Turns off Firebase Analytics, Crashlytics, Performance and Sessions, plus AppsFlyer.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_FACER)
    category("Privacy")
    dependsOn(firebaseManifestPatch())

    execute {
        var appsFlyerStarts = 0
        var purchaseObservers = 0
        classDefForEach { classDef ->
            when {
                classDef.superclass == "Lcom/appsflyer/AppsFlyerLib;" -> {
                    mutableClassDefBy(classDef).methods
                        .filter { it.name == "start" && it.returnType == "V" && it.implementation != null }
                        .forEach {
                            it.addInstructions(0, "return-void")
                            appsFlyerStarts++
                        }
                }
                classDef.type == "Lcom/appsflyer/api/PurchaseClientImpl;" -> {
                    mutableClassDefBy(classDef).methods
                        .filter { it.name == "startObservingTransactions" && it.returnType == "V" && it.implementation != null }
                        .forEach {
                            it.addInstructions(0, "return-void")
                            purchaseObservers++
                        }
                }
            }
        }
        if (appsFlyerStarts == 0) throw PatchException("AppsFlyerLib.start implementation not found")
        // PurchaseClientImpl ships in the phone APK only, not in the Wear OS companion.
        if (purchaseObservers > 1) throw PatchException("Expected at most 1 PurchaseClientImpl.startObservingTransactions, found $purchaseObservers")
    }
}
