package mongodb

import org.mongodb.scala._
import com.typesafe.config.ConfigFactory

object MongoDBConnection {

	val mongodbconfig = ConfigFactory.load().getConfig("mongodb")

	val connection_string:String = mongodbconfig.getString("CONNECTION_STRING")
	val database_name:String = mongodbconfig.getString("DATABASE")
	val collection_name:String = mongodbconfig.getString("COLLECTION")
	val collection_name2:String = mongodbconfig.getString("COLLECTION2")
	val collection_name3:String = mongodbconfig.getString("COLLECTION3")

	val mongoclient:MongoClient = MongoClient(connection_string)	
	val database:MongoDatabase = mongoclient.getDatabase(database_name)
	//val collection:MongoCollection[Document] = database.getCollection(collection_name)

	val observer = new Observer[Completed] {
		override def onNext(result: Completed): Unit = Unit//println("Inserted")
		override def onError(e: Throwable): Unit = println("Failed")
		override def onComplete(): Unit = Unit//println("Completed")
	}

	def insert(JSONString:String) {
		val collection:MongoCollection[Document] = database.getCollection(collection_name)
		val observable: Observable[Completed] = collection.insertOne(Document(JSONString))
		
		// Explictly subscribe:
		observable.subscribe(observer)
	}

	def insert_trans(JSONString:String) {
		val collection:MongoCollection[Document] = database.getCollection(collection_name2)
		val observable: Observable[Completed] = collection.insertOne(Document(JSONString))

		// Explictly subscribe:
		observable.subscribe(observer)
	}

	def insert_hastags(JSONString:String) {
		val collection:MongoCollection[Document] = database.getCollection(collection_name3)
		val observable: Observable[Completed] = collection.insertOne(Document(JSONString))

		// Explicitly subscribe
		observable.subscribe(observer)
	}

}