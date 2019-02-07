package wc;
/**
 * 
 * @author Vaibhav
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import wc.EdgeDirection;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class TriangleCount extends Configured implements Tool {
	private static final Logger logger = LogManager.getLogger(FollowerCount.class);

	
	
	public static class FindSinglePathMapper extends Mapper<Object, Text, Text, Text> {
		
		@Override
		public void map(final Object key, final Text value, final Context context) throws IOException, InterruptedException {
			final StringTokenizer itr = new StringTokenizer(value.toString());
			while (itr.hasMoreTokens()) {
				
					// Processing the CSV
					String[] ids = value.toString().split(",");
			
					/**
					 * Just like explained in lecture with the airplane example
					 * the to and from journey the triangle also has its to and from 
					 * trail
					 */
					
					Text from = new Text(ids[0]+";" + ids[1]+ ";" + EdgeDirection.From.toString());
					Text to = new Text(ids[0]+";" + ids[1]+ ";" + EdgeDirection.To.toString());
					context.write(new Text(ids[0]), from );
					context.write(new Text(ids[1]), to);	
			}
		}
	}
	

		
		
	

	@Override
	public int run(final String[] args) throws Exception {
		final Configuration conf = getConf();
		final Job job = Job.getInstance(conf, "Follower Count");
		job.setJarByClass(FollowerCount.class);
		final Configuration jobConf = job.getConfiguration();
		jobConf.set("mapreduce.output.textoutputformat.separator", "\t");
		job.setMapperClass(FindSinglePathMapper.class);
//		job.setCombinerClass(MyReducer.class);
//		job.setReducerClass(MyReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(final String[] args) {
		if (args.length != 2) {
			throw new Error("Two arguments required:\n<input-dir> <output-dir>");
		}
		try {
			ToolRunner.run(new FollowerCount(), args);
		} catch (final Exception e) {
			logger.error("", e);
		}
			
		
	}

}