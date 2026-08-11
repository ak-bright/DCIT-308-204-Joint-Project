# ---------------------------------------------------------------------------
# build.ps1 — Windows PowerShell build/run script (mirror of build.sh).
#
# Usage:
#   .\build.ps1 compile   # compile all main sources -> out\main
#   .\build.ps1 test      # compile + run the JUnit 5 test suite
#   .\build.ps1 run       # compile + launch the console application
#   .\build.ps1 bench     # compile + run the performance benchmark harness
#   .\build.ps1 all       # compile + test (default)
# ---------------------------------------------------------------------------
param([string]$Cmd = "all")
$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

$SEP  = ";"   # Windows classpath separator
$LIBS = "lib\junit-platform-console-standalone-1.10.2.jar$SEP" +
        "lib\sqlite-jdbc-3.45.3.0.jar$SEP" +
        "lib\slf4j-api-1.7.36.jar$SEP" +
        "lib\slf4j-nop-1.7.36.jar"
$MAIN_OUT = "out\main"
$TEST_OUT = "out\test"

function Ensure-Deps {
    if (-not (Test-Path "lib\junit-platform-console-standalone-1.10.2.jar")) {
        Write-Host "Dependencies missing — run: bash scripts/fetch-deps.sh"
        exit 1
    }
}

function Compile-Main {
    Ensure-Deps
    Write-Host "==> Compiling main sources"
    New-Item -ItemType Directory -Force -Path $MAIN_OUT | Out-Null
    $srcs = Get-ChildItem -Recurse -Filter *.java src\main\java | ForEach-Object { $_.FullName }
    $srcs | Set-Content -Encoding utf8 out\sources.txt
    & javac -d $MAIN_OUT -cp $LIBS "@out\sources.txt"
    if ($LASTEXITCODE -ne 0) { throw "main compile failed" }
    Write-Host "    done -> $MAIN_OUT"
}

function Compile-Test {
    Compile-Main
    Write-Host "==> Compiling test sources"
    New-Item -ItemType Directory -Force -Path $TEST_OUT | Out-Null
    $srcs = Get-ChildItem -Recurse -Filter *.java src\test\java | ForEach-Object { $_.FullName }
    $srcs | Set-Content -Encoding utf8 out\test-sources.txt
    & javac -d $TEST_OUT -cp "$LIBS$SEP$MAIN_OUT" "@out\test-sources.txt"
    if ($LASTEXITCODE -ne 0) { throw "test compile failed" }
    Write-Host "    done -> $TEST_OUT"
}

switch ($Cmd) {
    "compile" { Compile-Main }
    "test" {
        Compile-Test
        Write-Host "==> Running JUnit 5 test suite"
        & java -jar lib\junit-platform-console-standalone-1.10.2.jar -cp "$MAIN_OUT$SEP$TEST_OUT" --scan-classpath --details=tree
    }
    "run" {
        Compile-Main
        Write-Host "==> Launching console application"
        & java -cp "$MAIN_OUT$SEP$LIBS" ui.Main
    }
    "bench" {
        Compile-Main
        Write-Host "==> Running performance benchmark harness"
        & java -cp "$MAIN_OUT$SEP$LIBS" performance.BenchmarkRunner
    }
    "all" {
        Compile-Test
        Write-Host "==> Running JUnit 5 test suite"
        & java -jar lib\junit-platform-console-standalone-1.10.2.jar -cp "$MAIN_OUT$SEP$TEST_OUT" --scan-classpath --details=tree
    }
    default { Write-Host "Unknown command: $Cmd"; exit 1 }
}
