#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$PROJECT_DIR/build/hbase"
mkdir -p "$BUILD_DIR"

javac -encoding UTF-8 -classpath "$(hbase classpath)" \
  -d "$BUILD_DIR" "$PROJECT_DIR/src/hbase/HBaseOperations.java"

jar -cvf "$PROJECT_DIR/build/hbase-operations.jar" -C "$BUILD_DIR" .
java -classpath "$PROJECT_DIR/build/hbase-operations.jar:$(hbase classpath)" \
  HBaseOperations demo

