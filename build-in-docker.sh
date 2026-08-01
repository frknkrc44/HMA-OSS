#!/usr/bin/env bash
###############################################################################
# HMA-OSS Zygisk (PostBoot) — Docker build helper
#
# Builds the module ZIP inside a preconfigured Android build container.
# Requires: docker (or podman) on the host.
###############################################################################
set -euo pipefail

IMAGE="${IMAGE:-mingc/android-build-box:latest}"
ROOT="$(cd "$(dirname "$0")" && pwd)"

if ! [ -f "$ROOT/local.properties" ]; then
    cat > "$ROOT/local.properties" <<'EOF'
sdk.dir=/opt/android-sdk
officialBuild=false
localBuild=true
EOF
    echo "[+] Created default local.properties"
fi

echo "[+] Using container image: $IMAGE"
# Defensive: ensure gradlew is executable inside the mounted volume.
chmod +x "$ROOT/gradlew" 2>/dev/null || true
docker run --rm -it \
    -v "$ROOT":/src -w /src \
    -e GRADLE_USER_HOME=/src/.gradle-cache \
    "$IMAGE" \
    bash -c '
        set -e
        yes | sdkmanager "platforms;android-36" >/dev/null 2>&1 || true
        ./gradlew :app:assembleRelease
        ./gradlew :zygote:assembleRelease
        echo
        echo "[+] Build finished. Module ZIP(s):"
        ls -la zygote/build/outputs/magisk/release/ || true
    '
