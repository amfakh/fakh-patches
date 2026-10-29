package app.fakh.patches.smartlife.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_SMARTLIFE = Compatibility(
        name = "Smart Life",
        packageName = "com.tuya.smartlife",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0xFF5A1F,
        targets = listOf(AppTarget(version = "7.12.3", versionCode = 877)),
    )
}
