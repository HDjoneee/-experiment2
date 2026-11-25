#!/usr/bin/env python3
import argparse
from itertools import combinations
from pathlib import Path

from pyspark import SparkConf, SparkContext


def parse_line(raw_line):
    _, items_text = raw_line.strip().split("\t", 1)
    return tuple(sorted(set(items_text.split(","))))


def generate_candidates(previous_frequent, size):
    previous_set = set(previous_frequent)
    candidates = set()
    for left, right in combinations(previous_frequent, 2):
        merged = tuple(sorted(set(left) | set(right)))
        if len(merged) != size:
            continue
        if all(tuple(subset) in previous_set for subset in combinations(merged, size - 1)):
            candidates.add(merged)
    return sorted(candidates)


def write_result(path, rows):
    output = Path(path)
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="\n") as target:
        target.write("size\titemset\tsupport\n")
        for size, itemset, support in rows:
            target.write(f"{size}\t{','.join(itemset)}\t{support}\n")


def main():
    parser = argparse.ArgumentParser(description="Parallel frequent itemset mining with Spark")
    parser.add_argument("input")
    parser.add_argument("output")
    parser.add_argument("--min-support", type=int, default=5)
    parser.add_argument("--max-size", type=int, default=3)
    args = parser.parse_args()

    conf = SparkConf().setAppName("ParallelAprioriExperiment4")
    sc = SparkContext(conf=conf)
    sc.setLogLevel("WARN")
    try:
        transactions = sc.textFile(args.input).filter(lambda line: line.strip()).map(parse_line).cache()
        transaction_count = transactions.count()
        all_items = sorted(transactions.flatMap(lambda transaction: transaction).distinct().collect())
        candidates = [(item,) for item in all_items]
        result = []

        for size in range(1, args.max_size + 1):
            if size > 1:
                candidates = generate_candidates(previous_frequent, size)
            if not candidates:
                break

            broadcast_candidates = sc.broadcast(set(candidates))
            counts = dict(
                transactions.flatMap(
                    lambda transaction: [
                        (itemset, 1)
                        for itemset in combinations(transaction, size)
                        if itemset in broadcast_candidates.value
                    ]
                ).reduceByKey(lambda left, right: left + right).collect()
            )
            broadcast_candidates.unpersist()

            previous_frequent = sorted(
                itemset for itemset in candidates if counts.get(itemset, 0) >= args.min_support
            )
            result.extend((size, itemset, counts[itemset]) for itemset in previous_frequent)
            if not previous_frequent:
                break

        write_result(args.output, result)
        print(f"transactions={transaction_count}")
        print(f"frequent_itemsets={len(result)}")
        print(f"output={args.output}")
        for size, itemset, support in result:
            print(f"({','.join(itemset)},{support})")
    finally:
        sc.stop()


if __name__ == "__main__":
    main()

