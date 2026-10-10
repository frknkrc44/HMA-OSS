package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logE;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logI;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getBooleanField;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getIntField;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getObjectField;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getThisObject;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.setBooleanField;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PROCESS_LIST_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PROCESS_RECORD_INTERNAL_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.STORAGE_MANAGER_SERVICE_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.STORAGE_MANAGER_SERVICE_LIFECYCLE_CLASS;

import static icu.nullptr.hidemyapplist.common.util.CollectionUtils.firstWithType;

import android.annotation.SuppressLint;
import android.os.SystemProperties;

import com.android.server.am.ProcessRecord;

import org.frknkrc44.hma_oss.common.BuildConfig;
import org.frknkrc44.hma_oss.zygote.ZygoteEntry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import icu.nullptr.hidemyapplist.common.OSUtils;
import icu.nullptr.hidemyapplist.common.PropertyUtils;

public class AppDataIsolationHook extends ABaseFrameworkHook {

    @SuppressLint("PrivateApi")
    public AppDataIsolationHook() {
        super("AppDataIsolationHook");

        try {
            processRecordIntClass = Class.forName(
                    PROCESS_RECORD_INTERNAL_CLASS, true, ZygoteEntry.classLoader);
        } catch (Throwable ignore) {}
    }

    private Class<?> processRecordIntClass = null;
    private boolean volDHookSkipped = false;

    private static final String APPDATA_ISOLATION_ENABLED = "mAppDataIsolationEnabled";
    private static final String VOLD_APPDATA_ISOLATION_ENABLED = "mVoldAppDataIsolationEnabled";
    private static final String FUSE_PROP = "persist.sys.fuse";

    @SuppressLint("PrivateApi")
    @Override
    public void load() {
        if (!isAltIsolationEnabled()) return;
        super.load();

        service.hooker.hookBefore(
                PROCESS_LIST_CLASS,
                "startProcess",
                (methodName, frame, returnValue) -> {
                    final var thisObject = getThisObject(frame);
                    Class<?> processListClazz;
                    try {
                        processListClazz = Class.forName(
                                PROCESS_LIST_CLASS,
                                true,
                                ZygoteEntry.classLoader
                        );
                    } catch (Throwable ignore) {
                        processListClazz = thisObject.getClass();
                    }

                    if (service.config.getAltAppDataIsolation()) {
                        final var enabled = getBooleanField(
                                thisObject, APPDATA_ISOLATION_ENABLED, processListClazz);

                        if (!enabled) {
                            setBooleanField(thisObject,
                                    APPDATA_ISOLATION_ENABLED, true, processListClazz);

                            logI(TAG, null, () -> "ProcessList - App data isolation is forced");
                        }
                    }

                    if (service.config.getAltVoldAppDataIsolation()) {
                        final var enabled = getBooleanField(
                                thisObject, VOLD_APPDATA_ISOLATION_ENABLED, processListClazz);

                        if (!enabled) {
                            setBooleanField(thisObject,
                                    VOLD_APPDATA_ISOLATION_ENABLED, true, processListClazz);

                            logI(TAG, null, () -> "ProcessList - VolD app data isolation is forced");
                        }
                    }
                }
        );

        if (!PropertyUtils.isVoldAppDataIsolationEnabled()) {
            if (OSUtils.isSamsung()) {
                // used for full VolD isolation
                service.hooker.hookBefore(
                        STORAGE_MANAGER_SERVICE_LIFECYCLE_CLASS,
                        "onStart",
                        (methodName, frame, returnValue) -> {
                            if (!service.config.getAltVoldAppDataIsolation() || volDHookSkipped) return;

                            final var fuseEnabled = SystemProperties.getBoolean(FUSE_PROP, false);

                            if (!fuseEnabled) {
                                logE(TAG, null, () -> "StorageManagerService - FUSE storage is not enabled, skip VolD hook");
                                volDHookSkipped = true;
                                return;
                            }

                            final var thisObject = getThisObject(frame);
                            final var storageManagerService = getObjectField(
                                    thisObject,
                                    "mStorageManagerService"
                            );

                            final var enabled = getBooleanField(
                                    storageManagerService, VOLD_APPDATA_ISOLATION_ENABLED, null);

                            if (!enabled) {
                                setBooleanField(storageManagerService,
                                        VOLD_APPDATA_ISOLATION_ENABLED, true, null);

                                logI(TAG, null, () -> "StorageManagerService - VolD app data isolation is forced");
                            }
                        }
                );
            } else {
                // used for full VolD isolation
                service.hooker.hookBefore(
                        STORAGE_MANAGER_SERVICE_CLASS,
                        "onVolumeStateChangedLocked",
                        (methodName, frame, returnValue) -> {
                            if (!service.config.getAltVoldAppDataIsolation() || volDHookSkipped) return;

                            final var fuseEnabled = SystemProperties.getBoolean(FUSE_PROP, false);

                            if (!fuseEnabled) {
                                logE(TAG, null, () -> "StorageManagerService - FUSE storage is not enabled, skip VolD hook");
                                volDHookSkipped = true;
                                return;
                            }

                            final var storageManagerService = getThisObject(frame);

                            final var enabled = getBooleanField(
                                    storageManagerService, VOLD_APPDATA_ISOLATION_ENABLED, null);

                            if (!enabled) {
                                setBooleanField(storageManagerService,
                                        VOLD_APPDATA_ISOLATION_ENABLED, true, null);

                                logI(TAG, null, () -> "StorageManagerService - VolD app data isolation is forced");
                            }
                        }
                );

                // used for partial VolD isolation
                service.hooker.hookAfter(
                        PROCESS_LIST_CLASS,
                        "needsStorageDataIsolation",
                        (methodName, frame, returnValue) -> {
                            final var args = dumpArgs(frame, true);
                            final var record = firstWithType(args, ProcessRecord.class);

                            final var uid = getIntFieldFromClazz(record, "uid");
                            final var apps = getCallingApps(service.pms, uid);

                            if (service.config.getDetailLog()) {
                                final var processName = getProcessNameFromClazz(record);
                                final var mountMode = getIntFieldFromClazz(record, "mMountMode");
                                final var isolated = getBooleanFieldFromClazz(record, "isolated");
                                final var appZygote = getBooleanFieldFromClazz(record, "appZygote");

                                logD(TAG, null, () -> String.format(Locale.US,
                                        "@needsStorageDataIsolation %d and %s - %s value without override: %s, mount node: %s, isolated: %s, appZygote: %s",
                                        uid, Arrays.toString(apps), processName,
                                        returnValue.getResult(), mountMode, isolated, appZygote
                                ));
                            }

                            for (String app : apps) {
                                // Do not isolate this module for safety
                                if (BuildConfig.APP_PACKAGE_NAME.equals(app)) {
                                    returnValue.setResult(false);
                                    return;
                                }

                                if (service.isAppDataIsolationExcluded(app)) {
                                    returnValue.setResult(false);
                                    return;
                                }

                                if (service.config.getSkipSystemAppDataIsolation() && service.systemApps.contains(app)) {
                                    logD(TAG, null, () -> String.format(Locale.US,
                                            "@needsStorageDataIsolation %d and %s - skipped system app",
                                            uid, app
                                    ));
                                    returnValue.setResult(false);
                                    return;
                                }
                            }
                        }
                );

                // used for partial VolD isolation
                service.hooker.hookBefore(
                        STORAGE_MANAGER_SERVICE_CLASS,
                        "remountAppStorageDirs",
                        (methodName, frame, returnValue) -> {
                            final var fuseEnabled = SystemProperties.getBoolean(FUSE_PROP, false);

                            if (!fuseEnabled) {
                                logE(TAG, null, () -> "StorageManagerService - FUSE storage is not enabled, skip VolD hook");
                                volDHookSkipped = true;
                                return;
                            }

                            if (!volDHookSkipped && service.config.getAltVoldAppDataIsolation()) {
                                @SuppressWarnings("unchecked")
                                final var pidPkgMap = (Map<Integer, String>) getArgument(frame, 1);

                                final var keysToRemove = new HashSet<Integer>();
                                for (var entry : pidPkgMap.entrySet()) {
                                    final var pid = entry.getKey();
                                    final var packageName = entry.getValue();

                                    if (service.config.getSkipSystemAppDataIsolation() && service.systemApps.contains(packageName)) {
                                        logD(TAG, null, () -> String.format(Locale.US,
                                                "@remountAppStorageDirs SYSTEM %d - %s is marked to remove",
                                                pid, packageName
                                        ));
                                        keysToRemove.add(pid);
                                    }

                                    if (service.isAppDataIsolationExcluded(packageName) || BuildConfig.APP_PACKAGE_NAME.equals(packageName)) {
                                        logD(TAG, null, () -> String.format(Locale.US,
                                                "@remountAppStorageDirs USER %d - %s is marked to remove",
                                                pid, packageName
                                        ));
                                        keysToRemove.add(pid);
                                    }
                                }

                                keysToRemove.forEach(pidPkgMap::remove);
                            }
                        }
                );
            }
        }
    }

    private int getIntFieldFromClazz(Object obj, String name) throws IllegalAccessException {
        try {
            return getIntField(obj, name);
        } catch (Throwable ignore) {
            return getIntField(obj, name, processRecordIntClass);
        }
    }

    private Object getProcessNameFromClazz(Object obj) throws IllegalAccessException {
        try {
            return getObjectField(obj, "processName");
        } catch (Throwable ignore) {
            return getObjectField(obj, "processName", processRecordIntClass);
        }
    }

    private boolean getBooleanFieldFromClazz(Object obj, String name) throws IllegalAccessException {
        try {
            return getBooleanField(obj, name, null);
        } catch (Throwable ignore) {
            return getBooleanField(obj, name, processRecordIntClass);
        }
    }

    private boolean isAltIsolationEnabled() {
        final var config = service.config;

        return (!PropertyUtils.isAppDataIsolationEnabled() && config.getAltAppDataIsolation()) ||
                (!PropertyUtils.isVoldAppDataIsolationEnabled() && config.getAltVoldAppDataIsolation());
    }
}
