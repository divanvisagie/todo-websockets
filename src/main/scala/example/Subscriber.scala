package example

import akka.actor.{Actor, ActorRef, _}
import akka.event.Logging

class Subscriber(room: ActorRef) extends Actor{
  import Subscriber._
  val log = Logging(context.system, this)

  def receive = {
    case Connected(outgoing: ActorRef) =>
      context.become(connected(outgoing))
  }

  def connected(outgoing: ActorRef): Receive = {
    log.debug("New user connected")
    room ! TodoCollectionHandler.Join

    {
      case Subscriber.IncomingMessage(text: String) =>
        println(s"Incoming message $text")
        room ! TodoCollectionHandler.NewTodoMessage(text)

      case TodoCollectionHandler.ListUpdatedMessage(text: String) =>
        println(s"Received chat message $text")
        outgoing ! OutgoingMessage(text)
    }
  }
}

object Subscriber {
  final case class Connected(actorRef: ActorRef)
  case class IncomingMessage(text: String)
  case class OutgoingMessage(text: String)
}


