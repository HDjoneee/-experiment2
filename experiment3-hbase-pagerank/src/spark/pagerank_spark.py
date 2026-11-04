#!/usr/bin/env python3
import sys
from pyspark import SparkConf, SparkContext


def parse_line(line):
    page, raw_links = line.rstrip("\n").split("\t", 1)
    links = [item.strip() for item in raw_links.split(",") if item.strip()]
    return page.strip(), links


def contributions(item):
    _, (links, rank) = item
    if not links:
        return []
    value = rank / len(links)
    return [(target, value) for target in links]


def main():
    if len(sys.argv) < 3:
        print("Usage: pagerank_spark.py <input> <output> [iterations]", file=sys.stderr)
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2]
    iterations = int(sys.argv[3]) if len(sys.argv) > 3 else 10

    conf = SparkConf().setAppName("PageRankSpark")
    sc = SparkContext(conf=conf)
    sc.setLogLevel("WARN")

    parsed = sc.textFile(input_path).filter(lambda line: line.strip()).map(parse_line)
    source_links = parsed.collect()
    all_pages = sorted({page for page, _ in source_links} |
                       {target for _, links in source_links for target in links})
    link_map = dict(source_links)
    graph = sc.parallelize([(page, link_map.get(page, [])) for page in all_pages]).cache()
    ranks = graph.mapValues(lambda _: 1.0)

    for _ in range(iterations):
        incoming = graph.join(ranks).flatMap(contributions).reduceByKey(lambda a, b: a + b)
        ranks = graph.leftOuterJoin(incoming).mapValues(
            lambda pair: 0.15 + 0.85 * (pair[1] if pair[1] is not None else 0.0)
        )

    result = ranks.sortBy(lambda item: (-item[1], item[0])).map(
        lambda item: "(%s,%.10f)" % (item[0], item[1])
    )
    result.saveAsTextFile(output_path)
    for line in result.collect():
        print(line)
    sc.stop()


if __name__ == "__main__":
    main()

