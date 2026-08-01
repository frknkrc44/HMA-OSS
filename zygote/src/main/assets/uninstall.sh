#!/system/bin/sh
# HMA-OSS Zygisk PostBoot — uninstall hook
# Cleans up the DEFEX-safe tmpfs runtime staging area.

rm -rf /dev/.hma_oss 2>/dev/null
rm -f  /data/local/tmp/hma-oss-postboot.status 2>/dev/null
rm -f  /data/local/tmp/hma-oss-postboot.log    2>/dev/null

exit 0
