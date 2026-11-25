#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$PROJECT_DIR/output"

python3 "$PROJECT_DIR/src/serial_apriori.py" \
  "$PROJECT_DIR/data/transactions.txt" \
  "$PROJECT_DIR/output/serial_result.txt" \
  --min-support 5 --max-size 3

