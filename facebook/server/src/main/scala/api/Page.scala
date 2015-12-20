import akka.actor._
import com.redis._
import serialization._
import Parse.Implicits._

class Page extends Profile with LikedBy with IDGenerator {
    def sendLikesOf(id: String, sender: ActorRef) = {
        val x = {
            if(id.headOption.getOrElse("").toString == prefix("page").toString) {
                val m = rc.smembers[String]("likesOf:page:"+id.toString).get
                if(!m.isEmpty) {
                    val size = rc.scard("likesOf:page:"+id.toString).getOrElse(0).toString
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

    def receive = handleLikesOf orElse handleLikedBy orElse {
        case u: CreatePage => {
            val pageID = getUniqueID("page")
            var x: RestMessage = {
                if(!rc.sismember("pages",u.webAddress)) {
                    val s = rc.hmset("page:"+pageID,Map("name"->u.name,"webAddress"->u.webAddress,"about"->u.about))
                    if (s) {
                        rc.sadd("pages",u.webAddress)
                        Album.createAlbum("Photos",pageID) match {
                            case a: AlbumCreated => rc.hset("page:"+pageID,"defaultAlbum",a.id)
                            case _ => 
                        }
                        PageCreated(pageID)
                    } else {
                        ErrorMessage("Page not created")
                    }
                } else {
                    ErrorMessage("Given Web Address id already exists")
                }
            }
            sender ! x
        }
        case u: GetPageDetails => {
            val x = {
                if(u.pageID.headOption.getOrElse("").toString == prefix("page").toString) {
                    val m = rc.hgetall[String,String]("page:"+u.pageID.toString).get
                    if(!m.isEmpty) {
                        extractDetails("page:"+u.pageID.toString,m)
                    } else {
                        ErrorMessage("Page Not found")
                    }
                } else {
                    ErrorMessage("Not a valid page id")
                }
            }
            sender ! x
        }
        case u: DeletePage => {
            val x = {
                if(u.pageID.headOption.getOrElse("").toString == prefix("page").toString) {
                    val webAddress = rc.hget[String]("page:" + u.pageID.toString,"webAddress").getOrElse("")
                    if(!webAddress.isEmpty) {
                        rc.srem("pages",webAddress)
                        rc.del("page:"+u.pageID)
                        PageDeleted("Page " + u.pageID.toString + " deleted successfully")
                    } else {
                        ErrorMessage("The given page does not exist")
                    }
                } else {
                    ErrorMessage("Not a valid page id")
                }
            }
            sender ! x
        }
        case u: GetPosts => {
            val x = {
                if(u.profileID.headOption.getOrElse("").toString == prefix("page").toString) {
                    val webAddress = rc.hget[String]("page:" + u.profileID.toString,"webAddress").getOrElse("")
                    if(!webAddress.isEmpty) {
                        val size = rc.scard("posts:page:"+u.profileID.toString).getOrElse(0).toString
                        val m = rc.smembers[String]("posts:page:"+u.profileID.toString).get
                        Posts(size,m.map(_.get).map(e => extractPostDetails(e,"page:"+u.profileID.toString,rc)))
                    } else {
                        ErrorMessage("The given page does not exist")
                    }
                } else {
                    ErrorMessage("Not a valid page id")
                }
            }
            sender ! x
        }
        case u: GetAlbumsList => {
            val x = {
                if(u.profileID.headOption.getOrElse("").toString == prefix("page").toString) {
                    val webAddress = rc.hget[String]("page:" + u.profileID.toString,"webAddress").getOrElse("")
                    if(!webAddress.isEmpty) {
                        val size = rc.scard("albumIDs:page:"+u.profileID.toString).getOrElse(0).toString
                        val m = rc.smembers[String]("albumIDs:page:"+u.profileID.toString).get
                        Albums(size,m.map(_.get).map(e => extractDetails(e,rc.hgetall[String,String](e).getOrElse(Map()))))
                    } else {
                        ErrorMessage("The given page does not exist")
                    }
                } else {
                    ErrorMessage("Not a valid page id")
                }
            }
            sender ! x
        }
        case _ =>
    }
}