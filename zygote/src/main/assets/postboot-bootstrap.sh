#!/system/bin/sh
###############################################################################
# HMA-OSS Zygisk — PostBoot idempotent bootstrap
#
# Purpose
#   Make HMA-OSS operational under a KernelSU *temporary root* (a.k.a. Jailbreak
#   / late-load) session, where the normal Magisk / KernelSU boot lifecycle
#   (post-fs-data.sh → service.sh) does *not* run during init but is instead
#   triggered manually by the user later in the boot.
#
#   This script mirrors the working technique validated by NeoZygisk-PostBoot
#   (https://github.com/igorcv88/NeoZygisk-PostBoot) but applied to a Java-only
#   Zygisk module (HMA-OSS). It is deliberately non-destructive:
#
#     * never calls   `ksud soft-reboot`
#     * never restarts zygote, system_server, or Android userspace
#     * never kills any process
#     * never sets   `ctl.restart zygote`
#     * refuses to run if init is traced by an unknown process
#     * is fully idempotent: repeat invocations converge on one healthy state
#
# Runtime layout (DEFEX-safe, kernel-backed tmpfs)
#     /dev/.hma_oss/                        runtime root
#     /dev/.hma_oss/lock                    exclusive lock
#     /dev/.hma_oss/module.prop             mirror of installed module.prop
#     /dev/.hma_oss/classes.mirror/         classes*.dex staged for zygisk load
#     /dev/.hma_oss/zygisk/                 zygisk/<abi>.so staged
#     /dev/.hma_oss/status                  last bootstrap result (key=value)
#     /dev/.hma_oss/log                     rolling diagnostic log
#
# Persistent diagnostics
#     /data/local/tmp/hma-oss-postboot.status   copy of /dev/.hma_oss/status
#     /data/local/tmp/hma-oss-postboot.log      copy of /dev/.hma_oss/log
###############################################################################

set -u

MODDIR="${MODDIR:-/data/adb/modules/hma_oss_zygisk}"
RUN=/dev/.hma_oss
LOCK="$RUN/lock"
STATUS="$RUN/status"
LOG="$RUN/log"
PERSIST_STATUS=/data/local/tmp/hma-oss-postboot.status
PERSIST_LOG=/data/local/tmp/hma-oss-postboot.log

# --- Helpers -----------------------------------------------------------------

log() {
    _ts="$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo now)"
    printf '[%s] %s\n' "$_ts" "$*" >>"$LOG" 2>/dev/null
}

set_status() {
    printf '%s\n' "$1" >"$STATUS" 2>/dev/null
    cp -f "$STATUS" "$PERSIST_STATUS" 2>/dev/null
    cp -f "$LOG"    "$PERSIST_LOG"    2>/dev/null
    # Make persist copies world-readable so the manager app (non-root)
    # can render the current PostBoot status inside its UI widget.
    chmod 0644 "$PERSIST_STATUS" 2>/dev/null
    chmod 0644 "$PERSIST_LOG"    2>/dev/null
}

fatal() {
    log "FATAL: $*"
    set_status "RESULT=FAILED
REASON=$1
PHASE=$PHASE"
    exit 1
}

# --- Phase 0: sanity ---------------------------------------------------------

PHASE=0
[ -d "$MODDIR" ] || {
    # module not yet materialised on disk — nothing to do
    exit 0
}

# Only run on ARM64 (matches Zygisk provider constraint)
ABI="$(getprop ro.product.cpu.abi 2>/dev/null)"
case "$ABI" in
    arm64-v8a) : ;;
    *)
        # silently no-op on non-arm64 — HMA-OSS is targeted at arm64 devices
        exit 0
        ;;
esac

# --- Phase 1: stage runtime under /dev/.hma_oss (DEFEX-safe) -----------------

PHASE=1
mkdir -p "$RUN" 2>/dev/null || fatal "MKDIR_RUN"
chmod 0755 "$RUN" 2>/dev/null
# Restrict to root explicitly — no group/other write
chown root:root "$RUN" 2>/dev/null

# Exclusive lock via mkdir (POSIX atomic)
_locked=0
_i=0
while [ $_i -lt 20 ]; do
    if mkdir "$LOCK" 2>/dev/null; then
        _locked=1
        break
    fi
    _i=$((_i + 1))
    sleep 1
done
if [ "$_locked" -ne 1 ]; then
    set_status "RESULT=BUSY
PHASE=1"
    exit 0
fi
trap 'rmdir "$LOCK" 2>/dev/null' EXIT INT TERM

# Reset diagnostic log for this run
: >"$LOG" 2>/dev/null
log "HMA-OSS PostBoot bootstrap start (moddir=$MODDIR abi=$ABI)"

# Stage module.prop
if [ -f "$MODDIR/module.prop" ]; then
    cp -f "$MODDIR/module.prop" "$RUN/module.prop" 2>/dev/null \
        || fatal "STAGE_MODULE_PROP"
fi

# Stage dex classes (the actual HMA code that runs inside zygote)
mkdir -p "$RUN/classes.mirror" 2>/dev/null
_dex_ok=0
for f in "$MODDIR"/classes*.dex; do
    [ -f "$f" ] || continue
    cp -f "$f" "$RUN/classes.mirror/" 2>/dev/null && _dex_ok=1
done
[ "$_dex_ok" -eq 1 ] || log "WARN: no classes*.dex staged; module may not be built with dex output"

# Stage the zygisk loader shim (libzygisk_loader.so)
mkdir -p "$RUN/zygisk" 2>/dev/null
if [ -f "$MODDIR/zygisk/arm64-v8a.so" ]; then
    cp -f "$MODDIR/zygisk/arm64-v8a.so" "$RUN/zygisk/arm64-v8a.so" 2>/dev/null \
        || fatal "STAGE_ZYGISK_SO"
fi

# Restore SELinux context of runtime files so the running zygote can read them.
# The zygote's context (u:r:zygote:s0) can read files labelled u:object_r:system_file
# via the tmpfs on /dev; ensure we use the /dev default label.
if command -v chcon >/dev/null 2>&1; then
    chcon -R u:object_r:system_file:s0 "$RUN" 2>/dev/null || \
    chcon -R u:object_r:tmpfs:s0       "$RUN" 2>/dev/null || true
fi
chmod -R 0755 "$RUN" 2>/dev/null

# --- Phase 2: verify Zygisk provider is alive & post-boot-capable ------------

PHASE=2

# Detect any of the supported Zygisk providers
PROVIDER=""
for p in zygisksu rezygisk admirepowered zygisk_on_ksu; do
    if [ -d "/data/adb/modules/$p" ] \
        && [ ! -f "/data/adb/modules/$p/disable" ] \
        && [ ! -f "/data/adb/modules/$p/remove" ]; then
        PROVIDER="$p"
        break
    fi
done

if [ -z "$PROVIDER" ]; then
    set_status "RESULT=NO_ZYGISK_PROVIDER
PHASE=2
HINT=install NeoZygisk-PostBoot, ReZygisk or a compatible Zygisk provider first"
    log "no Zygisk provider found"
    exit 0
fi
log "Zygisk provider detected: $PROVIDER"

# Detect the provider's runtime socket. NeoZygisk-PostBoot exposes cp64.sock
# under /dev/.neozygisk once its ptrace monitor has completed injection.
INJECTED=0
if [ -S /dev/.neozygisk/cp64.sock ] || [ -S /dev/.neozygisk/init_monitor ]; then
    INJECTED=1
fi

# --- Phase 3: verify HMA can be discovered by the running Zygisk provider ---

PHASE=3

# The Zygisk provider scans /data/adb/modules/*/zygisk/<abi>.so at boot.
# In late-load we cannot rescan. Instead, the *user* must trigger a KernelSU
# Manager "Soft Reboot" AFTER installation; the provider then re-enumerates
# modules and picks up HMA. We do NOT force a reboot here.

# Sanity: our zygisk lib is present in the module dir
if [ ! -f "$MODDIR/zygisk/arm64-v8a.so" ]; then
    set_status "RESULT=MISSING_ZYGISK_LIB
PHASE=3"
    log "zygisk/arm64-v8a.so not found under $MODDIR"
    exit 0
fi

# Read module id & disable/remove flags
MOD_DISABLED=0
[ -f "$MODDIR/disable" ] && MOD_DISABLED=1
[ -f "$MODDIR/remove"  ] && MOD_DISABLED=1

if [ "$MOD_DISABLED" = "1" ]; then
    set_status "RESULT=MODULE_DISABLED
PHASE=3"
    exit 0
fi

# --- Phase 4: report state ---------------------------------------------------

PHASE=4

# Detect zygote injection state by checking /proc/<pid>/maps for our lib name.
# The zygote loader library is named libzygisk_loader.so; when the provider
# has injected HMA it will be mapped in the zygote64 process.
ZYGOTE_PID=""
for pid in $(pgrep -f zygote64 2>/dev/null); do
    [ -n "$pid" ] || continue
    ZYGOTE_PID="$pid"
    break
done

LIB_MAPPED=0
if [ -n "$ZYGOTE_PID" ] && [ -r "/proc/$ZYGOTE_PID/maps" ]; then
    if grep -q 'libzygisk_loader\.so\|hma_oss' "/proc/$ZYGOTE_PID/maps" 2>/dev/null; then
        LIB_MAPPED=1
    fi
fi

SYS_SERVER_PID=""
for pid in $(pgrep -f system_server 2>/dev/null); do
    [ -n "$pid" ] || continue
    SYS_SERVER_PID="$pid"
    break
done

SS_MAPPED=0
if [ -n "$SYS_SERVER_PID" ] && [ -r "/proc/$SYS_SERVER_PID/maps" ]; then
    if grep -q 'libzygisk_loader\.so\|hma_oss' "/proc/$SYS_SERVER_PID/maps" 2>/dev/null; then
        SS_MAPPED=1
    fi
fi

if [ "$INJECTED" = "1" ] && [ "$SS_MAPPED" = "1" ]; then
    RESULT="HEALTHY_INJECTED"
elif [ "$INJECTED" = "1" ]; then
    RESULT="WAITING_FOR_KERNELSU_SOFT_REBOOT"
else
    RESULT="WAITING_FOR_ZYGISK_PROVIDER"
fi

set_status "RESULT=$RESULT
PHASE=4
PROVIDER=$PROVIDER
RUNTIME=$RUN
MODULE_DIR=$MODDIR
ZYGOTE_PID=${ZYGOTE_PID:-none}
SYSTEM_SERVER_PID=${SYS_SERVER_PID:-none}
ZYGOTE_LIB_MAPPED=$LIB_MAPPED
SYSTEM_SERVER_LIB_MAPPED=$SS_MAPPED
PROVIDER_SOCKET_READY=$INJECTED
POSTBOOT_MODE=1"

log "bootstrap done: RESULT=$RESULT"
exit 0
