import spray.json._
import fb._

object MyJsonProtocol extends DefaultJsonProtocol {
    implicit val timeoutFormat = jsonFormat1(Timeout)
    implicit val testMessageFormat = jsonFormat1(TestMessage)
    implicit object restMessageFormat extends RootJsonFormat[RestMessage] {
        def write(rm: RestMessage) = rm match {
            case t: Timeout => t.toJson
            case tm: TestMessage => tm.toJson
        }

        def read(value: JsValue) = value  match {
            case _ => deserializationError("Unable to Read Rest Message")
        }
        
    }
}