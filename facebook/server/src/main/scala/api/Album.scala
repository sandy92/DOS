import akka.actor._
import com.redis._
import serialization._
import Parse.Implicits._

class Album extends Actor with RedisApi with IDGenerator with LikedBy {
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
        case a: CreateAlbum => {
            val x = Album.createAlbum(a.name,a.profileID)
            sender ! x
        }
        case u: GetAlbumDetails => {
            val x = {
                if(u.albumID.headOption.getOrElse("").toString == prefix("album").toString) {
                    val m = rc.hgetall[String,String]("album:"+u.albumID.toString).get
                    if(!m.isEmpty) {
                        extractDetails("album:"+u.albumID.toString,m).get
                    } else {
                        ErrorMessage("Album Not found")
                    }
                } else {
                    ErrorMessage("Not a valid album id")
                }
            }
            sender ! x
        }
        case u: GetPhotosFromAlbum => {
            val x = {
                if(u.albumID.headOption.getOrElse("").toString == prefix("album").toString) {
                    val m = rc.smembers[String]("photos:album:"+u.albumID.toString).get
                    if(!m.isEmpty) {
                        Photos(m.size.toString,m.map(_.get).map(e => extractDetails(e,rc.hgetall[String,String](e).getOrElse(Map()))))
                    } else {
                        ErrorMessage("Photos are not available")
                    }
                } else {
                    ErrorMessage("Not a valid album id")
                }
            }
            sender ! x
        }
        /*case u: DeleteUser => {
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
        }*/
        case _ =>
    }
}

object Album extends RedisApi with IDGenerator {
    def createAlbum(name: String, profileID: String) = {
        val profile = prefixLookup(profileID.headOption.getOrElse("").toString)
        val albumID = getUniqueID("album")
        if(profile == "user") {
            if(rc.exists("user:"+profileID)) {
                if(rc.hsetnx("albums:user:"+profileID,name,"album:"+albumID)) {
                    rc.sadd("albumIDs:user:"+profileID,"album:"+albumID)
                    rc.hmset("album:"+albumID,Map("name" -> name, "profile" -> ("user:"+profileID).toString))
                    AlbumCreated(albumID)
                } else {
                    ErrorMessage("Given album name already exists")
                }
            }
        } else if (profile == "page"){
            if(rc.exists("page:"+profileID)) {
                if(rc.hsetnx("albums:page:"+profileID,name,"album:"+albumID)) {
                    rc.sadd("albumIDs:page:"+profileID,"album:"+albumID)
                    rc.hmset("album:"+albumID,Map("name" -> name, "profile" -> ("page:"+profileID).toString))
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