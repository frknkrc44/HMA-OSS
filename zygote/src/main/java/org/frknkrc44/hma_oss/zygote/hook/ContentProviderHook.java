package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.ContentProviderUtils.getOverriddenDatabaseName;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.CONTENT_PROVIDER_TRANSPORT_CLASS;
import static icu.nullptr.hidemyapplist.common.util.CollectionUtils.firstWithType;

import android.content.AttributionSource;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ContentProviderHook extends ABaseFrameworkHook {

    public ContentProviderHook() {
        super("ContentProviderHook");
    }

    private static final String[] DEFAULT_PROJECTION = new String[] { "name", "value" };

    @SuppressWarnings({"DataFlowIssue", "SimplifyStreamApiCallChains"})
    @Override
    public void load() {
        super.load();

        service.hooker.hookAfter(
                CONTENT_PROVIDER_TRANSPORT_CLASS,
                "query",
                (methodName, frame, returnValue) -> {
                    final var args = dumpArgs(frame, true);
                    final var callingApps = getCallingPackages(args);
                    final var caller = Arrays.stream(callingApps)
                            .filter(item -> service.isAnySettingsReplacementsEnabled(item))
                            .findFirst()
                            .orElse(null);
                    if (caller == null) return;

                    Uri uri = null;
                    List<String> segments = null;
                    String[] projection = null;
                    Bundle projArgs = null;
                    for (var arg : args) {
                        if (arg instanceof Uri uriArg) {
                            uri = uriArg;
                            if (!"settings".equals(uri.getAuthority())) return;

                            segments = uri.getPathSegments();
                            if (segments.isEmpty()) return;

                            continue;
                        }

                        if (uri != null && projection == null) {
                            projection = (String[]) arg;
                            continue;
                        }

                        if (projection != null) {
                            projArgs = (Bundle) arg;
                            break;
                        }
                    }

                    if (service.config.getDetailLog()) {
                        final var finalUri = uri;
                        final var finalProjection = projection;
                        final var finalProjArgs = projArgs;
                        logD(TAG, null, () -> "@spoofSettings QUERY in " + Arrays.toString(callingApps) +
                                ": " + finalUri + ", " + Arrays.toString(finalProjection) + ", " + finalProjArgs);
                    }

                    final var database = segments.get(0);
                    if (segments.size() >= 2) {
                        final var name = segments.get(1);

                        if (service.config.getDetailLog()) {
                            final var finalProjArgs = projArgs;
                            logD(TAG, null, () -> "@spoofSettings QUERY received caller: " + caller +
                                    ", database: " + database + ", , name: " + name + ", args: " + finalProjArgs);
                        }

                        final var overriddenDatabase = getOverriddenDatabaseName(database, name);
                        final var replacement = service.getSpoofedSetting(caller, name, overriddenDatabase);
                        if (replacement != null) {
                            final var columnNames = List.of(projection != null
                                    ? projection
                                    : DEFAULT_PROJECTION
                            );
                            final var nameInColumns = columnNames.contains("name");
                            final var valueInColumns = columnNames.contains("value");

                            final String[] returnedArr;
                            if (nameInColumns && valueInColumns) {
                                returnedArr = new String[] { replacement.getName(), replacement.getValue() };
                            } else if (nameInColumns) {
                                returnedArr = new String[] { replacement.getName() };
                            } else if (valueInColumns) {
                                returnedArr = new String[] { replacement.getValue() };
                            } else {
                                return;
                            }

                            logD(TAG, null, () -> "@spoofSettings QUERY " + name +
                                    " in " + overriddenDatabase + " replaced for " + caller);

                            final var cursor = new MatrixCursor(columnNames.toArray(new String[0]), 1);
                            cursor.addRow(returnedArr);
                            returnValue.setResult(cursor);

                            service.increaseSettingsFilterCount(caller);
                        }
                    } else {
                        logD(TAG, null, () -> "@spoofSettings LIST_QUERY received caller: " + caller + ", database: " + database);

                        final var result = (Cursor) returnValue.getResult();
                        final var columns = new LinkedHashMap<String, ArrayList<String>>();
                        for (int i = 0; i < result.getColumnCount(); i++) {
                            columns.put(result.getColumnName(i), new ArrayList<>());
                        }
                        final var columnKeys = columns.keySet().stream().collect(Collectors.toList());

                        logD(TAG, null, () -> "@spoofSetting LIST_QUERY columns: " + columnKeys);

                        final var keyColumn = columns.get("name");
                        final var valueColumn = columns.get("value");
                        if (keyColumn == null || valueColumn == null) return;

                        var filteredEntryCount = 0;

                        while (result.moveToNext()) {
                            final var name = result.getString(columnKeys.indexOf("name"));
                            keyColumn.add(name);

                            final var dbName = getOverriddenDatabaseName(database, name);
                            final var replacement = service.getSpoofedSetting(caller, name, dbName);
                            String value;
                            if (replacement != null) {
                                logD(TAG, null, () -> "@spoofSettings QUERY " + name +
                                        " in " + database + " replaced for " + caller);

                                filteredEntryCount++;

                                value = replacement.getValue();
                            } else {
                                value = result.getString(columnKeys.indexOf("value"));
                            }

                            valueColumn.add(value);

                            if (columns.size() > 2) {
                                final var nvPair = Arrays.asList(DEFAULT_PROJECTION);
                                for (var otherCol : columnKeys.stream().filter(e -> !nvPair.contains(e)).toArray()) {
                                    final var other = result.getString(columnKeys.indexOf((String) otherCol));

                                    columns.get(otherCol).add(other);
                                }
                            }
                        }

                        service.increaseSettingsFilterCount(caller, filteredEntryCount);

                        final var cursor = new MatrixCursor(columnKeys.toArray(new String[0]), columns.size());
                        if (!columns.isEmpty()) {
                            final var columnKSize = columns.values().stream().findFirst().get().size();
                            for (int i = 0; i < columnKSize; i++) {
                                final var innerList = new ArrayList<String>();

                                final int finalI = i;
                                columns.values().forEach(val -> innerList.add(val.get(finalI)));

                                cursor.addRow(innerList);
                            }
                        }
                        returnValue.setResult(cursor);
                    }
                }
        );

        service.hooker.hookBefore(
                CONTENT_PROVIDER_TRANSPORT_CLASS,
                "call",
                (methodName, frame, returnValue) -> {
                    final var args = dumpArgs(frame, true);
                    final var callingApps = getCallingPackages(args);
                    final var caller = Arrays.stream(callingApps)
                            .filter(item -> service.isAnySettingsReplacementsEnabled(item))
                            .findFirst()
                            .orElse(null);
                    if (caller == null) return;

                    String name = null;
                    String method = null;
                    for (int i = args.length - 1; i >= 0; i--) {
                        final var arg = args[i];
                        if (arg instanceof String strArg) {
                            if (name == null) {
                                name = strArg;
                            } else {
                                method = strArg;
                                break;
                            }
                        }
                    }

                    if (service.config.getDetailLog()) {
                        final var finalName = name;
                        final var finalMethod = method;

                        logD(TAG, null, () -> "@spoofSettings CALL received caller: " +
                                Arrays.toString(callingApps) + ", method: " + finalMethod + ", name: " + finalName);
                    }

                    switch (method) {
                        case "GET_global", "GET_secure", "GET_system" -> {
                            final var database = method.substring(method.indexOf('_') + 1);
                            final var replacement = service.getSpoofedSetting(caller, name, database);
                            if (replacement != null) {
                                final var ret = new Bundle();
                                ret.putString(Settings.NameValueTable.VALUE, replacement.getValue());
                                ret.putInt("_generation_index", -1);
                                returnValue.setResult(ret);

                                if (service.config.getDetailLog()) {
                                    final var finalName = name;
                                    logD(TAG, null, () -> "@spoofSettings CALL " + finalName +
                                            " in " + database + " replaced for " + caller);
                                }

                                service.increaseSettingsFilterCount(caller);
                            }
                        }
                    }
                }
        );
    }

    private String[] getCallingPackages(Object[] args) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                final var attrSource = firstWithType(args, AttributionSource.class);
                return new String[] { attrSource.getPackageName() };
            } else {
                final var attrSource = firstWithType(args, String.class);
                return new String[] { attrSource };
            }
        } catch (Throwable ignore) {
            try {
                return getCallingApps(service.pms);
            } catch (RemoteException e) {
                return new String[0];
            }
        }
    }
}
