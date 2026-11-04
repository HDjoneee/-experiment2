#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$PROJECT_DIR/build/mapreduce"
JAR_FILE="$PROJECT_DIR/build/pagerank-mapreduce.jar"
INPUT_PATH="${1:-/exp3/pagerank/input}"
OUTPUT_PATH="${2:-/exp3/pagerank/output}"

mkdir -p "$BUILD_DIR"
javac -encoding UTF-8 -classpath "$(hadoop classpath)" \
  -d "$BUILD_DIR" "$PROJECT_DIR/src/mapreduce/PageRankDriver.java"
jar -cvf "$JAR_FILE" -C "$BUILD_DIR" .

hdfs dfs -mkdir -p "$INPUT_PATH"
hdfs dfs -put -f "$PROJECT_DIR/data/graph.txt" "$INPUT_PATH/graph.txt"
hdfs dfs -rm -r -f "$OUTPUT_PATH" >/dev/null 2>&1 || true
hadoop jar "$JAR_FILE" PageRankDriver "$INPUT_PATH" "$OUTPUT_PATH" 10
hdfs dfs -cat "$OUTPUT_PATH/final/part-*"

