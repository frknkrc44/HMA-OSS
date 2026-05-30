#!/system/bin/sh

# Enforce kernelsu kernel and manager version matched


if [ "$KSU" ]; then
    # get language
    SYSTEM_LANG=$(getprop persist.sys.locale)
    if [ -z "$SYSTEM_LANG" ]; then
        SYSTEM_LANG=$(getprop persist.sys.language)
    fi

    ui_print "- KernelSU detected"
    # kernel and manager version mismatch
    if [ "$KSU_VER_CODE" != "$KSU_KERNEL_VER_CODE" ]; then
        if echo "$SYSTEM_LANG" | grep -q "zh"; then
            abort "! KernelSU 管理器版本 ($KSU_VER_CODE) 与 内核版本 ($KSU_KERNEL_VER_CODE) 不匹配"
        else
            abort "! KernelSU manager version ($KSU_VER_CODE) and kernel version ($KSU_KERNEL_VER_CODE) mismatch."
        fi
    fi
    # not installed zygisk (warning)
    if [ -d "/data/adb/modules/zygisksu" ] || [ -d "/data/adb/modules_update/zygisksu" ]; then
        ui_print "- ZygiskNext / NeoZygisk Detected"
    elif [ -d "/data/adb/modules/rezygisk" ] || [ -d "/data/adb/modules_update/rezygisk" ]; then
        ui_print "- ReZygisk Detected"
    else
        if echo "$SYSTEM_LANG" | grep -q "zh"; then
            ui_print "! 警告: 未找到已知的 Zygisk 实现, 模块需要 Zygisk 才能正常运行"
            ui_print "安装将在3秒后继续..."
        else
            ui_print "! WARNING: No known Zygisk framework were found, the module need Zygisk framework to work."
            ui_print "Installation will continue in 3 seconds..."
        fi
        sleep 3
    fi
fi