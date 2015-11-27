import akka.actor._

class Test extends Actor {
    def receive = {
        case e: TestMessage => 
            Thread sleep 3000
            sender ! TestMessage((e.randMsg.toInt+1).toString)
        case _ =>
    }
}