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

case class Todo(uid: String, text: String)
case class NewTodo(text: String)
case class DeleteTodo(uid: String)



class CollectionHandler(collectionName: String) extends Actor {
  import CollectionHandler._

  implicit val encodeIntOrString: Encoder[Either[Int, String]] =
    Encoder.instance(_.fold(_.asJson, _.asJson))

  implicit val decodeIntOrString: Decoder[Either[Int, String]] =
    Decoder[Int].map(Left(_)).or(Decoder[String].map(Right(_)))

  val log = Logging(context.system, this)
  var subscribers: Set[ActorRef] = Set.empty
  var todos: mutable.Map[String,Todo] = mutable.Map[String,Todo]()

  val mongoClient = MongoClient("localhost", 27017)

  def uuid: String = java.util.UUID.randomUUID.toString

  def updateEveryone(): Unit = {
    val jsonString = collectionValues.asJson.toString()
    val count = todos.values.toList.length
    log.info(s"updating all $count subscribers with $jsonString")
    subscribers.foreach { subscriber =>
      subscriber ! Subscriber.OutgoingMessage(jsonString)
    }
  }

  val collection = mongoClient("local")(collectionName)

  def collectionValues: Array[Map[String,String]] = {

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
      decode[NewTodo](data) match {
        case Right(todo) =>
          collection.insert(MongoDBObject(
            "text" -> todo.text,
            "uid" -> uuid
          ))
          updateEveryone()
        case Left(error) =>
          log.info(s"Unable to parse new todo: $error")
      }

    case UpdateMessage(data) =>
      decode[Todo](data) match {
        case Right(todo) =>
          val query = MongoDBObject("uid" -> todo.uid)
          val update = $set("text" -> todo.text)
          collection.update(query, update)
          updateEveryone()
        case Left(error) =>
          log.info(s"Unable to parse todo: $error")
      }

    case DeleteMessage(data) =>
      decode[DeleteTodo](data) match {
        case Right(todo) =>
          todos.remove(todo.uid)
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