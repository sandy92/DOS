import java.security.{ MessageDigest => md }
import akka.actor._
import scala.collection.mutable._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._

case object InitializeSystem
case object CreateSlave
case object StartMining
case object KillSystem

object Hash {
    val inst = md.getInstance("SHA-256")
    def sha256(s: String) = {
        inst.digest(s.getBytes).map("%02x".format(_)).mkString
    }
}

class MiningSlaveActor extends Actor {
    var input = ""
    var hashValue = ""
    def receive = {
        case msg: String =>
            input = "kvsandeep;%s".format(msg)
            hashValue = Hash.sha256(input)
            sender ! hashValue
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
    var count = 0
    //queue ++= List.range(0,1000000).map("%06d".format(_))
    queue ++= List.range(0,1000).map("%03d".format(_))

    def receive = {
        case InitializeSystem =>
            slaveActorList += context.system.actorOf(Props[MiningSlaveActor], name = "slave" + slaveActorList.length.toString)
        case CreateSlave =>
            // slaveActorList += context.system.actorOf(Props[MiningSlaveActor], name = "slave" + slaveActorList.length.toString)
            var c = Future {
                context.system.actorOf(Props[MiningSlaveActor], name = "slave" + slaveActorList.length.toString)
            }
            c.onComplete {
                case Success(actor) => slaveActorList += actor
                case _ =>
            }
            println("slaveCreated")
        case StartMining =>
            var c = Future {
                println("Mining Started")
                while(queue.length > 0 ) {
                    slaveActorList(count%slaveActorList.length) ! queue.dequeue
                    count += 1
                    //println("hey")
                    println(slaveActorList.length)
                }
                println("Mining Ends")
            }
        case KillSystem =>
            context.system.shutdown
        case msg: String =>
            println(msg)
        case _ =>
    }

}

object Bitcoin extends App{
    val system = ActorSystem("BitcoinActorSystem")
    val master = system.actorOf(Props[MiningMasterActor], name="master")
    master ! InitializeSystem
    master ! CreateSlave
    master ! StartMining
    //master ! CreateSlave
    println("main here")
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
