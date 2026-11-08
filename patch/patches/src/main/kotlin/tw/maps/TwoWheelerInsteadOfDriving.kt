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
 * For a Maps copy dedicated to a motorcycle: every driving route becomes a two-wheeler route,
 * however and wherever it is started, and Android Auto never refuses it for its mode.
 *
 * Head-unit testing on 26.44 showed that only the first destination searched on the car screen
 * used two-wheeler mode; later ones (and phone-started driving routes) were built as DRIVE and
 * then refused with "switch to two-wheeler directions and start again".
 *
 *  1. yff.p(cqlj, ckbl, ...) -- the single place a route request's travel mode is written
 *     (ckbl.k into cjfo.c). DRIVE (ckbl.a) is replaced with TWO_WHEELER (ckbl.f). Walking,
 *     transit and cycling are untouched.
 *  2. trq.h(ckbl) -- "mode differs from what Android Auto supports" -> always false.
 *  3. siy.d (SessionAwareNavigationModeTracker.Listener.onNewSession) -- ends car navigation
 *     when the session mode differs from btxk.h(); treat every session as matching.
 *
 * Stock Maps is not touched, so it still plans driving routes for a car.
 */
@Suppress("unused")
val twoWheelerInsteadOfDriving = bytecodePatch(
    name = "Two-wheeler instead of driving",
    description = "26.44.06.1006895251 only. For a Maps copy used on a motorcycle: every driving route is planned as " +
        "a two-wheeler route (no freeways), on the phone and on Android Auto, and Android Auto never refuses " +
        "navigation for its travel mode. Walking, transit and cycling are unchanged. Keep stock Maps for driving a car.",
    default = true,
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

        // 2. trq.h(mode): never report a mode mismatch.
        val guard = mutableClassDefBy("Ltrq;").methods.singleOrNull {
            it.name == "h" && it.returnType == "Z" && it.parameterTypes.map(CharSequence::toString) == listOf("Lckbl;")
        } ?: throw PatchException("trq.h(ckbl) not found; Maps changed")
        if (guard.implementation?.instructions?.none { methodRef(it)?.let { r -> r.definingClass == "Lbtxk;" && r.name == "h" } == true } != false) {
            throw PatchException("trq.h no longer compares against btxk.h(); Maps changed")
        }
        guard.addInstructions(0, "const/4 v0, 0x0\nreturn v0")

        // 3. siy.d: treat the new session's mode as the supported one.
        val tracker = mutableClassDefBy("Lsiy;").methods.singleOrNull { method ->
            method.name == "d" && method.implementation?.instructions?.any {
                (it.opcode == Opcode.CONST_STRING || it.opcode == Opcode.CONST_STRING_JUMBO) &&
                    ((it as ReferenceInstruction).reference as StringReference).string ==
                    "SessionAwareNavigationModeTracker.Listener.onNewSession"
            } == true
        } ?: throw PatchException("SessionAwareNavigationModeTracker.onNewSession not found; Maps changed")
        val trackerInsns = tracker.implementation!!.instructions.toList()
        val call = trackerInsns.indexOfFirst { methodRef(it)?.let { r -> r.definingClass == "Lbtxk;" && r.name == "h" } == true }
        val result = trackerInsns.getOrNull(call + 1)
        val compare = trackerInsns.getOrNull(call + 2)
        if (call < 0 || result?.opcode != Opcode.MOVE_RESULT_OBJECT || compare?.opcode != Opcode.IF_EQ) {
            throw PatchException("onNewSession no longer compares the session mode with btxk.h(); Maps changed")
        }
        val supported = (result as OneRegisterInstruction).registerA
        compare as TwoRegisterInstruction
        val sessionMode = if (compare.registerB == supported) compare.registerA else compare.registerB
        if (sessionMode > 15 || supported > 15) throw PatchException("onNewSession registers out of range")
        tracker.addInstruction(call + 2, "move-object v$supported, v$sessionMode")
    }
}
