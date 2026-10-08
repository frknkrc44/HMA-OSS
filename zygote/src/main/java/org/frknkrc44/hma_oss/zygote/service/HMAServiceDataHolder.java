package org.frknkrc44.hma_oss.zygote.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Objects;

import icu.nullptr.hidemyapplist.common.FilterHolder;
import icu.nullptr.hidemyapplist.common.PresetCache;
import icu.nullptr.hidemyapplist.common.RiskyPackageUtils;

public class HMAServiceDataHolder {
    private final Object filterCountLock = new Object();

    private final ArrayList<UidHideCacheItem> uidHideCache = new ArrayList<>();

    private PresetCache presetCache = new PresetCache();

    private FilterHolder filterHolder = new FilterHolder();

    public PresetCache getPresetCache() {
        return presetCache;
    }

    void setPresetCache(PresetCache presetCache) {
        this.presetCache = presetCache;
    }

    public FilterHolder getFilterHolder() {
        return filterHolder;
    }

    void setFilterHolder(FilterHolder filterHolder) {
        this.filterHolder = filterHolder;
    }

    public boolean addIntoPresetCache(String presetName, String packageName) {
        var returnedValue = false;

        final var preset = presetCache.getCache().get(presetName);
        if (preset != null) {
            returnedValue |= preset.add(packageName);
        }

        final var riskyPkgUtils = RiskyPackageUtils.Companion.getInstance();
        if (riskyPkgUtils.appHasGMSConnection(packageName, true)) {
            returnedValue |= presetCache.getRiskyPackageCache().add(packageName);
        }

        return returnedValue;
    }

    public boolean removeFromPresetCache(String presetName, String packageName) {
        var returnedValue = false;

        final var preset = presetCache.getCache().get(presetName);
        if (preset != null) {
            returnedValue = preset.remove(packageName);
        }

        return returnedValue | presetCache.getRiskyPackageCache().remove(packageName);
    }

    public String findCallerByUid(int uid) {
        for (var item : uidHideCache) {
            if (item.uid == uid) {
                return item.caller;
            }
        }

        return null;
    }

    public boolean shouldHideFromUid(int uid, @Nullable String query) {
        if (query == null) return false;

        for (var item : uidHideCache) {
            if (item.uid == uid && item.blockedQueries.contains(query)) {
                return true;
            }
        }

        return false;
    }

    public void putShouldHideUidCache(int uid, @NonNull String caller, @NonNull String query) {
        for (var item : uidHideCache) {
            if (item.uid == uid) {
                item.blockedQueries.add(query);
                return;
            }
        }

        final var cache = new UidHideCacheItem(uid, caller, new ArrayList<>());
        cache.blockedQueries.add(query);
        uidHideCache.add(cache);
    }

    public void clearUidCache() {
        uidHideCache.clear();
    }

    public void increaseFilterCount(
            int callingUid,
            int amount,
            FilterHolder.FilterType filterType,
            Runnable writeFilterCount
    ) {
        if (amount < 1) return;

        final var caller = findCallerByUid(callingUid);
        if (caller == null) return;

        increaseFilterCount(caller, amount, filterType, writeFilterCount);
    }

    public void increaseFilterCount(
            String caller,
            int amount,
            FilterHolder.FilterType filterType,
            Runnable writeFilterCount
    ) {
        if (caller == null || amount < 1) return;

        synchronized (filterCountLock) {
            final var filterCounts = filterHolder.getFilterCounts();

            if (!filterCounts.containsKey(caller)) {
                filterCounts.put(caller, new FilterHolder.FilterCount());
            }

            final var filterCount = Objects.requireNonNull(filterCounts.get(caller));
            switch (filterType) {
                case PACKAGE_MANAGER -> filterCount.increasePackageManagerCount(amount);
                case ACTIVITY_LAUNCH -> filterCount.increaseActivityLaunchCount(amount);
                case INSTALLER -> filterCount.increaseInstallerCount(amount);
                case SETTINGS -> filterCount.increaseSettingsCount(amount);
                case OTHERS -> filterCount.increaseOthersCount(amount);
            }
        }

        writeFilterCount.run();
    }

    private record UidHideCacheItem(int uid, String caller, ArrayList<String> blockedQueries) {}
}
