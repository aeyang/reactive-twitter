package controllers

import play.api._
import play.api.mvc._

import play.api.libs.oauth.{ConsumerKey, RequestToken}
import play.api.Play.current
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  /**
    * Create an Action to render Tweets HTML page
    * @return
    */
  // Use Action.async to return a Future of a result for the next step
  def tweets = Action.async{
    // Retrieve the Twitter credentials from application.conf using a for-yield block
    // The type of credentials is an Option of a Tuple with two elements in the tuple
    val credentials: Option[(ConsumerKey, RequestToken)] = for {
      apiKey      <- Play.configuration.getString("twitter.apiKey")
      apiSecret   <- Play.configuration.getString("twitter.apiSecret")
      token       <- Play.configuration.getString("twitter.token")
      tokenSecret <- Play.configuration.getString("twitter.tokenSecret")
    } yield (
      // Yield a tuple consisting of two elements
      ConsumerKey(apiKey, apiSecret),
      RequestToken(token, tokenSecret)
    )

    credentials.map {
      case (consumerKey, requestToken) => {
        // Return type mandates Future, so return a Ok wrapped in a Future
        Future.successful{
          Ok
        }
      }
    } getOrElse {
      // Return type mandates Future, so return an Error message wrapped in a Future
      Future.successful {
        InternalServerError("Twitter credentials missing")
      }
    }
  }
}
