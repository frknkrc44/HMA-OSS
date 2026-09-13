#!/system/bin/sh

# ref: https://github.com/PerformanC/ReZygisk/blob/main/module/src/rezygisk.sh

set -e

# INFO: This script gets moved to /data/adb/post-fs-data.d/hmaoss.sh

# INFO: This script is utilized so that when HMA-OSS is disabled, it still can clean up its
#         module.prop, making it not have traces of its old status.

MODDIR=/data/adb/modules/hma_oss_zygisk

# INFO: Resets HMA-OSS's module.prop to its default state which is saved upon installation.
cp "$MODDIR/module.prop.bak" "$MODDIR/module.prop"

(
    sleep 5

    try=1
    while [ "$try" -le 10 ]; do
        STATUS_FILE=$(ls -1 /data/misc/hide_my_applist_*/status.json 2>/dev/null | head -n 1)
        [ -n "$STATUS_FILE" ] && [ -s "$STATUS_FILE" ] && break
        sleep 1
        try=$((try + 1))
    done

    sh "$MODDIR/update_desc.sh"
) &

true
