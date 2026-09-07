#!/system/bin/sh

MODDIR="${0%/*}"

try=1
while [ "$try" -le 10 ]; do
    STATUS_FILE=$(ls -1 /data/misc/hide_my_applist_*/status.json 2>/dev/null | head -n 1)
    [ -n "$STATUS_FILE" ] && [ -s "$STATUS_FILE" ] && break
    sleep 1
    try=$((try + 1))
done

sh "$MODDIR/update_desc.sh"
