trait RestMessage

case class TimeoutMessage(message: String) extends RestMessage
case class TestMessage(randMsg: String) extends RestMessage

// User related messages
case class CreateUser(name: String, email: String, age: Int) extends RestMessage {
    require(!name.isEmpty, "The name should not be empty" )
    require(!email.isEmpty, "The email should not be empty" )
    require(age > 0, "The age should be greater than 0" )
}
case class UserCreated(id: String) extends RestMessage
case class GetUserDetails(userID: String) extends RestMessage
case class DeleteUser(userID: String) extends RestMessage
case class UserDeleted(message: String) extends RestMessage
case class UserDetails(userID: String, name: String, email: String, age: Int) extends RestMessage

// Friends list related messages
case class GetFriendsList(userID: String) extends RestMessage
case class FriendsList(total: String, friends: Set[Option[RestMessage]]) extends RestMessage

// Posts related messages
case class PostCreated(id: String) extends RestMessage
case class GetPosts(profileID: String) extends RestMessage
case class Posts(total: String, posts: Set[Option[RestMessage]]) extends RestMessage
case class GetPostDetails(postID: String) extends RestMessage
case class PostDetails(postID: String, postedBy: Option[RestMessage], postedOn: Option[RestMessage], date: String) extends RestMessage
case class DeletePost(postID: String, profileID: String) extends RestMessage {
    require(!postID.isEmpty, "The post id should not be empty" )
    require(!profileID.isEmpty, "The profile id should not be empty" )
}
case class PostDeleted(message: String) extends RestMessage

// Page related messages
case class CreatePage(name: String, webAddress: String, about: String) extends RestMessage {
    require(!name.isEmpty, "The name should not be empty" )
    require(!webAddress.isEmpty, "The user ID should not be empty" )
    require(!about.isEmpty, "The bio for the page should not be empty" )
}
case class PageCreated(id: String) extends RestMessage
case class GetPageDetails(pageID: String) extends RestMessage
case class DeletePage(pageID: String) extends RestMessage
case class PageDeleted(message: String) extends RestMessage
case class PageDetails(pageID: String, name: String, webAddress: String, about: String) extends RestMessage

// Album related messages
case class CreateAlbum(name: String, profileID: String) extends RestMessage {
    require(!name.isEmpty, "The name should not be empty" )
    require(!profileID.isEmpty, "The profile ID should not be empty" )
}
case class AlbumCreated(id: String) extends RestMessage
case class GetAlbumDetails(pageID: String) extends RestMessage
case class DeleteAlbum(pageID: String) extends RestMessage
case class AlbumDeleted(message: String) extends RestMessage
case class AlbumDetails(pageID: String, name: String, webAddress: String, about: String) extends RestMessage
case class GetPhotosFromAlbum(id: String) extends RestMessage

// Photo related messages
case class UploadPhoto(name: String, profileID: String, image: Array[Byte], albumID: String) extends RestMessage {
    require(!name.isEmpty, "The name should not be empty" )
    require(!profileID.isEmpty, "The profile ID should not be empty" )
    require(!image.isEmpty, "Please upload a valid image" )
    require(!albumID.isEmpty, "The album id should not be empty" )
}
case class PhotoUploaded(id: String) extends RestMessage
/*case class GetAlbumDetails(pageID: String) extends RestMessage
case class DeleteAlbum(pageID: String) extends RestMessage
case class AlbumDeleted(message: String) extends RestMessage
case class AlbumDetails(pageID: String, name: String, webAddress: String, about: String) extends RestMessage
case class GetPhotos(id: String) extends RestMessage*/

// Like
case class GetLikesOf(id: String) extends RestMessage
case class GetLikedBy(id: String) extends RestMessage
case class Likes(total: String, likes: Set[Option[RestMessage]]) extends RestMessage

case class ErrorMessage(error: String) extends RestMessage

trait RedisApi {
  import com.redis._
  val rc = new RedisClient("localhost", 6379)
}