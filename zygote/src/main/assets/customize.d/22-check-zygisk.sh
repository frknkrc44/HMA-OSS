#!/system/bin/sh

# check for a Zygisk framework was installed and enabled

# get language
[ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(getprop persist.sys.locale 2>/dev/null)
[ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(getprop persist.sys.language 2>/dev/null)
[ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(settings get system system_locales 2>/dev/null)

# default language
ZYGISK_DETECTED_MSG(){
    echo "- Found $1 framework"
}
MAGISK_ZYGISK_NAME="Magisk Built-in Zygisk"
ZYGISK_MULTI_ERR="! Multiple Zygisk frameworks were found. Aborting installation to prevent conflicts"
ZYGISK_NOT_FOUND_ERR="! No known Zygisk frameworks (e.g. ZygiskNext) is found, HMA-OSS requires Zygisk to work. Installation aborted"

# language pack
if echo "$SYSTEM_LANG" | grep -q "zh"; then
    ZYGISK_DETECTED_MSG(){
        echo "- 检测到 $1 框架"
    }
    MAGISK_ZYGISK_NAME="Magisk 内置 Zygisk"
    ZYGISK_MULTI_ERR="! 检测到多个 Zygisk 框架, 为了避免冲突, 安装程序已退出"
    ZYGISK_NOT_FOUND_ERR="! 未找到已知的 Zygisk 框架 (例如 ZygiskNext), HMA-OSS 需要 Zygisk 才能正常运行, 安装程序已退出"
fi


find_zygisk(){
    if [ -d "/data/adb/modules/$1" ] || [ -d "/data/adb/modules_update/$1" ]; then
        [ -f "/data/adb/modules/$1/disable" ] && return
        [ -f "/data/adb/modules/$1/remove" ] && return

        [ ! -z "$ZYGISK_ID" ] && abort "$ZYGISK_MULTI_ERR"

        ZYGISK_ID="$1"
        ZYGISK_NAME="$2"
    fi
}

# add known zygisk frameworks here...
find_zygisk "zygisksu" "ZygiskNext / NeoZygisk"
find_zygisk "rezygisk" "ReZygisk"
find_zygisk "admirepowered" "Zygisk Mod"
find_zygisk "zygisk_on_ksu" "Zygisk on KernelSU"

if (! ([ "$KSU" ] || [ "$APATCH" ])) && [ -z "$ZYGISK_ID" ]
then
    MAGISK_ZYGISK=$(magisk --sqlite "SELECT value FROM settings WHERE key = 'zygisk'" 2> /dev/null | cut -f2 -d=)

    if [ "$MAGISK_ZYGISK" == "1" ]
    then
        ZYGISK_ID="magisk"
        ZYGISK_NAME="$MAGISK_ZYGISK_NAME"
    fi
fi

# not installed zygisk
if [ -z "$ZYGISK_ID" ]; then
    abort "$ZYGISK_NOT_FOUND_ERR"
else
    ui_print "$(ZYGISK_DETECTED_MSG "$ZYGISK_NAME")"
fi
