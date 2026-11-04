# 实验三：HBase 操作与 PageRank 算法实现

本仓库目录包含《大数据管理与分析》实验三的可复现实验代码：

- `src/hbase/HBaseOperations.java`：HBase Java API 实现；
- `scripts/hbase_shell_commands.hbase`：与 Java 程序对应的 HBase Shell 操作；
- `src/mapreduce/PageRankDriver.java`：MapReduce 版 PageRank，固定迭代 10 次；
- `src/spark/pagerank_spark.py`：PySpark 版 PageRank，固定迭代 10 次；
- `data/graph.txt`：5 个网页的测试图；
- `expected/pagerank_after_10.txt`：两个 PageRank 程序应得到的 10 次迭代结果。

## 1. 数据说明

HBase 部分把关系数据转换为宽表。`Student` 以学生姓名为 RowKey，使用 `Info` 和 `Score` 两个列族；课程成绩以 `Score:课程名` 存储。`Course` 以课程号为 RowKey，使用 `Info` 列族。

原题学生表中 Mary 和 Lisi 的学号都写成 `2015003`，而选课表出现 `2015002`。为了保持关系一致，本实现将 Mary 的学号按选课表修正为 `2015002`，Lisi 保持 `2015003`。

PageRank 测试数据格式为 `网页<TAB>出链1,出链2`：

```text
A\tB,C
B\tC,D
C\tA
D\tC
E\tA,D
```

每个网页初始 PR 值为 1.0，阻尼系数为 0.85，更新公式为：

```text
PR(p) = 0.15 + 0.85 * Σ(PR(q) / L(q))
```

## 2. HBase Shell

先启动 Hadoop、ZooKeeper 和 HBase，然后执行：

```bash
hbase shell scripts/hbase_shell_commands.hbase
```

脚本涵盖 `list`、`describe`、`scan`、`put`、`delete`、`alter`、`truncate` 和 `count`。

## 3. HBase Java API

```bash
chmod +x scripts/*.sh
./scripts/run_hbase.sh
```

`HBaseOperations` 提供题目要求的 `createTable`、`addRecord`、`scanColumn`、`modifyData`、`deleteRow`，以及列出表、扫描全表、增删列族/列、清空表和统计行数等方法。

## 4. MapReduce PageRank

```bash
./scripts/run_mapreduce.sh /exp3/pagerank/input /exp3/pagerank/output
```

程序先初始化完整网页集合，再依次创建 10 个迭代 Job，最后格式化为 `(网页,PR值)`，PR 值保留 10 位小数。

## 5. Spark PageRank

本地伪分布式测试：

```bash
./scripts/run_spark.sh data/graph.txt output/spark
```

也可以直接使用 HDFS 路径：

```bash
spark-submit --master local[*] src/spark/pagerank_spark.py \
  hdfs:///exp3/pagerank/input/graph.txt hdfs:///exp3/pagerank/spark-output 10
```

## 6. 预期结果

```text
(C,1.7268203245)
(A,1.6776251686)
(B,0.8662591194)
(D,0.5792953875)
(E,0.1500000000)
```

由于 MapReduce 和 Spark 使用相同初值、阻尼系数和迭代次数，两者结果应一致。若输出目录已存在，运行前需删除旧目录。

