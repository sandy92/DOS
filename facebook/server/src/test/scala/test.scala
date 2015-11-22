import fb._
import org.scalatest._
import akka.actor._
import akka.testkit._
import spray.http.StatusCodes._
import spray.routing._
import spray.testkit.ScalatestRouteTest
import scala.collection.mutable._
import spray.json._
import MyJsonProtocol._

class TestResponseSpeed extends FlatSpec with ScalatestRouteTest with Matchers {

  def TestResponse = TestActorRef(new MyServiceActor())

  val myRoute = TestResponse.underlyingActor.myRoute
  "The response" should "contain hello world" in {
    Get("/") ~> myRoute ~> check {
      responseAs[String] should include("Hello World!")
      info(responseAs[String])
    }
  }
}
