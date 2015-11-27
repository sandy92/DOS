trait IDGenerator {
    def prefix(idType: String) = idType match {
        case "user" => "1"
        case "page" => "2"
        case _ => "9" 
    }

    def prefixLookup(prefix: String) = prefix match {
        case "1" => "user"
        case "2" => "page"
        case "9" => "other"
        case "_" => "other"
    }

    def getUniqueID(idType: String) = {
        prefix(idType) + System.currentTimeMillis + "%02d" format scala.util.Random.nextInt(100)
    }

    def extractDetails(id: String, m: Map[String,String]) = {
        val x = id.split(":")
        if(x.length > 1) {
            x(0) match {
                case "user" => UserDetails(x(1),m("name"),m("email"),m("age").toInt)
                case "page" => PageDetails(x(1),m("name"),m("webAddress"),m("about"))
                case _ => null
            }
        } else {
            null
        }
    }
}