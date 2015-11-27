import akka.actor._
import com.redis._
import serialization._
import Parse.Implicits._

class User extends Profile with IDGenerator {
    def sendLikesOf(id: String, sender: ActorRef) = {
        val x = {
            if(id.headOption.getOrElse("").toString == prefix("user").toString) {
                val m = rc.smembers[String]("likesOf:user:"+id.toString).get
                if(!m.isEmpty) {
                    val size = rc.scard("likesOf:user:"+id.toString).getOrElse(0).toString
                    Likes(size,m.map(_.get).map(e => extractDetails(e,rc.hgetall[String,String](e).getOrElse(Map()))))
                } else {
                    ErrorMessage("User did not like anything")
                }
            } else {
                ErrorMessage("Not a valid user id")
            }
        }
        sender ! x
    }

    def receive = handleLikesOf orElse {
        case u: CreateUser => {
            val userID = getUniqueID("user")
            var x: RestMessage = {
                if(!rc.sismember("users",u.email)) {
                    val s = rc.hmset("user:"+userID,Map("name"->u.name,"email"->u.email,"age"->u.age))
                    if (s) {
                        rc.sadd("users",u.email)
                        UserCreated(userID)
                    } else {
                        ErrorMessage("User not created")
                    }
                } else {
                    ErrorMessage("Given email id already exists")
                }
            }
            sender ! x
        }
        case u: GetUserDetails => {
            val x = {
                if(u.userID.headOption.getOrElse("").toString == prefix("user").toString) {
                    val m = rc.hgetall[String,String]("user:"+u.userID.toString).get
                    if(!m.isEmpty) {
                        extractDetails("user:"+u.userID.toString,m)
                    } else {
                        ErrorMessage("User Not found")
                    }
                } else {
                    ErrorMessage("Not a valid user id")
                }
            }
            sender ! x
        }
        case u: DeleteUser => {
            val x = {
                if(u.userID.headOption.getOrElse("").toString == prefix("user").toString) {
                    val email = rc.hget[String]("user:" + u.userID.toString,"email").getOrElse("")
                    if(!email.isEmpty) {
                        rc.srem("users",email)
                        rc.del("user:"+u.userID)
                        UserDeleted("User " + u.userID.toString + " deleted successfully")
                    } else {
                        ErrorMessage("The given user does not exist")
                    }
                } else {
                    ErrorMessage("Not a valid user id")
                }
            }
            sender ! x
        }
        case _ =>
    }
}