# libxposed backend

The manager APK is also a modern Xposed module, requiring API **101+** and the
`system` scope. It uses `onSystemServerStarting`, not a legacy load-package or
zygote callback. The API is a compile-only dependency; neither legacy Xposed nor
AndroidVMTools is packaged into this backend. Hot reload is intentionally disabled:
the system service, UID observer and logging executor live until process exit.

## Maintaining both backends

`zygote/` remains the upstream Zygisk implementation. `adaptUpstreamSources` imports
its business hooks, HMA service/data holder and utilities into `xposed/build/` at
build time, relocating their package to `org.frknkrc44.hma_oss.xposed`. Its only
engine-type substitution is the explicit stack-frame import. Any additional
upstream `com.v7878` import fails the build and requires reviewing the adapter.
Generated files must not be edited or committed.

The entry, service bootstrap/handshake, method resolver, reflection utilities and
hook dispatcher are maintained here. This avoids a second drifting copy of the
upstream rules, configuration schema and AIDL implementation while keeping the
engines independent. No dependency on the `:zygote` APK is introduced.

`HookFrame` preserves the upstream layout: instance/constructor calls put the
receiver at index 0, static calls have no receiver slot. `HookDispatch` forwards
changed arguments with `Chain.proceed`, supports early null results, and restores
the original outcome on callback failure without invoking the original twice.
Matching overloads have separate libxposed hook handles.

The module embeds the **manager variant's** signing certificate in generated
`Magic.java`, preserving upstream manager verification. Both backends use the same
configuration files and service protocol. The manager receives an optional backend
identifier in the existing Binder handshake; upstream handshakes without it are
recognized as Zygisk. The libxposed handshake also supplies the running framework's
API version, which the manager displays instead of assuming API 102.

`minApiVersion=101` and `targetApiVersion=102`, as in FuseHide. The entry and hook
dispatcher use the API-101 surface common to both versions: there is no dependency
on API-102-only `setId` or `detach`. The static `system` scope and disabled hot
reload make these optional features unnecessary here.

## Reflective invocations

The adapter deliberately leaves `getInvoker()` at its documented default, the
full hook chain, matching ordinary reflective calls rather than bypassing hooks:

- `getDefaultBrowserPackageNameAsUser` (browser fallback) and
  `queryIntentActivitiesAsUser` (launch-intent lookup) call into PackageManager;
  their downstream package queries must still be eligible for HMA filtering.
- `getPackageName` / `getManifestPackageName` read package-setting or parsed-package
  names in the package hooks; neither is itself a direct HMA hook target.
- `InputMethodInfoSafeList.extractFrom` unwraps the result in the input-method
  *after* hook; the hooked method is the list getter, not `extractFrom`.

These call sites do not establish a same-method recursion from source inspection.
Do not switch the common reflection helper to `Invoker.Type.ORIGIN` without a
reproducer and a call-site-specific reason: that would change filter behavior.

## Backend selection

Enable either the HMA Zygisk module or the APK in an API-101+ framework. Reboot after
changing backend. If both new backends are enabled, a process-wide Java properties
lock allows only the first entry to start. The claim is retained on initialization
failure, since some hooks may already be installed. This does not depend on private
LSPosed database schemas. Older, unpatched Zygisk binaries do not participate in the
guard and must be disabled before enabling this APK in the framework.

## Build and test

```sh
git submodule update --init external/AndroidVMTools
./gradlew :app:assembleDebug :xposed:testDebugUnitTest
./gradlew :app:lint
```

The upstream Gradle configuration still needs AndroidVMTools even when building
only the manager. Unit tests cover receiver/argument mapping, constructors, early
returns, exceptions and callback failure recovery. Device validation also needs an
API-101+ framework with system-process support; installing the APK alone is not a
module activation test. Check the final APK for `META-INF/xposed/{java_init.list,
module.prop,scope.list}` and verify the minified release entry as well.
