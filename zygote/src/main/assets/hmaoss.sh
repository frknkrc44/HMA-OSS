#!/system/bin/sh

# ref: https://github.com/PerformanC/ReZygisk/blob/main/module/src/rezygisk.sh

set -e

# INFO: This script gets moved to /data/adb/post-fs-data.d/hmaoss.sh

# INFO: This script is utilized so that when HMA-OSS is disabled, it still can clean up its
#         module.prop, making it not have traces of its old status.

MODDIR=/data/adb/modules/hma_oss_zygisk

# INFO: Resets HMA-OSS's module.prop to its default state which is saved upon installation.
cp "$MODDIR/module.prop.bak" "$MODDIR/module.prop"

exit 0
