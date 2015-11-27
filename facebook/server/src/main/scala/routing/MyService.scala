import akka.actor._
import spray.routing._
import spray.http._
import spray.http.StatusCodes._
import spray.httpx._
import spray.util._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import MediaTypes._
import MyJsonProtocol._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends HttpService with Actor with PerRequestCreator {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  implicit def actorRefFactory = context

  implicit val jsonRejectionHandler = RejectionHandler {
    case ValidationRejection(msg, cause) :: Nil =>
    complete(BadRequest, """{ "error":"""" + msg + """"}""")
  }

  implicit def myExceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: IllegalArgumentException =>
        complete(BadRequest, """{ "error":"""" + e.getMessage + """"}""")
    }

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
    pathPrefix("user") {
      pathEnd {
        get {
          respondWithMediaType(`application/json`) { // XML is marshalled to `text/xml` by default, so we simply override here
            complete {
              """{ "message":"Hello User!" }"""
            }
          }
        }
      } ~
      pathPrefix("create") {
        pathEnd {
          put {
            formFields('name, 'email ,'age.as[Int]) { (name, email, age) =>
              user {
                CreateUser(name, email,age)
              }
            }
          }
        }
      } ~
      pathPrefix("[0-9]+".r) { id =>
        pathEnd {
          get {
            user {
              GetUserDetails(id)
            }
          } ~ 
          delete {
            user {
              DeleteUser(id)
            }
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

    def user(message : RestMessage): Route =
      ctx => perRequest(ctx, Props[User], message)
}