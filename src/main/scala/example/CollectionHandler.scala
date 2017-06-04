package example

import akka.actor.{Actor, ActorRef, Terminated}
import akka.event.Logging

class CollectionHandler(collectionName: String) extends Actor {
  import CollectionHandler._
  val log = Logging(context.system, this)
  var subscribers: Set[ActorRef] = Set.empty

  def uuid: String = java.util.UUID.randomUUID.toString

  def receive: Receive = {
    case Subscribe =>
      subscribers += sender()
      context.watch(sender())

    case Terminated(subscriber) =>
      subscribers -= subscriber

  }
}
object CollectionHandler {
  case object Subscribe
}