package example

import akka.actor.{Actor, ActorRef, Terminated}
import akka.event.Logging
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.semiauto._


case class Todo(text: String)


class TodoCollectionHandler extends Actor {
  import TodoCollectionHandler._
  val log = Logging(context.system, this)

  var subscribers: Set[ActorRef] = Set.empty
  var todos: Set[Todo] = Set.empty

  def sendTodosTo(subscriber: ActorRef): Unit = {
    val todoString = todos.asJson.toString()
    subscriber ! ListUpdatedMessage(todoString)
  }

  def receive: Receive = {
    case Join =>
      subscribers += sender()
      sendTodosTo(sender())
      context.watch(sender())

    case Terminated(user) =>
      subscribers -= user

    case newTodoMessage: NewTodoMessage =>
      decode[Todo](newTodoMessage.message).map {
        t: Todo => todos += t
      }

      subscribers.foreach { subscriber =>
        subscriber ! ListUpdatedMessage(todos.asJson.toString())
      }

    case msg: ListUpdatedMessage => {
      log.debug("Relaying the chat message to others")
      subscribers.foreach(_ ! msg)
    }
  }
}

object TodoCollectionHandler {
  case object Join
  case class NewTodoMessage(message: String)
  case class ListUpdatedMessage(message: String)
}