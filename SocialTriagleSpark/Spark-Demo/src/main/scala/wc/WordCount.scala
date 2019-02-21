package wc

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.log4j.Level

object WordCountMain {

  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
//    if (args.length != 2) {
//      logger.error("Usage:\nwc.WordCountMain <input dir> <output dir>")
//      System.exit(1)
//    }
    val conf = new SparkConf().setAppName("Word Count")
    val sc = new SparkContext(conf)

//    RDD_R(sc, "input/edges.csv", "output")
      RDD_G(sc, "input/edges.csv", "output")
  }

  
  // Already implemented in Assignment 1
  def RDD_R(sc: SparkContext, inputPath: String, outputPath: String) = {
    val textFile = sc.textFile(inputPath)
    val counts = textFile.map(line => line.split(",")(0))
      .map(word => (word, 1))
      .reduceByKey(_ + _)
    
    counts.saveAsTextFile(outputPath)
    println(counts.toDebugString);
  }
  
  
  def RDD_G(sc : SparkContext, inputPath: String, outputPath: String) = {

	val textFile = sc.textFile(inputPath)

    	val counts = textFile.map(line => line.split(",")(0))
			.map(word => (word, 1))
			.groupByKey()
			.mapValues(id => id.sum)			 
                 
    	counts.saveAsTextFile(outputPath)
	
	  println(counts.toDebugString);

}

}