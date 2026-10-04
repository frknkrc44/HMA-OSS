#!/system/bin/sh

# If you run this by hand, please use ./update_desc.sh or use full path
#  instead of 'sh update_desc.sh' after cd
MODDIR="${0%/*}"

ORIG_DESC=$(grep "^description=" "$MODDIR/module.prop.bak" | cut -d= -f2-)
ORIG_DESC_FIX=$(printf '%s\n' "$ORIG_DESC" | sed 's/[&/\]/\\&/g')

STATUS_FILE=$(printf '%s' /data/misc/hide_my_applist_*/status.json)

echo "Status file: $STATUS_FILE"

if [ -z "$STATUS_FILE" ] || [ ! -s "$STATUS_FILE" ]; then
    MODE="0"
else
    MODE=$(grep -o '"workMode":[0-9]*' $STATUS_FILE | cut -f2 -d:)
fi

case "$MODE" in
    1) STATUS="[✅ System service loaded]" ;;
    2) STATUS="[⚠️ Sick mode - Disabled hooks]" ;;
    3) STATUS="[⏳ Loading]" ;;
    4) STATUS="[❌ System service crashed]"  ;;
    *) STATUS="[❓ Unknown]" ;;
esac

echo "Detected status: $STATUS"

sed -i "s/^description=.*/description=$STATUS $ORIG_DESC_FIX/" "$MODDIR/module.prop"
