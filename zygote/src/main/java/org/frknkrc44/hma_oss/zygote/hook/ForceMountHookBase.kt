package org.frknkrc44.hma_oss.zygote.hook

import java.util.concurrent.atomic.AtomicReference

abstract class ForceMountHookBase : IFrameworkHook {
    protected var lastForceMountedApp: AtomicReference<String?> = AtomicReference(null)
}
