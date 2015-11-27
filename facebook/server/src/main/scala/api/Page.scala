import akka.actor._
import com.redis._
import serialization._
import Parse.Implicits._

class Page extends Profile with LikedBy with IDGenerator {
    def sendLikesOf(id: String, sender: ActorRef) = {

    }

    def receive = handleLikesOf orElse handleLikedBy orElse {
        case u: CreatePage => {
            val pageID = getUniqueID("page")
            var x: RestMessage = {
                if(!rc.sismember("pages",u.webAddress)) {
                    val s = rc.hmset("page:"+pageID,Map("name"->u.name,"webAddress"->u.webAddress,"about"->u.about))
                    if (s) {
                        rc.sadd("pages",u.webAddress)
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
        case _ =>
    }
}