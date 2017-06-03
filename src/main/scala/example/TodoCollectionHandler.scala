package example

import akka.actor.{Actor, ActorRef, Terminated}
import akka.event.Logging
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.semiauto._

import scala.collection.mutable


case class Todo(text: String, uid: String)
case class TodoAction(action: String, text: String)

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

  def handleTodoAction(todoAction: TodoAction): Unit = {
    todoAction.action.toLowerCase match {
      case "add" =>
        val uid = uuid
        val todo = Todo(todoAction.text, uid)
        todos += (uid -> todo)
      case _ => log.warning("Unsupported action")
    }

    subscribers.foreach { subscriber =>
      subscriber ! ListUpdatedMessage(todos.values.asJson.toString())
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
      decode[TodoAction](newTodoMessage.message).map {
        case todoAction: TodoAction =>
          handleTodoAction(todoAction)
        case _ =>
          log.error("Failed to parse json message")
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