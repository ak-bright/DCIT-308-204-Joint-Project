#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# build.sh — one-stop build/run script for the Hospital & Clinic Operations
# Optimizer. No Maven/Gradle required: this project compiles with plain `javac`.
#
# Usage:
#   bash build.sh compile     # compile all main sources -> out/main
#   bash build.sh test        # compile + run the JUnit 5 test suite
#   bash build.sh run         # compile + launch the console application
#   bash build.sh bench       # compile + run the performance benchmark harness
#   bash build.sh all         # compile + test (default)
# ---------------------------------------------------------------------------
set -euo pipefail
cd "$(dirname "$0")"

# On Git-Bash / MSYS / Cygwin the JVM still expects ';' between classpath entries.
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
  *)                    SEP=':' ;;
esac

LIBS="lib/junit-platform-console-standalone-1.10.2.jar${SEP}lib/sqlite-jdbc-3.45.3.0.jar${SEP}lib/slf4j-api-1.7.36.jar${SEP}lib/slf4j-nop-1.7.36.jar"
MAIN_OUT="out/main"
TEST_OUT="out/test"

ensure_deps() {
  if [ ! -f lib/junit-platform-console-standalone-1.10.2.jar ]; then
    echo "Dependencies missing — running scripts/fetch-deps.sh"; bash scripts/fetch-deps.sh
  fi
}

compile_main() {
  ensure_deps
  echo "==> Compiling main sources"
  mkdir -p "$MAIN_OUT"
  # Collect every .java under src/main/java into a single argfile for javac.
  find src/main/java -name '*.java' > out/sources.txt
  javac -encoding UTF-8 -d "$MAIN_OUT" -cp "$LIBS" @out/sources.txt
  echo "    done -> $MAIN_OUT"
}

compile_test() {
  compile_main
  echo "==> Compiling test sources"
  mkdir -p "$TEST_OUT"
  find src/test/java -name '*.java' > out/test-sources.txt
  javac -encoding UTF-8 -d "$TEST_OUT" -cp "${LIBS}${SEP}${MAIN_OUT}" @out/test-sources.txt
  echo "    done -> $TEST_OUT"
}

cmd="${1:-all}"
case "$cmd" in
  compile) compile_main ;;
  test)
    compile_test
    echo "==> Running JUnit 5 test suite"
    # The SQLite/SLF4J jars must be on the launcher classpath too, so the
    # database integration tests can load the JDBC driver at runtime.
    java -jar lib/junit-platform-console-standalone-1.10.2.jar \
         -cp "${MAIN_OUT}${SEP}${TEST_OUT}${SEP}${LIBS}" \
         --scan-classpath="${TEST_OUT}" --details=tree
    ;;
  run)
    compile_main
    echo "==> Launching console application"
    java -cp "${MAIN_OUT}${SEP}${LIBS}" ui.Main
    ;;
  bench)
    compile_main
    echo "==> Running performance benchmark harness"
    java -cp "${MAIN_OUT}${SEP}${LIBS}" performance.BenchmarkRunner
    ;;
  all)
    compile_test
    echo "==> Running JUnit 5 test suite"
    java -jar lib/junit-platform-console-standalone-1.10.2.jar \
         -cp "${MAIN_OUT}${SEP}${TEST_OUT}${SEP}${LIBS}" \
         --scan-classpath="${TEST_OUT}" --details=tree
    ;;
  *) echo "Unknown command: $cmd"; exit 1 ;;
esac
