#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
INPUT_PATH="${1:-$PROJECT_DIR/data/graph.txt}"
OUTPUT_PATH="${2:-$PROJECT_DIR/output/spark}"

rm -rf "$OUTPUT_PATH"
spark-submit --master local[*] \
  "$PROJECT_DIR/src/spark/pagerank_spark.py" "$INPUT_PATH" "$OUTPUT_PATH" 10

cat "$OUTPUT_PATH"/part-*

