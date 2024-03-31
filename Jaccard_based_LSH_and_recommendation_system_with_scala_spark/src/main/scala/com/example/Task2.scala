package com.example

import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable
import scala.util.control.Breaks._

object Task2 {
  def itemBased(user: String, bus: String, userBusDict: Map[String, Set[String]],
                busUserDict: Map[String, Set[String]], userAvgDict: Map[String, Double],
                busAvgDict: Map[String, Double], busUserRDict: Map[String, Map[String, Double]],
                wDict: mutable.Map[(String, String), Double]): Double = {
    println("itemBased check 1")
    if (!userBusDict.contains(user)) {
      return 3.5
    }
    if (!busUserDict.contains(bus)) {
      return userAvgDict(user)
    }

    println("itemBased check 2")

    val wList = mutable.ListBuffer[(Double, Double)]()

    println("itemBased check 3")
    var x = 0
    for (bus1 <- userBusDict(user)) {
      x = x+1
      println(x)
      //println("itemBased check 4")
      val temp = if (bus1 < bus) (bus1, bus) else (bus, bus1)
      val w = wDict.getOrElseUpdate(temp, {
        val userInter = busUserDict(bus) & busUserDict(bus1)
        if (userInter.size <= 1) {
          (5.0 - math.abs(busAvgDict(bus) - busAvgDict(bus1))) / 5
        } else if (userInter.size == 2) {
          val userInterList = userInter.toList
          val w1 = (5.0 - math.abs(busUserRDict(bus)(userInterList(0)) - busUserRDict(bus1)(userInterList(0)))) / 5
          val w2 = (5.0 - math.abs(busUserRDict(bus)(userInterList(1)) - busUserRDict(bus1)(userInterList(1)))) / 5
          (w1 + w2) / 2
        } else {
          val (r1, r2) = userInter.map(user => (busUserRDict(bus)(user), busUserRDict(bus1)(user))).unzip
          val avg1 = r1.sum / r1.size
          val avg2 = r2.sum / r2.size
          val temp1 = r1.map(_ - avg1)
          val temp2 = r2.map(_ - avg2)
          val X = temp1.zip(temp2).map { case (x, y) => x * y }.sum
          val Y = math.sqrt(temp1.map(x => x * x).sum) * math.sqrt(temp2.map(x => x * x).sum)
          if (Y == 0) 0 else X / Y
        }
      })
      //println("itemBased check 5")
      wList += ((w, busUserRDict(bus1)(user)))
    }

    println("itemBased check 6")

    val wListCan = wList.sortBy(-_._1).take(15)
    var X = 0.0
    var Y = 0.0
    for ((w, r) <- wListCan) {
      X += w * r
      Y += math.abs(w)
    }
    println("itemBased check 7")
    if (Y == 0) 3.5 else X / Y
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 3) {
      println("Usage: ItemBasedRecommendation <trainPath> <valPath> <outputPath>")
      sys.exit(1)
    }

    val trainPath = args(0)
    val valPath = args(1)
    val outputPath = args(2)

    val conf = new SparkConf().setAppName("task2_1").setMaster("local[*]")
    val sc = new SparkContext(conf)

    val linesTrain = sc.textFile(trainPath).filter(_ != "").map(_.split(",")).map(row => (row(1), row(0), row(2)))
    println(linesTrain,"linestrain")

    val busUserTrain = linesTrain.map { case (bus, user, _) => (bus, user) }.groupByKey().mapValues(_.toSet).collect().toMap
    println("check 1")
    val userBusTrain = linesTrain.map { case (bus, user, _) => (user, bus) }.groupByKey().mapValues(_.toSet).collect().toMap
    println("check 2")
    //val busAvgDict = linesTrain.map { case (bus, _, rating) => (bus, rating.toDouble) }.groupByKey().mapValues(ratings => ratings.sum / ratings.size).collect().toMap
    val busAvgDict = linesTrain.map { case (bus, _, rating) =>
      if (rating == "stars") (bus, 3.5) // Handle "stars" rating
      else (bus, rating.toDouble)
    }.groupByKey().mapValues(ratings => ratings.sum / ratings.size).collect().toMap

    println("check 3")

    val userAvgDict = linesTrain.map { case (_, user, rating) =>
      if (rating == "stars") (user, 3.5) // Handle "stars" rating
      else (user, rating.toDouble)
    }.groupByKey().mapValues(ratings => ratings.sum / ratings.size).collect().toMap

    println("check 4")
    val busUserRDict = linesTrain.map { case (bus, user, rating) =>
      if (rating == "stars") (bus, (user, 3.5)) // Handle "stars" rating
      else (bus, (user, rating.toDouble))
    }.groupByKey().mapValues(_.toMap).collect().toMap

    println("check 5")
    val linesVal = sc.textFile(valPath).filter(_ != "").map(_.split(",")).map(row => (row(1), row(0)))

    println("check 6")
    val wDict = mutable.Map.empty[(String, String), Double]

    println(wDict,"wDict")

    var resultStr = "user_id, business_id, prediction\n"
    linesVal.collect().foreach { case (bus, user) =>
      val prediction = itemBased(user, bus, userBusTrain, busUserTrain, userAvgDict, busAvgDict, busUserRDict, wDict)
      resultStr += s"$bus,$user,$prediction\n"
    }

    println(resultStr,"resultStr")

    sc.parallelize(Seq(resultStr)).saveAsTextFile(outputPath)

    println("Pray it wrote it")

    sc.stop()
  }
}
