#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
"$PROJECT_DIR/scripts/run_serial.sh"
"$PROJECT_DIR/scripts/run_parallel.sh"
"$PROJECT_DIR/scripts/compare_results.sh"

