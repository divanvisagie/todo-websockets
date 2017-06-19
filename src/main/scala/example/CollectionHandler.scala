package example

import akka.actor.{Actor, ActorRef, Terminated}
import akka.event.Logging
import com.mongodb.DBObject
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.commons.conversions.scala._
import com.mongodb.casbah.WriteConcern
import com.mongodb.casbah.Imports._
import com.mongodb.CommandResult
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.semiauto._

import org.bson.types.ObjectId

import scala.collection.mutable

class CollectionHandler(collectionName: String) extends Actor {
  type JsonMessage = Map[String,String]

  import CollectionHandler._

  val log = Logging(context.system, this)
  var subscribers: Set[ActorRef] = Set.empty

  val mongoClient = MongoClient("localhost", 27017)

  def uuid: String = java.util.UUID.randomUUID.toString

  def updateEveryone(): Unit = {
    val jsonString = collectionValues.asJson.toString()
    log.info(s"updating all ${collectionValues.length} subscribers with $jsonString")
    subscribers.foreach { subscriber =>
      subscriber ! Subscriber.OutgoingMessage(jsonString)
    }
  }

  private val collection = mongoClient("local")(collectionName)

  def collectionValues: Array[JsonMessage] = {

    val collectionItems = collection.find()
    collectionItems.map { item =>
      val uid = item.get("_id").toString
      val map = mutable.Map[String,String](
        "uid" -> uid
      )
      item.keySet().forEach { key =>
        val value = item.get(key).toString
        if (key != "_id") map += (key -> value)
      }
      map.toMap
    }.toArray
  }

  def receive: Receive = {
    case Subscribe(subscriber: ActorRef) =>
      log.info(s"new subscriber to $collectionName")
      subscribers += subscriber
      context.watch(subscriber)
      val jsonString = collectionValues.asJson.toString()
      subscriber ! Subscriber.OutgoingMessage(jsonString)

    case Terminated(subscriber) =>
      subscribers -= subscriber

    case AddMessage(data) =>
      decode[JsonMessage](data) match {
        case Right(item) =>
          val dbObject = (item ++ Map("uid" -> uuid)).asDBObject
          collection.insert(dbObject)
          updateEveryone()
        case Left(error) =>
          log.info(s"Unable to parse new todo: $error")
      }

    case UpdateMessage(data) =>
      decode[JsonMessage](data) match {
        case Right(item) =>
          val query = MongoDBObject("uid" -> item("uid"))
          val update = item.asDBObject
          collection.update(query, update)
          updateEveryone()
        case Left(error) =>
          log.info(s"Unable to parse todo: $error")
      }

    case DeleteMessage(data) =>
      decode[JsonMessage](data) match {
        case Right(item) =>
          val query = item.asDBObject
          collection.remove(query)
          updateEveryone()
        case Left(error) =>
          log.info(s"Unable to parse error: $error")
      }
  }

}
object CollectionHandler {
  case class Subscribe(subscriber: ActorRef)
  case class UpdateMessage(data: String)
  case class AddMessage(data: String)
  case class DeleteMessage(data: String)
}