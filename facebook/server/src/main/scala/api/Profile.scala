import akka.actor._

trait Profile extends Actor with RedisApi with LikesOf{
}