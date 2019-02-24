package wc

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.log4j.Level
import org.apache.spark.sql.SparkSession
import java.io._


// Uses 
object DsRepJoin {

  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nwc.RddRepJoin <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("DsRepJoin")

    val sc = new SparkContext(conf)

    val maxFilter = 5000
    val textFile = sc.textFile("input/edges.csv")

     val spark: SparkSession =
    SparkSession
        .builder()
        .appName("AppName")
        .config("spark.master", "local")
        .getOrCreate()
    import spark.implicits._


    //First filter the records based on id lesser than maxFilter
    val filteredEdgesTemp = textFile.map(line => line.split(","))
      .filter(edge => edge(0).toInt < maxFilter && edge(1).toInt < maxFilter)
      .map(edge => (edge(0), edge(1)))

    val filteredEdges = spark.createDataset(filteredEdgesTemp)

    val edgeHashMap = sc.broadcast(filteredEdgesTemp.map { case (a, b) => (a, b) }.collectAsMap)
    val path2Map =   filteredEdges.flatMap { case(key, value) =>
                      edgeHashMap.value.get(value).map { otherValue =>
                      (key, otherValue)
                  }
                }
    
    val fullTriangle = path2Map.flatMap {case(key, value) =>
                      edgeHashMap.value.get(value).map { otherValue => if(otherValue == key)
                      (key, otherValue) else ("xxx", "yyy")
                  }
                }
    fullTriangle.filter($"_1".contains("xxx")) 

    
    val count = fullTriangle.count()/3
    
    println("Join for full Triangle:")
    println(fullTriangle.explain)
     println("Answer = "+count)
    
  }

}
