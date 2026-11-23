/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.patches.mazii

import app.fakh.patches.mazii.shared.Constants.COMPATIBILITY_MAZII
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch

/** PreferencesHelper.isPremium: stored flag or an unexpired `timeEnd`, or the account's premium state. */
private object IsPremiumFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    strings = listOf("timeEnd", "isPremium"),
)

/** PreferencesHelper.isAccountPremium: the signed-in account's `premium`, `lifetime` or `premiumExpiredDate`. */
private object IsAccountPremiumFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    strings = listOf("0000-00-00 00:00:00", "yyyy-MM-dd HH:mm:ss"),
)

/** Account.Result.getPremium: read directly by the home screen, profile and login. */
private object AccountPremiumFingerprint : Fingerprint(
    definingClass = "Lcom/mazii/dictionary/feature/share/data/model/account/Account\$Result;",
    name = "getPremium",
    returnType = "Z",
    parameters = listOf(),
)

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Premium unlock",
    description = "Unlocks the client-side Premium checks, which also stops the app from showing ads.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_MAZII)
    category("Premium")

    execute {
        listOf(IsPremiumFingerprint, IsAccountPremiumFingerprint, AccountPremiumFingerprint).forEach {
            it.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )
        }
    }
}
