package actors

import akka.actor.{Actor, ActorRef, Props}
import play.api.Logger
import play.api.libs.json.Json

/**
 * This class represents a WebSocket connection with a client
 *
 * An actor is a lightweight object that sends and receives messages asynchronously.
 */
class TwitterStreamer(out: ActorRef) extends Actor {
  // The receive method handles messages sent to this actor.
  // receive is a "partial function"
  def receive = {
    case "subscribe" =>
      Logger.info("Received subscription from a client")
      // ! is an alias for the "tell" method. That means send message and don't wait for
      // delivery confirmation or reply
      out ! Json.obj("text" -> "Hello, world!")
  }
}


// Companion object of the TwitterStreamer actor
object TwitterStreamer {
  // Helper method that initializes a new Props object
  def props(out: ActorRef) = Props(new TwitterStreamer(out))

  private var broadcastEnumerator: Option[Enumerator[JsObject]] = None;

  def connect(): Unit = {
    credentials.map {
      case (consumerKey, requestToken) =>
        // Set up joined iteratee/enumerator pair
        val (iteratee, enumerator) = Concurrent.joined[Array[Byte]]
        // Set up stream transformation pipeline
        val jsonStream: Enumerator[JsObject] = enumerator &>
        Encoding.decode() &>
        Enumeratee.grouped(JsonIteratees.jsSimpleObject)

        // Initialize the broadcast enumerator using the transformed stream as a source
        val (be, __) = Concurrent.broadcast(jsonStream)
        broadcastEnumerator = Some(be_)

        val url = "https://stream.twitter.com/1.1/statuses/filter.json"
        WS
          .url(url)
          .sign(OAuthCalculator(consumerKey, requestToken))
          .withQueryString("track" -> "Washington")
          .get {response =>
            Logger.info("Status: " + response.status)
            iteratee
          }.map { _ =>
            Logger.info("Twitter stream closed")
          }
        } getOrElse {
          Logger.error("Twitter credentials missing")
        }
    }

    // This method first checks if a broadcast-Enumerator exists. If not, we establish a connection.
    // Then, create the twitterClient iteratee which sends each JSON object to the browser using
    //   the actor reference.
    def subscribe(out: ActorRef) : Unit = {
      if (broadcastEnumerator.isEmpty) {
        connect()
      }
      val twitterClient = Iteratee.foreach[JsObject] { t => out ! t }
      broadcastEnumerator.foreach { enumerator =>
        enumerator run twitterClient
      }
    }


    // When a client subscribes, call the subscribe method
    def receive = {
      case "subscribe" =>
        Logger.info("Received subscription from client")
        TwitterStreamer.subscribe(out)
    }
  }