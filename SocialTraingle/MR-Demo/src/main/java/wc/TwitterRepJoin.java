package wc;

import java.util.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


public class TwitterRepJoin extends Configured implements Tool {
	private static final Logger logger = LogManager.getLogger(TwitterRepJoin.class);
	private static int max = 10000;
	public static class ReplicatedJoinMapper extends Mapper<Object, Text, Text, Text> {
			
		private Map<String, List<String>> idFollower = new HashMap<>();

		 enum TriangleCounter{
			 TriangleCounter
		 }
		
		
		@Override
		public void setup(Context context) throws IOException,InterruptedException {
			URI[] uris = context.getCacheFiles();	
						
			BufferedReader read = new BufferedReader(new FileReader(new File("./theFile")));
			String line = read.readLine();
			while(line != null) {
				String[] users = line.split(",");
				if(Integer.parseInt(users[0]) > max || Integer.parseInt(users[1]) > max) {
					line = read.readLine();
					continue;
				}
				
				if(idFollower.containsKey(users[0])) {
					idFollower.get(users[0]).add(users[1]);
				}
				else {
					idFollower.put(users[0], new ArrayList<String>());
					idFollower.get(users[0]).add(users[1]);
				}
				line = read.readLine();
			}
			
			read.close();
		}

		@Override
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			String[] users = line.split(",");
			
			List<String> followers = idFollower.get(users[0]);
			
			if(Integer.parseInt(users[0]) > max || Integer.parseInt(users[1]) > max) {
				return;
			}
			
			for(String follower: followers) {
				if(idFollower.containsKey(follower)) {
					List<String> followers2 = idFollower.get(follower);
					for(String follower2: followers2) {
						if(idFollower.containsKey(follower2)) {
							List<String> followers3 = idFollower.get(follower2);
							if(followers3.contains(users[0])) {
								context.getCounter(CounterName.Reapeted).increment(1);
							}
						}
					}
				}
			}				
		}
	
		@Override
		public void cleanup(final Context context) throws IOException, InterruptedException{

			long triangles = context.getCounter(CounterName.Reapeted).getValue()/3;
			Text triangleText = new Text(triangles+"");
			context.write(new Text("Answer :"),triangleText);
		}

	}

	@Override
	public int run(String[] args) throws Exception {
		final Configuration conf = getConf();
		Job job = Job.getInstance(conf, "TwitterRepJoin");
		job.addCacheFile(new URI("s3://mr-input-2/edges.csv" + "#theFile"));
	//	job.addCacheFile(new Path("s3://mr-input-2/edges4.csv").toUri());
		job.setJarByClass(TwitterRepJoin.class);
		job.setMapperClass(ReplicatedJoinMapper.class);
		job.setNumReduceTasks(0);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		FileInputFormat.addInputPath(job, new Path("s3://mr-input-2/edges.csv"));
		FileOutputFormat.setOutputPath(job, new Path("output11"));
		
		job.waitForCompletion(true);
		
		Counters counter = job.getCounters();
		Counter c1 = counter.findCounter(CounterName.Reapeted);
		System.out.print("counter"+ c1.getDisplayName()+"___"+c1.getValue());
		
		return 0;
	}
	
	public static void main(final String[] args) {
		try {
			ToolRunner.run(new TwitterRepJoin(), args);
		} catch (final Exception e) {
			logger.error("", e);
		}

	}
}