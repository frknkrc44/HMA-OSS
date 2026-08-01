# HMA-OSS Zygisk — PostBoot port for KernelSU temporary root

This fork of [`frknkrc44/HMA-OSS`](https://github.com/frknkrc44/HMA-OSS) makes the
HMA-OSS Zygisk module operational when KernelSU is loaded as **temporary root**
(a.k.a. *Jailbreak mode* / *late-load mode*).

The technique mirrors what
[`igorcv88/NeoZygisk-PostBoot`](https://github.com/igorcv88/NeoZygisk-PostBoot)
did for the NeoZygisk provider — applied here to a Java-only Zygisk *module*.

---

## 1. Problem

Normal HMA-OSS installs assume KernelSU is loaded at init, so the KernelSU
module lifecycle (`post-fs-data.sh` → `service.sh`) runs before the Android
zygote forks. In temporary-root sessions:

1. The kernel boots with no root. Android has already forked `zygote64` /
   `system_server` before KernelSU is activated.
2. When the user later runs the exploit (e.g. *Root My Galaxy*) KernelSU is
   loaded **after** boot completed. Module lifecycle scripts do **not** replay.
3. The running zygote already has no `libzygisk_loader.so` mapped, and Samsung
   DEFEX rejects `app_process64` reads of any library that lives under
   `/data/adb` in this state.
4. Result: even if HMA is installed on disk, it is never injected.

The NeoZygisk provider fork (`NeoZygisk-PostBoot`) solves the *provider* side
with three phases:

| Phase | Change | File(s) |
|-------|--------|---------|
| 1     | Runtime relocated from `/data/adb/neozygisk` → `/dev/.neozygisk` (kernel-backed tmpfs, bypasses Samsung DEFEX) | native `WORK_DIRECTORY`, generated scripts |
| 2     | Idempotent bootstrap invoked from both `post-fs-data.sh` **and** `service.sh` — converges on one healthy monitor with no destructive cleanup | `module/src/postboot-bootstrap.sh` |
| 3     | Read-only verifier only — the user performs the *KernelSU Manager Soft Reboot* themselves. The module never calls `ksud soft-reboot`, `ctl.restart zygote`, or restarts userspace | `module/src/postboot-activate.sh` |

The lifecycle proven on hardware (`SM-S938B`, `S938BXXSBCZG3`, kernel
`6.6.98-android15-8-...`) is:

1. Full reboot.
2. Run Root My Galaxy exploit.
3. KernelSU Manager → **Soft Reboot** once.
4. Live verify: NeoZygisk monitors init, zygote64 is injected, `zygiskd64`
   running, LSPosed + Zygisk Assistant loaded.

## 2. What this HMA-OSS fork adds

HMA-OSS is a *module* running inside whatever Zygisk provider is present. It
has no ptrace injector of its own. What it needs to work in late-load is:

1. **Idempotent install-time and runtime scripts** so the module reaches a
   consistent state whether it was installed during a normal cold boot or
   in a late-loaded KernelSU session.
2. **A DEFEX-safe runtime mirror** at `/dev/.hma_oss` so the running zygote
   can open the module's classes/lib without hitting Samsung DEFEX denials
   (same technique the provider uses under `/dev/.neozygisk`).
3. **User-invokable verifier** so the user can confirm HMA is loaded before
   or after the KernelSU Manager Soft Reboot.
4. **Detection of a PostBoot-capable provider** during install so the
   installer picks the right mode.
5. **Absolute non-destructiveness** — never kills zygote, never restarts
   userspace, never issues `ksud soft-reboot`.

### Files added / changed in this fork

```
zygote/src/main/assets/
├── action.sh                                (rewritten — dual verify + launch)
├── post-fs-data.sh                          (new — calls postboot-bootstrap.sh)
├── service.sh                               (new — calls postboot-bootstrap.sh, delayed)
├── uninstall.sh                             (new — cleans /dev/.hma_oss)
├── postboot-bootstrap.sh                    (new — idempotent bootstrap; the core)
├── postboot-activate.sh                     (new — read-only verifier)
└── customize.d/
    ├── 22-check-zygisk.sh                   (rewritten — detects PostBoot provider)
    └── 24-postboot-stage.sh                 (new — stages runtime at install time)

zygote/build.gradle.kts                      (module id/name/description updated)
docs/POSTBOOT_PORT.md                        (this document)
docs/BUILD.md                                (build & flash instructions)
```

### File-by-file summary

**`postboot-bootstrap.sh`** — Four-phase idempotent bootstrap:

* Phase 0: sanity (module dir present, `ro.product.cpu.abi` == arm64-v8a).
* Phase 1: stage runtime under `/dev/.hma_oss` (`module.prop`,
  `classes*.dex`, `zygisk/arm64-v8a.so`), restore SELinux label to
  `u:object_r:system_file:s0` (fallback `u:object_r:tmpfs:s0`), exclusive
  lock via atomic `mkdir "$RUN/lock"`.
* Phase 2: detect a Zygisk provider (`zygisksu`, `rezygisk`, `admirepowered`,
  `zygisk_on_ksu`) and whether it has finished injecting (probes
  `/dev/.neozygisk/cp64.sock` / `init_monitor`).
* Phase 3: verify the installed module is enabled and its `zygisk/*.so` is
  present.
* Phase 4: probe `/proc/<zygote64>/maps` and `/proc/<system_server>/maps`
  for `libzygisk_loader.so` / `hma_oss` and write a machine-readable status
  file (`/dev/.hma_oss/status`, mirrored to
  `/data/local/tmp/hma-oss-postboot.status`).

Possible `RESULT=` values:

| Value | Meaning |
|-------|---------|
| `HEALTHY_INJECTED` | HMA is mapped in `system_server`, everything is live |
| `WAITING_FOR_KERNELSU_SOFT_REBOOT` | Provider ready but HMA not yet in system_server — user must do **one** Soft Reboot |
| `WAITING_FOR_ZYGISK_PROVIDER` | Provider installed but its injection isn't up yet |
| `NO_ZYGISK_PROVIDER` | No Zygisk provider installed at all |
| `MODULE_DISABLED` | HMA module is marked `disable` or `remove` |
| `BUSY` | Another bootstrap invocation owns the lock |
| `FAILED` | Fail-closed error; see `/data/local/tmp/hma-oss-postboot.log` |

**`postboot-activate.sh`** — Read-only verifier the user invokes as
`postboot-activate.sh verify` or `... status`. Runs the bootstrap once
(idempotent, non-destructive), prints the resulting status and human
next-step guidance. Legacy aliases `start` / `activate` also perform
verification only — no lifecycle action.

**`post-fs-data.sh`** / **`service.sh`** — Thin wrappers that fire the
bootstrap. `service.sh` waits ~6 s so `system_server` has settled before
we scan its maps.

**`action.sh`** — Runs the verifier and echoes the result via `ui_print`,
then launches the classic manager UI.

**`customize.d/22-check-zygisk.sh`** — Same detection as upstream, but
additionally flags `HMA_POSTBOOT_MODE=1` when the detected provider ships a
`postboot-activate.sh` / `postboot-bootstrap.sh` (i.e. NeoZygisk-PostBoot).

**`customize.d/24-postboot-stage.sh`** — Only runs when
`HMA_POSTBOOT_MODE=1` and ABI is `arm64-v8a`. Primes `/dev/.hma_oss` at
install time so late-load users get an immediate healthy state after their
next KernelSU Manager Soft Reboot.

**`uninstall.sh`** — Removes `/dev/.hma_oss` and the persistent
`/data/local/tmp/hma-oss-postboot.*` diagnostics on uninstall.

## 3. Safety boundary (identical to NeoZygisk-PostBoot §3)

The packaged module MUST NOT contain any of:

- `ksud soft-reboot`
- `ctl.restart` / `setprop ctl.restart`
- kernel or device reboot commands
- zygote / system_server / monitor / daemon killing
- automatic recovery or watchdog behaviour

Every dangerous restart path was withdrawn upstream after it reproduced
Samsung's *Device Services Uninstalled* failure. The known-good lifecycle is
external and user initiated through KernelSU Manager from a clean
post-exploit session.

## 4. Required user lifecycle

### First install in a clean kernel session

1. Full reboot.
2. Run your KernelSU temporary-root exploit (Root My Galaxy / KernelSU LKM
   installer / etc.).
3. Confirm KernelSU is active.
4. Install **NeoZygisk-PostBoot** (or another late-load-capable Zygisk
   provider) first.
5. Install this HMA-OSS Zygisk (PostBoot) module.
6. KernelSU Manager → **Soft Reboot** once.
7. Tap the module Action, or run:
   ```
   su -c '/system/bin/sh /data/adb/modules/hma_oss_zygisk/postboot-activate.sh verify'
   ```

### Updating HMA-OSS while a healthy session is live

1. Install the update — do **not** Soft Reboot in that kernel session.
2. Full device reboot.
3. Re-run the exploit.
4. KernelSU Manager → Soft Reboot **once**.
5. Verify.

## 5. Provider compatibility

Install exactly one Zygisk provider. Do not stack NeoZygisk-PostBoot beside
Zygisk Next, ReZygisk or any other provider — HMA-OSS's install-time check
(`22-check-zygisk.sh`) will abort with `ZYGISK_MULTI_ERR`.

Providers known to work with this fork:

| Provider | Late-load support | Notes |
|----------|-------------------|-------|
| `NeoZygisk-PostBoot` (`zygisksu`) | **yes** | Recommended for KernelSU temporary root |
| `NeoZygisk` (upstream, `zygisksu`) | permanent root only | Works normally in permanent-root cold boots |
| `ReZygisk` (`rezygisk`) | partial | See ReZygisk docs for temp-root status |
| `Zygisk Mod` (`admirepowered`) | permanent root only | |
| `Zygisk on KernelSU` (`zygisk_on_ksu`) | permanent root only | |

## 6. Attribution

The idempotent-bootstrap + DEFEX-safe-tmpfs pattern is adapted with
attribution from **NeoZygisk-PostBoot** by igorcv88, itself derived from
NeoZygisk by JingMatrix. This HMA-OSS PostBoot port keeps the safety
contract intact and applies the same tmpfs relocation to the HMA runtime.

## 7. Non-goals

This fork deliberately does not:

- add any C/C++ code (HMA-OSS is Java-only; the provider does injection);
- ship a bundled Zygisk provider;
- attempt automated repair, watchdog, or restart of an unhealthy provider;
- support 32-bit-only devices (arm64-v8a only, matching the provider).
