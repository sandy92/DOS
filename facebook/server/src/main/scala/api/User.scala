import akka.actor._
import com.redis._
import serialization._
import Parse.Implicits._

class User extends Actor with Profile with IDGenerator {
    def receive = {
        case u: CreateUser => {
            val userID = getUniqueID("user")
            var x: RestMessage = {
                if(!rc.sismember("users",u.email)) {
                    rc.hmset(userID,Map("name"->u.name,"email"->u.email,"age"->u.age))
                    if (rc.hmset(userID,Map("name"->u.name,"email"->u.email,"age"->u.age))) {
                        rc.sadd("users",u.email)
                        UserCreated(userID)
                    } else {
                        ErrorMessage("User not created")
                    }
                } else {
                    ErrorMessage("Given email id already exists")
                }
            }
            //val d = rc.hmget[String,String](userID,"name","email","age")
            sender ! x
        }
        case u: GetUserDetails => {
            val x = {
                if(u.userID.headOption.getOrElse("").toString == prefix("user").toString) {
                    val m = rc.hgetall[String,String](u.userID.toString).get
                    if(!m.isEmpty) {
                        UserDetails(u.userID,m("name"),m("email"),m("age").toInt)
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
                    val email = rc.hget[String](u.userID.toString,"email").getOrElse("")
                    if(!email.isEmpty) {
                        rc.srem("users",email)
                        rc.del(u.userID)
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