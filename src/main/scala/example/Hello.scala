package example

import akka.NotUsed
import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.http.scaladsl.model.StatusCodes
import example.User.OutgoingMessage
import scala.concurrent.duration._


object Hello extends App {
  import scala.concurrent.ExecutionContext
  import ExecutionContext.Implicits.global

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val socketConnectionHandler = system.actorOf(Props(new SocketConnectionHandler), "todoRoom")



  def newUser(): Flow[Message, Message, NotUsed] = {
    val userActor = system.actorOf(Props(new User(socketConnectionHandler)))

    val incomingMessages: Sink[Message, NotUsed] = Flow[Message].map {
      case TextMessage.Strict(text) => User.IncomingMessage(text)
      case _ =>
        println("Unexpected message type")
        User.IncomingMessage("Unexpected message")
    }.to(Sink.actorRef[User.IncomingMessage](userActor, PoisonPill))

    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef[User.OutgoingMessage](10, OverflowStrategy.fail)
          .mapMaterializedValue { outActor =>
            userActor ! User.Connected(outActor)
            NotUsed
          }.map { outMsg: User.OutgoingMessage => TextMessage(outMsg.text)}

    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }

  val greeterWebSocketService =
    Flow[Message]
      .collect {
        case tm: TextMessage => TextMessage(Source.single("Hello ") ++ tm.textStream)
      }

  val staticResources =
    pathPrefix("") {
        pathSingleSlash {
          getFromDirectory("web_client/build/default/index.html")
        } ~
        getFromDirectory("web_client/build/default")
    }
  val route =
    staticResources ~
    path("ping") {
      get {
        complete("pong")
      }
    } ~
    path ("todo") {
      get {
        handleWebSocketMessages(newUser())
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 5000)

  def sendMessageToClient(): Unit = {
    socketConnectionHandler ! SocketConnectionHandler.ChatMessage("This is your server speaking")
  }
//  system.scheduler.schedule(0 seconds, 1 seconds)(sendMessageToClient())

  println("Hello World")

}

