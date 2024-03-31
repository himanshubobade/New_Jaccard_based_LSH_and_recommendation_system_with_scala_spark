SET JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202
SET HADOOP_HOME=C:\Program Setups\spark_setup\hadoop

spark-submit --class com.example.Task1 --master local[*] "C:\Program Setups\IntelliJ Projects\Jaccard_based_LSH_and_recommendation_system_with_scala_spark\target\scala-2.12\Assignment1.jar" yelp_train.csv output.csv