import akka.actor._
import scala.math._
import scala.collection.mutable
import scala.collection.immutable
import scala.util.Random

// Needed for the async ticker code
import scala.concurrent.future
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
//import scala.async.Async.{async, await}


// Initializes the system with the required number of child nodes and updates their neighbours
case object InitializeSystem
// Starts the gossip
case class StartSystem(algorithm: String)
// Shuts down the system (which in turn stops the gossip)
case object StopSystem
// Updates the neighbours for a given child
case class UpdateNeighbours(n: Array[ActorRef])
// Adds the given array to the list of neighbours. Separate class has been defined to make sure this function executes only once.
case class AddRandNeighbours(n: Array[ActorRef])
// Used for printing all the children and their neighbours
case object PrintNeighbours

// Gossip message
case class Rumour(msg: String, start: Boolean)

// Gossip message
case class PushSum(value: Double, weight: Double)

case object GossipSuccess
case class PushSumSuccess(state: Double)

class MasterActor(numNodes: Int, topology: String, algorithm: String) extends Actor {
    var children = Array.empty[ActorRef]
    val rumour = "rumour"
    val ticker = 5000
    var t = System.currentTimeMillis
    def generateChildNodes(numNodes: Int) = {
        val buf = mutable.ArrayBuffer.empty[ActorRef];
        for( i <- 0 to numNodes-1) {
            buf += context.actorOf(Props(classOf[ChildActor],this,i), name="child"+i)
        }
        buf.toArray
    }
    def updateChildNeighbourList(topology: String) = {
        def edgeOutOfCube(i: Int, n: Int, s: String) = {
            s match {
                case "-1" => {
                    if(i%n == 0) {
                        true
                    } else {
                        false
                    }
                }
                case "+1" => {
                    if(i%n == n-1) {
                        true
                    } else {
                        false
                    }
                }
                case "-n" => {
                    var j = i % pow(n,2).toInt
                    if (j < n) {
                        true
                    } else {
                        false
                    }
                }
                case "+n" => {
                    var j = i % pow(n,2).toInt
                    if (j > (pow(n,2)-n- 1)) {
                        true
                    } else {
                        false
                    }
                }
                case "-n2" => {
                    var j = pow(n,2).toInt
                    if(i < j) {
                        true
                    } else {
                        false
                    }
                }
                case "+n2" => {
                    var j = pow(n,2).toInt
                    var k = pow(n,3).toInt
                    if(i > (k-j-1)) {
                        true
                    } else {
                        false
                    }
                }
                case _ => false
            }
        }
        topology match {
            case "full" => {
                for( i <- 0 to this.children.length-1) {
                    this.children(i) ! UpdateNeighbours(this.children diff Array(this.children(i)))
                }
            }
            case "line" => {
                var buf = mutable.ArrayBuffer.empty[ActorRef]
                for( i <- 0 to this.children.length-1) {
                    if(i >= 1) {
                        buf += this.children(i-1)
                    }
                    if(i < this.children.length-1) {
                        buf += this.children(i+1)
                    }
                    this.children(i) ! UpdateNeighbours(buf.toArray)
                    buf.clear
                }
            }
            case "3D" => {
                var buf = mutable.ArrayBuffer.empty[ActorRef]
                var n = cbrt(this.children.length).toInt
                var n2 = pow(n,2).toInt
                for( i <- 0 to this.children.length-1) {
                    if(!edgeOutOfCube(i,n,"-1")) {
                        buf += this.children(i-1)
                    }
                    if(!edgeOutOfCube(i,n,"+1")) {
                        buf += this.children(i+1)
                    }
                    if(!edgeOutOfCube(i,n,"-n")) {
                        buf += this.children(i-n)
                    }
                    if(!edgeOutOfCube(i,n,"+n")) {
                        buf += this.children(i+n)
                    }
                    if(!edgeOutOfCube(i,n,"-n2")) {
                        buf += this.children(i-n2)
                    }
                    if(!edgeOutOfCube(i,n,"+n2")) {
                        buf += this.children(i+n2)
                    }
                    this.children(i) ! UpdateNeighbours(buf.toArray)
                    buf.clear
                }
            }
            case "imp3D" => {
                var buf = mutable.ArrayBuffer.empty[ActorRef]
                var randomNodesArray = mutable.ArrayBuffer.empty[ActorRef]
                var n = cbrt(this.children.length).toInt
                var n2 = pow(n,2).toInt
                val random = new Random
                for( i <- 0 to this.children.length-1) {
                //for( i <- 0 to 1) {
                    if(!edgeOutOfCube(i,n,"-1")) {
                        buf += this.children(i-1)
                    }
                    if(!edgeOutOfCube(i,n,"+1")) {
                        buf += this.children(i+1)
                    }
                    if(!edgeOutOfCube(i,n,"-n")) {
                        buf += this.children(i-n)
                    }
                    if(!edgeOutOfCube(i,n,"+n")) {
                        buf += this.children(i+n)
                    }
                    if(!edgeOutOfCube(i,n,"-n2")) {
                        buf += this.children(i-n2)
                    }
                    if(!edgeOutOfCube(i,n,"+n2")) {
                        buf += this.children(i+n2)
                    }
                    var temp = this.children diff buf diff Array(this.children(i))
                    /*println("---")
                    for( j <- 0 to this.children.length-1) {
                        println(this.children(j))
                    }
                    println("---")
                    for( j <- 0 to buf.length-1) {
                        println(buf(j))
                    }
                    println("---")
                    for( j <- 0 to temp.length-1) {
                        println(temp(j))
                    }
                    println("---")*/
                    temp = temp diff randomNodesArray
                    /*for( j <- 0 to temp.length-1) {
                        println(temp(j))
                    }
                    println("---")
                    println("---")*/
                    //println(temp.toArray)
                    //temp = temp diff randomNodesArray
                    //println(temp.toArray)
                    if (temp.length > 0) {
                        var randNode = temp(random.nextInt(temp.length))
                        //println(" ---here--- ")
                        //println(randNode)
                        //buf += randNode
                        //println(randNode)
                        randomNodesArray += this.children(i)
                        randomNodesArray += randNode
                        /*println("---")
                        for( j <- 0 to randomNodesArray.length-1) {
                            println(randomNodesArray(j))
                        }
                        println("---")*/
                        this.children(i) ! UpdateNeighbours(buf.toArray)
                        this.children(i) ! AddRandNeighbours(Array(randNode))
                        randNode ! AddRandNeighbours(Array(this.children(i)))
                        //println(" ---here--- ")
                    } else {
                        this.children(i) ! UpdateNeighbours(buf.toArray)
                    }
                    /*println("----")
                    println(this.children(i))
                    for( j <- 0 to buf.length-1) {
                        println(buf(j))
                    }
                    println("----")*/
                    buf.clear
                    //println(randomNodesArray.length)
                }
            }
            case _ =>
        }
    }
    def printChildNeighbourList() = {
        for( i <- 0 to this.children.length-1) {
            this.children(i) ! PrintNeighbours
        }
    }
    def receive = {
        case InitializeSystem => {
            //println("system started")
            children = generateChildNodes(numNodes)
            updateChildNeighbourList(topology)
            //println("system initiated completely")
            //println("Child list created "+ children.length)
            //printChildNeighbourList()

            // Sleeping for 2 seconds so we have enough time for all the child nodes to be initiated properly
            Thread sleep 2000

            t = System.currentTimeMillis
            self ! StartSystem(algorithm)
        }
        case StartSystem(algorithm: String) => {
            algorithm match {
                case "gossip" => {
                    Future {
                        while(true) {
                            if(this.children.length > 0){
                                this.children(0) ! Rumour(rumour,true)
                            }
                            Thread sleep ticker
                        }
                    }
                }
                case "push-sum" => {
                    if(this.children.length > 0){
                        this.children(0) ! PushSum(0,0)
                    }
                }
                case _ => 
            }
        }

        case GossipSuccess => {
            //println("Gossip propogated across network successfully ........")
            self ! StopSystem
        }
        case PushSumSuccess(state: Double) => {
            //println("Push sum calculated across network successfully -- " + state.toString)
            self ! StopSystem
        } 
        case StopSystem => {
            //println("Total time taken: " + ((System.currentTimeMillis-t).toDouble/1000.toDouble).toString + " seconds")
            println("OUTPUT :: " + algorithm + " : " + topology + " : " + numNodes.toString + " nodes : " + (System.currentTimeMillis-t).toString + " milliseconds")
            context.system.shutdown
        }
        case _ =>
    }
    class ChildActor(index: Int) extends Actor {
        var neighbours = Array.empty[ActorRef]
        var randNeighbourAdded = false
        val gossipLimit = 10
        var maxGossipCount = 0
        val rumourArray: mutable.Map[String, Int] = mutable.Map()
        var pushSumValue = (index+1).toDouble
        var pushSumWeight = 1.toDouble
        val random = new Random
        val q = new mutable.Queue[Double]
        addToQueue(pushSumValue/pushSumWeight)

        def addToQueue(a: Double) = {
            // hardcoded the size of the queue, used to determine convergence of push-sum algorithm
            if(this.q.length >= 4) {
                for(i <- 1 to this.q.length - 4) {
                    this.q.dequeue
                }
            }

            this.q += a
        }

        def isPushSumConverging = {
            var status = false
            var d = pow(10,-10)
            // Using the same queue length as above
            if(this.q.length >=4) {
                if((abs(this.q(1) - this.q(0)) < d) && (abs(this.q(2) - this.q(1)) < d) && (abs(this.q(3) - this.q(2)) < d)) {
                    status = true
                }
            }
            status
        }

        def receive = {
            case UpdateNeighbours(n: Array[ActorRef]) => {
                //println("here4")
                neighbours = neighbours ++ n
            }
            case AddRandNeighbours(n: Array[ActorRef]) => {
                if(!randNeighbourAdded) {
                    neighbours = neighbours ++ n
                    randNeighbourAdded = true
                }
            }
            case PrintNeighbours => {
                var output = ""
                output += "children of " + self.toString + "are: \n"
                for( i <- 0 to neighbours.length-1) {
                    output += "\t-- " + neighbours(i).toString + "\n"
                }
                println(output)
            }
            case Rumour(rumour: String, start: Boolean) => {
                if(!start) {
                    if(rumourArray contains rumour) {
                        rumourArray(rumour) += 1
                        maxGossipCount = max(maxGossipCount,rumourArray(rumour))
                    } else {
                        rumourArray(rumour) = 1
                        maxGossipCount = max(maxGossipCount,1)
                    }    
                }
                //println(self.toString + " received the message: Gossip count is " + maxGossipCount)
                if (maxGossipCount >= gossipLimit) {
                    //println("here1")
                    context.parent ! GossipSuccess
                } else {
                    //println("here2")
                    // pick a random neighbour and pass message
                    if(neighbours.length > 0)
                    {
                    //println("here3")
                        neighbours(random.nextInt(neighbours.length)) ! Rumour(rumour, false)
                    }
                }
            }
            case PushSum(value: Double, weight: Double) => {
                if(isPushSumConverging) {
                    context.parent ! PushSumSuccess(pushSumValue/pushSumWeight)
                } else {
                    if(neighbours.length > 0)
                    {
                        pushSumValue = (value + pushSumValue)/(2.toDouble)
                        pushSumWeight = (weight + pushSumWeight)/(2.toDouble)
                        addToQueue(pushSumValue/pushSumWeight)
                        neighbours(random.nextInt(neighbours.length)) ! PushSum(pushSumValue,pushSumWeight)
                    }
                }
            }
            case _ =>
        }
    }
}

object Gossip extends App {
    //println(args(0))
    if(args.length >= 3) {
        val topology = args(1)
        val algorithm = args(2)
        var numNodes = 0
        if(topology == "3D" || topology == "imp3D") {
            numNodes = pow(ceil(cbrt(args(0).toInt)), 3).toInt
            //println("Nearest cube is " + numNodes.toString)
        } else {
            numNodes = args(0).toInt
        }
        //println(topology)
        val system = ActorSystem("NetworkTopology")
        val master = system.actorOf(Props(new MasterActor(numNodes, topology, algorithm)), name="master")
        master ! InitializeSystem
    } else {
        println("Not enough arguments")
    }
}
