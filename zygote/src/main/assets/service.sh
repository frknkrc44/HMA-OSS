#!/system/bin/sh
# HMA-OSS Zygisk PostBoot — late_start service hook
#
# In permanent-root boots this runs after class_late_start. In temporary-root
# it fires only if KernelSU / KernelSU Manager re-plays the module lifecycle
# after late-load (e.g. after a manual Soft Reboot). Either way the bootstrap
# is idempotent.

MODDIR="${MODDIR:-/data/adb/modules/hma_oss_zygisk}"
BOOTSTRAP="$MODDIR/postboot-bootstrap.sh"

if [ -f "$BOOTSTRAP" ]; then
    # sleep a few seconds so system_server has settled before we probe it
    ( sleep 6 ; MODDIR="$MODDIR" /system/bin/sh "$BOOTSTRAP" >/dev/null 2>&1 ) &
fi

exit 0
