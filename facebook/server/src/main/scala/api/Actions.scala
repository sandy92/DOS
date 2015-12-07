import akka.actor._

trait LikesOf {
    this: Actor =>

    def sendLikesOf(id: String, sender: ActorRef): Unit

    def handleLikesOf: Receive = {
        case a: GetLikesOf => sendLikesOf(a.id, sender)
    }
}

trait LikedBy {
    this: Actor =>

    def sendLikedBy(id: String, sender: ActorRef): Unit

    def handleLikedBy: Receive = {
        case a: GetLikedBy => sendLikedBy(a.id, sender)
    }
}

trait Comment {
}

trait Share {
}