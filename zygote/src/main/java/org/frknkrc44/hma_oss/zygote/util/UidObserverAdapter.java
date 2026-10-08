package org.frknkrc44.hma_oss.zygote.util;

import android.app.IUidObserver;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Copied from Rikka's HiddenApi project, because that
 * library is outdated and should be removed
 */
public class UidObserverAdapter extends IUidObserver.Stub {
    /**
     * Report that there are no longer any processes running for an uid.
     */
    @Override
    public void onUidGone(int uid, boolean disabled) throws RemoteException {

    }

    /**
     * Report that an uid is now active (no longer idle).
     */
    @Override
    public void onUidActive(int uid) throws RemoteException {

    }

    /**
     * Report that an uid is idle -- it has either been running in the background for
     * a sufficient period of time, or all of its processes have gone away.
     */
    @Override
    public void onUidIdle(int uid, boolean disabled) throws RemoteException {

    }

    @Override
    public void onUidStateChanged(int uid, int procState, long procStateSeq) throws RemoteException {
        onUidStateChanged(uid, procState, procStateSeq, 0);
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
    @Override
    public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) throws RemoteException {

    }

    /**
     * Report when the cached state of an uid has changed.
     * If true, an uid has become cached -- that is, it has some active processes that are
     * all in the cached state.  It should be doing as little as possible at this point.
     * If false, that an uid is no longer cached.  This will only be called after
     * onUidCached() has been reported true.  It will happen when either one of its actively
     * running processes is no longer cached, or it no longer has any actively running processes.
     */
    @Override
    public void onUidCachedChanged(int uid, boolean cached) throws RemoteException {

    }

    @SuppressWarnings("NullableProblems")
    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (Throwable ignored) {
            return true;
        }
    }
}
