package org.frknkrc44.hma_oss.zygote.util

import android.content.Context
import android.content.pm.UserInfo
import android.os.IUserManager
import com.v7878.unsafe.Reflection.getDeclaredMethods
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.waitForService

object UserManagerUtils {
    private const val TAG = "UserManagerUtils"

    private val userService by lazy { waitForService(Context.USER_SERVICE) as IUserManager }

    private val getUsersInvocation by lazy {
        val method = getDeclaredMethods(IUserManager::class.java).first { it.name == "getUsers" }
        val args = Array(method.parameterCount) { false }

        Pair(method, args)
    }

    @Suppress("UNCHECKED_CAST")
    private val userList get(): List<UserInfo> {
        return getUsersInvocation.let {
            it.first.invoke(userService, *it.second) as List<UserInfo>
        }
    }

    val userIds get(): IntArray {
        return try {
            val users = userList

            try {
                mutableSetOf<Int>().apply {
                    for (user in users) {
                        userService.getProfileIds(
                            user.id, false).forEach(::add)
                    }
                }.toIntArray()
            } catch (_: Throwable) {
                users.map { it.id }.toIntArray()
            }
        } catch (cause: Throwable) {
            logE(TAG, cause) { "getUserIds failed" }

            intArrayOf(0)
        }
    }
}
