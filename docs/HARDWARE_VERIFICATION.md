# Hardware verification checklist — HMA-OSS Zygisk (PostBoot)

This checklist replays the exact lifecycle validated by NeoZygisk-PostBoot §4
on the target hardware, then adds the HMA-OSS-specific verification steps.
No step here can be executed by the build agent — a physical device with
KernelSU temporary-root support (e.g. Samsung Galaxy S25 Ultra `SM-S938B`
running `S938BXXSBCZG3`) is required.

Fill in the `[ ]` boxes and paste the collected outputs into
`docs/HARDWARE_VALIDATION_RESULT.md` before cutting a release.

---

## 0. Prerequisites (host)

- [ ] Built `HMA-OSS-ZYGISK-POSTBOOT-<version>-release.zip`
      (see `docs/BUILD.md`). SHA-256:
      `_________________________________________________________________`
- [ ] Downloaded latest NeoZygisk-PostBoot ZIP from
      https://github.com/igorcv88/NeoZygisk-PostBoot/releases/latest
- [ ] Downloaded KernelSU LKM exploit (Root My Galaxy S938B or equivalent).
- [ ] Confirmed device is a supported arm64-v8a target with
      `getprop ro.product.cpu.abi` == `arm64-v8a`.

## 1. Clean full boot

- [ ] Full device reboot (not Soft Reboot).
- [ ] After boot completes, confirm **no** Zygisk provider is currently
      injected: `adb shell 'ls /dev/.neozygisk 2>&1'` returns *No such file*.
- [ ] `adb shell getprop | grep -i ksu` reports no active KernelSU.

## 2. Temporary root

- [ ] Run the KernelSU LKM exploit (e.g. Root My Galaxy).
- [ ] `adb shell su -c id` returns `uid=0(root) gid=0(root) …`.
- [ ] `adb shell ksud debug info` reports a UAPI version.

## 3. Install NeoZygisk-PostBoot

- [ ] Flash NeoZygisk-PostBoot via KernelSU Manager → *Install from storage*.
- [ ] Do **not** Soft Reboot yet.
- [ ] `adb shell ls -la /data/adb/modules/zygisksu/postboot-activate.sh`
      exists.

## 4. Install HMA-OSS Zygisk (PostBoot)

- [ ] Flash `HMA-OSS-ZYGISK-POSTBOOT-<version>-release.zip` via KernelSU
      Manager. Installer output must include:
      ```
      - Found NeoZygisk / NeoZygisk-PostBoot / ZygiskNext framework
      - PostBoot-capable Zygisk provider detected (...)
      - Staging PostBoot runtime at /dev/.hma_oss
      - Perform ONE KernelSU Manager 'Soft Reboot' to activate HMA-OSS
      ```
- [ ] `adb shell ls -la /data/adb/modules/hma_oss_zygisk/` shows
      `postboot-bootstrap.sh`, `postboot-activate.sh`,
      `zygisk/arm64-v8a.so`, `classes*.dex`, `module.prop`.
- [ ] `adb shell 'ls -la /dev/.hma_oss/'` shows `module.prop`,
      `classes.mirror/`, `zygisk/arm64-v8a.so`, `status`.

## 5. KernelSU Manager Soft Reboot ×1

- [ ] Open KernelSU Manager → tap **Soft Reboot** exactly once.
- [ ] Do NOT manually run `ksud soft-reboot`, `killall zygote`,
      `setprop ctl.restart zygote`, or any reboot command.

## 6. Verify PostBoot state

Run:

```
adb shell 'su -c /system/bin/sh /data/adb/modules/hma_oss_zygisk/postboot-activate.sh verify'
```

Paste the output. Expected minimum:

```
RESULT=HEALTHY_INJECTED
PHASE=4
PROVIDER=zygisksu
RUNTIME=/dev/.hma_oss
MODULE_DIR=/data/adb/modules/hma_oss_zygisk
ZYGOTE_PID=<n>
SYSTEM_SERVER_PID=<n>
ZYGOTE_LIB_MAPPED=1
SYSTEM_SERVER_LIB_MAPPED=1
PROVIDER_SOCKET_READY=1
POSTBOOT_MODE=1
```

- [ ] `RESULT=HEALTHY_INJECTED`
- [ ] `ZYGOTE_LIB_MAPPED=1`
- [ ] `SYSTEM_SERVER_LIB_MAPPED=1`
- [ ] `PROVIDER_SOCKET_READY=1`

## 7. Cross-check with NeoZygisk

- [ ] `adb shell su -c 'cat /data/adb/modules/zygisksu/module.prop'` shows
      `zygote64: injected`, `daemon64: running`.
- [ ] `adb shell su -c 'cat /proc/1/status | grep TracerPid'` matches the
      NeoZygisk monitor PID.
- [ ] `adb shell su -c 'grep libzygisk_loader /proc/$(pgrep -f system_server)/maps'`
      returns at least one match.

## 8. Manager UI widget

- [ ] Open the HMA-OSS manager app.
- [ ] Tap **PostBoot status** on the Home screen.
- [ ] Summary card reads: *"HMA-OSS is active in this KernelSU session."*
- [ ] Raw status block shows the same key/value pairs as §6.
- [ ] Refresh button re-loads without error.

## 9. Uninstall & cleanup

- [ ] Uninstall the module via KernelSU Manager.
- [ ] Full device reboot.
- [ ] `adb shell ls /dev/.hma_oss 2>&1` returns *No such file*.
- [ ] `adb shell ls /data/local/tmp/hma-oss-postboot.*` returns *No such file*.

## 10. Safety regression (must NOT reproduce)

- [ ] `dmesg` shows **no** `DEFEX` denial referencing
      `/data/adb/modules/hma_oss_zygisk`.
- [ ] Device does **not** enter Samsung *Device Services Uninstalled*
      failure state.
- [ ] No unexpected zygote / system_server restart is observed in
      `logcat -b crash`.

---

If any box in §6 or §7 is unchecked, do **not** ship. File the collected
`postboot-activate.sh verify` output plus the associated `dmesg` slice as
`docs/HARDWARE_VALIDATION_RESULT.md`.
