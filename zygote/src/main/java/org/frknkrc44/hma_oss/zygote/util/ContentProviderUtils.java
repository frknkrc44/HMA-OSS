package org.frknkrc44.hma_oss.zygote.util;

import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getStaticObjectField;

import static icu.nullptr.hidemyapplist.common.Constants.SETTINGS_GLOBAL;
import static icu.nullptr.hidemyapplist.common.Constants.SETTINGS_SECURE;
import static icu.nullptr.hidemyapplist.common.Constants.SETTINGS_SYSTEM;

import android.provider.Settings;

import java.util.Collection;
import java.util.Set;

public class ContentProviderUtils {
    private ContentProviderUtils() {}

    public static String getOverriddenDatabaseName(String database, String name) {
        if (SettingsSecure.getMovedToGlobal().contains(name) ||
            SettingsSystem.getMovedToGlobal().contains(name) ||
            SettingsSystem.getMovedToSecureThenGlobal().contains(name)) {
            return SETTINGS_GLOBAL;
        }

        if (SettingsGlobal.getMovedToSecure().contains(name) ||
            SettingsSystem.getMovedToSecure().contains(name)) {
            return SETTINGS_SECURE;
        }

        if (SettingsGlobal.getMovedToSystem().contains(name)) {
            return SETTINGS_SYSTEM;
        }

        return database;
    }

    private static class SettingsSystem {
        private SettingsSystem() {}

        private static Collection<String> sMovedToSecure = null;
        private static Collection<String> sMovedToGlobal = null;
        private static Collection<String> sMovedToSecureThenGlobal = null;

        public static Collection<String> getMovedToSecure() {
            if (sMovedToSecure == null) {
                sMovedToSecure = getMovedToField(Settings.System.class, "MOVED_TO_SECURE");
            }

            return sMovedToSecure;
        }

        public static Collection<String> getMovedToGlobal() {
            if (sMovedToGlobal == null) {
                sMovedToGlobal = getMovedToField(Settings.System.class, "MOVED_TO_GLOBAL");
            }

            return sMovedToGlobal;
        }

        public static Collection<String> getMovedToSecureThenGlobal() {
            if (sMovedToSecureThenGlobal == null) {
                sMovedToSecureThenGlobal = getMovedToField(
                        Settings.System.class, "MOVED_TO_SECURE_THEN_GLOBAL");
            }

            return sMovedToSecureThenGlobal;
        }
    }

    private static class SettingsSecure {
        private SettingsSecure() {}

        private static Collection<String> sMovedToGlobal = null;

        public static Collection<String> getMovedToGlobal() {
            if (sMovedToGlobal == null) {
                sMovedToGlobal = getMovedToField(Settings.Secure.class, "MOVED_TO_GLOBAL");
            }

            return sMovedToGlobal;
        }
    }

    private static class SettingsGlobal {
        private SettingsGlobal() {}

        private static Collection<String> sMovedToSecure = null;
        private static Collection<String> sMovedToSystem = null;

        public static Collection<String> getMovedToSecure() {
            if (sMovedToSecure == null) {
                sMovedToSecure = getMovedToField(Settings.Global.class, "MOVED_TO_SECURE");
            }

            return sMovedToSecure;
        }

        public static Collection<String> getMovedToSystem() {
            if (sMovedToSystem == null) {
                sMovedToSystem = getMovedToField(Settings.Global.class, "MOVED_TO_SYSTEM");
            }

            return sMovedToSystem;
        }
    }

    @SuppressWarnings("all")
    private static Collection<String> getMovedToField(Class<?> clazz, String name) {
        try {
            return (Collection<String>) getStaticObjectField(clazz, name);
        } catch (Throwable ignored) {
            return Set.of();
        }
    }
}
