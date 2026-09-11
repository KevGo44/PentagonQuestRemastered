#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .cache
formatter=.cache/google-java-format.jar
if [[ ! -f "$formatter" ]]; then
  curl --fail --location --output "$formatter" https://repo.maven.apache.org/maven2/com/google/googlejavaformat/google-java-format/1.27.0/google-java-format-1.27.0-all-deps.jar
fi
digest=$(shasum -a 256 "$formatter" | cut -d ' ' -f 1)
if [[ "$digest" != ed07239f3cb72e25bf2a0eae63e76831f9f11963bd19fc36a6f1d87016ac1763 ]]; then
  echo 'Formatter checksum mismatch.' >&2
  exit 1
fi
rg --files src tools -g '*.java' -0 | xargs -0 java -jar "$formatter" --replace
