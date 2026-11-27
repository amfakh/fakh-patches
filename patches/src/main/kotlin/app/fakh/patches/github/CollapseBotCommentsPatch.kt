/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.patches.github

import app.fakh.patches.github.shared.Constants.COMPATIBILITY_GITHUB
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefForEach
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val BOTS = "Lapp/fakh/extension/github/BotComments;"

private const val STRING = "Ljava/lang/String;"

private const val OBJECT = "Ljava/lang/Object;"

/**
 * Wraps the HTML getter of the comment model [type]. The model is built from a response fragment, an address and the
 * kind of the comment, and stores five strings, the third of which is the HTML, and two authors, the first of which
 * wrote the comment.
 */
private fun collapse(type: String, author: String) {
    val comment = mutableClassDefBy(type)
    val constructor = comment.methods.single { it.name == "<init>" }
    val stored = constructor.instructions.filter { it.opcode == Opcode.IPUT_OBJECT }.mapNotNull { it.field }
    val html = stored.filter { it.type == STRING }[2]
    val writer = stored.first { it.type == author }
    val kind = stored.single { it.type == constructor.parameterTypes[2].toString() }
    val getter = comment.methods.singleOrNull { method ->
        method.parameterTypes.isEmpty() && method.returnType == STRING &&
            method.instructions.map { it.opcode } == listOf(Opcode.IGET_OBJECT, Opcode.RETURN_OBJECT) &&
            method.instructions.first().field?.name == html.name
    } ?: throw PatchException("Comment HTML getter not found in $type")

    comment.methods.add(
        ImmutableMethod(
            type,
            "fakhBody",
            listOf(ImmutableMethodParameter(type, null, null)),
            STRING,
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(4),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    iget-object v0, p0, ${writer.smali}
                    iget-object v1, p0, ${html.smali}
                    iget-object v2, p0, ${kind.smali}
                    invoke-static { v0, v1, v2 }, $BOTS->body($OBJECT$STRING$OBJECT)$STRING
                    move-result-object v0
                    return-object v0
                """,
            )
        },
    )
    getter.addInstructions(
        0,
        """
            invoke-static/range { p0 .. p0 }, $type->fakhBody($type)$STRING
            move-result-object v0
            return-object v0
        """,
    )
}

@Suppress("unused")
val collapseBotCommentsPatch = patch(
    name = "Collapse bot comments",
    description = "Comments by bots and agents such as Copilot start collapsed. Descriptions stay open.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GITHUB)
    category("Feature")
    extendWith("extensions/extension.mpe")

    execute {
        var author: String? = null
        classDefForEach { classDef ->
            classDef.methods.forEach { method ->
                val read = method.instructions.mapNotNull { it.string }
                if (method.name == "toString" && "Author(loginString=" in read) author = classDef.type
            }
        }
        val authorType = author ?: throw PatchException("Comment author model not found")

        // Every comment model: it stores an author and an editor next to five strings, from three arguments.
        val comments = mutableListOf<String>()
        classDefForEach { classDef ->
            if (classDef.interfaces.isEmpty()) return@classDefForEach
            val constructor = classDef.methods.singleOrNull { it.name == "<init>" } ?: return@classDefForEach
            val types = constructor.parameterTypes.map(CharSequence::toString)
            if (types.size != 3 || types[1] != STRING) return@classDefForEach
            val stored = constructor.instructions.filter { it.opcode == Opcode.IPUT_OBJECT }.mapNotNull { it.field?.type }
            if (stored.count { it == authorType } == 2 && stored.count { it == STRING } == 5) comments += classDef.type
        }
        if (comments.isEmpty()) throw PatchException("Comment models not found")
        comments.forEach { collapse(it, authorType) }
    }
}
