package example

import akka.actor.{Actor, ActorRef, Terminated}

import scala.collection.mutable

class TodoCollectionHandler extends Actor {
  import TodoCollectionHandler._

  var subscribers: Set[ActorRef] = Set.empty
  var todos: Set[String] = Set.empty

  def sendTodosTo(subscriber: ActorRef): Unit = {
    val todoString = todos.mkString(",")
    subscriber ! ChatMessage(todoString)
  }

  def receive = {
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
        subscriber ! ChatMessage(todoString)
      }

    case msg: ChatMessage => {
      println("Relaying the chat message to others")
      subscribers.foreach(_ ! msg)
    }
  }
}

object TodoCollectionHandler {
  case object Join
  case class NewTodoMessage(message: String)
  case class ChatMessage(message: String)
}