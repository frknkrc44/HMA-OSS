package org.frknkrc44.hma_oss.zygote.util

import android.app.IUidObserver
import android.os.Parcel
import android.os.RemoteException

/**
 * Copied from Rikka's HiddenApi project, because that
 * library is outdated and should be removed
 */
open class UidObserverAdapter : IUidObserver.Stub() {
    /**
     * Report that there are no longer any processes running for an uid.
     */
    @Throws(RemoteException::class)
    override fun onUidGone(uid: Int, disabled: Boolean) {
    }

    /**
     * Report that an uid is now active (no longer idle).
     */
    @Throws(RemoteException::class)
    override fun onUidActive(uid: Int) {
    }

    /**
     * Report that an uid is idle -- it has either been running in the background for
     * a sufficient period of time, or all of its processes have gone away.
     */
    @Throws(RemoteException::class)
    override fun onUidIdle(uid: Int, disabled: Boolean) {
    }

    @Throws(RemoteException::class)
    override fun onUidStateChanged(uid: Int, procState: Int, procStateSeq: Long) {
        onUidStateChanged(uid, procState, procStateSeq, 0)
    }

    /**
     * General report of a state change of an uid.
     *
     * @param uid          The uid for which the state change is being reported.
     * @param procState    The updated process state for the uid.
     * @param procStateSeq The sequence no. associated with process state change of the uid,
     * see UidRecord.procStateSeq for details.
     * @param capability   the updated process capability for the uid.
     * Added from API 30 (11)
     */
    @Throws(RemoteException::class)
    override fun onUidStateChanged(uid: Int, procState: Int, procStateSeq: Long, capability: Int) {
    }

    /**
     * Report when the cached state of an uid has changed.
     * If true, an uid has become cached -- that is, it has some active processes that are
     * all in the cached state.  It should be doing as little as possible at this point.
     * If false, that an uid is no longer cached.  This will only be called after
     * onUidCached() has been reported true.  It will happen when either one of its actively
     * running processes is no longer cached, or it no longer has any actively running processes.
     *
     * @since API 27 (8.1)
     */
    @Throws(RemoteException::class)
    override fun onUidCachedChanged(uid: Int, cached: Boolean) {
    }

    @Throws(RemoteException::class)
    public override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        try {
            return super.onTransact(code, data, reply, flags)
        } catch (tr: Throwable) {
            return true
        }
    }
}
