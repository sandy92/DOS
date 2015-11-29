import com.redis._

trait IDGenerator {
    def prefix(idType: String) = idType match {
        case "user" => "1"
        case "page" => "2"
        case "album" => "3"
        case "photo" => "4"
        case "post" => "5"
        case "comment" => "6"
        case _ => "9" 
    }

    def prefixLookup(prefix: String) = prefix match {
        case "1" => "user"
        case "2" => "page"
        case "4" => "photo"
        case "5" => "post"
        case "6" => "comment"
        case "9" => "other"
        case "_" => "invalid prefix"
    }

    def getUniqueID(idType: String) = {
        prefix(idType) + System.currentTimeMillis + "%02d" format scala.util.Random.nextInt(100)
    }

    def extractDetails(id: String, m: Map[String,String]) = {
        val x = id.split(":")
        if(x.length > 1) {
            x(0) match {
                case "user" => Some(UserDetails(x(1),m.get("name").getOrElse(""),m.get("email").getOrElse(""),m.get("age").getOrElse("0").toInt))
                case "page" => Some(PageDetails(x(1),m.get("name").getOrElse(""),m.get("webAddress").getOrElse(""),m.get("about").getOrElse("")))
                case _ => None
            }
        } else {
           None
        }
    }

    def extractPostDetails(id: String, rc: RedisClient) = {
        val x = id.split(":")
        if(x.length > 1) {
            x(0) match {
                case "post" => {
                    val m = rc.hgetall[String,String](id).getOrElse(Map())
                    val postedBy = rc.hgetall[String,String](m.get("postedBy").getOrElse("")).getOrElse(Map())
                    val postedOn = rc.hgetall[String,String](m.get("postedOn").getOrElse("")).getOrElse(Map())
                    Some(PostDetails(x(1),extractDetails(m.get("postedBy").getOrElse(""),postedBy),extractDetails(m.get("postedOn").getOrElse(""),postedOn),m.get("date").getOrElse("")))
                }
                case _ => None
            }
        } else {
            None
        }
    }
}