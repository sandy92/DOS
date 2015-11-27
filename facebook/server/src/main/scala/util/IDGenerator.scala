trait IDGenerator {
    def prefix(idType: String) = idType match {
        case "user" => "1"
        case _ => "9" 
    }

    def getUniqueID(idType: String) = {
        prefix(idType) + System.currentTimeMillis + "%02d" format scala.util.Random.nextInt(100)
    }
}