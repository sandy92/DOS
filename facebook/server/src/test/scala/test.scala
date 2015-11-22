import org.scalatest._
import spray.http.StatusCodes._
import spray.testkit._

class TestResponseSpeed extends FlatSpec with MyService with ScalatestRouteTest with Matchers {
  def actorRefFactory = system

  "The response" should "contain hello world" in {
    Get("/") ~> myRoute ~> check {
      responseAs[String] should include("Hello World!")
    }
  }
}