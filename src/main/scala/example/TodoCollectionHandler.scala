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

class TodoCollectionHandler extends Actor {
  import TodoCollectionHandler._
  val log = Logging(context.system, this)

  var subscribers: Set[ActorRef] = Set.empty
  var todos: mutable.Map[String,Todo] = mutable.Map[String,Todo]()

  def uuid = java.util.UUID.randomUUID.toString

  def sendTodosTo(subscriber: ActorRef): Unit = {
    val todoString = todos.values.asJson.toString()
    subscriber ! ListUpdatedMessage(todoString)
  }

  def updateSubscribers(): Unit = {
    val todosJsonString = todos.values.asJson.toString()
    subscribers.foreach { subscriber =>
      subscriber ! ListUpdatedMessage(todosJsonString)
    }
  }

  def receive: Receive = {
    case Join =>
      subscribers += sender()
      sendTodosTo(sender())
      context.watch(sender())

    case Terminated(user) =>
      subscribers -= user

    case newTodoMessage: SubscriberMessage =>
      val isEdit = decode[Todo](newTodoMessage.message) match {
        case Right(todo) =>
          log.info("editing existing item")
          todos(todo.uid) = todo
          updateSubscribers()
          true
        case Left(error) =>
          log.info(s"could not make into a todo $error")
          false
      }
      if (!isEdit) {
        decode[NewTodo](newTodoMessage.message) match {
          case Right(newTodo) =>
            val uid = uuid
            val todo = Todo(uid, newTodo.text)
            todos += (uid -> todo)
            updateSubscribers()
          case Left(error) =>
            log.info(s"could not make into a newTodo $error")
        }
      }

    case msg: ListUpdatedMessage => {
      log.debug("Relaying the chat message to others")
      subscribers.foreach(_ ! msg)
    }
  }
}

object TodoCollectionHandler {
  case object Join
  case class SubscriberMessage(message: String)
  case class ListUpdatedMessage(message: String)
}