# Framework loads the entry by name and invokes its lifecycle methods. Neither
# its superclass nor its API interfaces are packaged with the manager APK.
-keep class org.frknkrc44.hma_oss.xposed.XposedEntry { *; }
-dontwarn io.github.libxposed.api.**

# Hooker objects are created as synthetic lambda classes and passed to an
# external libxposed framework. R8 cannot see the framework's invocation:
# without this rule it can strip the entire intercept(Chain) implementation,
# leaving a class that still implements Hooker but throws AbstractMethodError
# for every hooked call in system_server. Keep the exact external ABI.
-keep class * implements io.github.libxposed.api.XposedInterface$Hooker { *; }
-keep class org.frknkrc44.hma_oss.xposed.service.ProtectiveHooker { *; }

# Android invokes these hidden Binder callbacks by their original method names.
# Without keep rules R8 can inline/merge UidObserverAdapter and turn the virtual
# onUidActive(int) implementation into a renamed static helper, crashing
# system_server with AbstractMethodError on the first UID event.
-keep class rikka.hidden.compat.adapter.UidObserverAdapter { *; }
-keep class * extends rikka.hidden.compat.adapter.UidObserverAdapter { *; }
