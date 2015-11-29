import akka.actor._
import com.redis._
import serialization._
import Parse.Implicits._

class Photo extends Actor with RedisApi with IDGenerator with LikedBy {
    def sendLikedBy(id: String, sender: ActorRef) = {
        val x = {
            if(id.headOption.getOrElse("").toString == prefix("page").toString) {
                val m = rc.smembers[String]("likedBy:page:"+id.toString).get
                if(!m.isEmpty) {
                    val size = rc.scard("likedBy:page:"+id.toString).getOrElse(0).toString
                    Likes(size,m.map(_.get).map(e => extractDetails(e,rc.hgetall[String,String](e).getOrElse(Map()))))
                } else {
                    ErrorMessage("The given page did not like anything")
                }
            } else {
                ErrorMessage("Not a valid page id")
            }
        }
        sender ! x
    }

    def receive = handleLikedBy orElse {
        case u: UploadPhoto => {
            val photoID = getUniqueID("photo")
            val x = {
                val profile = prefixLookup(u.profileID.headOption.getOrElse("").toString)
                if (profile == "user" || profile == "page") {
                    if(rc.sismember("albumIDs:"+profile+":"+u.profileID,u.albumID)) {
                        val photoData = new sun.misc.BASE64Encoder().encode(u.image)
                        if(rc.hmset("photo:"+photoID,Map("name"->u.name,"data"->photoData, "albumID" -> u.albumID))) {
                            rc.rpush("photos:album:"+u.albumID, "photo:"+photoID)
                            PhotoUploaded(photoID)
                        } else {
                            ErrorMessage("Unable to create the image")
                        }
                    } else {
                        ErrorMessage("The given album does not exists")
                    }
                } else {
                    ErrorMessage("Not a valid profile")
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