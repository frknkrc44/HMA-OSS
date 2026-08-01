#!/system/bin/sh
# HMA-OSS Zygisk PostBoot — post-fs-data lifecycle hook
#
# Invoked by KernelSU/Magisk during the normal boot lifecycle (cold boot with
# permanent root). In temporary-root sessions this hook DOES NOT fire — the
# user must trigger `postboot-activate.sh` manually via KernelSU Manager's
# Action button (see action.sh). The bootstrap is idempotent so calling it
# from both places is safe.

MODDIR="${MODDIR:-/data/adb/modules/hma_oss_zygisk}"
BOOTSTRAP="$MODDIR/postboot-bootstrap.sh"

if [ -f "$BOOTSTRAP" ]; then
    MODDIR="$MODDIR" /system/bin/sh "$BOOTSTRAP" >/dev/null 2>&1 &
fi

exit 0
