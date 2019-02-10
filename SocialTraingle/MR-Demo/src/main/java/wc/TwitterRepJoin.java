package wc;

import java.util.*;
import java.io.*;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.jobcontrol.JobControl;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class TwitterRepJoin extends Configured implements Tool {
	private static final Logger logger = LogManager.getLogger(TwitterRepJoin.class);
	private static int max = 1000000000;
	private static Map<String, List<String>> idFollower = new HashMap<>();
	private static Map<String, List<String>> idFollower2 = new HashMap<>();
	
	public static class ReplicatedJoinMapper extends Mapper<Object, Text, Text, Text> {

		enum TriangleCounter {
			TriangleCounter
		}

		@Override
		public void setup(Context context) throws IOException, InterruptedException {
			URI[] uris = context.getCacheFiles();

			BufferedReader read = new BufferedReader(
					new InputStreamReader(new FileInputStream(new File(uris[0].getPath()))));

			String line = read.readLine();
			while (line != null) {
				String[] users = line.split(",");

				if (Integer.parseInt(users[0]) > max) {
					line = read.readLine();
					continue;
				}

				if (idFollower.containsKey(users[0])) {
					idFollower.get(users[0]).add(users[1]);
				} else {
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

			if (Integer.parseInt(users[0]) <= max) {
				List<String> followers = idFollower.get(users[0]);

				for (String follower : followers) {
					if (idFollower.containsKey(follower)) {
						List<String> followers2 = idFollower.get(follower);
						context.write(new Text(users[0]+";"), new Text(Arrays.toString(followers2.toArray())));
					}
				}
			}

		}

	}

	public static class FindPath3 extends Mapper<Object, Text, Text, Text> {

		@Override
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			
			String line = value.toString();
			
			String[] lineSplit = line.split(";");
			String mainUser = lineSplit[0];
			String[] followers = lineSplit[1].substring(2, lineSplit[1].length()-1).split(",");
			
						for (String follower2 : followers) {
							if (idFollower.containsKey(follower2)) {
//								context.write(new Text(mainUser+ " " +follower2+" "+ Arrays.toString(followers3.toArray())),new Text(""));
					
								List<String> followers3 = idFollower.get(follower2);
								
								if (followers3.contains(mainUser)) {
									context.getCounter(CounterName.Reapeted).increment(1);
								}
							}						
						}
					
		

		}

		@Override
		public void cleanup(final Context context) throws IOException, InterruptedException {

			long triangles = context.getCounter(CounterName.Reapeted).getValue()/3;
			Text triangleText = new Text(triangles + "");
			context.write(new Text("Answer :"), triangleText);
		}

	}

	@Override
	public int run(String[] args) throws Exception {
		
//		Job jobOne = Job(jobOneConf, "Job-1");
//	    FileInputFormat.addInputPath(jobOne, jobOneInput);
//	    FileOutputFormat.setOutputPath(jobOne, jobOneOutput);
//	    ControlledJob jobOneControl = new ControlledJob(jobOneConf);
//	    jobOneControl.setJob(jobOne);
//
//	    Job jobTwo = Job(jobTwoConf, "Job-2");
//	    FileInputFormat.addInputPath(jobTwo, jobOneOutput); // here we set the job-1's output as job-2's input
//	    FileOutputFormat.setOutputPath(jobTwo, jobTwoOutput); // final output
//	    ControlledJob jobTwoControl = new ControlledJob(jobTwoConf);
//	    jobTwoControl.setJob(jobTwo);
//
//	    JobControl jobControl = new JobControl("job-control");
//	    jobControl.add(jobOneControl);
//	    jobControl.add(jobTwoControl);
//	    jobTwoControl.addDependingJob(jobOneControl); // this condition makes the job-2 wait until job-1 is done
//
//	    Thread jobControlThread = new Thread(jobControl);
//	    jobControlThread.start();
//	    jobControlThread.join(); 
		
		
		
//		final Configuration conf = getConf();
//		Job job1 = Job.getInstance(conf, "TwitterRepJoin");
//		job1.addCacheFile(
//				new Path("/home/vaibhav/Desktop/lspdpNew/parallelDataProcessing/SocialTraingle/MR-Demo/input/edges4.csv")
//						.toUri());
//		job1.setJarByClass(TwitterRepJoin.class);
//		job1.setMapperClass(ReplicatedJoinMapper.class);
//		job1.setMapperClass(FindPath3.class);
//		job1.setNumReduceTasks(0);
//
//		job1.setOutputKeyClass(Text.class);
//		job1.setOutputValueClass(Text.class);
//
//		FileInputFormat.addInputPath(job, new Path("input/edges4.csv"));
//		FileOutputFormat.setOutputPath(job, new Path("output"));
//
//		job.waitForCompletion(true);
//
//		Counters counter = job.getCounters();
//		Counter c1 = counter.findCounter(CounterName.Reapeted);
//		System.out.print("counter" + c1.getDisplayName() + "___" + c1.getValue());

	
		
		JobControl jobControl = new JobControl("jobChain"); 
	    Configuration conf1 = getConf();

	    Job job1 = Job.getInstance(conf1);  
	    job1.setJarByClass(TwitterRepJoin.class);
	    job1.setJobName("Twitter Rep");

	    job1.addCacheFile(
				new Path("/home/vaibhav/Desktop/lspdpNew/parallelDataProcessing/SocialTraingle/MR-Demo/input/edges1.csv").toUri());
	    FileInputFormat.setInputPaths(job1, "/home/vaibhav/Desktop/lspdpNew/parallelDataProcessing/SocialTraingle/MR-Demo/input/edges1.csv");
	    FileOutputFormat.setOutputPath(job1, new Path("output" + "/temp"));

	    job1.setMapperClass(ReplicatedJoinMapper.class);

	    job1.setOutputKeyClass(Text.class);
	    job1.setOutputValueClass(Text.class);

	    ControlledJob controlledJob1 = new ControlledJob(conf1);
	    controlledJob1.setJob(job1);

	    jobControl.addJob(controlledJob1);
	    Configuration conf2 = getConf();

	    Job job2 = Job.getInstance(conf2);
	    job2.setJarByClass(TwitterRepJoin.class);
	    job2.setJobName("Twitter Rep2");

	    FileInputFormat.setInputPaths(job2, new Path("output" + "/temp"));
	    FileOutputFormat.setOutputPath(job2, new Path("output" + "/final"));

	    job2.setMapperClass(FindPath3.class);
	    
	    job2.setOutputKeyClass(Text.class);
	    job2.setOutputValueClass(Text.class);
	    
	    ControlledJob controlledJob2 = new ControlledJob(conf2);
	    controlledJob2.setJob(job2);

	    // make job2 dependent on job1
	    controlledJob2.addDependingJob(controlledJob1); 
	    // add the job to the job control
	    jobControl.addJob(controlledJob2);
	    Thread jobControlThread = new Thread(jobControl);
	    jobControlThread.start();

	while (!jobControl.allFinished()) {
	    System.out.println("Jobs in waiting state: " + jobControl.getWaitingJobList().size());  
	    System.out.println("Jobs in ready state: " + jobControl.getReadyJobsList().size());
	    System.out.println("Jobs in running state: " + jobControl.getRunningJobList().size());
	    System.out.println("Jobs in success state: " + jobControl.getSuccessfulJobList().size());
	    System.out.println("Jobs in failed state: " + jobControl.getFailedJobList().size());
	try {
	    Thread.sleep(5000);
	    } catch (Exception e) {

	    }

	  } 
	
	   System.exit(0);  
	   return (job1.waitForCompletion(true) ? 0 : 1);   
	  } 
		

	public static void main(final String[] args) {
		try {
			ToolRunner.run(new TwitterRepJoin(), args);
		} catch (final Exception e) {
			logger.error("", e);
		}

	}
}