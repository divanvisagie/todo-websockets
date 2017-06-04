package example

import akka.actor.{Actor, ActorRef, Terminated}
import akka.event.Logging
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.semiauto._
import scala.collection.mutable

case class Todo(uid: String, text: String)
case class NewTodo(text: String)
case class DeleteTodo(uid: String)

class CollectionHandler(collectionName: String) extends Actor {
  import CollectionHandler._
  val log = Logging(context.system, this)
  var subscribers: Set[ActorRef] = Set.empty
  var todos: mutable.Map[String,Todo] = mutable.Map[String,Todo]()

  def uuid: String = java.util.UUID.randomUUID.toString

  def updateEveryone(): Unit = {
    val jsonString = todos.values.asJson.toString()
    val count = todos.values.toList.length
    log.info(s"updating all $count subscribers with $jsonString")
    subscribers.foreach { subscriber =>
      subscriber ! Subscriber.OutgoingMessage(jsonString)
    }
  }

  def receive: Receive = {
    case Subscribe(subscriber: ActorRef) =>
      log.info(s"new subscriber to $collectionName")
      subscribers += subscriber
      context.watch(subscriber)
      val jsonString = todos.values.asJson.toString()
      subscriber ! Subscriber.OutgoingMessage(jsonString)

    case Terminated(subscriber) =>
      subscribers -= subscriber

    case AddMessage(data) =>
      decode[NewTodo](data) match {
        case Right(todo) =>
          val uid: String = uuid
          todos += (uid -> Todo(uid,todo.text))
          updateEveryone()
        case Left(error) =>
          log.info(s"Unable to parse new todo: $error")
      }

    case UpdateMessage(data) =>
      decode[Todo](data) match {
        case Right(todo) =>
          todos(todo.uid) = todo
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