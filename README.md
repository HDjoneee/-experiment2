# Hadoop MapReduce 倒排索引实验

本项目使用 Java 和 Hadoop MapReduce 对英文文档构建倒排索引。程序对文本进行英文分词、统一转为小写、过滤停词，并统计每个单词在各文档中的出现次数及总次数。

## 项目结构

```text
experiment2-github/
├── src/
│   └── InvertedIndex.java
├── config/
│   └── stopwords.txt
├── data/
│   └── README.md
├── .gitignore
└── README.md
```

## 输出格式

```text
data <document01.txt,6>;<document02.txt,3>;<total,9>.
```

其中，`<document01.txt,6>` 表示单词 `data` 在 `document01.txt` 中出现 6 次，`<total,9>` 表示该单词在全部文档中共出现 9 次。

## 编译与打包

在项目根目录执行：

```bash
mkdir -p build/classes

javac -classpath "$(hadoop classpath)" \
  -d build/classes \
  src/InvertedIndex.java

jar -cvf inverted-index.jar -C build/classes .
```

## 上传实验数据到 HDFS

假设本地 `data/input/` 中放置了 `document01.txt` 至 `document10.txt`：

```bash
hdfs dfs -mkdir -p /user/$USER/inverted/input
hdfs dfs -put data/input/*.txt /user/$USER/inverted/input/
hdfs dfs -put config/stopwords.txt /user/$USER/inverted/
```

## 运行程序

```bash
hadoop jar inverted-index.jar InvertedIndex \
  /user/$USER/inverted/input \
  /user/$USER/inverted/stopwords.txt \
  /user/$USER/inverted/output
```

如果输出目录已经存在，应先确认路径无误后删除旧目录：

```bash
hdfs dfs -rm -r /user/$USER/inverted/output
```

## 查看结果

```bash
hdfs dfs -cat /user/$USER/inverted/output/part-r-00000 | head -50
```

## 实现思路

Mapper 从当前输入分片中获取文档名，使用正则表达式提取英文单词，并过滤停词，然后输出 `<word, documentName>`。Hadoop Shuffle 按单词进行分组。Reducer 统计同一单词在每个文档中的次数，同时计算总次数，最后按指定格式输出倒排索引。

