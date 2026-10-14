import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class InvertedIndex {

    public static class IndexMapper
            extends Mapper<LongWritable, Text, Text, Text> {

        private final Set<String> stopWords = new HashSet<>();
        private final Text outputWord = new Text();
        private final Text outputDocument = new Text();
        private static final Pattern WORD_PATTERN =
                Pattern.compile("[A-Za-z]+");
        private String documentName;

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException {

            FileSplit fileSplit = (FileSplit) context.getInputSplit();
            documentName = fileSplit.getPath().getName();
            outputDocument.set(documentName);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream("stopwords.txt"),
                            StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim().toLowerCase(Locale.ROOT);
                    if (!line.isEmpty()) {
                        stopWords.add(line);
                    }
                }
            }
        }

        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {

            Matcher matcher = WORD_PATTERN.matcher(value.toString());

            while (matcher.find()) {
                String word = matcher.group().toLowerCase(Locale.ROOT);

                if (!stopWords.contains(word)) {
                    outputWord.set(word);
                    context.write(outputWord, outputDocument);
                }
            }
        }
    }

    public static class IndexReducer
            extends Reducer<Text, Text, NullWritable, Text> {

        @Override
        protected void reduce(
                Text word,
                Iterable<Text> documentNames,
                Context context)
                throws IOException, InterruptedException {

            Map<String, Integer> documentCounts = new TreeMap<>();
            int total = 0;

            for (Text document : documentNames) {
                String name = document.toString();
                documentCounts.put(
                        name,
                        documentCounts.getOrDefault(name, 0) + 1
                );
                total++;
            }

            StringBuilder result = new StringBuilder();
            result.append(word.toString()).append(" ");

            for (Map.Entry<String, Integer> entry
                    : documentCounts.entrySet()) {
                result.append("<")
                      .append(entry.getKey())
                      .append(",")
                      .append(entry.getValue())
                      .append(">;");
            }

            result.append("<total,")
                  .append(total)
                  .append(">.");

            context.write(
                    NullWritable.get(),
                    new Text(result.toString())
            );
        }
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 3) {
            System.err.println(
                    "Usage: InvertedIndex <input> <stopwords> <output>"
            );
            System.exit(1);
        }

        Configuration configuration = new Configuration();
        Job job = Job.getInstance(
                configuration,
                "English Inverted Index"
        );

        job.setJarByClass(InvertedIndex.class);
        job.setMapperClass(IndexMapper.class);
        job.setReducerClass(IndexReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);
        job.setNumReduceTasks(1);

        job.addCacheFile(
                new URI(args[1] + "#stopwords.txt")
        );

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[2]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

