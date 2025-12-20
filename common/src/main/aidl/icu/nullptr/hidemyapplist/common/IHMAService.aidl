package icu.nullptr.hidemyapplist.common;

interface IHMAService {

    void stopService(boolean cleanEnv) = 0;

    void writeConfig(String json) = 1;

    int getServiceVersion() = 2;

    int getFilterCount() = 3;

    String getLogs() = 4;

    void clearLogs() = 5;

    void handlePackageEvent(String eventType, String packageName) = 6;

    String[] getPackagesForPreset(String presetName) = 7;

    String readConfig() = 8;

    void forceStop(String packageName, int userId) = 9;

    void log(int level, String tag, String message) = 10;

    String[] getPackageNames(int userId) = 11;

    PackageInfo getPackageInfo(String packageName, int userId) = 12;

    String[] listAllSettings(String databaseName) = 13;

    String getLogFileLocation() = 14;
}
