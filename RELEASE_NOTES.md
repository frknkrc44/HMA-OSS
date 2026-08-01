# HMA-OSS Zygisk (PostBoot) — v1.0-postboot

First tagged release of the **PostBoot port** of
[`frknkrc44/HMA-OSS`](https://github.com/frknkrc44/HMA-OSS) adapting the
HMA-OSS Zygisk module to work under **KernelSU temporary root** (a.k.a.
*Jailbreak mode* / *late-load mode*).

Mirrors the technique validated by
[`igorcv88/NeoZygisk-PostBoot`](https://github.com/igorcv88/NeoZygisk-PostBoot)
against the *module* side (HMA-OSS itself), so the two components — the
PostBoot-capable Zygisk provider and this module — reach a healthy
`RESULT=HEALTHY_INJECTED` state after **one** KernelSU Manager Soft Reboot
from a clean post-exploit session.

## Highlights

- **DEFEX-safe runtime** staged at `/dev/.hma_oss` (kernel-backed tmpfs;
  avoids Samsung DEFEX denials against `/data/adb/modules/hma_oss_zygisk/*`).
- **Idempotent 4-phase bootstrap** (`postboot-bootstrap.sh`) invoked from
  both `post-fs-data.sh` and `service.sh`; safe to re-run.
- **Read-only verifier** (`postboot-activate.sh verify`) surfaces a
  machine-readable status file with next-step guidance.
- **Live manager UI widget** — Home → *PostBoot status* renders the same
  status file without needing root.
- **PostBoot-provider auto-detection** in `customize.d/22-check-zygisk.sh`
  (NeoZygisk-PostBoot ships `postboot-activate.sh` under its module dir).
- **Safety contract enforced by CI**: no `ksud soft-reboot`,
  `ctl.restart zygote`, `killall zygote`, `/system/bin/reboot`, etc. in
  executable code paths — checked by
  `.github/workflows/postboot-guardrails.yml` on every push.

## Required user lifecycle

1. Full device reboot.
2. Run your KernelSU temporary-root exploit (e.g. *Root My Galaxy*).
3. Install **NeoZygisk-PostBoot** first (via KernelSU Manager → *Install
   from storage*).
4. Install `HMA-OSS-ZYGISK-POSTBOOT-<version>-release.zip` (this asset).
5. KernelSU Manager → **Soft Reboot** *once*. Do **not** run
   `ksud soft-reboot`, `killall zygote`, or any reboot command manually.
6. Verify:
   ```
   su -c '/system/bin/sh /data/adb/modules/hma_oss_zygisk/postboot-activate.sh verify'
   ```
   Expected: `RESULT=HEALTHY_INJECTED`, `ZYGOTE_LIB_MAPPED=1`,
   `SYSTEM_SERVER_LIB_MAPPED=1`, `PROVIDER_SOCKET_READY=1`.
   Or tap **Home → PostBoot status** in the HMA-OSS manager.

## Compatibility

- **arm64-v8a only** (matches the provider's constraint).
- **Zygisk provider required**: one of NeoZygisk-PostBoot (recommended),
  NeoZygisk (permanent-root only), ReZygisk, Zygisk Mod, Zygisk on
  KernelSU. Do not stack multiple providers — the installer will abort.
- **KernelSU 1.0+** (temporary-root sessions supported).
- **Tested reference hardware**: Samsung SM-S938B on `S938BXXSBCZG3`,
  kernel `6.6.98-android15-8-…`. See
  [`docs/HARDWARE_VERIFICATION.md`](docs/HARDWARE_VERIFICATION.md) for the
  full 10-step validation checklist.

## Assets

| File | Description |
|------|-------------|
| `HMA-OSS-ZYGISK-POSTBOOT-<version>-release.zip` | Flashable KernelSU / Magisk module archive. |
| `HMA-OSS-ZYGISK-POSTBOOT-<version>-release.zip.sha256` | SHA-256 checksum sidecar. |

## Documentation

- Technical write-up: [`docs/POSTBOOT_PORT.md`](docs/POSTBOOT_PORT.md).
- Build instructions: [`docs/BUILD.md`](docs/BUILD.md).
- Hardware validation: [`docs/HARDWARE_VERIFICATION.md`](docs/HARDWARE_VERIFICATION.md).

## Attribution

The idempotent-bootstrap + DEFEX-safe-tmpfs pattern is adapted from
NeoZygisk-PostBoot by [@igorcv88](https://github.com/igorcv88), itself
derived from NeoZygisk by
[@JingMatrix](https://github.com/JingMatrix/NeoZygisk). The HMA-OSS
codebase belongs to [@frknkrc44](https://github.com/frknkrc44).
