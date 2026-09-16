package org.frknkrc44.hma_oss.zygote.util

import android.content.pm.UserInfo
import android.os.IUserManager
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.waitForService

object UserManagerUtils {
    private val userService by lazy { waitForService("user") as? IUserManager }

    fun getUsers(excludePartial: Boolean, excludeDying: Boolean, excludePreCreated: Boolean): List<UserInfo> {
        return try {
            userService!!.getUsers(excludePartial, excludeDying, excludePreCreated)
        } catch (_: Throwable) {
            userService!!.getUsers(excludeDying)
        }
    }

    val userIds get(): IntArray {
        return try {
            val users = getUsers(
                excludePartial = false,
                excludeDying = false,
                excludePreCreated = false,
            )

            return try {
                mutableSetOf<Int>().apply {
                    for (user in users) {
                        userService!!.getProfileIds(
                            user.id, false).forEach(::add)
                    }
                }.toIntArray()
            } catch (_: Throwable) {
                users.map { it.id }.toIntArray()
            }
        } catch (_: Throwable) {
            intArrayOf(0)
        }
    }
}
