import spray.http._
import spray.client.pipelining._
import scala.util._
import akka.actor._
import spray.httpx._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import scala.concurrent._
import scala.concurrent.duration._
import spray.json._
import spray.routing._
import spray.httpx.SprayJsonSupport._
import MediaTypes._

case object StartActor
case object StopSystem

object Client extends App {
  implicit val system = ActorSystem("api-client")
  import system.dispatcher // execution context for futures
  //import system.log

  //val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  //val response: Future[HttpResponse] = pipeline(Get("http://example.com"))

  /*Future {
    for( i <- 0 to 1000000) {
      system.actorOf(Props[ClientActor],name="c"+i.toString) ! StartActor
    }
  }*/

  system.actorOf(Props[ClientActor],name="c1") ! StartActor

  /*response onComplete {
      case Success(r) =>
        log.info(
          """|Response for GET request to github.com:
             |status : {}
             |headers: {}
             |body   : {}""".stripMargin,
          r.status.value, r.headers.mkString("\n  ", "\n  ", ""), r.entity.asString
        )
        //system.stop(conduit) // the conduit can be stopped when all operations on it have been completed
        //startExample2()
        system.shutdown()

      case Failure(error) =>
        log.error(error, "Couldn't get https://github.com/")
        system.shutdown() // also stops all conduits (since they are actors)
    }*/
}

class ClientActor extends Actor {
  import MyJsonProtocol._
  implicit val system = context.system
  import system.dispatcher
  import system.log

  var userID: String = ""
  var pageID: String = ""

  val pipeline: HttpRequest => Future[HttpResponse] = (
    addHeader("Content-Type", "application/x-www-form-urlencoded")
    ~> sendReceive
    )

  def randomString(length: Int): String = Random.alphanumeric.take(length).mkString

  def receive = {
    case StartActor =>
      /*implicitly[Marshaller[Int]]
      implicitly[Marshaller[CreateUser]]
      implicitly[Marshaller[Future[Int]]]
      implicitly[Marshaller[Future[CreateUser]]]*/
      //pipeline(Put("http://localhost:8080/user/create",FormData(Seq("name" -> Random.nextString(10),"email" -> Random.nextString(10),"age" -> (18 + Random.nextInt(20)))))) onComplete {
      pipeline(Put("http://localhost:8080/user/create",FormData(Map("name" -> Random.nextString(10),"email" -> Random.nextString(10),"age" -> (18 + Random.nextInt(20)).toString)))) onComplete {
        case Success(r) =>
          val x = r.entity.asString.parseJson.convertTo[Map[String,String]]
          val id = x.get("id").getOrElse("")
          if(!id.isEmpty) {
            if(!userID.isEmpty) {
              userID = id
            }
          } else {
            println("Error:" + x.get("error").getOrElse(""))
          }
          system.shutdown
        case Failure(e) =>
          system.shutdown
      }
      println(CreateUser(randomString(10),randomString(10),(18 + Random.nextInt(20))).toString)
      /*var count = 0
      system.scheduler.schedule(0 seconds,1000 + Random.nextInt(500) milliseconds) {
        count = count + 1
        if(count >= 60) {
          system.shutdown
        }
        //println(self.path.toString)
        pipeline(Get("http://localhost:8080")) onComplete {
          case Success(r) =>
            log.info(r.entity.asString + " :: " + self.path.name.toString)
          case Failure(e) =>
        }
      }*/
    case _ =>
  }
}

object Data {
  //import fabricator.*
  //val contact = fabricator.contact()
}
