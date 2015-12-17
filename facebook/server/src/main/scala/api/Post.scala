import akka.actor._
import com.redis._
import serialization._
import Parse.Implicits._
import spray.json._
import MyJsonProtocol._
import java.security._

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
                val pbl = prefixLookup(u.requestedBy.headOption.getOrElse("").toString)
                if(u.postID.headOption.getOrElse("").toString == prefix("post").toString) {
                    val pkey = rc.hget[String](pbl+":"+u.requestedBy, "publicKey").getOrElse("")
                    if(!pkey.isEmpty){
                        val validSignature = {
                            val s = "'"+u.requestedBy+"' requested post with id '"+u.postID+"'"
                            val m = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"))
                            val hash = m.map("%02x".format(_)).mkString
                            Crypto.rsa.decrypt(u.signature,Crypto.rsa.decodePublicKey(pkey)) == hash
                        } 
                        if(validSignature) {
                            val m = rc.hgetall[String,String]("post:"+u.postID.toString).get
                            if(!m.isEmpty) {
                                val p = prefixLookup(u.requestedBy.headOption.getOrElse("").toString)
                                if(rc.hexists("access:post:"+u.postID.toString,p+":"+u.requestedBy)) {
                                    extractPostDetails("post:"+u.postID.toString,p+":"+u.requestedBy,rc).get
                                } else {
                                    ErrorMessage("Access denied")
                                }
                            } else {
                                ErrorMessage("Post Not found")
                            }
                        } else {
                            ErrorMessage("Unable to authenticate the user")
                        }
                    } else {
                        ErrorMessage("Access denied: Not a valid public key")
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
                        val pkey = rc.hget[String](pbl+":"+u.postedBy, "publicKey").getOrElse("")
                        val randNumber = rc.hget[String](pbl+":"+u.postedBy, "randNumber").getOrElse("")
                        if(!pkey.isEmpty){
                            val validSignature = {
                                val s = "Posted '"+u.message+"' on '"+u.postedOn+"' by '"+u.postedBy+"' for '"+u.accessList+"'"
                                val m = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"))
                                val hash = m.map("%02x".format(_)).mkString
                                Crypto.rsa.decrypt(u.signature,Crypto.rsa.decodePublicKey(pkey)) == hash
                            } 
                            if(validSignature) {
                                val postID = getUniqueID("post")
                                if(rc.hmset("post:"+postID, Map("message" -> u.message, "postedBy" -> (pbl+":"+u.postedBy).toString, "postedOn" -> (pol+":"+u.postedOn).toString, "date" -> (System.currentTimeMillis / 1000).toString))) {
                                    rc.sadd("posts:"+pol+":"+u.postedOn, "post:"+postID)
                                    val al = u.accessList.parseJson.convertTo[Map[String,String]]
                                    al.foreach(e => rc.hset("access:post:"+postID,prefixLookup(e._1.headOption.getOrElse("").toString) + ":" + e._1,e._2))
                                    PostCreated(postID)
                                } else {
                                    ErrorMessage("Unable to create the post")
                                }
                            } else {
                                ErrorMessage("Unable to authenticate the user")
                            }
                        } else {
                            ErrorMessage("Access denied: Not a valid public key")
                        }
                    }
                }
            }
            //val x = u
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

/*object Post extends RedisApi with IDGenerator {
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
}*/