package com.example
import org.apache.spark.sql.SparkSession

import scala.collection.mutable
import scala.util.Random

object Task1 {
  def main(args: Array[String]): Unit = {
    println("check 1")

    val spark = SparkSession.builder()
      .appName("Task1")
      .getOrCreate()

    import spark.implicits._

    // Load data and preprocess
    val lines = spark.read.textFile("yelp_train.csv").as[String]

    lines.take(10).foreach(println)

    val header = lines.first()
    val data = lines.filter(_ != header).map(_.split(",")).map(arr => (arr(1), arr(0)))

    println(header)
    data.take(10).foreach(println)

    println("check 2")

    // Create a dictionary of businesses and users who have reviewed them
    val busUser = data.rdd.groupByKey().mapValues(_.toSet).collectAsMap().toMap
    println("check 3")
    busUser.take(5).foreach(println)

    // Create a dictionary of users and their unique IDs
    val users = data.rdd.map(_._2).distinct().zipWithIndex().collectAsMap().toMap
    println("check 4")
    users.take(5).foreach(println)

    // Parameters for LSH
    val n = 60 // Number of hash functions
    val m = users.size // Number of bins
    val p = 1000000009

    // Generate hash functions
    val hashFuncs = generateHashFunctions(n, m, p)
    println("check 5")
    println(hashFuncs.mkString(", "))

    // Generate signature matrix
    val signDict = generateSignatureMatrix(busUser, users, hashFuncs, m, p)
    println("check 6")
    signDict.take(5).foreach{ case (key, value) => println(s"$key -> ${value.mkString(", ")}") }

    // Divide the signature matrix into bands
    val r = 2
    val bandsDict = divideSignatureMatrixIntoBands(signDict, r)
    println("check 7")
    bandsDict.take(5).foreach(println)

    // Find candidate pairs
    val candidates = findCandidatePairs(bandsDict)
    println("check 8")
    candidates.take(5).foreach(println)

    // Calculate Jaccard similarity for candidate pairs
    val result = candidates.filter(pair => jaccardSimilarity(busUser(pair._1), busUser(pair._2)) >= 0.5)
      .map(pair => (pair, jaccardSimilarity(busUser(pair._1), busUser(pair._2))))
      .toSeq.sortBy(_._1)
    println("check 9")
    result.foreach(println)

    // Write results to output file
    writeResults("output_path.txt", result)

    println("check 10")

    // Calculate precision and recall
    val (precision, recall) = calculatePrecisionRecall("output_path.txt", "pure_jaccard_similarity.csv")
    println(s"Precision: $precision")
    println(s"Recall: $recall")

    println("check 11")

    spark.stop()

    println("check 12")
  }

  def jaccardSimilarity(set1: Set[String], set2: Set[String]): Double = {
    if (set1.isEmpty || set2.isEmpty) return 0.0
    val intersection = set1.intersect(set2).size
    val union = set1.union(set2).size
    intersection.toDouble / union
  }

  def generateHashFunctions(n: Int, m: Int, p: Int, seed: Option[Int] = None): Array[(Int, Int)] = {
    val rnd = new Random(seed.getOrElse(System.currentTimeMillis().toInt))
    Array.fill(n)((rnd.nextInt(m), rnd.nextInt(m)))
  }

  def generateSignatureMatrix(busUser: Map[String, Set[String]], users: Map[String, Long], hashFuncs: Array[(Int, Int)], m: Int, p: Int): Map[String, Array[Int]] = {
    busUser.mapValues { userSet =>
      hashFuncs.map { case (a, b) =>
        userSet.map(user => ((a * users(user).toInt + b) % p % m).toInt).min
      }.toArray
    }.toMap
  }

//  def divideSignatureMatrixIntoBands(signDict: Map[String, Array[Int]], r: Int): Map[(Int, Seq[Int]), Seq[String]] = {
//    signDict.flatMap { case (bus, minhashSign) =>
//      minhashSign.grouped(r).zipWithIndex.map { case (band, idx) =>
//        ((idx, band.toSeq), bus)
//      }
//    }.groupBy(_._1).mapValues(_.map(_._2).toSeq).toMap
//  }

  def divideSignatureMatrixIntoBands(signDict: Map[String, Array[Int]], r: Int): Map[(Int, Seq[Int]), Seq[String]] = {
    val bandsDict = mutable.Map[(Int, Seq[Int]), mutable.Buffer[String]]()
    for ((bus, minhashSign) <- signDict) {
      for (i <- minhashSign.indices by r) {
        val band = minhashSign.slice(i, i + r).toSeq
        val bandKey = (i / r, band)
        if (!bandsDict.contains(bandKey)) {
          bandsDict(bandKey) = mutable.Buffer[String]()
        }
        bandsDict(bandKey) += bus
      }
    }
    bandsDict.mapValues(_.toSeq).toMap
  }

//  def findCandidatePairs(bandsDict: Map[(Int, Seq[Int]), Seq[String]]): Set[(String, String)] = {
//    bandsDict.values.toSet.flatMap { values: Seq[String] =>
//      if (values.size > 1) {
//        values.combinations(2).collect {
//          case Seq(a, b) => (a, b)
//        }.toSet
//      } else {
//        Set.empty[(String, String)]
//      }
//    }
//  }
def findCandidatePairs(bandsDict: Map[(Int, Seq[Int]), Seq[String]]): Set[(String, String)] = {
  bandsDict.values.flatMap { values =>
    if (values.size > 1) {
      values.combinations(2).map {
        case Seq(a, b) => (a, b)
      }
    } else {
      Seq.empty[(String, String)]
    }
  }.toSet
}

  def writeResults(outputPath: String, result: Seq[((String, String), Double)]): Unit = {
    val writer = new java.io.PrintWriter(outputPath)
    writer.println("business_id_1, business_id_2, similarity")
    result.foreach { case ((bus1, bus2), sim) =>
      writer.println(s"$bus1,$bus2,$sim")
    }
    writer.close()
  }

  def calculatePrecisionRecall(outputPath: String, groundTruthPath: String): (Double, Double) = {
    val outputLines = scala.io.Source.fromFile(outputPath).getLines().drop(1).toSet
    val groundTruthLines = scala.io.Source.fromFile(groundTruthPath).getLines().drop(1).toSet

    val truePositives = outputLines.intersect(groundTruthLines).size
    val falsePositives = outputLines.diff(groundTruthLines).size
    val falseNegatives = groundTruthLines.diff(outputLines).size

    val precision = truePositives.toDouble / (truePositives + falsePositives)
    val recall = truePositives.toDouble / (truePositives + falseNegatives)

    (precision, recall)
  }
}