package app.fakh.patches.fomo

import app.fakh.patches.fomo.shared.Constants.COMPATIBILITY_FOMO
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val fomoHidePostDividerPatch = bytecodePatch(
    name = "Hide post divider",
    description = "Removes the gray band between two posts in the feed.",
) {
    compatibleWith(COMPATIBILITY_FOMO)
    category("Theme")
    dependsOn(colorHooksPatch)

    execute {
        returnTrue("isHidePostDividerIncluded")
    }
}
