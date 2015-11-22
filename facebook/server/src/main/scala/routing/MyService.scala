import akka.actor._
import spray.routing._
import spray.http._
import spray.httpx._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import MediaTypes._
import MyJsonProtocol._
import fb._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends HttpService with Actor with PerRequestCreator {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  implicit def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)

  val myRoute =
    path("") {
      get {
        respondWithMediaType(`application/json`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            """{ "message":"Hello World!" }"""
          }
        }
      }
    } ~
    path("user") {
      get {
        respondWithMediaType(`application/json`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            """{ "message":"Hello User!" }"""
          }
        }
      }
    } ~
    path("test") {
      get {
        parameters('id) { id =>
          testMessage {
            TestMessage(id)
          }
        }
      }
    } ~
    path("na") {
      get {
        parameters('id) { id =>
          Thread sleep 3000
          complete {
            TestMessage((id.toInt+1).toString)
          }
        }
      }
    }

    def testMessage(message : RestMessage): Route =
      ctx => perRequest(ctx, Props[Test], message)
}