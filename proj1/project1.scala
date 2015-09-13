import java.security.{ MessageDigest => md }
import akka.actor._
import scala.collection.mutable._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._

case object InitializeSystem
case object KillActor
case class AddSlave(s: ActorRef)
case object SlaveAdded
case object JobRequest
case class Job(s: String, numZeros: Int)
case class CoinFound(input: String, hash: String)

object Hash {
    def sha256(s: String) = {
        md.getInstance("SHA-256").digest(s.getBytes).map(e => "%02x".format(e)).mkString
    }
}

class MiningSlaveActor extends Actor {
    var input = ""
    var hashValue = ""
    def receive = {
        case SlaveAdded =>
            sender ! JobRequest
        case KillActor =>
            context.stop(self)
        case Job(msg: String, numZeros: Int) =>
            input = "kvsandeep;%s".format(msg)
            hashValue = Hash.sha256(input)
            if(this.checkZeros(hashValue, numZeros)) {
                println(" Found")
                sender ! CoinFound(input, hashValue)
            } else {
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
    val numZeros = 5
    val queue = new Queue[String]
    var count = 0

    def receive = {
        case InitializeSystem =>
            queue ++= List.range(0,1000000).map("%06d".format(_))
        case AddSlave(s: ActorRef) =>
            slaveActorList.append(s)
            s ! SlaveAdded
        case JobRequest =>
            if(queue.length > 0) {
                sender ! Job(queue.dequeue, numZeros)
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
    val system = ActorSystem("BitcoinActorSystem")
    val master = system.actorOf(Props[MiningMasterActor], name="master")
    val slave1 = system.actorOf(Props[MiningSlaveActor], name="slave1")
    val slave2 = system.actorOf(Props[MiningSlaveActor], name="slave2")
    val slave3 = system.actorOf(Props[MiningSlaveActor], name="slave3")
    val slave4 = system.actorOf(Props[MiningSlaveActor], name="slave4")

    master ! InitializeSystem

    master ! AddSlave(slave1)
    master ! AddSlave(slave2)
    master ! AddSlave(slave3)
    master ! AddSlave(slave4)
    /*
    if(args.length > 0 && false)
    {
        var k = args(0).toInt
        // Assuming int here, write appropriate code for type checking
        // Should consider the case if there are more leading zeros than expected
        var list = this.generateList(1)
        for(i <- list)
        {
            // miner ! i
            //if(this.checkZeros(value,k)) {
            //    println(input + "\t" + value)
            //}
        }
    }
    else
    {
        println("Please enter a valid input")
    }
    */
    //system.shutdown

    def generateList(k:Int) = {
        if(k>0) {
            List.range(0,1000000).map("%06d".format(_))
        } else {
            List()
        }
    }
}
