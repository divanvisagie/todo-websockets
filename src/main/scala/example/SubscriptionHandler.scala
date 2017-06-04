package example

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.event.Logging
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.semiauto._

import scala.collection.mutable
import scala.collection.script.Update


case class Todo(uid: String, text: String)
case class NewTodo(text: String)

class SubscriptionHandler extends Actor {
  import SubscriptionHandler._
  val log = Logging(context.system, this)

  var collections: mutable.Map[String,ActorRef] = mutable.Map[String,ActorRef]()

  def subscribeToCollection(collection: String): Unit = {
    if (!collections.keys.exists(_ == collection)) {
      val collectionHandler = context.system.actorOf(
        Props(new CollectionHandler(collection))
      )
      collections += (collection -> collectionHandler)
    }
    collections(collection) ! CollectionHandler.Subscribe
  }


  def receive: Receive = {

    case subscriberMessage: SubscriberMessage =>
      val splitMessage = subscriberMessage.message.split("=>")
      val action = splitMessage(0).trim.toUpperCase()
      val message = splitMessage(1)

      action match {
        case "SUBSCRIBE" =>
          decode[SubscriptionMessage](message) match {
            case Right(subscriptionMessage) =>
              log.info(s"should now subscribe to collection ${subscriptionMessage.collection}")
              subscribeToCollection(subscriptionMessage.collection)
            case Left(error) =>
              log.info(s"could not parse subscription message: $error")
          }
        case "UPDATE" =>
          decode[UpdateMessage](message) match {
            case Right(todo) =>
              log.info(s"update todo $todo")
          }
        case "ADD" =>

        case _ => log.info(s"Action: $action is not a supported action")
      }

  }
}

object SubscriptionHandler {
  case object Join
  case class MessageMetadata(action: String, collection: String, data: String)
  case class UpdateMessage(data: String)
  case class SubscriberMessage(message: String)
  case class ListUpdatedMessage(message: String)
  case class SubscriptionMessage(collection: String)

  def parseSubscriberMessage(message: String): MessageMetadata = {
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
        data = data
      )
    }


  }
}