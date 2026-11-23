package app.fakh.patches.fomo

import app.fakh.patches.fomo.shared.Constants.COMPATIBILITY_FOMO
import app.fakh.patches.shared.Feature
import app.fakh.patches.shared.register
import app.morphe.patcher.patch.bytecodePatch

private val HIDE_POST_DIVIDER = Feature("hide_post_divider", "Hide gaps and dividers", "Theme")

@Suppress("unused")
val fomoHidePostDividerPatch = bytecodePatch(
    name = "Hide post divider",
    description = "Removes the gray band between two posts in the feed.",
) {
    compatibleWith(COMPATIBILITY_FOMO)
    category("Theme")
    dependsOn(colorHooksPatch)

    execute {
        register(HIDE_POST_DIVIDER)
    }
}
