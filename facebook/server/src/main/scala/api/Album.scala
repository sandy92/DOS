import akka.actor._
import com.redis._
import serialization._
import Parse.Implicits._

class Album extends Actor with RedisApi with IDGenerator with LikedBy {
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
        case a: CreateAlbum => {
            val x = Album.createAlbum(a.name,a.profileID)
            sender ! x
        }
        case u: GetUserDetails => {
            val x = {
                if(u.userID.headOption.getOrElse("").toString == prefix("user").toString) {
                    val m = rc.hgetall[String,String]("user:"+u.userID.toString).get
                    if(!m.isEmpty) {
                        extractDetails("user:"+u.userID.toString,m).get
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

object Album extends RedisApi with IDGenerator {
    def createAlbum(name: String, profileID: String) = {
        val profile = prefixLookup(profileID.headOption.getOrElse("").toString)
        val albumID = getUniqueID("album")
        if(profile == "user") {
            if(rc.exists("user:"+profileID)) {
                println("here")
                if(rc.hsetnx("albums:user:"+profileID,name,albumID)) {
                    rc.sadd("albumIDs:user:"+profileID,albumID)
                    AlbumCreated(albumID)
                } else {
                    ErrorMessage("Given album name already exists")
                }
            }
        } else if (profile == "page"){
            if(rc.exists("page:"+profileID)) {
                if(rc.hsetnx("albums:page:"+profileID,name,albumID)) {
                    rc.sadd("albumIDs:page:"+profileID,albumID)
                    AlbumCreated(albumID)
                } else {
                    ErrorMessage("Given album name already exists")
                }
            }
        } else {
            ErrorMessage("Not a valid profile")
        }
    }
}