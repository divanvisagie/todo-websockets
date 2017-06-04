package example

import akka.actor.{Actor, ActorRef, _}
import akka.event.Logging

class Subscriber(subscriptionHandler: ActorRef) extends Actor{
  import Subscriber._
  val log = Logging(context.system, this)

  def receive: Receive = {
    case Connected(outgoing: ActorRef) =>
      log.debug("New subscriber connected")
      subscriptionHandler ! SubscriptionHandler.Join
      context.become(connected(outgoing))
  }

  def connected(outgoing: ActorRef): Receive = {
    case Subscriber.IncomingMessage(text: String) =>
      log.info(s"Received incoming message $text")
      subscriptionHandler ! SubscriptionHandler.SubscriberMessage(text)

    case SubscriptionHandler.ListUpdatedMessage(text: String) =>
      log.debug(s"Received list update message $text")
      outgoing ! OutgoingMessage(text)
  }
}

object Subscriber {
  final case class Connected(actorRef: ActorRef)
  case class IncomingMessage(text: String)
  case class OutgoingMessage(text: String)
}


