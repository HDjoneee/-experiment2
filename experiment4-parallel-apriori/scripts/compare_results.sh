#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SERIAL="$PROJECT_DIR/output/serial_result.txt"
PARALLEL="$PROJECT_DIR/output/parallel_result.txt"

if cmp -s "$SERIAL" "$PARALLEL"; then
  echo "RESULT: IDENTICAL"
  sha256sum "$SERIAL" "$PARALLEL"
else
  echo "RESULT: DIFFERENT"
  diff -u "$SERIAL" "$PARALLEL" || true
  exit 1
fi

