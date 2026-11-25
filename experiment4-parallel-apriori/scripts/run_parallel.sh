#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
HDFS_DIR="${1:-/exp4/apriori/input}"
OUTPUT_FILE="$PROJECT_DIR/output/parallel_result.txt"

mkdir -p "$PROJECT_DIR/output"
rm -f "$OUTPUT_FILE"
hdfs dfs -mkdir -p "$HDFS_DIR"
hdfs dfs -put -f "$PROJECT_DIR/data/transactions.txt" "$HDFS_DIR/transactions.txt"

spark-submit \
  --master yarn \
  --deploy-mode client \
  --conf spark.pyspark.python=python3 \
  "$PROJECT_DIR/src/parallel_apriori.py" \
  "$HDFS_DIR/transactions.txt" \
  "$OUTPUT_FILE" \
  --min-support 5 --max-size 3

