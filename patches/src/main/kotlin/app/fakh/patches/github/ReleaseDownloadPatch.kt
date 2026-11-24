/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.patches.github

import app.fakh.patches.github.shared.Constants.COMPATIBILITY_GITHUB
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val RELEASE_DOWNLOAD = "Lapp/fakh/extension/github/ReleaseDownload;"

/**
 * The lambda that handles the release asset overflow menu ("download" and "share"). Both branches are in the same
 * method and are not obfuscated away because the action strings and Android intent actions are literals.
 */
private object ReleaseAssetDownloadFingerprint : Fingerprint(
    strings = listOf("download", "android.intent.action.VIEW", "android.intent.action.SEND"),
)

@Suppress("unused")
val releaseDownloadPatch = patch(
    name = "Native release download",
    description = "Downloads release assets directly with the system download manager instead of opening the browser.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GITHUB)
    category("Feature")
    dependsOn(githubSettingsPatch)
    extendWith("extensions/extension.mpe")

    execute {
        addRow("fakh_download_folder", "Release download folder")
        val method = ReleaseAssetDownloadFingerprint.method
        val instructions = method.implementation!!.instructions

        val downloadAt = instructions.indexOfFirst {
            ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == "download"
        }
        if (downloadAt < 0) throw PatchException("Release asset download branch not found")

        val viewAt = (downloadAt until instructions.size).firstOrNull { index ->
            ((instructions[index] as? ReferenceInstruction)?.reference as? StringReference)?.string ==
                "android.intent.action.VIEW"
        } ?: throw PatchException("Release asset VIEW action not found")

        val assetUrlAt = (downloadAt until viewAt).lastOrNull { index ->
            val instruction = instructions[index]
            instruction.opcode == Opcode.IGET_OBJECT &&
                ((instruction as ReferenceInstruction).reference as? FieldReference)?.type == "Ljava/lang/String;"
        } ?: throw PatchException("Release asset URL field read not found")

        val createChooserAt = (viewAt until instructions.size).firstOrNull { index ->
            val ref = (instructions[index] as? ReferenceInstruction)?.reference as? MethodReference
            ref?.definingClass == "Landroid/content/Intent;" && ref.name == "createChooser"
        } ?: throw PatchException("Release asset createChooser call not found")

        val startActivityAt = (createChooserAt + 1 until instructions.size).firstOrNull { index ->
            val ref = (instructions[index] as? ReferenceInstruction)?.reference as? MethodReference
            ref != null && ref.returnType == "V" &&
                ref.parameterTypes.map(CharSequence::toString) == listOf("Landroid/content/Intent;")
        } ?: throw PatchException("Release asset startActivity call not found")

        val assetRegister = (instructions[assetUrlAt] as TwoRegisterInstruction).registerB
        val chooserRegister = (instructions[startActivityAt] as FiveRegisterInstruction).registerD
        method.replaceInstruction(
            startActivityAt,
            "invoke-static { v$chooserRegister }, $RELEASE_DOWNLOAD->download(Landroid/content/Intent;)V",
        )
        method.addInstruction(
            assetUrlAt,
            "invoke-static { v$assetRegister }, $RELEASE_DOWNLOAD->prepare(Ljava/lang/Object;)V",
        )
    }
}
