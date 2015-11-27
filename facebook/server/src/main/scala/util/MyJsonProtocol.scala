import spray.json._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.routing._
import MediaTypes._

object MyJsonProtocol extends DefaultJsonProtocol {
    implicit val timeoutFormat = jsonFormat1(TimeoutMessage)
    implicit val testMessageFormat = jsonFormat1(TestMessage)

    implicit val errorMessageFormat = jsonFormat1(ErrorMessage)

    implicit val createUserFormat = jsonFormat3(CreateUser)
    implicit val userCreatedFormat = jsonFormat1(UserCreated)
    implicit val getUserDetailsFormat = jsonFormat1(GetUserDetails)
    implicit val deleteUserFormat = jsonFormat1(DeleteUser)
    implicit val userDeletedFormat = jsonFormat1(UserDeleted)
    implicit val userDetailsFormat = jsonFormat4(UserDetails)

    implicit object restMessageFormat extends RootJsonFormat[RestMessage] {
        def write(rm: RestMessage) = rm match {
            case t: TimeoutMessage => t.toJson
            case tm: TestMessage => tm.toJson
            case em: ErrorMessage => em.toJson
            case cu: CreateUser => cu.toJson
            case uc: UserCreated => uc.toJson
            case gud: GetUserDetails => gud.toJson
            case du: DeleteUser => du.toJson
            case ud: UserDeleted => ud.toJson
            case ud: UserDetails => ud.toJson
        }

        def read(value: JsValue) = value  match {
            case _ => deserializationError("Unable to Read Rest Message")
        }
        
    }
}