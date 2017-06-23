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

  def collectionValues: Array[Json] = {

    val collectionItems = collection.find()
    collectionItems.map { item =>
      val uuid = item.get("uuid").toString

      val uidJsonString = Json.fromString(uuid)
      val fields = Iterable(("uuid",uidJsonString))
      val uidJson = Json.fromFields(fields)

      val data = item.get("data").toString
      val json = parse(data).getOrElse(Json.Null)
      uidJson.deepMerge(json)
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
      collection.insert(Map(
        "uuid" -> uuid,
        "data" -> data
      ).asDBObject)
      updateEveryone()

    case UpdateMessage(data, uuid) =>
      val query = MongoDBObject("uuid" -> uuid)
      collection.update(query, Map(
        "uuid" -> uuid,
        "data" -> data
      ))
      updateEveryone()

    case DeleteMessage(uuid) =>
      val query = MongoDBObject("uuid" -> uuid)
      collection.remove(query)
      updateEveryone()
  }
}

object CollectionHandler {
  case class Subscribe(subscriber: ActorRef)
  case class AddMessage(data: String)
  case class UpdateMessage(data: String, uuid: String)
  case class DeleteMessage(uuid: String)
}