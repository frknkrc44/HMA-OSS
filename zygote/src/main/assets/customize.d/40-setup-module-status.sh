#!/system/bin/sh

# ref: https://github.com/PerformanC/ReZygisk/blob/e42886f48eb1c9eabcc94f08a3c3af0cdbffb99e/module/src/customize.sh#L102-L114

mkdir -p /data/adb/boot-completed.d

cp "$MODPATH"/hmaoss.sh /data/adb/boot-completed.d/hmaoss.sh

chmod +x /data/adb/boot-completed.d/hmaoss.sh

# remove old update desc files
rm -f "/data/adb/post-fs-data.d/hmaoss.sh" "/data/adb/post-mount.d/hmaoss.sh"

cp "$MODPATH/module.prop" "$MODPATH/module.prop.bak"
