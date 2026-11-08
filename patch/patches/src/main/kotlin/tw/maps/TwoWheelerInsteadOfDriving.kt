package tw.maps

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.SupportedAbi
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/**
 * Optional, for a Maps copy dedicated to a motorcycle: every driving route becomes a two-wheeler
 * route, however and wherever it is started.
 *
 * yff.p(cqlj, ckbl, ...) is the single place a route request's travel mode is written
 * (ckbl.k into cjfo.c). DRIVE (ckbl.a) is replaced with TWO_WHEELER (ckbl.f). Walking, transit
 * and cycling are untouched. Without this patch, driving routes stay driving; use
 * "Allow any travel mode on Android Auto" so Android Auto accepts either mode.
 */
@Suppress("unused")
val twoWheelerInsteadOfDriving = bytecodePatch(
    name = "Two-wheeler instead of driving",
    description = "26.44.06.1006895251 only. Optional, motorcycle-only: every driving route is planned as a " +
        "two-wheeler route (no freeways). Leave off to keep driving routes for driving.",
    default = false,
) {
    compatibleWith(
        Compatibility(
            name = "Google Maps",
            packageName = "com.google.android.apps.maps",
            apkFileType = ApkFileType.APKM,
            targets = listOf(
                AppTarget(
                    version = "26.44.06.1006895251",
                    versionCodes = mapOf(SupportedAbi.ARM64_V8A to 1068856906),
                    isExperimental = true,
                ),
            ),
        ),
    )
    execute {
        fun fieldRef(insn: Any?) = (insn as? ReferenceInstruction)?.reference as? FieldReference
        fun methodRef(insn: Any?) = (insn as? ReferenceInstruction)?.reference as? MethodReference

        // 1. Route request travel mode: DRIVE -> TWO_WHEELER.
        val writer = mutableClassDefBy("Lyff;").methods.singleOrNull {
            it.name == "p" && it.returnType == "V" &&
                it.parameterTypes.map(CharSequence::toString) == listOf("Lcqlj;", "Lckbl;", "I", "I", "Z")
        } ?: throw PatchException("yff.p(cqlj, ckbl, I, I, Z) not found; Maps changed")
        val writerImpl = writer.implementation ?: throw PatchException("yff.p has no body")
        if (writerImpl.instructions.none { fieldRef(it)?.definingClass == "Lcjfo;" }) {
            throw PatchException("yff.p no longer writes the request's cjfo travel mode; Maps changed")
        }
        // Registers: parameters occupy the last 6 (this, cqlj, ckbl, I, I, Z); ckbl is the third.
        val modeRegister = writerImpl.registerCount - 6 + 2
        if (modeRegister <= 0) throw PatchException("yff.p has no scratch register")
        writer.addInstructionsWithLabels(
            0,
            """
                sget-object v0, Lckbl;->a:Lckbl;
                if-ne v$modeRegister, v0, :keep_mode
                sget-object v$modeRegister, Lckbl;->f:Lckbl;
            """,
            ExternalLabel("keep_mode", writerImpl.instructions.first()),
        )

    }
}
