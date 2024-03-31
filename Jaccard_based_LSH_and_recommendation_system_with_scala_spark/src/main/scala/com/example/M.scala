package com.example

import org.apache.spark.sql.SparkSession
object M {
  def main(args: Array[String]): Unit = {
    println("Hello world object!")
    if (args.length < 1) {
      println("Usage: SparkSubmitExample <input-file>")
//      sys.exit(1)
    }

    // Initialize SparkSession
    val spark = SparkSession.builder()
      .appName("SparkSubmitExample")
      .getOrCreate()

    println("I read till here")
    // Read input file
    val inputFile = args(0)
    val data = spark.read.textFile(inputFile)
    println(data.show())
  }
}