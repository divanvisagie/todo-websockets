package example

import akka.actor.{Actor, ActorRef, Terminated}
import example.Subscriber.{IncomingMessage, OutgoingMessage}
import akka.NotUsed
import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.ws.Message
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.io.StdIn

class Subscriber(room: ActorRef) extends Actor{
  import Subscriber._

  def receive = {
    case Connected(outgoing: ActorRef) =>
      context.become(connected(outgoing))
    case _ => {
      println(Subscriber.toString +" Unhandled Message")
    }
  }

  def connected(outgoing: ActorRef): Receive = {
    println("New user connected")
    room ! TodoCollectionHandler.Join

    {
      case Subscriber.IncomingMessage(text: String) =>
        println(s"Incoming message $text")
        room ! TodoCollectionHandler.NewTodoMessage(text)

      case TodoCollectionHandler.ChatMessage(text: String) =>
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


