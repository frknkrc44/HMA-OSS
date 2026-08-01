#!/system/bin/sh

# check for a Zygisk framework was installed and enabled
# PostBoot fork: also flags whether a *post-boot capable* provider is present
# so the runtime can degrade gracefully in KernelSU temporary-root sessions.

# get language
[ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(getprop persist.sys.locale 2>/dev/null)
[ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(getprop persist.sys.language 2>/dev/null)
[ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(settings get system system_locales 2>/dev/null)

# default language
ZYGISK_DETECTED_MSG(){
    echo "- Found $1 framework"
}
POSTBOOT_MSG(){
    echo "- PostBoot-capable Zygisk provider detected ($1). Late-load supported."
}
FALLBACK_ZYGISK_NAME="Zygisk"
ZYGISK_MULTI_ERR="! Multiple Zygisk frameworks were found. Aborting installation to prevent conflicts"
ZYGISK_NOT_FOUND_ERR="! No known Zygisk frameworks (e.g. NeoZygisk / NeoZygisk-PostBoot) is found, HMA-OSS requires Zygisk to work. Installation aborted"

# language pack
if echo "$SYSTEM_LANG" | grep -q "zh"; then
    ZYGISK_DETECTED_MSG(){
        echo "- 检测到 $1 框架"
    }
    POSTBOOT_MSG(){
        echo "- 检测到支持临时root/延迟加载的Zygisk框架 ($1)"
    }
    ZYGISK_MULTI_ERR="! 检测到多个 Zygisk 框架, 为了避免冲突, 安装程序已退出"
    ZYGISK_NOT_FOUND_ERR="! 未找到已知的 Zygisk 框架 (例如 NeoZygisk-PostBoot), HMA-OSS 需要 Zygisk 才能正常运行, 安装程序已退出"
fi

ZYGISK_NAME=""
POSTBOOT_PROVIDER=""

find_zygisk(){
    if [ -d "/data/adb/modules/$1" ] || [ -d "/data/adb/modules_update/$1" ]; then
        [ -f "/data/adb/modules/$1/disable" ] && return
        [ -f "/data/adb/modules/$1/remove" ] && return

        [ ! -z "$ZYGISK_NAME" ] && abort "$ZYGISK_MULTI_ERR"

        ZYGISK_NAME="$2"

        # PostBoot detection: the provider ships an activation/bootstrap script
        # under its module dir (NeoZygisk-PostBoot uses postboot-activate.sh).
        for cand in \
            "/data/adb/modules/$1/postboot-activate.sh" \
            "/data/adb/modules_update/$1/postboot-activate.sh" \
            "/data/adb/modules/$1/postboot-bootstrap.sh" \
            "/data/adb/modules_update/$1/postboot-bootstrap.sh"
        do
            if [ -f "$cand" ]; then
                POSTBOOT_PROVIDER="$2"
                break
            fi
        done
    fi
}

# add known zygisk frameworks here...
find_zygisk "zygisksu"        "NeoZygisk / NeoZygisk-PostBoot / ZygiskNext"
find_zygisk "rezygisk"        "ReZygisk"
find_zygisk "admirepowered"   "Zygisk Mod"
find_zygisk "zygisk_on_ksu"   "Zygisk on KernelSU"

if [ -z "$ZYGISK_NAME" ] && [ "$ZYGISK_ENABLED" == "1" ]
then
    ZYGISK_NAME="$FALLBACK_ZYGISK_NAME"
fi

# not installed zygisk
if [ -z "$ZYGISK_NAME" ]; then
    abort "$ZYGISK_NOT_FOUND_ERR"
else
    ui_print "$(ZYGISK_DETECTED_MSG "$ZYGISK_NAME")"
fi

if [ -n "$POSTBOOT_PROVIDER" ]; then
    ui_print "$(POSTBOOT_MSG "$POSTBOOT_PROVIDER")"
    # Export for later customize.d scripts
    export HMA_POSTBOOT_MODE=1
else
    export HMA_POSTBOOT_MODE=0
fi
