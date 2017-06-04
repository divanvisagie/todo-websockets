package example

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import example.CollectionHandler.{AddMessage, UpdateMessage}

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

  def receive: Receive = {
    case subscriberMessage: SubscriberMessage =>
      val messageMetadata = parseSubscriberMessageMetadata(subscriberMessage.message)

      messageMetadata.action match {
        case "SUBSCRIBE" =>
          subscribeToCollection(messageMetadata.collection, sender())
        case "UPDATE" =>
          collections(messageMetadata.collection) ! UpdateMessage(messageMetadata.data)
        case "ADD" =>
          collections(messageMetadata.collection) ! AddMessage(messageMetadata.data)
        case _ => log.info(s"Action: ${messageMetadata.action} is not a supported action")
      }

  }
}

object SubscriptionHandler {
  case object Join
  case class MessageMetadata(action: String, collection: String, data: String)

  case class SubscriberMessage(message: String)
  case class ListUpdatedMessage(message: String)
  case class SubscriptionMessage(collection: String)

  def parseSubscriberMessageMetadata(message: String): MessageMetadata = {
    val splitData = message.split("=>")

    val metaSection = splitData(0).trim
    val action = metaSection.split(":")(0).trim
    val collection = metaSection.split(":")(1).trim

    if (splitData.length == 1) {
      MessageMetadata(
        action = action,
        collection = collection,
        data = ""
      )
    } else {
      val data = splitData(1)
      MessageMetadata(
        action = action,
        collection = collection,
        data = data.trim
      )
    }
  }
}