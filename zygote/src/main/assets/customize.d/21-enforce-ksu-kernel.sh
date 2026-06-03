#!/system/bin/sh

# Enforce kernelsu kernel and manager version matched and zygisk framework is installed

if [ "$KSU" ]; then
    # get language
    [ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(getprop persist.sys.locale 2>/dev/null)
    [ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(getprop persist.sys.language 2>/dev/null)
    [ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(settings get system system_locales 2>/dev/null)

    # language pack
    if echo "$SYSTEM_LANG" | grep -q "zh"; then
        KSU_DETECTED_MSG="- 检测到 KernelSU"
        KSU_VERSION_MISMATCH_WARN="! 警告: KernelSU 管理器 ($KSU_VER_CODE) 与内核 ($KSU_KERNEL_VER_CODE) 版本不匹配! HMA-OSS 不对由此引发的系统异常（如: 卡米）负责，且模块可能无法激活!"
        INSTALLER_CONTINUE_MSG(){
            echo "- 安装将在 $1 秒后继续"
        }
        ZYGISK_DETECTED_MSG(){
            echo "- 检测到 $1 框架"
        }
        ZYGISK_DISABLED_ERR(){
            echo "! $1 已禁用, HMA-OSS 需要 Zygisk 才能正常运行, 安装程序已退出"
        }
        ZYGISK_REMOVED_ERR(){
            echo "! $1 已卸载, HMA-OSS 需要 Zygisk 才能正常运行, 安装程序已退出"
        }
        ZYGISK_MULTI_ERR="! 检测到多个 Zygisk 框架, 为了避免冲突, 安装程序已退出"
        ZYGISK_NOT_FOUND_ERR="! 未找到已知的 Zygisk 框架 (例如 ZygiskNext), HMA-OSS 需要 Zygisk 才能正常运行, 安装程序已退出"
    else
        KSU_DETECTED_MSG="- KernelSU found."
        KSU_VERSION_MISMATCH_WARN="! KernelSU manager version ($KSU_VER_CODE) does not match kernel version ($KSU_KERNEL_VER_CODE). HMA-OSS not take any responsibilities for incompatibilities!"
        INSTALLER_CONTINUE_MSG(){
            echo "- The installer will continue in $1 seconds"
        }
        ZYGISK_DETECTED_MSG(){
            echo "- Found $1 framework."
        }
        ZYGISK_DISABLED_ERR(){
            echo "! $1 is disabled, HMA-OSS requires Zygisk to work."
        }
        ZYGISK_REMOVED_ERR(){
            echo "! $1 is removed, HMA-OSS requires Zygisk to work."
        }
        ZYGISK_MULTI_ERR="! Multiple Zygisk frameworks were found. Aborting installation to prevent conflicts."
        ZYGISK_NOT_FOUND_ERR="! No known Zygisk frameworks (e.g. ZygiskNext) is found, HMA-OSS requires Zygisk to work. Installation aborted."
    fi
    ui_print "$KSU_DETECTED_MSG"
    # kernel and manager version mismatch
    if [ "$KSU_VER_CODE" != "$KSU_KERNEL_VER_CODE" ]; then
        ui_print "$KSU_VERSION_MISMATCH_WARN"
        ui_print "$(INSTALLER_CONTINUE_MSG 5)"
        sleep 5
    fi
    find_zygisk(){
        if [ -d "/data/adb/modules/$1" ] || [ -d "/data/adb/modules_update/$1" ]; then
            [ -f "/data/adb/modules/$1/disable" ] && return
            [ -f "/data/adb/modules/$1/remove" ] && return

            if [ ! -z "$ZYGISK_ID" ]; then
                [ ! -f "/data/adb/modules/$ZYGISK_ID/disable" ] && abort "$ZYGISK_MULTI_ERR"
                [ ! -f "/data/adb/modules/$ZYGISK_ID/remove" ] && abort "$ZYGISK_MULTI_ERR"
            fi

            ZYGISK_ID="$1"
            ZYGISK_NAME="$2"
        fi
    }
    # add known zygisk frameworks here...
    find_zygisk "zygisksu" "ZygiskNext / NeoZygisk"
    find_zygisk "rezygisk" "ReZygisk"
    find_zygisk "admirepowered" "Zygisk Mod"
    find_zygisk "zygisk_on_ksu" "Zygisk on KernelSU"

    # not installed zygisk
    if [ -z "$ZYGISK_ID" ]; then
        abort "$ZYGISK_NOT_FOUND_ERR"
    elif [ -f "/data/adb/modules/$ZYGISK_ID/disable" ]; then
        abort "$(ZYGISK_DISABLED_ERR "$ZYGISK_NAME")"
    elif [ -f "/data/adb/modules/$ZYGISK_ID/remove" ]; then
        abort "$(ZYGISK_REMOVED_ERR "$ZYGISK_NAME")"
    else
        ui_print "$(ZYGISK_DETECTED_MSG "$ZYGISK_NAME")"
    fi
fi
