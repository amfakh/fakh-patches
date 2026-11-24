/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.patches.warunglife

import app.fakh.patches.warunglife.shared.Constants.COMPATIBILITY_WARUNGLIFE
import app.fakh.patches.warunglife.shared.editGameScript
import app.fakh.patches.warunglife.shared.gameScriptPatch
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.rawResourcePatch

@Suppress("unused")
val warungLifeLegendaryFishPatch = rawResourcePatch(
    name = "Legendary fish multiplier",
    description = "Makes legendary fish more likely to bite.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_WARUNGLIFE)
    category("Feature")
    dependsOn(gameScriptPatch)

    val multiplier = intOption(
        key = "multiplier",
        default = 5,
        values = listOf(2, 4, 5, 10, 20, 50, 100).associateBy { "${it}x" },
        title = "Multiplier",
        description = "How many times as likely a legendary fish is. 100x makes every cast a chance at one.",
        required = true,
    ) { value -> value != null && value in 2..100 }

    execute {
        editGameScript { script -> script.multiplyLegendaryChance(multiplier.value!!) }
    }
}
