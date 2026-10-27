package org.frknkrc44.hma_oss.zygote.util

import com.v7878.foreign.Arena
import com.v7878.foreign.FunctionDescriptor
import com.v7878.foreign.Linker
import com.v7878.foreign.SymbolLookup
import com.v7878.foreign.ValueLayout
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.GP_FLAG_DISABLE
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.GP_FLAG_ENABLE
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.GP_MAGIC1
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.GP_SYSCALL_AARCH64
import java.lang.invoke.MethodHandle

object SyscallUtils {
    const val TAG = "SyscallUtils"

    fun doGPSyscall(enabled: Boolean, uid: Int) {
        val syscall = syscallHandler ?: return
        val command = if (enabled) GP_FLAG_ENABLE else GP_FLAG_DISABLE

        Arena.ofConfined().use { arena ->
            val cStr = arena.allocate(ValueLayout.JAVA_LONG)
            val address = cStr.nativeAddress()

            try {
                syscall.invokeExact(
                    GP_SYSCALL_AARCH64,
                    GP_MAGIC1,
                    command,
                    uid.toLong(),
                    0L,
                    0L,
                    cStr,
                )

                (address == cStr.get(ValueLayout.JAVA_LONG, 0)).apply {
                    logD(TAG) { "GP handler works: $this" }
                }
            } catch (e: Throwable) {
                logE(TAG, e) { "An error occurred while calling syscall $GP_SYSCALL_AARCH64" }
            }
        }
    }

    private val syscallHandler: MethodHandle? by lazy {
        val libc = SymbolLookup.libraryLookup("libc.so", Arena.global())
        val syscallAddress = libc.find("syscall").orElse(null) ?: return@lazy null
        val descriptor = FunctionDescriptor.ofVoid(
            ValueLayout.JAVA_INT, // syscall number and args
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
        )

        val linker = Linker.nativeLinker()
        linker.downcallHandle(syscallAddress, descriptor)
    }
}
