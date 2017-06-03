package example

import akka.actor.{Actor, ActorRef, Terminated}
import akka.event.Logging

class TodoCollectionHandler extends Actor {
  import TodoCollectionHandler._
  val log = Logging(context.system, this)

  var subscribers: Set[ActorRef] = Set.empty
  var todos: Set[String] = Set.empty

  def sendTodosTo(subscriber: ActorRef): Unit = {
    val todoString = todos.mkString(",")
    subscriber ! ListUpdatedMessage(todoString)
  }

  def receive: Receive = {
    case Join =>
      subscribers += sender()
      sendTodosTo(sender())
      context.watch(sender())

    case Terminated(user) =>
      subscribers -= user

    case todo: NewTodoMessage =>
      todos += todo.message
      val todoString = todos.mkString(",")
      subscribers.foreach { subscriber =>
        subscriber ! ListUpdatedMessage(todoString)
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