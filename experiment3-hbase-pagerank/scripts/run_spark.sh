#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
INPUT_PATH="${1:-$PROJECT_DIR/data/graph.txt}"
OUTPUT_PATH="${2:-$PROJECT_DIR/output/spark}"
INPUT_ABS="$(readlink -f "$INPUT_PATH")"
OUTPUT_ABS="$(readlink -m "$OUTPUT_PATH")"

rm -rf "$OUTPUT_ABS"
spark-submit --master local[*] \
  "$PROJECT_DIR/src/spark/pagerank_spark.py" \
  "file://$INPUT_ABS" "file://$OUTPUT_ABS" 10

cat "$OUTPUT_ABS"/part-*

