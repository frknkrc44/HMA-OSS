# libxposed backend

The manager APK includes a libxposed backend. Its module metadata requires API
101, targets API 102, uses the static `system` scope and protective exception
handling, and disables hot reload. `XposedEntry` starts the backend from
`onSystemServerStarting`; the libxposed API is a compile-only dependency.

## Source adaptation

`adaptUpstreamSources` copies the business hooks, `HMAService` data holders and
utilities from `zygote/` into `xposed/build/`, relocates them to
`org.frknkrc44.hma_oss.xposed`, and replaces the `EmulatedStackFrame` import with
`HookFrame`. Any other upstream `com.v7878` import fails the build. Generated
sources must not be edited or committed.

Backend-specific lifecycle, service delivery, method resolution, reflection and
hook dispatch code lives under `xposed/src/`. The generated business code keeps
the same configuration files, AIDL service and filtering behavior as the Zygisk
backend.

## Hook dispatch

`HookFrame` stores the receiver at index 0 for instance and constructor calls;
static calls contain arguments only. `HookDispatch` forwards changed arguments
through `Chain.proceed` and supports explicit null replacement. Callback failures
discard partial changes without invoking the original method twice. Exceptions from
the original method are cleaned and rethrown without running an after callback. Each
matched overload has its own hook handle.

## Service startup

`SystemServerHook` waits for the package and activity services before creating
`HMAService`. `UserService` observes the manager UID and sends the Binder through
its content provider with the backend identifier and running framework API version.
The generated `Magic.java` contains the matching manager variant's signing
certificate for service verification.

## Reflection

`ZLUtils.callMethodWithTypes` uses the default `getInvoker()` chain. PackageManager
lookups can therefore continue through HMA filters. Keep any origin-only invocation
decision local to a verified call site rather than changing this shared helper.

## Backend ownership

The libxposed and Zygisk entries claim a process-wide Java property before startup;
only the first backend initializes in `system_server`. The claim remains set after
an initialization failure because hooks may already have been installed. Zygisk
backends without this registry must be disabled before enabling the APK backend.

## Build and test

```sh
git submodule update --init external/AndroidVMTools
./gradlew :app:assembleDebug :xposed:testDebugUnitTest
./gradlew :app:lint
```

Verify the final APK contains `META-INF/xposed/java_init.list`, `module.prop` and
`scope.list`.
