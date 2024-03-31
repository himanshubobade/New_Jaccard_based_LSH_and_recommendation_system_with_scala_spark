package com.example

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import scala.math.sqrt
import java.io.{File, PrintWriter}
import scala.util.control.Breaks._
object Task2_1 {
  def load_data(sc: SparkContext, file_path: String, include_ratings: Boolean = true): RDD[Array[String]] = {
    val data = sc.textFile(file_path)
    val header = data.first()
    if (include_ratings) {
      data.filter(line => line != header)
        .map(line => line.split(","))
        .filter(tokens => tokens.length == 3)
    } else {
      data.filter(line => line != header)
        .map(line => line.split(","))
    }
  }

  def create_user_business_mappings(train_data: RDD[Array[String]]): (Map[String, Set[String]], Map[String, Set[String]]) = {
    val business_to_users = train_data.map(tokens => (tokens(0), tokens(1))).groupByKey().mapValues(_.toSet).collectAsMap().toMap
    val user_to_businesses = train_data.map(tokens => (tokens(1), tokens(0))).groupByKey().mapValues(_.toSet).collectAsMap().toMap
    (business_to_users, user_to_businesses)
  }

  def calculate_average_ratings(train_data: RDD[Array[String]]): (Map[String, Double], Map[String, Double]) = {
    val business_average_ratings = train_data.map(tokens => (tokens(0), tokens(2).toDouble)).groupByKey().mapValues(ratings => ratings.sum / ratings.size).collectAsMap().toMap
    val user_average_ratings = train_data.map(tokens => (tokens(1), tokens(2).toDouble)).groupByKey().mapValues(ratings => ratings.sum / ratings.size).collectAsMap().toMap
    (business_average_ratings, user_average_ratings)
  }

  def create_business_user_ratings(train_data: RDD[Array[String]]): Map[String, Map[String, String]] = {
    train_data.map(tokens => (tokens(0), (tokens(1), tokens(2)))).groupByKey().mapValues(user_ratings => user_ratings.map(user_rating => (user_rating._1, user_rating._2)).toMap).collectAsMap().toMap
  }

  def calculate_similarity_score(ratings1: Iterable[Double], ratings2: Iterable[Double]): Double = {
    val mean1 = ratings1.sum / ratings1.size
    val mean2 = ratings2.sum / ratings2.size
    val deviations1 = ratings1.map(rating => rating - mean1)
    val deviations2 = ratings2.map(rating => rating - mean2)
    val numerator = (deviations1 zip deviations2).map { case (dev1, dev2) => dev1 * dev2 }.sum
    val denominator = sqrt(deviations1.map(dev => dev * dev).sum) * sqrt(deviations2.map(dev => dev * dev).sum)
    if (denominator == 0) 0 else numerator / denominator
  }

  def calculate_similarity(business: String, user: String, user_to_businesses: Map[String, Set[String]], business_to_users: Map[String, Set[String]], user_average_ratings: Map[String, Double], business_average_ratings: Map[String, Double], business_user_ratings: Map[String, Map[String, String]], similarity_cache: scala.collection.mutable.Map[(String, String), Double]): Double = {
    // Your implementation here
    try{
    val similarity_scores = user_to_businesses(user).toList.flatMap { related_business =>
      val pair = if (business < related_business) (business, related_business) else (related_business, business)
      val similarity = similarity_cache.getOrElseUpdate(pair, {
        val common_users = business_to_users(business) intersect business_to_users(related_business)
        common_users.size match {
          case 0 | 1 => (5.0 - (business_average_ratings(business) - business_average_ratings(related_business)).abs) / 5
          case 2 =>
            val common_users_list = common_users.toList
            val sim1 = (5.0 - (business_user_ratings(business)(common_users_list.head).toDouble - business_user_ratings(related_business)(common_users_list.head).toDouble).abs) / 5
            val sim2 = (5.0 - (business_user_ratings(business)(common_users_list(1)).toDouble - business_user_ratings(related_business)(common_users_list(1)).toDouble).abs) / 5
            (sim1 + sim2) / 2
          case _ =>
            val ratings1 = common_users.map(user => business_user_ratings(business)(user).toDouble).toList
            val ratings2 = common_users.map(user => business_user_ratings(related_business)(user).toDouble).toList
            calculate_similarity_score(ratings1, ratings2)
        }
      })
      if (business_user_ratings(related_business).contains(user)) {
        Some((similarity, business_user_ratings(related_business)(user).toDouble))
      } else {
        None
      }
      }

      val top_similarities = similarity_scores.sortBy(-_._1).take(15)
      val numerator = top_similarities.map { case (similarity, rating) => similarity * rating }.sum
      val denominator = top_similarities.map { case (similarity, _) => similarity.abs }.sum
      if (denominator == 0) 3.5 else numerator / denominator
  } catch {
      case e: Exception =>
        // Code to handle any other type of exception
        println(s"An unexpected error occurred: ${e.getMessage}")
        10
    }


  }

  def main(args: Array[String]): Unit = {
    val start_time = System.nanoTime()

    val conf = new SparkConf().setAppName("Task2_1")
    val sc = new SparkContext(conf)

    println("check1")

    val train_data = load_data(sc, "yelp_train.csv", include_ratings = true).map(tokens => Array(tokens(1), tokens(0), tokens(2)))
    println("check2")
    val validation_data = load_data(sc, "yelp_val.csv", include_ratings = false).map(tokens => Array(tokens(1), tokens(0)))
    println("check3")

    //train_data.take(10).foreach(println)
    train_data.take(10).foreach(arr => arr.foreach(println))
    println(train_data)
    println("check4")
    validation_data.take(10).foreach(arr => arr.foreach(println))
    println(validation_data)
    //validation_data.take(10).foreach(println)
    println("check5")


    val (business_to_users, user_to_businesses) = create_user_business_mappings(train_data)
    println(business_to_users.getClass)
    val business_to_users1 = business_to_users.values.headOption

    // Print the first value if it exists
    business_to_users1.foreach { firstValue =>
      println(s"The first value in the HashMap is: $firstValue")
    }
    println("check6")
    println(user_to_businesses.getClass)
    val user_to_businesses1 = user_to_businesses.values.headOption

    // Print the first value if it exists
    user_to_businesses1.foreach { firstValue =>
      println(s"The first value in the HashMap is: $firstValue")
    }
    println("check7")


    val (business_average_ratings, user_average_ratings) = calculate_average_ratings(train_data)
    println(business_average_ratings.getClass)
    val business_average_ratings1 = business_average_ratings.values.headOption

    // Print the first value if it exists
    business_average_ratings1.foreach { firstValue =>
      println(s"The first value in the HashMap is: $firstValue")
    }
    println("check8")
    println(user_average_ratings.getClass)
    val user_average_ratings1 = user_average_ratings.values.headOption

    // Print the first value if it exists
    user_average_ratings1.foreach { firstValue =>
      println(s"The first value in the HashMap is: $firstValue")
    }
    println("check9")
    val business_user_ratings = create_business_user_ratings(train_data)
    println(business_user_ratings.getClass)
    val business_user_ratings1 = business_user_ratings.values.headOption

    // Print the first value if it exists
    business_user_ratings1.foreach { firstValue =>
      println(s"The first value in the HashMap is: $firstValue")
    }
    println("check10")

    val similarity_cache = scala.collection.mutable.Map[(String, String), Double]()

    var results = "user_id, business_id, prediction\n"
    println("check11")
    var i=0
    breakable {
      validation_data.collect().foreach { tokens =>
        try {
          val predicted_rating = calculate_similarity(tokens(0), tokens(1), user_to_businesses, business_to_users, user_average_ratings, business_average_ratings, business_user_ratings, similarity_cache)
          println("checkpoint predicted rating", i)
          i = i + 1
//          println(predicted_rating)
          results += s"${tokens(1)},${tokens(0)},$predicted_rating\n"
//          println("results", results)
        }    catch {
          case e: Exception =>
            // Code to handle any other type of exception
            println(s"An unexpected error occurred: ${e.getMessage}")
            break
        }
      }
    }
    println("check14")

    val output_file = new PrintWriter(new File("output_2.1.csv"))
    output_file.write(results)
    output_file.close()

    val end_time = System.nanoTime()
    println("Runtime: " + (end_time - start_time) / 1e9d)
  }
}
