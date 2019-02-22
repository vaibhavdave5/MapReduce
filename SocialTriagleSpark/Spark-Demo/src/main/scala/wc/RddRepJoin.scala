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

    

    val edgeHashMap = sc.broadcast(filteredEdges.map { case (a, b) => (a, b) }.collectAsMap)
    
    
    
    val path2Map =   filteredEdges.flatMap { case(key, value) =>
                      edgeHashMap.value.get(value).map { otherValue =>
                      (key, otherValue)
                  }
                }
    
    val fullTriangle = path2Map.flatMap {case(key, value) =>
                      edgeHashMap.value.get(value).map { otherValue => if(otherValue == key)
                      (key, otherValue) else None
                  }
                }
    
    val count = fullTriangle.count()/3
    
    println("Join for full Triangle:")
    println(fullTriangle.toDebugString)
     println("Answer = "+count)
    
  }

}
