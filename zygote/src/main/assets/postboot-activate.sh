#!/system/bin/sh
###############################################################################
# HMA-OSS Zygisk — PostBoot activation verifier
#
# Read-only utility the user (or KernelSU Manager Action) invokes to check
# whether HMA-OSS is active in the current temporary-root session.
#
# Usage:
#     su -c '/system/bin/sh /data/adb/modules/hma_oss_zygisk/postboot-activate.sh verify'
#     su -c '/system/bin/sh /data/adb/modules/hma_oss_zygisk/postboot-activate.sh status'
#
# This script performs **no destructive actions**. It re-runs the bootstrap
# (which is itself non-destructive & idempotent) and then prints the live
# status. If HMA is not yet injected, it explains the next required step —
# always a *user-initiated* KernelSU Manager "Soft Reboot".
###############################################################################

set -u

MODDIR="${MODDIR:-/data/adb/modules/hma_oss_zygisk}"
BOOTSTRAP="$MODDIR/postboot-bootstrap.sh"
RUN=/dev/.hma_oss
STATUS="$RUN/status"

cmd="${1:-status}"

case "$cmd" in
    verify|activate|start)
        # Alias for verify — never restarts anything
        if [ -x "$BOOTSTRAP" ] || [ -f "$BOOTSTRAP" ]; then
            MODDIR="$MODDIR" /system/bin/sh "$BOOTSTRAP" >/dev/null 2>&1
        fi
        cmd=status
        ;;
esac

echo "== HMA-OSS PostBoot verifier =="
echo

if [ ! -d "$MODDIR" ]; then
    echo "RESULT=NOT_INSTALLED"
    echo "HINT=module directory not present: $MODDIR"
    exit 1
fi

if [ ! -f "$STATUS" ]; then
    # Bootstrap has never run in this session — try one clean run
    if [ -f "$BOOTSTRAP" ]; then
        MODDIR="$MODDIR" /system/bin/sh "$BOOTSTRAP" >/dev/null 2>&1
    fi
fi

if [ -f "$STATUS" ]; then
    cat "$STATUS"
else
    echo "RESULT=UNKNOWN"
    echo "HINT=bootstrap has not produced a status file yet"
fi
echo
echo "-- next-step guidance --"
if [ -f "$STATUS" ] && grep -q '^RESULT=HEALTHY_INJECTED' "$STATUS"; then
    echo "HMA-OSS is active in this KernelSU session."
elif [ -f "$STATUS" ] && grep -q '^RESULT=WAITING_FOR_KERNELSU_SOFT_REBOOT' "$STATUS"; then
    cat <<'EOM'
The Zygisk provider is alive but HMA-OSS is not yet loaded in system_server.
Perform ONE KernelSU Manager "Soft Reboot" from a clean post-exploit session.
Do NOT run any manual `ctl.restart zygote`, `killall zygote`, or reboot commands.
EOM
elif [ -f "$STATUS" ] && grep -q '^RESULT=WAITING_FOR_ZYGISK_PROVIDER' "$STATUS"; then
    cat <<'EOM'
No PostBoot-capable Zygisk provider has completed injection yet.
Install and activate NeoZygisk-PostBoot (or a compatible late-load provider)
before running this verifier again.
EOM
elif [ -f "$STATUS" ] && grep -q '^RESULT=NO_ZYGISK_PROVIDER' "$STATUS"; then
    cat <<'EOM'
No Zygisk provider is installed at all. HMA-OSS Zygisk requires an
underlying Zygisk framework. For KernelSU temporary root, install:
  https://github.com/igorcv88/NeoZygisk-PostBoot
EOM
elif [ -f "$STATUS" ] && grep -q '^RESULT=MODULE_DISABLED' "$STATUS"; then
    echo "The module is disabled or marked for removal. Enable it in KernelSU Manager."
else
    echo "Consult /data/local/tmp/hma-oss-postboot.log for diagnostics."
fi

exit 0
