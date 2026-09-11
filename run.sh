#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -f target/aschensiegel-1.0.0.jar ]]; then
  ./build.sh -q package
fi
java_options=(--enable-native-access=ALL-UNNAMED -Xms256m -Xmx2g)
if [[ "$(uname -s)" == Darwin ]]; then java_options+=(-XstartOnFirstThread); fi
exec java "${java_options[@]}" -jar target/aschensiegel-1.0.0.jar "$@"
