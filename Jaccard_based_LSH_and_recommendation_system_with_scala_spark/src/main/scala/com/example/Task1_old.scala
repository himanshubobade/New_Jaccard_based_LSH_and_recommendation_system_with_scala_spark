package com.example

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.collection.mutable
import scala.util.Random

object Task1_old {
  def main(args: Array[String]): Unit = {
    val inputPath = args(0)
    val outputPath = args(1)

    val spark = SparkSession.builder()
      .appName("task1")
      .getOrCreate()
    val sc = spark.sparkContext

    val s_t = System.currentTimeMillis()

    val lines = sc.textFile(inputPath)
    val first = lines.first()
    val filteredLines = lines.filter(row => row != first).map(row => row.split(","))
    println("Printing first line of input file",first)

    val busUser = filteredLines.map(row => (row(1), row(0))).groupByKey().mapValues(_.toSet)
    val busUserDict = busUser.collect().toMap

    val users = filteredLines.map(row => row(0)).distinct()
    val usersDict = users.collect().zipWithIndex.toMap

    val n = 60
    val m = usersDict.size
    val p = 1e9 + 7
    val hashFuncs = Array.fill(2)(Array.fill(n)(Random.nextInt(m - 1) + 1))

    val signDict = mutable.Map[String, Array[Int]]()
    for ((bus, userList) <- busUser.collect()) {
      val minHashSignList = Array.tabulate(n)(i =>
        userList.map(user => (hashFuncs(0)(i) * usersDict(user) + hashFuncs(1)(i)) % p % m).min.toInt)
      signDict(bus) = minHashSignList
    }

    val r = 2
    val b = n / r
    val bandsDict = mutable.Map[(Int, Seq[Int]), mutable.ArrayBuffer[String]]()
    for ((bus, minHashSign) <- signDict) {
      for (i <- 0 until b) {
        val idx = (i, minHashSign.slice(i * r, i * r + r).toList)
        bandsDict.getOrElseUpdate(idx, mutable.ArrayBuffer[String]()) += bus
      }
    }

    val bandsDictFi = bandsDict.filter(_._2.size > 1)

    val candidates = mutable.Set[(String, String)]()
    for (values <- bandsDictFi.values) {
      for (pair <- values.toList.combinations(2)) {
        candidates += ((pair(0), pair(1)))
      }
    }

    val result = candidates.filter { case (bus1, bus2) =>
      val user1 = busUserDict(bus1)
      val user2 = busUserDict(bus2)
      val js = user1.intersect(user2).size.toDouble / user1.union(user2).size.toDouble
      js >= 0.5
    }.toSeq.sortBy(_._1)
    println("reached till line number 68")

    val resultStr = "business_id_1, business_id_2, similarity\n" +
      result.map { case (bus1, bus2) =>
        s"$bus1,$bus2,${busUserDict(bus1).intersect(busUserDict(bus2)).size.toDouble / busUserDict(bus1).union(busUserDict(bus2)).size.toDouble}"
      }.mkString("\n")

    println("the output string looks like below")
    println(resultStr)
    //sc.parallelize(Seq(resultStr)).count()
    sc.parallelize(Seq(resultStr))

    val resultRDD = sc.parallelize(Seq(resultStr))

    // Convert RDD to DataFrame
    import spark.implicits._
    val resultDF: DataFrame = resultRDD.toDF("csvData") // Assuming your data is in a column named "csvData"

    println("resultant dataframe is as shown",resultDF.show())
    println(outputPath,"output path")

    // Save DataFrame as CSV using spark-csv library
    resultDF.write
      .option("header", "false") // Change to "true" if your data has a header
      .csv(outputPath)

    println("The file should ideally be written")

    val e_t = System.currentTimeMillis()
    println(s"Duration: ${(e_t - s_t) / 1000.0} seconds")

    spark.stop()
  }
}
