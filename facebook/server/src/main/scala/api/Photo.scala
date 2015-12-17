import akka.actor._
import com.redis._
import serialization._
import Parse.Implicits._
import spray.json._
import MyJsonProtocol._

class Photo extends Actor with RedisApi with IDGenerator with LikedBy {
    override def postStop = closeRedisConnection

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
                    if(rc.sismember("albumIDs:"+profile+":"+u.profileID,"album:"+u.albumID)) {
                        val photoData = u.image
                        if(rc.hmset("photo:"+photoID,Map("name"->u.name,"data"->photoData, "album" -> ("album:"+u.albumID).toString))) {
                            rc.sadd("photos:album:"+u.albumID, "photo:"+photoID)
                            val al = u.accessList.parseJson.convertTo[Map[String,String]]
                            al.foreach(e => rc.hset("access:photo:"+photoID,prefixLookup(e._1.headOption.getOrElse("").toString) + ":" + e._1,e._2))
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
        case u: GetPhotoDetails => {
            val x = {
                if(u.photoID.headOption.getOrElse("").toString == prefix("photo").toString) {
                    val m = rc.hgetall[String,String]("photo:"+u.photoID.toString).get
                    if(!m.isEmpty) {
                        val p = prefixLookup(u.requestedBy.headOption.getOrElse("").toString)
                        if(rc.hexists("access:photo:"+u.photoID.toString,p+":"+u.requestedBy)) {
                            extractDetails("photo:"+u.photoID.toString,m,rc.hget[String]("access:photo:"+u.photoID.toString,p+":"+u.requestedBy).getOrElse("")).get
                        } else {
                            ErrorMessage("Access denied")
                        }
                    } else {
                        ErrorMessage("Photo Not found")
                    }
                } else {
                    ErrorMessage("Not a valid photo id")
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