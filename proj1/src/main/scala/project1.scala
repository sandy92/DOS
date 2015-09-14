import java.security.{ MessageDigest => md }
import akka.actor._
import scala.collection.mutable._
import scala.util._

case class InitializeSystem(k: Int)
case object KillActor
case class AddSlave(s: ActorRef)
case object SlaveAdded
case object JobRequest
case class Job(msgListBuffer: ListBuffer[String], numZeros: Int)
case class CoinFound(input: String, hash: String)

object Hash {
    def sha256(s: String) = {
        md.getInstance("SHA-256").digest(s.getBytes).map(e => "%02x".format(e)).mkString
    }
}

class MiningSlaveActor(remoteUrl: String) extends Actor {
    val remote = context.actorSelection(remoteUrl)
    var input = ""
    var hashValue = ""
    def receive = {
        case "START" =>
            remote ! AddSlave(self)
        case SlaveAdded =>
            sender ! JobRequest
        case KillActor =>
            context.system.shutdown
        case Job(msgListBuffer: ListBuffer[String], numZeros: Int) =>
            var flag = true
            for(msg <- msgListBuffer) {
                input = "kvsandeep;%s".format(msg)
                hashValue = Hash.sha256(input)
                if(this.checkZeros(hashValue, numZeros)) {
                    sender ! CoinFound(input, hashValue)
                    flag = false
                }
            }
            if(flag) {
                sender ! JobRequest
            }

        case _ =>
    }

    def checkZeros(s: String, k: Int) = {
        if(k>0 && k<=s.length) {
            s.substring(0,k) == "0"*k
        } else {
            false
        }
    }
}

class MiningMasterActor extends Actor {
    val slaveActorList = ArrayBuffer.empty[ActorRef]
    val queue = new Queue[String]
    val messageSize = 1000
    var numZeros = 0

    def receive = {
        case InitializeSystem(k: Int) =>
            numZeros = k
            queue ++= List.range(0,2000000).map("%07d".format(_))
        case AddSlave(s: ActorRef) =>
            slaveActorList.append(s)
            s ! SlaveAdded
        case JobRequest =>
            if(queue.length > 0) {
                var count = 0
                val a = ListBuffer.empty[String]
                do {
                    a += queue.dequeue
                    count = count + 1
                } while(queue.length > 0 && count < messageSize)
                sender ! Job(a, numZeros)
            } else {
                println("Input stream exhausted")
                context.system.shutdown
            }
        case CoinFound(input: String, hash:String) =>
            println(input + "\t" +hash)
            self ! KillActor
        case KillActor =>
            while(slaveActorList.length > 0)
            {
                slaveActorList(0) ! KillActor
                slaveActorList -= slaveActorList(0)
            }
            context.system.shutdown
        case "print" =>
            println(slaveActorList)
        case _ =>
    }

}

object Bitcoin extends App{
    var input = ""

    if(args.length == 1) {
        input = args(0)
        if(isNumeric(input)) {
            var k = input.toInt
            val system = ActorSystem("BitcoinActorRemoteSystem")
            val master = system.actorOf(Props[MiningMasterActor], name="master")
            master ! InitializeSystem(k)
            //val slave1 = system.actorOf(Props(new MiningSlaveActor("akka.tcp://BitcoinActorRemoteSystem@127.0.0.1:5150/user/master")), name="slave1")

            //slave1 ! "START"
        } else if(isIPAddress(input)) {
            // Connect to remote and process data
            val system = ActorSystem("BitcoinActorLocalSystem")
            val localSlave = system.actorOf(Props(new MiningSlaveActor("akka.tcp://BitcoinActorRemoteSystem@"+ input +":5150/user/master")), name="localSlave")

            localSlave ! "START"
        } else {
            println("Please enter a valid input")
        }
    } else {
        println("Please enter a valid input")
    }

    def isNumeric(s: String) = { s.forall(_.isDigit) }

    def isIPAddress(s: String) = {
        val ipRegex = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$".r
        s match {
            case ipRegex(_*) => true
            case _ => false
        }
    }
}
