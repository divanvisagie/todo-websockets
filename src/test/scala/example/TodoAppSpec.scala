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
    "return action==SUBSCRIBE and collection==todos and data==''" in {
    val messageMetadata: MessageMetadata = SubscriptionHandler.parseSubscriberMessage("SUBSCRIBE:todos")
    messageMetadata.action     shouldEqual "SUBSCRIBE"
    messageMetadata.collection shouldEqual "todos"
    messageMetadata.data       shouldEqual ""
  }

  "parsing 'UPDATE: TODOS => data'" should
    "return action==UPDATE and collection==todos and data==data" in {
    val messageMetadata = SubscriptionHandler.parseSubscriberMessage("UPDATE:todos => data")
    messageMetadata.action     shouldEqual "UPDATE"
    messageMetadata.collection shouldEqual "todos"
    messageMetadata.data       shouldEqual "data"
  }
}