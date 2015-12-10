import akka.actor._
import com.redis._
import serialization._
import Parse.Implicits._

class Post extends Actor with RedisApi with LikedBy with IDGenerator {
    override def postStop = closeRedisConnection
    def sendLikedBy(id: String, sender: ActorRef) = {
        val x = {
            if(id.headOption.getOrElse("").toString == prefix("post").toString) {
                val m = rc.smembers[String]("likedBy:post:"+id.toString).get
                if(!m.isEmpty) {
                    val size = rc.scard("likedBy:post:"+id.toString).getOrElse(0).toString
                    Likes(size,m.map(_.get).map(e => extractDetails(e,rc.hgetall[String,String](e).getOrElse(Map()))))
                } else {
                    ErrorMessage("The given post did not like anything")
                }
            } else {
                ErrorMessage("Not a valid post id")
            }
        }
        sender ! x
    }

    def receive = handleLikedBy orElse {
        case u: GetPostDetails => {
            val x = {
                if(u.postID.headOption.getOrElse("").toString == prefix("post").toString) {
                    val m = rc.hgetall[String,String]("post:"+u.postID.toString).get
                    if(!m.isEmpty) {
                        extractPostDetails("post:"+u.postID.toString,rc).get
                    } else {
                        ErrorMessage("Post Not found")
                    }
                } else {
                    ErrorMessage("Not a valid post id")
                }
            }
            sender ! x
        }
        case u: CreatePost => {
            val x = {
                val pbl = prefixLookup(u.postedBy.headOption.getOrElse("").toString)
                val pol = prefixLookup(u.postedBy.headOption.getOrElse("").toString)
                if(pbl != "user" && pbl != "page") {
                    ErrorMessage("Not a valid sender profile id")
                } else if (pol != "user" && pol != "page") {
                    ErrorMessage("Not a valid receiver profile id")
                } else {
                    if(!rc.exists((pbl+":"+u.postedBy).toString)) {
                        ErrorMessage("Not a valid sender profile id")
                    } else if(!rc.exists((pol+":"+u.postedOn).toString)) {
                        ErrorMessage("Not a valid receiver profile id")
                    } else {
                        val postID = getUniqueID("post")
                        if(rc.hmset("post:"+postID, Map("message" -> u.message, "postedBy" -> (pbl+":"+u.postedBy).toString, "postedOn" -> (pol+":"+u.postedOn).toString, "date" -> (System.currentTimeMillis / 1000).toString))) {
                            rc.sadd("posts:"+pol+":"+u.postedOn, "post:"+postID)
                            PostCreated(postID)
                        } else {
                            ErrorMessage("Unable to create the post")
                        }
                    }
                }
            }
            sender ! x
        }
        case u: DeletePost => {
            val x = {
                if(u.postID.headOption.getOrElse("").toString == prefix("post").toString) {
                    val m = rc.hgetall[String,String]("post:"+u.postID.toString).get
                    if(!m.isEmpty) {
                        val profile = prefixLookup(u.profileID.headOption.getOrElse("").toString)
                        val id = profile + ":" + u.profileID
                        val pb = m.get("postedBy").getOrElse("")
                        val po = m.get("postedOn").getOrElse("")
                        if(pb == id || po == id) {
                            if(!po.isEmpty)
                            {
                                rc.srem("posts:"+po,"post:"+u.postID)
                            }
                            rc.del("post:"+u.postID)
                            PostDeleted("Post " + u.postID.toString + " deleted successfully")
                        } else {
                            ErrorMessage("Access denied: Can't delete the post")
                        }
                    } else {
                        ErrorMessage("The given post does not exist")
                    }
                } else {
                    ErrorMessage("Not a valid post id")
                }
            }
            sender ! x
        }
        case _ =>
    }
}

object Post extends RedisApi with IDGenerator {
    def createPost(name: String, profileID: String) = {
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