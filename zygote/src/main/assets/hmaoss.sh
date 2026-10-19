#!/system/bin/sh

# ref: https://github.com/PerformanC/ReZygisk/blob/main/module/src/rezygisk.sh

set -e

# INFO: This script gets moved to /data/adb/post-fs-data.d/hmaoss.sh

# INFO: This script is utilized so that when HMA-OSS is disabled, it still can clean up its
#         module.prop, making it not have traces of its old status.

MODDIR=/data/adb/modules/hma_oss_zygisk
ACTION="$MODDIR/action.sh"
ACTION_BAK="$MODDIR/action.sh.bak"
DISABLE="$MODDIR/disable"

# If disabled, hide the action button and keep the description from showing a stale status.
if [ -f "$DISABLE" ]; then
    if [ -f "$ACTION" ] && [ ! -f "$ACTION_BAK" ]; then
        mv "$ACTION" "$ACTION_BAK"
    fi

    # Restore the original description instead of keeping a stale service status.
    cp "$MODDIR/module.prop.bak" "$MODDIR/module.prop"

    exit 0
fi

# If enabled, restore the action button if it was hidden during a previous disabled boot.
if [ -f "$ACTION_BAK" ] && [ ! -f "$ACTION" ]; then
    mv "$ACTION_BAK" "$ACTION"
fi

# INFO: Resets HMA-OSS's module.prop to its default state which is saved upon installation.
cp "$MODDIR/module.prop.bak" "$MODDIR/module.prop"

(
    sleep 5

    try=1
    while [ "$try" -le 10 ]; do
        STATUS_FILE=$(printf '%s' /data/misc/hide_my_applist_*/status.json)
        [ -n "$STATUS_FILE" ] && [ -s "$STATUS_FILE" ] && break
        sleep 1
        try=$((try + 1))
    done

    sh "$MODDIR/update_desc.sh"
) &

true
