#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ "$(uname -s)" != Darwin ]]; then
  echo 'This packaging target requires macOS and a JDK with jpackage.' >&2
  exit 1
fi
if [[ ! -f target/aschensiegel-1.0.0.jar ]]; then ./build.sh -q package; fi
mkdir -p target/package-input target/native
cp target/aschensiegel-1.0.0.jar target/package-input/
# jpackage refuses to overwrite an existing app; keep the previous one reviewable.
if [[ -d target/native/Pentagon.app ]]; then
  mv target/native/Pentagon.app "target/native/Pentagon-previous-$(date +%s).app"
fi
jpackage --type app-image --name Pentagon --app-version 1.0.0 \
  --input target/package-input --dest target/native \
  --main-jar aschensiegel-1.0.0.jar --main-class de.pentagon.core.Main \
  --java-options '-XstartOnFirstThread' \
  --java-options '--enable-native-access=ALL-UNNAMED' \
  --java-options '-Xmx2g' \
  --mac-package-identifier de.pentagon.aschensiegel
echo "App: $PWD/target/native/Pentagon.app"
