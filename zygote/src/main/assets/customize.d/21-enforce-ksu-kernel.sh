#!/system/bin/sh

# Enforce KernelSU kernel and manager versions matched

if [ "$KSU" ]; then
    # get language
    [ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(getprop persist.sys.locale 2>/dev/null)
    [ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(getprop persist.sys.language 2>/dev/null)
    [ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(settings get system system_locales 2>/dev/null)

    # language pack
    if echo "$SYSTEM_LANG" | grep -q "zh"; then
        KSU_VERSION_MISMATCH_WARN="! 警告: KernelSU 管理器 ($KSU_VER_CODE) 与内核 ($KSU_KERNEL_VER_CODE) 版本不匹配! HMA-OSS 不对由此引发的系统异常（如: 卡米）负责，且模块可能无法激活!"
        INSTALLER_CONTINUE_MSG(){
            echo "- 安装将在 $1 秒后继续"
        }
    else
        KSU_VERSION_MISMATCH_WARN="! KernelSU manager version ($KSU_VER_CODE) does not match kernel version ($KSU_KERNEL_VER_CODE). HMA-OSS not take any responsibilities for incompatibilities!"
        INSTALLER_CONTINUE_MSG(){
            echo "- The installer will continue in $1 seconds"
        }
    fi

    # kernel and manager version mismatch
    if [ "$KSU_VER_CODE" != "$KSU_KERNEL_VER_CODE" ]; then
        ui_print "$KSU_VERSION_MISMATCH_WARN"
        ui_print "$(INSTALLER_CONTINUE_MSG 5)"
        sleep 5
    fi
fi
