package wc;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.conf.Configuration;

public class Path2JoinRSCardinality extends Configured implements Tool{
private static final Logger logger = LogManager.getLogger(Path2JoinRSCardinality.class);
	
	public static class TokenizerMapper extends Mapper<Object, Text, Text, Text> {
				
		@Override
		public void map(final Object key, final Text value, final Context context) throws IOException, InterruptedException {
						
		
			// Split on comma since input is from a CSV file.
			final String[] nodes = value.toString().split(",");
										
					context.write(new Text(nodes[0]),new Text("from") );
					context.write(new Text(nodes[1]), new Text("to"));
				
			}			
		
	}

	public static class Path2Reducer extends Reducer<Text, Text, Text, Text> {
		private static BigInteger count = BigInteger.valueOf(0);
		
		
		@Override
		public void reduce(final Text key, final Iterable<Text> values, final Context context) throws IOException, InterruptedException {
			
			List<Text> from = new ArrayList<>();
			List<Text> to = new ArrayList<>();
			
			
			for(Text edge : values) {
				if(edge.equals(new Text("from")))
					from.add(edge);
				if(edge.equals(new Text("to")))
					to.add(edge);

			}
			int mul = from.size() * to.size();
			count = count.add(BigInteger.valueOf(mul));
							
						
		}
		
		@Override
		public void cleanup(final Context context) throws IOException, InterruptedException{
			Text countText = new Text();
			countText.set(count.toString());
			context.write(new Text("Cardinality="),countText);
			
		}
	}

	@Override
	public int run(final String[] args) throws Exception {
		final Configuration conf = getConf();
		final Job job = Job.getInstance(conf, "Cardinality");
		job.setJarByClass(Path2JoinRSCardinality.class);
		final Configuration jobConf = job.getConfiguration();
		jobConf.set("mapreduce.output.textoutputformat.separator", " ");
		job.setMapperClass(TokenizerMapper.class);
		job.setReducerClass(Path2Reducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path("input/edges.csv"));
		FileOutputFormat.setOutputPath(job, new Path("output"));
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	// MAIN
	public static void main(String[] args) {
		
		try {
			ToolRunner.run(new Path2JoinRSCardinality(), args);
		} catch (final Exception e) {
			logger.error("", e);
		}
	}

}
