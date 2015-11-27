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

case class ErrorMessage(error: String) extends RestMessage

trait RedisApi {
  import com.redis._
  val rc = new RedisClient("localhost", 6379)
}