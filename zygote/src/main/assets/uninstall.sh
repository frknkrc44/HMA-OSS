#!/system/bin/sh

rm -f /data/adb/post-fs-data.d/hmaoss.sh
rm -f /data/adb/post-mount.d/hmaoss.sh

# Only removes if dir is empty
rmdir /data/adb/post-mount.d 2>/dev/null || true

exit 0
