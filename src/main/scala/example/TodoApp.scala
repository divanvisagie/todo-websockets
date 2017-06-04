package example

import akka.NotUsed
import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}

object TodoApp extends App {
  import scala.concurrent.ExecutionContext
  import ExecutionContext.Implicits.global

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val subscriptionHandler = system.actorOf(Props(new SubscriptionHandler), "subscriptionHandler")

  def newConnection(): Flow[Message, Message, NotUsed] = {
    val subscriberActor = system.actorOf(Props(new Subscriber(subscriptionHandler)))

    val incomingMessages: Sink[Message, NotUsed] = Flow[Message].map {
      case TextMessage.Strict(text) => Subscriber.IncomingMessage(text)
      case _ =>
        println("Unexpected message type")
        Subscriber.IncomingMessage("Unexpected message")
    }.to(Sink.actorRef[Subscriber.IncomingMessage](subscriberActor, PoisonPill))

    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef[Subscriber.OutgoingMessage](10, OverflowStrategy.fail)
          .mapMaterializedValue { outActor =>
            subscriberActor ! Subscriber.Connected(outActor)
            NotUsed
          }.map { outMsg: Subscriber.OutgoingMessage => TextMessage(outMsg.text)}

    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }

  val staticResources =
    pathPrefix("") {
        pathEndOrSingleSlash {
          getFromDirectory("web_client/build/default/index.html")
        } ~
        getFromDirectory("web_client/build/default")
    }

  val route =
    staticResources ~
    path ("database") {
      get {
        handleWebSocketMessages(newConnection())
      }
    }

  val port = 5000
  val ip = "0.0.0.0"
  val bindingFuture = Http().bindAndHandle(route, ip, port)

  def sendMessageToClient(): Unit = {
    subscriptionHandler ! SubscriptionHandler.ListUpdatedMessage("This is your server speaking")
  }

  println(s"Served at http://$ip:$port")
}

