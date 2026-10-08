package org.frknkrc44.hma_oss.zygote.util;

import static org.frknkrc44.hma_oss.zygote.util.ContextUtils.getContentResolver;
import static org.frknkrc44.hma_oss.zygote.util.ContextUtils.getPackageManager;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.binderLocalScope;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.callMethod;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getObjectField;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.WEBVIEW_PROVIDER_KEY;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.WEBVIEW_UPDATE_SERVICE;

import android.os.Build;
import android.os.ServiceManager;
import android.provider.Settings;
import android.webkit.IWebViewUpdateService;

import androidx.annotation.Nullable;

import com.android.server.pm.PackageManagerService;

public class BrowserUtils {
    private BrowserUtils() {}

    private static final String TAG = "BrowserUtils";

    private static volatile boolean useAltMethod = false;
    private static IWebViewUpdateService sWebViewUpdateService = null;

    @Nullable
    public static String getDefaultBrowser(Object pmn, int userId) {
        if (!useAltMethod) {
            final var pmnMethod = getDefaultBrowserPMN(pmn, userId);
            return useAltMethod ? getDefaultBrowserPM(userId) : pmnMethod;
        }

        return getDefaultBrowserPM(userId);
    }

    @SuppressWarnings("DataFlowIssue")
    @Nullable
    public static String getWebviewProvider() {
        return binderLocalScope(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    return getWebViewUpdateService().getCurrentWebViewPackage().packageName;
                } else {
                    return getWebViewUpdateService().getCurrentWebViewPackageName();
                }
            } catch (Throwable ignored) {
                return Settings.Global.getString(getContentResolver(), WEBVIEW_PROVIDER_KEY);
            }
        });
    }

    /**
     * This method is mainly called on non-Samsung devices
     */
    @Nullable
    private static String getDefaultBrowserPMN(Object pmn, int userId) {
        if (pmn == null) return null;

        try {
            final var pms = (PackageManagerService) getObjectField(pmn, "mPm");
            assert pms != null;

            return switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.Q -> pms.getDefaultBrowserPackageName(userId);
                case Build.VERSION_CODES.R -> pms.getPermissionManagerServiceInternal().getDefaultBrowser(userId);
                default -> pms.getDefaultAppProvider().getDefaultBrowser(userId);
            };
        } catch (Throwable e) {
            logD(TAG, e, () -> "Getting default browser failed through PMN");

            useAltMethod = true;

            return null;
        }
    }

    /**
     * This method is mainly called on Samsung devices
     */
    @Nullable
    private static String getDefaultBrowserPM(int userId) {
        try {
            return (String) callMethod(
                    getPackageManager(),
                    "getDefaultBrowserPackageNameAsUser",
                    userId
            );
        } catch (Throwable e) {
            logD(TAG, e, () -> "Getting default browser failed through PM");

            return null;
        }
    }


    private static IWebViewUpdateService getWebViewUpdateService() {
        if (sWebViewUpdateService == null) {
            sWebViewUpdateService = IWebViewUpdateService.Stub
                    .asInterface(ServiceManager.getService(WEBVIEW_UPDATE_SERVICE));
        }

        return sWebViewUpdateService;
    }
}
