package controllers

import play.api._
import play.api.mvc._
import play.api.libs.oauth.{ConsumerKey, RequestToken, OAuthCalculator}
import play.api.Play.current
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws._
import play.api.libs.iteratee._
import play.api.Logger
import play.api.libs.json._
import play.extras.iteratees._

class Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  /**
    * Create an Action to render Tweets HTML page
    * @return Future
    */
  def tweets = Action.async {

    /*
    Define a logging iteratee that consumes a stream asynchronously and logs the
    contents when data is available

    `Iteratee.foreach[E] creates a new iteratee that consumes each input it receives
    by performing a side-effecting action.

    `foreach` is NOT a method of an iteratee. It is a method of the Iteratee library
    used to create a "foreach" iteratee.

    Iteratees lets you act on streams of data ASYNCHRONOUSLY. What does this mean?
    It means that if no data is coming through the stream, the iteratee won't hold onto
    the thread. Only when new data arrives will the iteratee make use of the thread again.
    This is in contrast to how java.io.OutputStream works for example.
    */
    //val loggingIteratee = Iteratee.foreach[Array[Byte]] { array =>
    //  Logger.info(array.map(_.toChar).mkString)
    //}

    // credentials is a function call
    credentials.map {
      case (consumerKey, requestToken) =>
        // Set up a joined iteratee and enumerator
        val (iteratee, enumerator) = Concurrent.joined[Array[Byte]]

        // Define the stream transformation pipeline. Each stage is connected using &> operation
        val jsonStream: Enumerator[JsObject] =
          enumerator &>
          Encoding.decode() &>
          Enumeratee.grouped(JsonIteratees.jsSimpleObject)

        val loggingIteratee = Iteratee.forEach[JsObject] { value =>
          Logger.info(value.toString)
        }

        // Plugs the transformed JSON stream into the logging iteratee to print out its results to the console
        // `run` tells the enumerator to feed data to the iteratee asap
        jsonStream run loggingIteratee

        WS
          .url("https://stream.twitter.com/1.1/statuses/filter.json")
          .sign(OAuthCalculator(consumerKey, requestToken))
          .withQueryString("track" -> "Washington") // Specify a query string
          // Execute HTTP request and retrieves the response as possibly an infinite stream
          .get{ response =>
            Logger.info("Status: " + response.status)
            // Start consuming response
            // iteratee is the entry point of the data streamed through the http request. The stream consumed by the iteratee
            // will be passed on to the enumerator, which itself is the data source of the jsonStream.
            // All of this is nonblocking.
            iteratee
          }
          .map { _ =>
            Ok("Stream closed") // This only fires when the stream is entirely consumed or closed
          }
        } getOrElse {
          // Return type mandates Future, so return an Error message wrapped in a Future
          Future.successful {
            InternalServerError("Twitter credentials missing")
          }
      }
  }

  /**
   * Method to retrieve Twitter API keys from conf
   *
   * Retrieve the Twitter credentials from application.conf using a for-yield block
   * The type of credentials is an Option of a Tuple with two elements in the tuple
   */
  def credentials: Option[(ConsumerKey, RequestToken)] = for {
      apiKey      <- Play.configuration.getString("twitter.apiKey")
      apiSecret   <- Play.configuration.getString("twitter.apiSecret")
      token       <- Play.configuration.getString("twitter.token")
      tokenSecret <- Play.configuration.getString("twitter.tokenSecret")
    } yield (
      // Yield a tuple consisting of two elements
      ConsumerKey(apiKey, apiSecret),
      RequestToken(token, tokenSecret)
    )
}
