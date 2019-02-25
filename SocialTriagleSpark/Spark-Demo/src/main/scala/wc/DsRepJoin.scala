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

    val maxFilter = 500
    val textFile = sc.textFile("s3://mr-input/edges.csv")

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


    // Creating Dataset
    val filteredEdges = spark.createDataset(filteredEdgesTemp)

    val edgeHashMap = sc.broadcast(filteredEdges.map { case (a, b) => (a, b) })
    
    // Initial Setuo
    val from = filteredEdgesTemp.map { case (a, b) => (a +"-"+ b) }.collect().toList
    val to = filteredEdgesTemp.map { case (a, b) => (a +"-"+ b) }.collect().toList
    var path2 = List[String]();

    
    // Creating Path2 edge
     from.foreach(t => 
      to.foreach(r =>
      if(t.split("-")(1) == r.split("-")(0))
        path2 = path2 :+ (t.split("-")(0)+"-"+r.split("-")(1))
      )
     ) 


    // Using the Dataset and flatMap and path2 to find full traingle 
       var fullTriangle = filteredEdges.flatMap {case(key, value) =>
                            for (r <- path2) 
                            yield {
                            if(value == r.split("-")(0) && key == r.split("-")(1)){
                                  (key)
                            }
                             else ("xx")    
                                }
                            }
                        

    // Filtering out the unneccessary rows in DataSet
    fullTriangle = fullTriangle.filter(x => !x.contains("xx"))
 
    val count = fullTriangle.count()/3
    
    println("Join for full Triangle:")
    println(fullTriangle.explain)
     println("Answer = "+count)
    
  }

}
