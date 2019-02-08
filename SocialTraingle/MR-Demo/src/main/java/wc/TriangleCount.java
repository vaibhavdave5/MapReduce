package wc;
/**
 * 
 * @author Vaibhav
 */

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
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;

public class TriangleCount extends Configured implements Tool {
	private static final Logger logger = LogManager.getLogger(TriangleCount.class);

	public static class FindSinglePathMapper extends Mapper<Object, Text, Text, Text> {

		@Override
		public void map(final Object key, final Text value, final Context context)
				throws IOException, InterruptedException {
			final StringTokenizer itr = new StringTokenizer(value.toString());
			while (itr.hasMoreTokens()) {

				// Processing the CSV
				String[] ids = value.toString().split(",");

				/**
				 * Just like explained in lecture with the airplane example the to and from
				 * journey the triangle also has its to and from trail
				 */

				Text from = new Text(ids[0] + ";" + ids[1] + ";" + EdgeDirection.From.toString());
				Text to = new Text(ids[0] + ";" + ids[1] + ";" + EdgeDirection.To.toString());
				context.write(new Text(ids[0]), from);
				context.write(new Text(ids[1]), to);
			}
		}
	}

	public static class Path2Reducer extends Reducer<Text, Text, Text, Text> {

		@Override
		public void reduce(final Text key, final Iterable<Text> values, final Context context)
				throws IOException, InterruptedException {

			ArrayList<Text> from = new ArrayList<>(); // List of from
			ArrayList<Text> to = new ArrayList<>(); // List of to

			for (Text edge : values) {
				String[] arr = edge.toString().split(";");
				String edgeDirection = arr[2];

				if (edgeDirection.equals("from"))
					from.add(new Text(arr[0] + ";" + arr[1]));
				if (edgeDirection.equals("to"))
					to.add(new Text(arr[0] + ";" + arr[1]));
			}

			for (Text fromEdge : from) {
				for (Text toEdge : to) {
					// from is a->b and toEdge is b->c
					// So we want to emit a->c
					System.out.println(fromEdge.toString()+" "+toEdge.toString());
			
					context.write(new Text(fromEdge.toString()),new Text(toEdge.toString()));
				}
			}

		}
	}

	@Override
	public int run(final String[] args) throws Exception {
		final Configuration conf = getConf();
		final Job job = Job.getInstance(conf, "RSJoin");
		job.setJarByClass(TriangleCount.class);
		final Configuration jobConf = job.getConfiguration();
		jobConf.set("mapreduce.output.textoutputformat.separator", ",");
		job.setMapperClass(FindSinglePathMapper.class);
		job.setReducerClass(Path2Reducer.class);		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path("input/edges1.csv"));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		if(! job.waitForCompletion(true)) {
			throw new Exception("MR job1 failed");
		}

		Job job2 = Job.getInstance(conf,"RSJoin Complete Triangle");
		job2.setJarByClass(TriangleCount.class);
	
//		job2.setReducerClass(CloseTriangleReducer.class);
//		MultipleInputs.addInputPath(job2, new Path(args[0]), TextInputFormat.class,ThirdEdgeMapper.class);
//		MultipleInputs.addInputPath(job2, new Path(args[1]), TextInputFormat.class,Path2EdgeMapper.class);
		//+"/part-r-00000"
		job2.setOutputKeyClass(Text.class);
		job2.setOutputValueClass(Text.class);
		FileOutputFormat.setOutputPath(job2, new Path(args[1]+"-Triangle"));
		return job2.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(final String[] args) {
		if (args.length != 2) {
			throw new Error("Two arguments required:\n<input-dir> <output-dir>");
		}
		try {
			ToolRunner.run(new TriangleCount(), args);
		} catch (final Exception e) {
			logger.error("", e);
		}

	}

}