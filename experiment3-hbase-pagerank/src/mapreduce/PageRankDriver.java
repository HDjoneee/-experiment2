import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class PageRankDriver extends Configured implements Tool {
    private static final double DAMPING = 0.85;

    public static class InitMapper extends Mapper<LongWritable, Text, Text, Text> {
        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) return;
            String[] parts = line.split("\\t", 2);
            String page = parts[0].trim();
            String links = parts.length == 2 ? parts[1].trim() : "";
            context.write(new Text(page), new Text("S\t" + links));
            if (!links.isEmpty()) {
                for (String link : links.split(",")) {
                    String target = link.trim();
                    if (!target.isEmpty()) context.write(new Text(target), new Text("P"));
                }
            }
        }
    }

    public static class InitReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text page, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            String links = "";
            for (Text value : values) {
                String text = value.toString();
                if (text.startsWith("S\t")) links = text.substring(2);
            }
            context.write(page, new Text("1.0000000000\t" + links));
        }
    }

    public static class RankMapper extends Mapper<LongWritable, Text, Text, Text> {
        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String[] kv = value.toString().split("\\t", 2);
            if (kv.length < 2) return;
            String page = kv[0];
            String[] fields = kv[1].split("\\t", 2);
            double rank = Double.parseDouble(fields[0]);
            String links = fields.length == 2 ? fields[1] : "";
            context.write(new Text(page), new Text("S\t" + links));
            if (!links.isEmpty()) {
                String[] targets = links.split(",");
                double share = rank / targets.length;
                for (String target : targets) {
                    context.write(new Text(target.trim()), new Text("C\t" + share));
                }
            }
        }
    }

    public static class RankReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text page, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            String links = "";
            double incoming = 0.0;
            for (Text value : values) {
                String text = value.toString();
                if (text.startsWith("S\t")) links = text.substring(2);
                else if (text.startsWith("C\t")) incoming += Double.parseDouble(text.substring(2));
            }
            double rank = (1.0 - DAMPING) + DAMPING * incoming;
            context.write(page, new Text(String.format(Locale.US, "%.10f\t%s", rank, links)));
        }
    }

    public static class FormatMapper extends Mapper<LongWritable, Text, NullWritable, Text> {
        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String[] fields = value.toString().split("\\t");
            if (fields.length >= 2) {
                context.write(NullWritable.get(), new Text("(" + fields[0] + "," + fields[1] + ")"));
            }
        }
    }

    private Job newJob(String name, Class<?> mapper, Class<?> reducer,
                       Path input, Path output) throws IOException {
        Job job = Job.getInstance(getConf(), name);
        job.setJarByClass(PageRankDriver.class);
        job.setMapperClass((Class<? extends Mapper>) mapper);
        if (reducer != null) job.setReducerClass((Class<? extends Reducer>) reducer);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setNumReduceTasks(1);
        FileInputFormat.addInputPath(job, input);
        FileOutputFormat.setOutputPath(job, output);
        return job;
    }

    @Override
    public int run(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: PageRankDriver <input> <outputRoot> [iterations]");
            return 1;
        }
        int iterations = args.length > 2 ? Integer.parseInt(args[2]) : 10;
        Path current = new Path(args[1] + "/iter-00");
        if (!newJob("pagerank-init", InitMapper.class, InitReducer.class,
                    new Path(args[0]), current).waitForCompletion(true)) return 1;

        for (int i = 1; i <= iterations; i++) {
            Path next = new Path(String.format("%s/iter-%02d", args[1], i));
            if (!newJob("pagerank-iteration-" + i, RankMapper.class, RankReducer.class,
                        current, next).waitForCompletion(true)) return 1;
            current = next;
        }

        Job format = Job.getInstance(getConf(), "pagerank-format");
        format.setJarByClass(PageRankDriver.class);
        format.setMapperClass(FormatMapper.class);
        format.setNumReduceTasks(0);
        format.setOutputKeyClass(NullWritable.class);
        format.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(format, current);
        FileOutputFormat.setOutputPath(format, new Path(args[1] + "/final"));
        return format.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        System.exit(ToolRunner.run(new Configuration(), new PageRankDriver(), args));
    }
}

