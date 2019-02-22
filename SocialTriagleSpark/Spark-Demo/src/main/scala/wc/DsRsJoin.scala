package wc

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.log4j.Level
import org.apache.spark.sql.SparkSession
import java.io._


object DsRsJoin {
  
  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\ntwitterAnalysis.DatasetRSJoin <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("DsRsJoin")
	
    val sc = new SparkContext(conf)    
   
    val spark = SparkSession
  			.builder()
  			.appName("DsRsJoin")
  			.getOrCreate()
	
  import spark.implicits._
	val maxFilter = 10000
	val edgeDatasetOnce = spark.read.csv(args(0))
	
	val filtered = edgeDatasetOnce.filter($"_c0" < maxFilter && $"_c1" < maxFilter)

	val left =  filtered.toDF("a","b")
	val right = filtered.toDF("c","d")
	val thirdEdge = filtered.toDF("p","q")
	val path2 = left.join(right, $"b" === $"c").drop("b").drop("c")

	
	println("Start")
	println(path2.explain)
	println("End")
	
	val fullTriangle = path2.join(thirdEdge,$"d" === $"p" &&  $"a" === $"q")
	val triangleCount = fullTriangle.count()/3
	println("Triangle count:")	
	println(triangleCount)
	// Printing the lineage graph and outputs
	println("Join for full Triangle:")	
	println(fullTriangle.explain)		

	val bw =  new BufferedWriter(new FileWriter(new File("answer.txt"))) 
	bw.write("TriangleCount" + triangleCount);
	bw.close()
	              
  fullTriangle.coalesce(1).write.csv(args(1))

    
  
  }



}