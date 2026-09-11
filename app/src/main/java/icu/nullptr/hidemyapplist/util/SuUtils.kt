package icu.nullptr.hidemyapplist.util

object SuUtils {

    fun execPrivileged(cmd: String): Boolean {
        return try {
            val exec = Runtime.getRuntime().exec("su -c $cmd")
            exec.waitFor()
            return exec.exitValue() == 0
        } catch (_: Throwable) {
            false
        }
    }
}
