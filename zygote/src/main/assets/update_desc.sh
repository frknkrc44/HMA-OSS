#!/system/bin/sh

# If you run this by hand, please use ./update_desc.sh or use full path
#  instead of 'sh update_desc.sh' after cd
MODDIR="${0%/*}"

ORIG_DESC=$(grep "^description=" "$MODDIR/module.prop.bak" | cut -d= -f2-)
ORIG_DESC_FIX=$(printf '%s\n' "$ORIG_DESC" | sed 's/[&/\]/\\&/g')

STATUS_FILE=$(ls -1 /data/misc/hide_my_applist_*/status.json 2>/dev/null | head -n 1)

if [ -z "$STATUS_FILE" ] || [ ! -s "$STATUS_FILE" ]; then
    MODE=""
else
    MODE=$(sed -n 's/.*"workMode"[[:space:]]*:[[:space:]]*\([-0-9]*\).*/\1/p' "$STATUS_FILE")
fi

case "$MODE" in
    1) STATUS="[✅ System service loaded]" ;;
    2) STATUS="[⚠️ Sick mode - Disabled hooks]" ;;
    3) STATUS="[⏳ Loading]" ;;
    -*) STATUS="[❌ Not loaded - Unknown error]" ;;
    *)  STATUS="[❓ Unknown]" ;;
esac

sed -i "s/^description=.*/description=$STATUS $ORIG_DESC_FIX/" "$MODDIR/module.prop"
