#!/usr/bin/env bash
# Build and install a module's debug APK onto the connected device.
#   ./tools/install.sh probe
#   ./tools/install.sh app
set -euo pipefail

MODULE="${1:-}"
case "$MODULE" in
  probe) APPLICATION_ID="dev.oneshot.probe" ;;
  app)   APPLICATION_ID="dev.oneshot" ;;
  *) echo "usage: $0 <probe|app>" >&2; exit 2 ;;
esac

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_HOME:-$HOME/android-sdk}"
ADB="$SDK/platform-tools/adb"
[ -x "$ADB" ] || ADB="adb"

cd "$ROOT"
./gradlew ":$MODULE:assembleDebug"

APK="$ROOT/$MODULE/build/outputs/apk/debug/$MODULE-debug.apk"
[ -f "$APK" ] || { echo "expected APK missing: $APK" >&2; exit 1; }

echo "==> devices"
"$ADB" devices

echo "==> installing $APK"
# -r keeps data so an existing probe report survives a rebuild; -t allows test builds.
"$ADB" install -r -t "$APK"

echo "==> $APPLICATION_ID installed"
