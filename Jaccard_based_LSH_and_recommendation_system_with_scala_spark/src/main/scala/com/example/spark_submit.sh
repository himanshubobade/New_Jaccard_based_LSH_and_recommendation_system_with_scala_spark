#!/bin/bash

java -cp "C:/Program Setups/spark setup/spark/spark-3.1.2-bin-hadoop3.2/conf/;C:/Program Setups/spark setup/spark/spark-3.1.2-bin-hadoop3.2/jars/*" -Xmx1g org.apache.spark.deploy.SparkSubmit --master local[*] --class com.example.M "C:/Users/HIMANSHUU/IdeaProjects/spark_sbt_setup/target/scala-2.12/Assignment1.jar" "C:/Program Setups/IntelliJ Projects/sample_input_file.txt"

##spark-submit --class com.example.M --master local[*] "D:/IntelliJ projects/Assignment1/target/scala-2.12/Assignment1.jar" C:\Users\HIMANSHUU\Desktop\LinkedInData1.txt

#spark-submit 2
##spark-submit --class com.example.M --master local[*] C:\Users\HIMANSHUU\IdeaProjects\spark_sbt_setup\target\scala-2.12\Assignment1.jar sample_input_file.txt