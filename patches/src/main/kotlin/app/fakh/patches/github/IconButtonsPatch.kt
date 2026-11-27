/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.patches.github

import app.fakh.patches.github.shared.Constants.COMPATIBILITY_GITHUB
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefForEach
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val BUTTONS = "Lapp/fakh/extension/github/IconButtons;"

private const val STRING = "Ljava/lang/String;"

/** The content of the "Finish your review" button, which reads that text. */
private object ReviewButtonFingerprint : Fingerprint(
    filters = listOf(resourceLiteral(ResourceType.STRING, "issue_pr_review_review_finish")),
)

private val MethodReference.types get() = parameterTypes.map(CharSequence::toString)

/** Whether this draws an icon: `(painter, contentDescription, Modifier, tint, Composer, Int, Int)`. */
private val Instruction.isIcon
    get() = method?.types?.let { it.size == 7 && it[1] == STRING && it[3] == "J" && it[5] == "I" && it[6] == "I" } == true

/** Whether this is `Modifier.padding(start, top, end, bottom)` with its default mask. */
private val Instruction.isPadding
    get() = method?.let {
        it.types == listOf(it.returnType, "F", "F", "F", "F", "I") && it.returnType.startsWith("L")
    } == true

/**
 * The places of the padding after the icon, the icon and the upper-casing of the text in the content of a button with
 * an icon, or null when this method draws no such content.
 */
private fun buttonContent(instructions: List<Instruction>): Triple<Int, Int, Int>? {
    instructions.forEachIndexed { upperAt, instruction ->
        val call = instruction.method ?: return@forEachIndexed
        if (call.definingClass != STRING || call.name != "toUpperCase") return@forEachIndexed
        val from = maxOf(0, upperAt - 30)
        val iconAt = (from until upperAt).lastOrNull { instructions[it].isIcon } ?: return@forEachIndexed
        val paddingAt = (from until iconAt).lastOrNull { instructions[it].isPadding } ?: return@forEachIndexed
        // An optional icon is boxed, which tells this content from one whose icon is always there.
        if ((paddingAt until iconAt).any { instructions[it].method?.name == "intValue" }) {
            return Triple(paddingAt, iconAt, upperAt)
        }
    }
    return null
}

/** Drops the text of a button with an icon when it is a floating one, and the space between the two. */
private fun MutableMethod.hideText(places: Triple<Int, Int, Int>) {
    val (paddingAt, iconAt, upperAt) = places
    val instructions = implementation!!.instructions.toList()
    val label = (instructions[upperAt] as Instruction35c).registerC
    if ((paddingAt until upperAt).any {
            val between = instructions[it]
            between.opcode.setsRegister() && (between as OneRegisterInstruction).registerA == label
        }
    ) {
        throw PatchException("Button content: the label register changes before the text")
    }
    val gap = (instructions[paddingAt] as? RegisterRangeInstruction)?.startRegister?.plus(3)
        ?: throw PatchException("Button content: padding registers not found")
    val description = (instructions[iconAt] as? RegisterRangeInstruction)?.startRegister?.plus(1)
        ?: throw PatchException("Button content: icon registers not found")

    // From the last place to the first, so the earlier ones stay where they were found. The registers changed for a
    // call get their value back, since later code may read them again.
    addInstructions(
        iconAt + 1,
        """
            const/16 v$description, 0x0
            invoke-static/range { v$label .. v$label }, $BUTTONS->text($STRING)$STRING
            move-result-object v$label
        """,
    )
    addInstructions(
        iconAt,
        """
            invoke-static/range { v$label .. v$label }, $BUTTONS->description($STRING)$STRING
            move-result-object v$description
        """,
    )
    addInstructions(paddingAt + 2, "const/high16 v$gap, 0x41000000")
    addInstructions(
        paddingAt,
        """
            invoke-static/range { v$label .. v$label }, $BUTTONS->gap($STRING)F
            move-result v$gap
        """,
    )
}

@Suppress("unused")
val iconOnlyButtonsPatch = patch(
    name = "Icon-only floating buttons",
    description = "Shows the floating Comment, New session and Finish your review buttons without their text.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GITHUB)
    category("Feature")
    extendWith("extensions/extension.mpe")

    execute {
        val contents = mutableListOf<Pair<String, String>>()
        classDefForEach { classDef ->
            classDef.methods.forEach { method ->
                val instructions = method.implementation?.instructions?.toList() ?: return@forEach
                if (buttonContent(instructions) != null) contents += classDef.type to method.name
            }
        }
        if (contents.isEmpty()) throw PatchException("Button content not found")
        contents.forEach { (type, name) ->
            val method = mutableClassDefBy(type).methods.first {
                it.name == name && buttonContent(it.implementation?.instructions?.toList().orEmpty()) != null
            }
            method.hideText(buttonContent(method.implementation!!.instructions.toList())!!)
        }

        // The review button keeps its icon and the arrow of its menu.
        var reviews = 0
        ReviewButtonFingerprint.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation!!.instructions.toList()
            val literalAt = match.instructionMatches.first().index
            val iconAt = (literalAt downTo 0).firstOrNull { instructions[it].isIcon } ?: return@forEach
            val textAt = (literalAt until instructions.size).first {
                instructions[it].opcode == Opcode.MOVE_RESULT_OBJECT
            }
            val text = (instructions[textAt] as OneRegisterInstruction).registerA
            val description = (instructions[iconAt] as RegisterRangeInstruction).startRegister + 1

            method.addInstructions(textAt + 1, "const-string v$text, \"\"")
            method.addInstructions(iconAt + 1, "const/16 v$description, 0x0")
            method.addInstructions(
                iconAt,
                """
                    invoke-static {}, $BUTTONS->reviewDescription()$STRING
                    move-result-object v$description
                """,
            )
            reviews++
        }
        if (reviews == 0) throw PatchException("Finish your review button not found")
    }
}
