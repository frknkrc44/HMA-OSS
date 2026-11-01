package org.frknkrc44.hma_oss.zygote.hook

import android.os.Binder
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import icu.nullptr.hidemyapplist.common.Utils.binderLocalScope
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackagesForUidLocal
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.COMPUTER_ENGINE_CLASS

/**
 * Stops a filtered caller from learning *which* application uids belong to hidden packages.
 *
 * ## The detection this closes
 *
 * `PackageManager.getNameForUid(int)` answers `null` when the appId has no package that is
 * visible to the caller. Hiding a package therefore flips the answer from "a name" to `null`,
 * and that transition is observable without any privilege:
 *
 * ```
 * for (uid in 10000..10699) {
 *     if (pm.getNameForUid(uid) == null && pm.getPackagesForUid(uid) == null) candidate(uid)
 * }
 * ```
 *
 * The caller ends up holding the uids of the hidden packages. This is the primitive behind
 * "a hidden app can still be found by uid".
 *
 * ## What the hook does
 *
 * For callers that HMA is configured to hide from, a `null` answer for an application uid that
 * *does* have an installed package is replaced with the platform's generic name for an unnamed
 * application uid - `u0_a42` for uid 10042.
 *
 * | situation | before | after |
 * |---|---|---|
 * | package visible to the caller | package name | package name |
 * | package hidden from the caller | `null` | `u0_a<appId - 10000>` |
 * | uid without any package | `null` | `null` |
 *
 * The hidden case now looks like "this uid has no name", which is what the platform already
 * reports for unnamed application uids, so the "named -> unnamed" correlation disappears. The
 * real package name is never disclosed.
 *
 * ## Blast radius
 *
 *  * only processes with an HMA scope entry are affected (`isHookEnabled`);
 *  * system and root callers are skipped;
 *  * uids without an installed package keep returning `null`;
 *  * any failure while resolving the caller or the uid returns the original value (`fail-open`).
 *
 * Android 13+ only (`ComputerEngine` hosts these methods); older releases are untouched.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
open class PmsUidNameHook : IFrameworkHook {

    override val TAG = "PmsUidNameHook"

    private companion object {
        /** android.os.UserHandle.PER_USER_RANGE; not exposed through the compile stubs. */
        const val PER_USER_RANGE = 100000

        /** First application uid of a user. */
        const val APP_UID_START = 10000

        /**
         * Classes that may host the uid -> name lookup, in order of preference. The hook walks
         * superclasses itself, so the first entry covers the current layout; the others are
         * fallbacks for ROMs that moved the implementation.
         */
        val TARGET_CLASSES = listOf(
            COMPUTER_ENGINE_CLASS,                                      // com.android.server.pm.ComputerEngine
            "com.android.server.pm.PackageManagerService\$ComputerEngine",
            "com.android.server.pm.Computer",
        )

        /**
         * Platform style name of an application uid without a package name, matching the naming
         * `Process` / `ActivityManager` use for unnamed app uids: uid 10042 -> "u0_a42".
         */
        fun syntheticName(uid: Int): String {
            val userId = uid / PER_USER_RANGE
            val appId = uid % PER_USER_RANGE
            val index = if (appId >= APP_UID_START) appId - APP_UID_START else appId
            return "u" + userId + "_a" + index
        }
    }

    override fun load() {
        val installed = installOn(TARGET_CLASSES)

        if (installed.isEmpty()) {
            logE(TAG) { "Not installed, no candidate class exposed getNameForUid: $TARGET_CLASSES" }
        } else {
            logI(TAG) { "Installed on $installed" }
        }
    }

    private fun installOn(classes: List<String>): List<String> {
        val installed = ArrayList<String>()

        for (clazz in classes) {
            if (installed.isNotEmpty()) break
            if (hooker.isHookAvailable(clazz, "getNameForUid")) {
                installed.add(clazz)
                break
            }

            hooker.hookAfter(
                clazz,
                "getNameForUid",
                1,
            ) { methodName, frame, returnValue ->
                if (returnValue.result != null) return@hookAfter

                // index 0 is `this`, the real argument starts at 1
                val uid = frame.getArgument(1) as? Int ?: return@hookAfter
                if (isCallerFiltered() != true) return@hookAfter

                val masked = maskedName(uid) ?: return@hookAfter
                logD(TAG) { "@$methodName masked hidden uid $uid as $masked" }
                returnValue.result = masked
            }

            hooker.hookAfter(
                clazz,
                "getNamesForUids",
                1,
            ) { methodName, frame, returnValue ->
                // index 0 is `this`, the real argument starts at 1
                val uids = frame.getArgument(1) as? IntArray ?: return@hookAfter
                val names = returnValue.result as? Array<*> ?: return@hookAfter
                if (names.isEmpty()) return@hookAfter
                if (isCallerFiltered() != true) return@hookAfter

                var maskedCount = 0
                val result = arrayOfNulls<String>(names.size)

                for (i in names.indices) {
                    val name = names[i] as? String
                    if (name != null) {
                        result[i] = name
                        continue
                    }

                    val uid = uids.getOrNull(i)
                    val masked = if (uid == null) null else maskedName(uid)
                    result[i] = masked
                    if (masked != null) maskedCount++
                }

                if (maskedCount > 0) {
                    logD(TAG) { "@$methodName masked $maskedCount hidden uid(s)" }
                    returnValue.result = result
                }
            }

            if (hooker.isHookAvailable(clazz, "getNameForUid")) installed.add(clazz)
        }

        return installed
    }

    /**
     * Name to report for [uid] instead of `null`, or `null` when the uid must keep answering
     * `null` (no package behind it, or not an application uid).
     */
    private fun maskedName(uid: Int): String? {
        if (uid < APP_UID_START) return null

        // Ask PMS without the caller's identity: "does this uid have a package at all",
        // regardless of what the filtered caller is allowed to see.
        val installed = runCatching {
            binderLocalScope { pms.getPackagesForUid(uid) }
        }.getOrNull()

        if (installed.isNullOrEmpty()) return null

        return syntheticName(uid)
    }

    /**
     * `null` when undeterminable (caller then keeps stock behaviour), otherwise whether the
     * calling process has an HMA scope entry.
     */
    private fun isCallerFiltered(): Boolean? {
        val callingUid = runCatching { Binder.getCallingUid() }.getOrNull() ?: return null
        if (callingUid == Process.SYSTEM_UID || callingUid == 0) return null
        if (callingUid < APP_UID_START) return null

        val callers = runCatching { getPackagesForUidLocal(pms, callingUid) }.getOrNull() ?: return null

        return callers.any { it != BuildConfig.APP_PACKAGE_NAME && service.isHookEnabled(it) }
    }
}
