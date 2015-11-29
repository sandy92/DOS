import spray.json._
import spray.http._
import spray.routing._
import spray.httpx.SprayJsonSupport._
import MediaTypes._

object MyJsonProtocol extends DefaultJsonProtocol with NullOptions {
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

    // Album formats
    implicit val createAlbumFormat = jsonFormat2(CreateAlbum)
    implicit val albumCreatedFormat = jsonFormat1(AlbumCreated)
    implicit val getAlbumDetailsFormat = jsonFormat1(GetAlbumDetails)
    implicit val deleteAlbumFormat = jsonFormat1(DeleteAlbum)
    implicit val albumDeletedFormat = jsonFormat1(AlbumDeleted)
    implicit val albumDetailsFormat = jsonFormat4(AlbumDetails)
    implicit val getPhotosFromAlbumFormat = jsonFormat1(GetPhotosFromAlbum)

    // Photo formats
    implicit val uploadPhotoFormat = jsonFormat4(UploadPhoto)
    implicit val photoUploadedFormat = jsonFormat1(PhotoUploaded)

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
            case p: UploadPhoto => p.toJson
            case p: PhotoUploaded => p.toJson
            case p: GetLikesOf => p.toJson
            case p: GetLikedBy => p.toJson
            case p: Likes => p.toJson
            case p: GetFriendsList => p.toJson
            case p: FriendsList => p.toJson
            case p: PostCreated => p.toJson
            case p: GetPosts => p.toJson
            case p: Posts => p.toJson
            case p: GetPostDetails => p.toJson
            case p: PostDetails => p.toJson
            case p: DeletePost => p.toJson
            case p: PostDeleted => p.toJson
        }

        def read(value: JsValue) = value  match {
            case _ => deserializationError("Unable to Read Rest Message")
        }
    }

    implicit val likesFormat = jsonFormat2(Likes)

    // Friends list formats
    implicit val getFriendsListFormat = jsonFormat1(GetFriendsList)
    implicit val friendsListFormat = jsonFormat2(FriendsList)

    // Posts formats
    implicit val postCreatedFormat = jsonFormat1(PostCreated)
    implicit val getPostsFormat = jsonFormat1(GetPosts)
    implicit val getPostDetailsFormat = jsonFormat1(GetPostDetails)
    implicit val postDetailsFormat = jsonFormat4(PostDetails)
    implicit val postsFormat = jsonFormat2(Posts)
    implicit val deletePostFormat = jsonFormat2(DeletePost)
    implicit val postDeletedFormat = jsonFormat1(PostDeleted)
}