package example

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import example.CollectionHandler.{AddMessage, DeleteMessage, UpdateMessage}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.semiauto._

import scala.collection.mutable

class SubscriptionHandler extends Actor {
  import SubscriptionHandler._
  val log = Logging(context.system, this)

  var collections: mutable.Map[String,ActorRef] = mutable.Map[String,ActorRef]()

  def subscribeToCollection(collection: String, subscriber: ActorRef): Unit = {
    if (!collections.keys.exists(_ == collection)) {
      val collectionHandler = context.system.actorOf(
        Props(new CollectionHandler(collection))
      )
      collections += (collection -> collectionHandler)
    }
    collections(collection) ! CollectionHandler.Subscribe(subscriber)
  }

  def applyAction(messageMetadata: MessageMetadata): Unit ={
    messageMetadata.action match {
      case "SUBSCRIBE" =>
        subscribeToCollection(messageMetadata.collection, sender())
      case "UPDATE" =>
        collections(messageMetadata.collection) ! UpdateMessage(messageMetadata.data, messageMetadata.uuid)
      case "ADD" =>
        collections(messageMetadata.collection) ! AddMessage(messageMetadata.data)
      case "DELETE" =>
        collections(messageMetadata.collection) ! DeleteMessage(messageMetadata.uuid)
      case _ => log.info(s"Action: ${messageMetadata.action} is not a supported action")
    }
  }

  def receive: Receive = {
    case subscriberMessage: SubscriberMessage =>
      decode[MessageMetadata](subscriberMessage.message) match {
        case Right(messageMetadata) =>
          applyAction(messageMetadata)
        case Left(e) =>
          log.info(s"Unable to parse subscriber message: ${subscriberMessage.message}, $e")
      }
  }
}

object SubscriptionHandler {
  case object Join
  case class MessageMetadata(action: String, collection: String, data: String, uuid: String)

  case class SubscriberMessage(message: String)
  case class ListUpdatedMessage(message: String)
  case class SubscriptionMessage(collection: String)
}