package wc

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.log4j.Level
import org.apache.spark.sql.SparkSession
import java.io._

// Uses RDD
object RddRepJoin {

  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nwc.RddRepJoin <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("RddRepJoin")

    val sc = new SparkContext(conf)

    val maxFilter = 1000
    val textFile = sc.textFile(args(0))

    //First filter the records based on id lesser than maxFilter
    val filteredEdges = textFile.map(line => line.split(","))
      .filter(edge => edge(0).toInt < maxFilter && edge(1).toInt < maxFilter)
      .map(edge => (edge(0), edge(1)))

    val edgeHashMap = filteredEdges.map { case (a, b) => (a, b) }.collectAsMap

    var count = 0;

    for ((k1, v1) <- edgeHashMap) {
      for ((k2, v2) <- edgeHashMap) {
        for ((k3, v3) <- edgeHashMap) {
          if (v1 == k2 && v2 == k3 && v3 == k1) {
            count += 1
          }
        }
      }
    }

    val bw = new BufferedWriter(new FileWriter(new File("answer.txt")))
    bw.write("TriangleCount" + count);
    bw.close()
  }

}
