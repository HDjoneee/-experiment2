#!/usr/bin/env python3
import argparse
from collections import Counter
from itertools import combinations
from pathlib import Path


def parse_transactions(path):
    transactions = []
    with open(path, "r", encoding="utf-8") as source:
        for raw_line in source:
            line = raw_line.strip()
            if not line:
                continue
            _, items_text = line.split("\t", 1)
            transactions.append(tuple(sorted(set(items_text.split(",")))))
    return transactions


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


def mine(transactions, min_support, max_size):
    all_items = sorted({item for transaction in transactions for item in transaction})
    candidates = [(item,) for item in all_items]
    result = []

    for size in range(1, max_size + 1):
        if size > 1:
            candidates = generate_candidates(previous_frequent, size)
        if not candidates:
            break

        candidate_set = set(candidates)
        counts = Counter()
        for transaction in transactions:
            for itemset in combinations(transaction, size):
                if itemset in candidate_set:
                    counts[itemset] += 1

        previous_frequent = sorted(
            itemset for itemset in candidates if counts[itemset] >= min_support
        )
        result.extend((size, itemset, counts[itemset]) for itemset in previous_frequent)
        if not previous_frequent:
            break

    return result


def write_result(path, rows):
    output = Path(path)
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="\n") as target:
        target.write("size\titemset\tsupport\n")
        for size, itemset, support in rows:
            target.write(f"{size}\t{','.join(itemset)}\t{support}\n")


def main():
    parser = argparse.ArgumentParser(description="Serial frequent itemset mining")
    parser.add_argument("input")
    parser.add_argument("output")
    parser.add_argument("--min-support", type=int, default=5)
    parser.add_argument("--max-size", type=int, default=3)
    args = parser.parse_args()

    transactions = parse_transactions(args.input)
    rows = mine(transactions, args.min_support, args.max_size)
    write_result(args.output, rows)
    print(f"transactions={len(transactions)}")
    print(f"frequent_itemsets={len(rows)}")
    print(f"output={args.output}")
    for size, itemset, support in rows:
        print(f"({','.join(itemset)},{support})")


if __name__ == "__main__":
    main()

