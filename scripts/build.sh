#!/usr/bin/env bash
#
# Build the debug APK in Docker.
#
# The image is native to the host architecture. On ARM64 it includes a pinned
# qemu-user 7.2 that runs the Android SDK's x86_64 aapt2; no binfmt_misc
# registration is needed.
#
set -euo pipefail
cd "$(dirname "$0")/.."

case "$(uname -m)" in
  x86_64|amd64|arm64|aarch64) ;;
  *)
    echo "Unsupported host architecture: $(uname -m)" >&2
    exit 1
    ;;
esac

docker build -t flightlink-build .

mkdir -p dist
docker run --rm \
  --user "$(id -u):$(id -g)" \
  -e HOME=/tmp \
  -e GRADLE_USER_HOME=/tmp/gradle-home \
  -v "$PWD":/workspace \
  -w /workspace \
  flightlink-build \
  env -u QEMU_VERSION \
    gradle --no-daemon \
      -Pandroid.aapt2FromMavenOverride=/opt/aapt2/aapt2 \
      assembleDebug

cp app/build/outputs/apk/debug/app-debug.apk dist/flightlink-debug.apk
echo
echo "APK: $(pwd)/dist/flightlink-debug.apk"
