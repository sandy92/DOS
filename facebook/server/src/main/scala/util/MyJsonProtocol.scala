import spray.json._
import spray.http._
import spray.routing._
import spray.httpx.SprayJsonSupport._
import MediaTypes._

object MyJsonProtocol extends DefaultJsonProtocol {
    implicit val timeoutFormat = jsonFormat1(TimeoutMessage)
    implicit val testMessageFormat = jsonFormat1(TestMessage)

    implicit val errorMessageFormat = jsonFormat1(ErrorMessage)

    // User formats
    implicit val createUserFormat = jsonFormat4(CreateUser)
    implicit val userCreatedFormat = jsonFormat2(UserCreated)
    implicit val getUserDetailsFormat = jsonFormat1(GetUserDetails)
    implicit val deleteUserFormat = jsonFormat1(DeleteUser)
    implicit val userDeletedFormat = jsonFormat1(UserDeleted)
    implicit val userDetailsFormat = jsonFormat5(UserDetails)

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
            case p: CreateAlbum => p.toJson
            case p: AlbumCreated => p.toJson
            case p: GetAlbumDetails => p.toJson
            case p: DeleteAlbum => p.toJson
            case p: AlbumDeleted => p.toJson
            case p: AlbumDetails => p.toJson
            case p: GetPhotosFromAlbum => p.toJson
            case p: GetAlbumsList => p.toJson
            case p: Albums => p.toJson
            case p: UploadPhoto => p.toJson
            case p: PhotoUploaded => p.toJson
            case p: GetPhotoDetails => p.toJson
            case p: DeletePhoto => p.toJson
            case p: PhotoDeleted => p.toJson
            case p: PhotoDetails => p.toJson
            case p: GetPhotos => p.toJson
            case p: Photos => p.toJson
            case p: GetLikesOf => p.toJson
            case p: GetLikedBy => p.toJson
            case p: Likes => p.toJson
            case p: GetFriendsList => p.toJson
            case p: FriendsList => p.toJson
            case p: AddFriend => p.toJson
            case p: FriendAdded => p.toJson
            case p: CreatePost => p.toJson
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
    implicit val addFriendFormat = jsonFormat2(AddFriend)
    implicit val FriendAddedFormat = jsonFormat1(FriendAdded)

    // Posts formats
    implicit val createPostFormat = jsonFormat5(CreatePost)
    implicit val postCreatedFormat = jsonFormat1(PostCreated)
    implicit val getPostsFormat = jsonFormat1(GetPosts)
    implicit val getPostDetailsFormat = jsonFormat3(GetPostDetails)
    implicit val postDetailsFormat = jsonFormat6(PostDetails)
    implicit val postsFormat = jsonFormat2(Posts)
    implicit val deletePostFormat = jsonFormat2(DeletePost)
    implicit val postDeletedFormat = jsonFormat1(PostDeleted)

    // Album formats
    implicit val createAlbumFormat = jsonFormat2(CreateAlbum)
    implicit val albumCreatedFormat = jsonFormat1(AlbumCreated)
    implicit val getAlbumDetailsFormat = jsonFormat1(GetAlbumDetails)
    implicit val deleteAlbumFormat = jsonFormat1(DeleteAlbum)
    implicit val albumDeletedFormat = jsonFormat1(AlbumDeleted)
    implicit val albumDetailsFormat = jsonFormat4(AlbumDetails)
    implicit val getPhotosFromAlbumFormat = jsonFormat1(GetPhotosFromAlbum)
    implicit val getAlbumsListFormat = jsonFormat1(GetAlbumsList)
    implicit val albumsFormat = jsonFormat2(Albums)

    // Photo formats
    implicit val uploadPhotoFormat = jsonFormat5(UploadPhoto)
    implicit val photoUploadedFormat = jsonFormat1(PhotoUploaded)
    implicit val getPhotoDetailsFormat = jsonFormat2(GetPhotoDetails)
    implicit val deletePhotoFormat = jsonFormat1(DeletePhoto)
    implicit val photoDeletedFormat = jsonFormat1(PhotoDeleted)
    implicit val photoDetailsFormat = jsonFormat5(PhotoDetails)
    implicit val getPhotosFormat = jsonFormat1(GetPhotos)
    implicit val photosFormat = jsonFormat2(Photos)
}