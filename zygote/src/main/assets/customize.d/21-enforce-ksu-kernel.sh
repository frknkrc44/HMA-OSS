#!/system/bin/sh

# Enforce KernelSU kernel and manager versions matched

if [ "$KSU" ]; then
    # get language
    [ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(getprop persist.sys.locale 2>/dev/null)
    [ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(getprop persist.sys.language 2>/dev/null)
    [ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(settings get system system_locales 2>/dev/null)

    # default language
    KSU_VERSION_MISMATCH_WARN="! KernelSU manager version ($KSU_VER_CODE) does not match kernel driver version ($KSU_KERNEL_VER_CODE). HMA-OSS will not take any responsibilities for incompatibilities!"
    KSU_VERSION_MISMATCH_ERR="! KernelSU manager version ($KSU_VER_CODE) does not match kernel driver version ($KSU_KERNEL_VER_CODE). Aborting..."
    INSTALLER_CONTINUE_MSG(){
        echo "- The installer will continue in $1 seconds"
    }

    # language pack
    if echo "$SYSTEM_LANG" | grep -q "zh"; then
        KSU_VERSION_MISMATCH_WARN="! 警告: KernelSU 管理器版本 ($KSU_VER_CODE) 与驱动版本 ($KSU_KERNEL_VER_CODE) 不匹配! HMA-OSS 不对由此引发的系统异常负责，且模块可能无法激活!"
        KSU_VERSION_MISMATCH_ERR="! 错误: KernelSU 管理器版本 ($KSU_VER_CODE) 与驱动版本 ($KSU_KERNEL_VER_CODE) 不匹配! 请升级LKM驱动版本以继续安装HMA-OSS"
        INSTALLER_CONTINUE_MSG(){
            echo "- 安装将在 $1 秒后继续"
        }
    fi

    # kernel and manager version mismatch
    if [ "$KSU_VER_CODE" != "$KSU_KERNEL_VER_CODE" ]; then
        if lsmod 2>/dev/null | grep -q kernelsu
        then
            abort "$KSU_VERSION_MISMATCH_ERR"
        else
            ui_print "$KSU_VERSION_MISMATCH_WARN"
            ui_print "$(INSTALLER_CONTINUE_MSG 5)"
            sleep 5
        fi
    fi
fi
