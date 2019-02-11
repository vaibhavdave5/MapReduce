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
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.hadoop.conf.Configuration;

public class TriangleCount extends Configured implements Tool {
	private static final Logger logger = LogManager.getLogger(TriangleCount.class);
	private final static int maxFilter = 10000;

	public static class FindSinglePathMapper extends Mapper<Object, Text, Text, Text> {

		@Override
		public void map(final Object key, final Text value, final Context context)
				throws IOException, InterruptedException {

				// Processing the CSV
				String[] ids = value.toString().split(",");

				/**
				 * Just like explained in lecture with the airplane example the to and from
				 * journey the triangle also has its to and from trail
				 */
				if(Integer.parseInt(ids[0]) < maxFilter && Integer.parseInt(ids[1])<maxFilter)
				{
				Text from = new Text(ids[0] + "-" + ids[1] + "-" + EdgeDirection.From.toString());
				Text to = new Text(ids[0] + "-" + ids[1] + "-" + EdgeDirection.To.toString());
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
				String[] arr = edge.toString().split("-");
				String edgeDirection = arr[2];

				if (edgeDirection.equals(EdgeDirection.From.toString()))
					from.add(new Text(arr[0] + "-" + arr[1]));
				if (edgeDirection.equals(EdgeDirection.To.toString()))
					to.add(new Text(arr[0] + "-" + arr[1]));
			}

			for (Text fromEdge : from) {
				for (Text toEdge : to) {
					// from is a->b and toEdge is b->c
					// So we want to emit a->c
			
					context.write(new Text(toEdge.toString().split("-")[0]) ,    // a
								  new Text(fromEdge.toString().split("-")[1]));  // c
				}
			}

		}
	}
	
	
	public static class CompleteTriangleMapper extends Mapper<Object, Text, Text, Text> {


		@Override
		public void map(final Object key, final Text value, final Context context) throws IOException, InterruptedException {

			// Split on comma since input is from a CSV file.
			final String[] nodes = value.toString().split(",");
			
			if(Integer.parseInt(nodes[0]) < maxFilter && Integer.parseInt(nodes[1])<maxFilter)
			{	
	
					Text edgeValsTo = new Text(EdgeDirection.To.toString());					
					context.write(new Text(nodes[1]+","+nodes[0]),edgeValsTo );
			}
				
		}
	}

	public static class Path2EdgeMapper extends Mapper<Object, Text, Text, Text> {


		@Override
		public void map(final Object key, final Text value, final Context context) throws IOException, InterruptedException {

			Text edgeValsFrom = new Text(EdgeDirection.From.toString());
			final String[] nodes = value.toString().split(",");
			context.write(new Text(nodes[0]+","+ nodes[1]),edgeValsFrom );

		}
	}

	public static class CompleteTriangleReducer extends Reducer<Text, Text, Text, Text> {
		private static long count = 0;

		@Override
		public void reduce(final Text key, final Iterable<Text> values, final Context context) throws IOException, InterruptedException {

			long fromEdges = 0;
			long toEdges = 0; 
			for(Text value : values) {

				
				if(value.toString().equals(EdgeDirection.From.toString())) {
					fromEdges++;
				}
				if(value.toString().equals(EdgeDirection.To.toString())) {
					toEdges++;
				}

			}
			if(toEdges>0) {
				count = count + fromEdges;
				context.getCounter(CounterName.Reapeted).increment(fromEdges);
			}
		}
		
		

		@Override
		public void cleanup(final Context context) throws IOException, InterruptedException{

			long triangles = count/3;
			Text triangleText = new Text(triangles+"");
			context.write(new Text("Answer :"),triangleText);
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
		FileInputFormat.addInputPath(job, new Path("input/edges.csv"));
		FileOutputFormat.setOutputPath(job, new Path("output1"));
		if(! job.waitForCompletion(true)) {
			throw new Exception("MR job1 failed");
		}

		Job job2 = Job.getInstance(conf,"RSJoin Complete Triangle");
		job2.setJarByClass(TriangleCount.class);
	
		job2.setReducerClass(CompleteTriangleReducer.class);
		MultipleInputs.addInputPath(job2, new Path("input/edges.csv"), TextInputFormat.class, CompleteTriangleMapper.class);
		MultipleInputs.addInputPath(job2, new Path("output1"), TextInputFormat.class, Path2EdgeMapper.class);
		//+"/part-r-00000"
		job2.setOutputKeyClass(Text.class);
		job2.setOutputValueClass(Text.class);
		FileOutputFormat.setOutputPath(job2, new Path("output1"+"-Triangle"));
		return job2.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(final String[] args) {
		try {
			ToolRunner.run(new TriangleCount(), args);
		} catch (final Exception e) {
			logger.error("", e);
		}

	}

}