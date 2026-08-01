#!/system/bin/sh
###############################################################################
# HMA-OSS PostBoot — install-time staging into the DEFEX-safe tmpfs runtime.
#
# When installing during a KernelSU *temporary-root* session, the module
# lifecycle scripts (post-fs-data.sh, service.sh) will NOT run automatically
# at init on this boot. We therefore attempt to prime `/dev/.hma_oss` here so
# that the module is discoverable by the running Zygisk provider immediately;
# a KernelSU Manager "Soft Reboot" is still required before actual injection.
#
# This script performs *no* destructive actions and never triggers a reboot.
###############################################################################

# get language
[ -z "$SYSTEM_LANG" ] && SYSTEM_LANG=$(getprop persist.sys.locale 2>/dev/null)
POSTBOOT_STAGE_MSG(){
    echo "- Staging PostBoot runtime at /dev/.hma_oss"
}
POSTBOOT_STAGE_SKIP(){
    echo "- Skipping PostBoot stage (permanent-root cold boot)"
}
POSTBOOT_SR_MSG(){
    echo "- Perform ONE KernelSU Manager 'Soft Reboot' to activate HMA-OSS"
}
POSTBOOT_HEALTHY_MSG(){
    echo "- HMA-OSS is already active in this session (no reboot needed)"
}
if echo "$SYSTEM_LANG" | grep -q "zh"; then
    POSTBOOT_STAGE_MSG(){ echo "- 正在暂存 PostBoot 运行时到 /dev/.hma_oss"; }
    POSTBOOT_STAGE_SKIP(){ echo "- 冷启动持久root场景, 跳过 PostBoot 预暂存"; }
    POSTBOOT_SR_MSG(){ echo "- 请执行一次 KernelSU 管理器的 Soft Reboot 以激活 HMA-OSS"; }
    POSTBOOT_HEALTHY_MSG(){ echo "- HMA-OSS 已经在当前会话中激活, 无需重启"; }
fi

# Only stage in PostBoot scenarios. HMA_POSTBOOT_MODE is exported by
# 22-check-zygisk.sh when a post-boot-capable Zygisk provider is present.
if [ "${HMA_POSTBOOT_MODE:-0}" != "1" ]; then
    ui_print "$(POSTBOOT_STAGE_SKIP)"
    exit 0
fi

# Only useful on arm64 (matches provider constraint & HMA-OSS support)
ABI="$(getprop ro.product.cpu.abi 2>/dev/null)"
case "$ABI" in
    arm64-v8a) : ;;
    *) exit 0 ;;
esac

ui_print "$(POSTBOOT_STAGE_MSG)"

RUN=/dev/.hma_oss
mkdir -p "$RUN"              2>/dev/null
mkdir -p "$RUN/classes.mirror" 2>/dev/null
mkdir -p "$RUN/zygisk"        2>/dev/null
chmod 0755 "$RUN"            2>/dev/null

# module.prop
[ -f "$MODPATH/module.prop" ] && cp -f "$MODPATH/module.prop" "$RUN/module.prop" 2>/dev/null

# classes*.dex
for f in "$MODPATH"/classes*.dex; do
    [ -f "$f" ] && cp -f "$f" "$RUN/classes.mirror/" 2>/dev/null
done

# zygisk shim
[ -f "$MODPATH/zygisk/arm64-v8a.so" ] && \
    cp -f "$MODPATH/zygisk/arm64-v8a.so" "$RUN/zygisk/arm64-v8a.so" 2>/dev/null

# Restore SELinux label so zygote can open these files
if command -v chcon >/dev/null 2>&1; then
    chcon -R u:object_r:system_file:s0 "$RUN" 2>/dev/null || \
    chcon -R u:object_r:tmpfs:s0       "$RUN" 2>/dev/null || true
fi
chmod -R 0755 "$RUN" 2>/dev/null

# Run the bootstrap once so /dev/.hma_oss/status reflects the current state
if [ -f "$MODPATH/postboot-bootstrap.sh" ]; then
    MODDIR="$MODPATH" /system/bin/sh "$MODPATH/postboot-bootstrap.sh" >/dev/null 2>&1
fi

# Give user the next step based on the recorded status
if [ -f "$RUN/status" ]; then
    if grep -q '^RESULT=HEALTHY_INJECTED' "$RUN/status" 2>/dev/null; then
        ui_print "$(POSTBOOT_HEALTHY_MSG)"
    else
        ui_print "$(POSTBOOT_SR_MSG)"
    fi
fi
