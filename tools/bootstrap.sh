#!/usr/bin/env bash
# Provisions the command-line Android toolchain for OneShot.
# No Android Studio required. Safe to re-run: every step is idempotent.
#
#   ./tools/bootstrap.sh
#
# Exits non-zero on the first failure so CI/agents cannot silently proceed
# with a half-provisioned SDK.

set -euo pipefail

GRADLE_VERSION="8.11.1"
SDK_PACKAGES=("platform-tools" "platforms;android-36" "build-tools;36.0.0")

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"

say() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }
die() { printf '\n\033[1;31mERROR: %s\033[0m\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------- SDK directory
say "SDK root: $SDK_DIR"
[ -d "$SDK_DIR" ] || mkdir -p "$SDK_DIR"

SDKMANAGER="$(ls "$SDK_DIR"/cmdline-tools/*/bin/sdkmanager.bat 2>/dev/null | sort -V | tail -1 || true)"
[ -n "$SDKMANAGER" ] || die "sdkmanager.bat not found under $SDK_DIR/cmdline-tools/*/bin"
say "sdkmanager: $SDKMANAGER"

# Java is required by sdkmanager; fail loudly with a useful message rather than
# letting the .bat print a cryptic stack trace.
command -v java >/dev/null 2>&1 || die "java not on PATH; sdkmanager needs a JDK (17+)"

# ------------------------------------------------------------------- licenses
say "Accepting SDK licenses"
yes 2>/dev/null | "$SDKMANAGER" --sdk_root="$SDK_DIR" --licenses >/dev/null || true

# ------------------------------------------------------------------- packages
say "Installing SDK packages: ${SDK_PACKAGES[*]}"
"$SDKMANAGER" --sdk_root="$SDK_DIR" "${SDK_PACKAGES[@]}"

for p in "${SDK_PACKAGES[@]}"; do
  case "$p" in
    platform-tools)     [ -f "$SDK_DIR/platform-tools/adb" ] || [ -f "$SDK_DIR/platform-tools/adb.exe" ] || die "platform-tools installed but adb is missing" ;;
    platforms\;*)       [ -d "$SDK_DIR/platforms/${p#platforms;}" ] || die "missing ${p}" ;;
    build-tools\;*)     [ -d "$SDK_DIR/build-tools/${p#build-tools;}" ] || die "missing ${p}" ;;
  esac
done

# ------------------------------------------------------------- local.properties
say "Writing local.properties"
SDK_DIR_POSIX="$(echo "$SDK_DIR" | sed -e 's|^/\([a-zA-Z]\)/|\1:/|')"
printf 'sdk.dir=%s\n' "$SDK_DIR_POSIX" > "$ROOT/local.properties"
cat "$ROOT/local.properties"

# Put adb on PATH for the rest of this script and print a hint for the user.
export PATH="$SDK_DIR/platform-tools:$PATH"
say "adb: $(adb version 2>/dev/null | head -1 || echo 'NOT RUNNABLE')"

# ---------------------------------------------------------------- Gradle wrapper
if [ -f "$ROOT/gradlew" ] && [ -f "$ROOT/gradle/wrapper/gradle-wrapper.jar" ]; then
  say "Gradle wrapper already present, skipping generation"
else
  say "Bootstrapping Gradle $GRADLE_VERSION to generate the wrapper"
  DIST_DIR="$ROOT/tools/gradle-dist"
  mkdir -p "$DIST_DIR"
  ZIP="$DIST_DIR/gradle-$GRADLE_VERSION-bin.zip"
  if [ ! -f "$ZIP" ]; then
    curl -fL --retry 3 -o "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  fi
  if [ ! -d "$DIST_DIR/gradle-$GRADLE_VERSION" ]; then
    (cd "$DIST_DIR" && unzip -qo "$ZIP")
  fi
  "$DIST_DIR/gradle-$GRADLE_VERSION/bin/gradle" -p "$ROOT" wrapper \
      --gradle-version "$GRADLE_VERSION" --distribution-type bin
fi

# The wrapper re-downloads the distribution from services.gradle.org even though bootstrap
# already has it locally; on a slow link that stalls for many minutes on a file lock.
# Seed the wrapper's dist directory with the copy we already verified instead.
say "Seeding the Gradle wrapper distribution from the local copy"
LOCAL_ZIP="$ROOT/tools/gradle-dist/gradle-$GRADLE_VERSION-bin.zip"
for dir in "$HOME/.gradle/wrapper/dists/gradle-$GRADLE_VERSION-bin"/*/; do
  [ -d "$dir" ] || continue
  if [ ! -f "$dir/gradle-$GRADLE_VERSION-bin.zip" ] && [ -f "$LOCAL_ZIP" ]; then
    rm -f "$dir"/*.part "$dir"/*.lck
    cp "$LOCAL_ZIP" "$dir/gradle-$GRADLE_VERSION-bin.zip"
    echo "    seeded $dir"
  fi
done

say "Verifying wrapper"
"$ROOT/gradlew" --version

cat <<'EOF'

Bootstrap complete. Next steps:
  ./gradlew :probe:assembleDebug
  ./tools/install.sh probe
  adb devices            # plug in the Xiaomi 17 Pro with USB debugging enabled
EOF
