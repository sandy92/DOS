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
                    val s = rc.hmset("user:"+userID,Map("name"->u.name,"email"->u.email,"age"->u.age, "publicKey"->u.publicKey))
                    if (s) {
                        rc.sadd("users",u.email)
                        Album.createAlbum("Photos",userID) match {
                            case a: AlbumCreated => rc.hset("user:"+userID,"defaultAlbum",a.id)
                            case _ => 
                        }
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
        case u: GetFriendsList => {
            val x = {
                if(u.userID.headOption.getOrElse("").toString == prefix("user").toString) {
                    val email = rc.hget[String]("user:" + u.userID.toString,"email").getOrElse("")
                    if(!email.isEmpty) {
                        val size = rc.scard("friends:user:"+u.userID.toString).getOrElse(0).toString
                        val m = rc.smembers[String]("friends:user:"+u.userID.toString).get
                        FriendsList(size,m.map(_.get).map(e => extractDetails(e,rc.hgetall[String,String](e).getOrElse(Map()))))
                    } else {
                        ErrorMessage("The given user does not exist")
                    }
                } else {
                    ErrorMessage("Not a valid user id")
                }
            }
            sender ! x
        }
        case u: GetPosts => {
            val x = {
                if(u.profileID.headOption.getOrElse("").toString == prefix("user").toString) {
                    val email = rc.hget[String]("user:" + u.profileID.toString,"email").getOrElse("")
                    if(!email.isEmpty) {
                        val size = rc.scard("posts:user:"+u.profileID.toString).getOrElse(0).toString
                        val m = rc.smembers[String]("posts:user:"+u.profileID.toString).get
                        Posts(size,m.map(_.get).map(e => extractPostDetails(e,rc)))
                    } else {
                        ErrorMessage("The given user does not exist")
                    }
                } else {
                    ErrorMessage("Not a valid user id")
                }
            }
            sender ! x
        }
        case u: GetAlbumsList => {
            val x = {
                if(u.profileID.headOption.getOrElse("").toString == prefix("user").toString) {
                    val email = rc.hget[String]("user:" + u.profileID.toString,"email").getOrElse("")
                    if(!email.isEmpty) {
                        val size = rc.scard("albumIDs:user:"+u.profileID.toString).getOrElse(0).toString
                        val m = rc.smembers[String]("albumIDs:user:"+u.profileID.toString).get
                        Albums(size,m.map(_.get).map(e => extractDetails(e,rc.hgetall[String,String](e).getOrElse(Map()))))
                    } else {
                        ErrorMessage("The given user does not exist")
                    }
                } else {
                    ErrorMessage("Not a valid user id")
                }
            }
            sender ! x
        }
        case u: AddFriend => {
            val x = {
                if(u.id.headOption.getOrElse("").toString == prefix("user").toString) {
                    if(u.requestedBy.headOption.getOrElse("").toString == prefix("user").toString) {
                        if(rc.exists("user:"+u.id)) {
                            if(rc.exists("user:"+u.requestedBy)) {
                                rc.sadd("friends:user:"+u.id,"user:"+u.requestedBy)
                                rc.sadd("friends:user:"+u.requestedBy,"user:"+u.id)
                                FriendAdded("Added " + u.requestedBy + " to the friends list")
                            } else {
                                ErrorMessage("The given friend does not exist")
                            }
                        } else {
                            ErrorMessage("The given user does not exist")
                        }
                    } else {
                        ErrorMessage("Not a valid friend id")
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