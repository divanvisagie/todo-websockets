package example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.http.scaladsl.model.StatusCodes


object Hello extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

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
        handleWebSocketMessages(greeterWebSocketService)
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

  println("Hello World")
}

