package com.android.server.pm.permission;

import androidx.annotation.Nullable;

public abstract class PermissionManagerServiceInternal {

    @Nullable
    public abstract String getDefaultBrowser(int userId);
}
