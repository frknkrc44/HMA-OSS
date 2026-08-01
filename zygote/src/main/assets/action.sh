#!/system/bin/sh
###############################################################################
# HMA-OSS Zygisk PostBoot — module Action
#
# Invoked from KernelSU / Magisk Manager when the user taps the module "Action"
# button. In this PostBoot fork the Action is dual-purpose:
#
#   1. Run the read-only PostBoot verifier and echo its result via ui_print /
#      /data/local/tmp so the user can confirm HMA-OSS is active in this
#      temporary-root session.
#   2. Also launch the HMA-OSS manager UI (the classic behaviour).
#
# We never restart zygote, system_server, kernel or userspace.
###############################################################################

MODDIR="${MODDIR:-/data/adb/modules/hma_oss_zygisk}"
ACTIVATE="$MODDIR/postboot-activate.sh"

# Run the verifier — output is written to /data/local/tmp/hma-oss-postboot.status
if [ -f "$ACTIVATE" ]; then
    MODDIR="$MODDIR" /system/bin/sh "$ACTIVATE" verify 2>/dev/null | \
        while IFS= read -r line; do
            echo "$line"
            if command -v ui_print >/dev/null 2>&1; then
                ui_print "$line"
            fi
        done
fi

# Launch the classic HMA-OSS manager UI
am start -n org.frknkrc44.hma_oss/org.frknkrc44.hma_oss.ui.activity.MainActivity 2>/dev/null

exit 0
