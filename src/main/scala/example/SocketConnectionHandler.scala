package example

import akka.actor.{Actor, ActorRef, Terminated}

class SocketConnectionHandler extends Actor {
  import SocketConnectionHandler._
  var users: Set[ActorRef] = Set.empty

  def receive = {
    case Join =>
      users += sender()
      context.watch(sender())

    case Terminated(user) =>
      users -= user

    case msg: ChatMessage => {
      println("Relaying the chat message to others")
      users.foreach(_ ! msg)
    }
  }
}

object SocketConnectionHandler {
  case object Join
  case class ChatMessage(message: String)
}