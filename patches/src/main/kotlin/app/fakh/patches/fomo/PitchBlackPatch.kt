package app.fakh.patches.fomo

import app.fakh.patches.fomo.shared.Constants.COMPATIBILITY_FOMO
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val fomoPitchBlackPatch = bytecodePatch(
    name = "Pitch black",
    description = "Keeps the dark mode background black, with slightly lighter black cards, instead of tinting it with the wallpaper. Includes Material You.",
) {
    compatibleWith(COMPATIBILITY_FOMO)
    category("Theme")
    dependsOn(colorHooksPatch, fomoMaterialYouPatch)

    execute {
        returnTrue("isPitchBlackIncluded")
    }
}
