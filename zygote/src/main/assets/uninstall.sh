#!/system/bin/sh

rm -f /data/adb/post-fs-data.d/hmaoss.sh
rm -f /data/adb/post-mount.d/hmaoss.sh
rm -f /data/adb/boot-completed.d/hmaoss.sh

# INFO: Only removes if dir is empty
rmdir /data/adb/post-fs-data.d
rmdir /data/adb/post-mount.d
rmdir /data/adb/boot-completed.d

true
