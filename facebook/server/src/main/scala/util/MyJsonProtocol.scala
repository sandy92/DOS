import spray.json._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.routing._
import MediaTypes._

object MyJsonProtocol extends DefaultJsonProtocol {
    implicit val timeoutFormat = jsonFormat1(TimeoutMessage)
    implicit val testMessageFormat = jsonFormat1(TestMessage)

    implicit val errorMessageFormat = jsonFormat1(ErrorMessage)

    // User formats
    implicit val createUserFormat = jsonFormat3(CreateUser)
    implicit val userCreatedFormat = jsonFormat1(UserCreated)
    implicit val getUserDetailsFormat = jsonFormat1(GetUserDetails)
    implicit val deleteUserFormat = jsonFormat1(DeleteUser)
    implicit val userDeletedFormat = jsonFormat1(UserDeleted)
    implicit val userDetailsFormat = jsonFormat4(UserDetails)

    // Page formats
    implicit val createPageFormat = jsonFormat3(CreatePage)
    implicit val pageCreatedFormat = jsonFormat1(PageCreated)
    implicit val getPageDetailsFormat = jsonFormat1(GetPageDetails)
    implicit val deletePageFormat = jsonFormat1(DeletePage)
    implicit val pageDeletedFormat = jsonFormat1(PageDeleted)
    implicit val pageDetailsFormat = jsonFormat4(PageDetails)

    // Likes
    implicit val getLikesOfFormat = jsonFormat1(GetLikesOf)
    implicit val getLikedByFormat = jsonFormat1(GetLikedBy)
    // implicit val likesFormat: JsonFormat[Likes] = lazyFormat(jsonFormat(Likes, "total", "likes"))

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
            case p: CreatePage => p.toJson
            case p: PageCreated => p.toJson
            case p: GetPageDetails => p.toJson
            case p: DeletePage => p.toJson
            case p: PageDeleted => p.toJson
            case p: PageDetails => p.toJson
            case p: GetLikesOf => p.toJson
            case p: GetLikedBy => p.toJson
            case p: Likes => p.toJson
        }

        def read(value: JsValue) = value  match {
            case _ => deserializationError("Unable to Read Rest Message")
        }
    }

    implicit val likesFormat = jsonFormat2(Likes)
}