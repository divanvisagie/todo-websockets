package example

import example.SubscriptionHandler.MessageMetadata
import org.scalatest._

class TodoAppSpec extends FlatSpec with Matchers {
  "The Hello object" should "say hello" in {
    true shouldEqual true
  }
}


class SubscriptionHandlerSpec extends FlatSpec with Matchers {
  "Parsing 'SUBSCRIBE:todos'" should
    "return action==subscribe and collection==todos and data==''" in {
    val messageMetadata: MessageMetadata = SubscriptionHandler.parseSubscriberMessage("SUBSCRIBE:todos")
    messageMetadata.action     shouldEqual "SUBSCRIBE"
    messageMetadata.collection shouldEqual "todos"
    messageMetadata.data       shouldEqual ""
  }
}