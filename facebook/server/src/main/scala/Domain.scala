trait RestMessage

case class TimeoutMessage(message: String) extends RestMessage
case class TestMessage(randMsg: String) extends RestMessage

// User related messages
case class CreateUser(name: String, email: String, age: Int, publicKey: String) extends RestMessage {
    require(!name.isEmpty, "The name should not be empty" )
    require(!email.isEmpty, "The email should not be empty" )
    require(age > 0, "The age should be greater than 0" )
    require(!publicKey.isEmpty, "The public key should not be empty" )
}
case class UserCreated(id: String, randNumber: String) extends RestMessage
case class GetUserDetails(userID: String) extends RestMessage
case class DeleteUser(userID: String) extends RestMessage
case class UserDeleted(message: String) extends RestMessage
case class UserDetails(userID: String, name: String, email: String, age: Int, publicKey: String) extends RestMessage

// Friends list related messages
case class GetFriendsList(userID: String) extends RestMessage
case class FriendsList(total: String, friends: Set[Option[RestMessage]]) extends RestMessage
case class AddFriend(id: String, requestedBy: String) extends RestMessage
case class FriendAdded(message: String) extends RestMessage

// Posts related messages
case class CreatePost(message: String, postedBy: String, postedOn: String, accessList: String, signature: String) extends RestMessage {
    require(!message.isEmpty, "The post id should not be empty" )
    require(!postedBy.isEmpty, "The 'post' sender profile id should not be empty" )
    require(!postedOn.isEmpty, "The 'post' receiver profile id should not be empty" )
    require(!accessList.isEmpty, "The access List should not be empty" )
    require(!signature.isEmpty, "The digital signature should not be empty" )
}
case class PostCreated(id: String) extends RestMessage
case class GetPosts(profileID: String) extends RestMessage
case class Posts(total: String, posts: Set[Option[RestMessage]]) extends RestMessage
case class GetPostDetails(postID: String, requestedBy: String, signature: String) extends RestMessage
case class PostDetails(postID: String, message: String, key: String, postedBy: Option[RestMessage], postedOn: Option[RestMessage], date: String) extends RestMessage
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
case class GetAlbumsList(profileID: String) extends RestMessage
case class Albums(total: String, albums: Set[Option[RestMessage]]) extends RestMessage
case class DeleteAlbum(albumID: String) extends RestMessage
case class AlbumDeleted(message: String) extends RestMessage
case class GetAlbumDetails(albumID: String) extends RestMessage
case class AlbumDetails(albumID: String, name: String, profileID: String, profileType: String) extends RestMessage
case class GetPhotosFromAlbum(albumID: String) extends RestMessage

// Photo related messages
case class UploadPhoto(name: String, profileID: String, image: String, albumID: String, accessList: String) extends RestMessage {
    require(!name.isEmpty, "The name should not be empty" )
    require(!profileID.isEmpty, "The profile ID should not be empty" )
    require(!image.isEmpty, "Please upload a valid image" )
    require(!albumID.isEmpty, "The album id should not be empty" )
    require(!accessList.isEmpty, "The access List should not be empty" )
}
case class PhotoUploaded(id: String) extends RestMessage
case class GetPhotoDetails(photoID: String, requestedBy:String) extends RestMessage
case class DeletePhoto(photoID: String) extends RestMessage
case class PhotoDeleted(message: String) extends RestMessage
case class PhotoDetails(photoID: String, name: String, data: String, key: String, albumID: String) extends RestMessage
case class GetPhotos(id: String) extends RestMessage
case class Photos(total: String, photos: Set[Option[RestMessage]]) extends RestMessage

// Like
case class GetLikesOf(id: String) extends RestMessage
case class GetLikedBy(id: String) extends RestMessage
case class Likes(total: String, likes: Set[Option[RestMessage]]) extends RestMessage

case class ErrorMessage(error: String) extends RestMessage

trait RedisApi {
  import com.redis._
  val rc = new RedisClient("localhost", 6379)

  def closeRedisConnection = {
    rc.quit
    rc.disconnect
  }
}