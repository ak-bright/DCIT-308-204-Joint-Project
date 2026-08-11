#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# fetch-deps.sh — download the 3 third-party JARs this project needs.
#
# We deliberately keep these OUT of git (see .gitignore) because they are large
# binaries. None of them are DSA libraries — the assessed data structures and
# algorithms are all hand-written. These jars only provide:
#   * junit-platform-console-standalone : the JUnit 5 test runner
#   * sqlite-jdbc                        : the SQLite JDBC driver (database layer)
#   * slf4j-api / slf4j-nop              : silence sqlite-jdbc's logging backend
#
# Run this once after cloning:  bash scripts/fetch-deps.sh
# ---------------------------------------------------------------------------
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p lib
base="https://repo1.maven.org/maven2"
fetch() { # url  dest
  if [ -f "$2" ]; then echo "  already have $2"; else echo "  downloading $2"; curl -sSL --max-time 120 -o "$2" "$1"; fi
}
fetch "$base/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar" lib/junit-platform-console-standalone-1.10.2.jar
fetch "$base/org/xerial/sqlite-jdbc/3.45.3.0/sqlite-jdbc-3.45.3.0.jar"                                                  lib/sqlite-jdbc-3.45.3.0.jar
fetch "$base/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"                                                           lib/slf4j-api-1.7.36.jar
fetch "$base/org/slf4j/slf4j-nop/1.7.36/slf4j-nop-1.7.36.jar"                                                           lib/slf4j-nop-1.7.36.jar
echo "Dependencies ready in lib/"
