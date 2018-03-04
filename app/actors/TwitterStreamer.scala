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


object TwitterStreamer {
  // Helper method that initializes a new Props object
  def props(out: ActorRef) = Props(new TwitterStreamer(out))
}