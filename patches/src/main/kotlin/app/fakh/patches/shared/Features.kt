package app.fakh.patches.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.mutableClassDefBy

/**
 * A runtime switch of a patch, listed in the app's Fakh Patches settings and read by its hooks through the
 * extension's `Features.on(id)`.
 *
 * @param id The key the switch is saved under and the hooks ask for.
 * @param title The English title; the extension translates it.
 * @param section The group the settings list it under.
 * @param dependsOn The feature this one needs on; this one is off while that one is.
 * @param restart Whether a change applies only after the app restarts.
 */
class Feature(
    val id: String,
    val title: String,
    val section: String,
    val dependsOn: Feature? = null,
    val restart: Boolean = true,
)

private val registered = linkedSetOf<Feature>()

/**
 * Lists [features] in the app's settings. Call it from the execute block of the patch that implements them, which
 * depends on [featuresPatch], so only the features of applied patches are listed.
 */
fun register(vararg features: Feature) {
    registered += features
}

/** Writes the features the applied patches registered into the extension's `Features.registry()`. */
val featuresPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    // Runs before the patches that depend on it, so a second patch run in one process starts empty.
    execute { registered.clear() }

    // Runs after every patch executed and registered its features.
    finalize {
        val registry = registered
            .sortedWith(compareBy({ it.section }, { it.title }))
            .joinToString("\n") { listOf(it.id, it.title, it.section, it.dependsOn?.id.orEmpty(), it.restart).joinToString("\t") }
        val literal = buildString {
            registry.forEach { c ->
                when (c) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\t' -> append("\\t")
                    else -> if (c.code < 0x20 || c.code > 0x7E) append("\\u%04x".format(c.code)) else append(c)
                }
            }
        }
        mutableClassDefBy("Lapp/fakh/extension/shared/Features;").methods.first { it.name == "registry" }.addInstructions(
            0,
            """
                const-string v0, "$literal"
                return-object v0
            """,
        )
    }
}
