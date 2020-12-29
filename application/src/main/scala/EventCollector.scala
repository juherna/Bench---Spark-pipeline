package events

import org.apache.spark.sql.streaming.StreamingQueryListener

//import kafka.MessagePublisher
//import com.typesafe.config.ConfigFactory

import graphite.GraphiteClient
import java.net.InetSocketAddress;

class EventCollector extends StreamingQueryListener {

  val inetSocketAddress: InetSocketAddress = new InetSocketAddress("monitoring", 2003)
  val graphiteClient: GraphiteClient = new GraphiteClient(inetSocketAddress)
  graphiteClient.send("scala.graphiteClient.test", "10", System.currentTimeMillis() / 1000)

  //val kafkaconfig = ConfigFactory.load().getConfig("kafka")

  override def onQueryStarted(event: StreamingQueryListener.QueryStartedEvent): Unit = {
    println("Start Event")
    //MessagePublisher.sendMessage(kafkaconfig.getString("TOPIC2"), event.name)
    graphiteClient.send("QueryStartedEvent", "1", System.currentTimeMillis() / 1000)
    if(event.name != null && !event.name.isEmpty())
      graphiteClient.send(event.name, "1", System.currentTimeMillis() / 1000)
  }

  override def onQueryProgress(event: StreamingQueryListener.QueryProgressEvent): Unit = {
    //println("Progress Event")
    val progress = event.progress
    val queryNameTag = s"query_name:${progress.name}"
    //MessagePublisher.sendMessage(kafkaconfig.getString("TOPIC2"), event.progress.prettyJson)
      
    if(event.progress.numInputRows.toString() != null && !event.progress.numInputRows.toString().isEmpty())
      graphiteClient.send("numInputRows", event.progress.numInputRows.toString(), System.currentTimeMillis() / 1000)

    if(event.progress.inputRowsPerSecond.toString() != null && !event.progress.inputRowsPerSecond.toString().isEmpty())
      graphiteClient.send("inputRowsPerSecond", event.progress.inputRowsPerSecond.toString(), System.currentTimeMillis() / 1000)

    if(event.progress.processedRowsPerSecond.toString() != null && !event.progress.processedRowsPerSecond.toString().isEmpty())
      graphiteClient.send("processedRowsPerSecond", event.progress.processedRowsPerSecond.toString(), System.currentTimeMillis() / 1000)
  }

  override def onQueryTerminated(event: StreamingQueryListener.QueryTerminatedEvent): Unit = {
    println("Term Event")
    //MessagePublisher.sendMessage(kafkaconfig.getString("TOPIC2"), event.id.toString())
    if(event.id.toString() != null && !event.id.toString().isEmpty())
    graphiteClient.send(event.id.toString(), "1", System.currentTimeMillis() / 1000)
  }
} 