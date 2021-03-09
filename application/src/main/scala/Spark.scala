package spark

import mongodb.MongoDBConnection

import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel
import org.apache.spark._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.Row
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.ForeachWriter


import com.typesafe.config.ConfigFactory

object SparkStructuredStreamer {

	val mongodbconfig = ConfigFactory.load().getConfig("mongodb")
	val sparkconfig = ConfigFactory.load().getConfig("spark")
	
	val spark = SparkSession.builder
							.master(sparkconfig.getString("MASTER_URL"))
							.config("spark.sql.streaming.metricsEnabled","true")
							.config("spark.metrics.appStatusSource.enabled","true")
							.appName("TweetStream")
							.getOrCreate()

	spark.sparkContext.setLogLevel("WARN")

	import events.EventCollector
	val listener = new EventCollector()
	spark.streams.addListener(listener)
	
	
	import spark.implicits._

	val kafkaconfig = ConfigFactory.load().getConfig("kafka")

	// This is the Spark Structured Streaming + Kafka integration
	// Do not have to explicitly use the Consumer API to consume from kafka
	val ds = spark.readStream
				  .format("kafka")
				  .option("kafka.bootstrap.servers", kafkaconfig.getString("BOOTSTRAP_SERVERS"))
				  .option("subscribe", kafkaconfig.getString("TOPIC"))
				  .load()

	/*
	This is the schema of a consuming a ProducerRecord from kafka. Value is the actual payload and
	the rest of the fields are metadata

	root
	 |-- key: binary (nullable = true)
	 |-- value: binary (nullable = true)
	 |-- topic: string (nullable = true)
	 |-- partition: integer (nullable = true)
	 |-- offset: long (nullable = true)
	 |-- timestamp: timestamp (nullable = true)
	 |-- timestampType: integer (nullable = true)

	 At this point, our key and values are in UTF8/binary, which was serialized this way by the
	 KafkaProducer for transmission of data through the kafka brokers.

	 from https://spark.apache.org/docs/2.4.0/structured-streaming-kafka-integration.html
	 "Keys/Values are always deserialized as byte arrays with ByteArrayDeserializer. 
	 Use DataFrame operations to explicitly deserialize the keys/values"
	*/

	// Transforms and preprocessing can be done here
	val selectds = ds.selectExpr("CAST(value AS STRING)") // deserialize binary back to String type

	// // Forma 1 de hacerlo
	// // Es necesario importar lo siguiente
	// import org.apache.spark.sql.functions.from_json
	// import org.apache.spark.sql.types.{StructType, StructField, StringType, ArrayType, LongType}
	// 
	// val tweet_schema = StructType(List(
	// 	StructField(
	// 		"entities", StructType(List(
	// 			StructField(
	// 				"hashtags", ArrayType(
	// 					StructType(List(
	// 						StructField(
	// 							"indices", ArrayType(LongType,true),true
	// 						),
	// 						StructField(
	// 							"text", StringType,true
	// 						)
	// 					)),
	// 					true
	// 				),
	// 				true
	// 			)
	// 		)),
	// 		true
	// 	)
	// ))
	// val ds_json_readed_0 = selectds.withColumn("tweet_simple", from_json($"value",tweet_schema))

	//forma 2 de hacerlo
	val str_tweet = """STRUCT<
    ------------------------- HASHTAGS -------------------------
    entities:STRUCT<
        hashtags: ARRAY<STRUCT<
            indices: ARRAY<LONG>,
            text: STRING
        >>
    >,
    --------------------- TWEET HOUR ---------------------
    created_at:STRING,
    ---------------------- USER DATA ------------------------
    user:STRUCT<
        description:STRING,
        name:STRING,
        screen_name:STRING,
        location:STRING,
        followers_count:LONG>
>"""

	val df1 = selectds.selectExpr("value", s"from_json( value, '$str_tweet' ) as tweet_simple")

	// Extract data processed
	spark.sql("set spark.sql.legacy.timeParserPolicy=LEGACY");
	import org.apache.spark.sql.functions.to_json
	val data_processed = df1.select($"value", to_json($"tweet_simple.entities.hashtags.text"), from_unixtime(
    unix_timestamp($"tweet_simple.created_at","EEE MMM dd HH:mm:ss ZZZZ yyyy")).alias("tweet_date"), 
    lower($"tweet_simple.user.description").alias("user_description"),
    lower($"tweet_simple.user.name").alias("user_name"),
    //lower($"tweet_simple.user.screen_name").alias("user_username"),
    lower($"tweet_simple.user.location").alias("user_location"),
    $"tweet_simple.user.followers_count".alias("user_followers"))


	// // We must create a custom sink for MongoDB
	// // ForeachWriter is the contract for a foreach writer that is a streaming format that controls streaming writes.
	val customwriter = new ForeachWriter[Row] {
		def open(partitionId: Long, version: Long): Boolean = {
	    	true
	    }
	    def process(record: Row): Unit = {
		    // Write string to connection
		    MongoDBConnection.insert(s"{value: ${record(0)}}")
			MongoDBConnection.insert_trans("{\"size\":" + record(0).toString().size.toString() + "}")
			MongoDBConnection.insert_hastags(s"""{"hashtags": ${record(1)}}""")
			MongoDBConnection.insert_tweet_date(s"""{"tweet_date": "${record(2)}"}""")
			MongoDBConnection.insert_user_description(s"""{"user_description": "${record(3)}"}""")
			MongoDBConnection.insert_user_name(s"""{"user_name": "${record(4)}"}""")
			MongoDBConnection.insert_user_location(s"""{"user_location": "${record(5)}"}""")
			MongoDBConnection.insert_user_followers(s"""{"followers": "${record(6)}"}""")
	    }
	    def close(errorOrNull: Throwable): Unit = {
	    	Unit
    	}
  	}

	val writedf = data_processed.writeStream
						  .foreach(customwriter)
						  .start()
	writedf.awaitTermination()

	// spark.stop()

}