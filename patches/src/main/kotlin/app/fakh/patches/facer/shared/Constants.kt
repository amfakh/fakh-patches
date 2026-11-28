/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.patches.facer.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_FACER = Compatibility(
        name = "Facer",
        packageName = "com.jeremysteckling.facerrel",
        signatures = setOf("062c0e02a3ff728381a6c86117fba8770373d3e17d0f19802638d8cabc5285a6"),
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x2BADE3,
        targets = listOf(
            AppTarget(version = "7.0.82_11172253_mobile_receiver.phone", versionCode = 11172253),
            AppTarget(version = "7.0.82_11172254_wear_receiver.watch", versionCode = 11172254),
        ),
    )

    val COMPATIBILITY_FACER_PHONE = Compatibility(
        name = "Facer",
        packageName = "com.jeremysteckling.facerrel",
        signatures = setOf("062c0e02a3ff728381a6c86117fba8770373d3e17d0f19802638d8cabc5285a6"),
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x2BADE3,
        targets = listOf(
            AppTarget(version = "7.0.82_11172253_mobile_receiver.phone", versionCode = 11172253),
        ),
    )
}
