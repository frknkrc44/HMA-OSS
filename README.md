<div align="center">
  <h2>HMA-OSS</h2>

  <img src="HideMyAss-OSS.svg" alt="HMA-OSS Logo" style="max-width:360px;width:60%;height:auto;">

  <p>
    <a href="https://github.com/frknkrc44/HMA-OSS" style="text-decoration:none">
      <img src="https://img.shields.io/github/stars/frknkrc44/HMA-OSS?label=Stars&logo=github">
    </a>
    <a href="https://github.com/frknkrc44/HMA-OSS/actions" style="text-decoration:none">
      <img src="https://img.shields.io/github/actions/workflow/status/frknkrc44/HMA-OSS/main.yml?branch=master&logo=github">
    </a>
    <a href="https://github.com/frknkrc44/HMA-OSS/releases/latest" style="text-decoration:none">
      <img src="https://img.shields.io/github/v/release/frknkrc44/HMA-OSS?label=Release">
    </a>
    <a href="https://apt.izzysoft.de/fdroid/index/apk/org.frknkrc44.hma_oss" style="text-decoration:none">
      <img src="https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/org.frknkrc44.hma_oss&label=IzzyOnDroid">
    </a>
    <a href="https://shields.rbtlog.dev/org.frknkrc44.hma_oss" style="text-decoration:none">
      <img src="https://shields.rbtlog.dev/simple/org.frknkrc44.hma_oss">
    </a>
    <a href="https://github.com/frknkrc44/HMA-OSS/releases/latest" style="text-decoration:none">
      <img src="https://img.shields.io/github/downloads/frknkrc44/HMA-OSS/total">
    </a>
    <a href="https://t.me/aerathfuns" style="text-decoration:none">
      <img src="https://img.shields.io/badge/Telegram-Channel-blue.svg?logo=telegram">
    </a>
    <a href="https://choosealicense.com/licenses/gpl-3.0/" style="text-decoration:none">
      <img src="https://img.shields.io/github/license/frknkrc44/HMA-OSS?label=License">
    </a>
  </p>
</div>

---

- **English**
- [中文（简体）](README_zh_CN.md)
- [Türkçe](README_tr.md)
- [日本語](README_ja.md)
- [Indonesia](README_id.md)

## About this module

Although it's bad practice to detect the installation of specific apps, not every app using root provides random package name support. In this case, if apps related to root (such as Fake Location and Storage Isolation) are detected, it is tantamount to detecting that the device is rooted.

Additionally, some apps use various loopholes to acquire your app list, in order to use it as fingerprinting data or for other nefarious purposes.

This module can work as an Zygisk module to hide apps or reject app list requests.

## PostBoot fork — KernelSU temporary root / Jailbreak / late-load

This branch (`postboot`) adapts HMA-OSS Zygisk so it works under **KernelSU
temporary root** (a.k.a. *Jailbreak mode* / *late-load mode*), where the normal
`post-fs-data.sh` → `service.sh` lifecycle does not run at boot.

The port mirrors the technique validated by
[**NeoZygisk-PostBoot**](https://github.com/igorcv88/NeoZygisk-PostBoot) —
DEFEX-safe tmpfs runtime staging at `/dev/.hma_oss`, an idempotent
non-destructive bootstrap, and a read-only user-invokable verifier. The module
never restarts zygote, `system_server`, or userspace, and never invokes
`ksud soft-reboot` — the required Soft Reboot is initiated by the user through
KernelSU Manager.

Full technical write-up: [`docs/POSTBOOT_PORT.md`](docs/POSTBOOT_PORT.md).
Build instructions: [`docs/BUILD.md`](docs/BUILD.md).

Required user lifecycle (once installed):

1. Full reboot → run KernelSU temporary-root exploit → confirm KernelSU active.
2. Install **NeoZygisk-PostBoot** (or another late-load-capable provider).
3. Install this HMA-OSS Zygisk (PostBoot) module.
4. KernelSU Manager → **Soft Reboot** *once*.
5. Verify with `su -c '/system/bin/sh /data/adb/modules/hma_oss_zygisk/postboot-activate.sh verify'`
   (or tap the module Action).

## About HMA-OSS

https://github.com/frknkrc44/HMA-OSS/wiki

## I want to contribute translation
You can contribute translation [here](https://crowdin.com/project/frknkrc44-hma-oss).

## Update log
[Reference to the commits page](https://github.com/frknkrc44/HMA-OSS/commits)
