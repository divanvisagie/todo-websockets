package example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}

object Hello extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val greeterWebSocketService =
    Flow[Message]
      .collect {
        case tm: TextMessage => TextMessage(Source.single("Hello ") ++ tm.textStream)
      }

  val route =
    path ("todo") {
      get {
        handleWebSocketMessages(greeterWebSocketService)
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080);

  println("Hello World")
}

