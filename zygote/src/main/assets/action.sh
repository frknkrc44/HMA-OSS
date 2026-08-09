#!/system/bin/sh

MODDIR="${0%/*}"

# launch manager first
echo "Launching HMA-OSS on user 0"
am start -n org.frknkrc44.hma_oss/org.frknkrc44.hma_oss.ui.activity.MainActivity --user 0

# reload module status
# NOTE: `update_desc.sh` already replace whole description= line so i don't think re-copy is neccessary
sh "$MODDIR/update_desc.sh"
echo "Updated module status"
