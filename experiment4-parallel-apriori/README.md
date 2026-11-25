# 实验四 并行化频繁项集挖掘

本目录使用 Apriori 思想对购物篮事务数据进行频繁项集挖掘，并分别实现非并行化版本和 Spark 并行版本。两个程序共享数据、候选生成规则、最小支持度和输出排序，因此可以逐字节比较结果。

## 目录结构

- `data/transactions.txt`：30 条购物篮事务；
- `src/serial_apriori.py`：纯 Python 单机版本；
- `src/parallel_apriori.py`：PySpark 并行版本；
- `scripts/run_serial.sh`：运行单机版本；
- `scripts/run_parallel.sh`：上传数据到 HDFS，并以 YARN client 模式运行 Spark；
- `scripts/compare_results.sh`：使用 `cmp`、`sha256sum` 验证结果完全一致；
- `expected/frequent_itemsets.txt`：支持度阈值为 5 时的预期结果。

## 数据格式

每行由事务编号、制表符和商品集合组成：

```text
T001\tmilk,bread,eggs,butter
```

本实验设置最小支持度计数为 5，最多挖掘三项集。结果格式为：

```text
size<TAB>itemset<TAB>support
```

## 运行环境

- Linux；
- Hadoop 3.3.6 伪分布式环境；
- Spark 3.5.9 on YARN；
- JDK 1.8.0_371；
- Python 3.8 或更高版本。

## 执行步骤

```bash
cd ~/experiment4-parallel-apriori
chmod +x scripts/*.sh
jps
```

先运行单机版：

```bash
./scripts/run_serial.sh
```

再运行并行版：

```bash
./scripts/run_parallel.sh
```

验证结果：

```bash
./scripts/compare_results.sh
```

也可以一次执行：

```bash
./scripts/run_all.sh
```

若结果完全一致，终端显示：

```text
RESULT: IDENTICAL
```

并且两个结果文件的 SHA-256 值相同。

## 报告截图建议

1. `jps`、`head data/transactions.txt`：环境与数据；
2. `./scripts/run_serial.sh`：非并行算法结果；
3. `./scripts/run_parallel.sh`：Spark/YARN 作业与结果；
4. `./scripts/compare_results.sh`：`RESULT: IDENTICAL` 和相同哈希值。

