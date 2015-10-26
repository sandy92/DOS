import akka.actor._

// calculating the hashes
import java.security._

// Needed to extract the response from the actor
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

// Needed for timeout
import scala.concurrent.duration._
import scala.language.postfixOps

import scala.collection.mutable._

// breaking out of the loops
import scala.util.control.Breaks._

import scala.util.Random

// Data types
case class Node(ref: ActorRef, id: Int)

// Actor Messages
case object PrintNode
case object GetPrintStatus

case object GetSuccessor
case class UpdateSuccessor(node: Node)

case object GetPredecessor
case class UpdatePredecessor(node: Node)
case class UpdatePredFT(node: Node)

case object UpdateOthers
case object GetFingerTable
case class UpdateFingerTable(f: Node)
case class UpdateFingerTableEntry(f: Node, i: Int)

case object PrepareNode
case object GetNodeStructure
case class GetCPF(id: Int)

case class AddNodeToRing(node: ActorRef)

// test cases
case object T1
case object T2
case object T3

// Utility objects which contains the common functions used across the program
object Utility {
    val fingerTableSize = 31
    val maxRingCount = Math.pow(2,fingerTableSize).toInt

    def getID(key: String) = {
        var hash = MessageDigest.getInstance("SHA-1").digest(key.getBytes).map(e => "%02x".format(e)).mkString
        hash = hash.substring(0,Math.ceil(fingerTableSize/8.0).toInt)  // Considering the first 31 bits for the chord ring
        var id = Integer.parseInt(hash,16)
        if(id < 0) {
            id = id >>> 1
        }
        id
    }

    def getNodeID(node: ActorRef) = {
        this.getID(node.path.toString)
    }

    def ringPosition(id: Long) = {
        var result = id
        if(id < 0) {
            while(result < 0) {
                result = result + maxRingCount.toLong
            }
            result.toInt
        } else {
            while(result >= 0) {
                result = result - maxRingCount.toLong
            }
            (result + maxRingCount).toInt
        }
    }

    def isInRange(id: Long, left: Long, right: Long) = {
        var tempID = this.ringPosition(id)
        var tempLeft = this.ringPosition(left+1)
        var tempRight = this.ringPosition(right-1)
        if (tempLeft <= tempRight) {
            ((tempID >= tempLeft) && (tempID <= tempRight))
        } else {
            ((tempID >= tempLeft) || (tempID <= tempRight))
        }
    }

    def isInLeftIncRange(id: Long, left: Long, right: Long) = {
        (this.ringPosition(id) == this.ringPosition(left)) || this.isInRange(id,left,right)
    }

    def isInRightIncRange(id: Long, left: Long, right: Long) = {
        (this.ringPosition(id) == this.ringPosition(right)) || this.isInRange(id,left,right)
    }
}

class ChordNode extends Actor {
    implicit val t = Timeout(3 seconds)
    val id = Utility.getNodeID(self)
    val messages = ArrayBuffer.empty[String]
    val fingerTable = ArrayBuffer.empty[Node]
    val addRequestQueue = Queue.empty[ActorRef]
    val m = Utility.fingerTableSize
    var pred = Node(self,this.id)
    var isRingPrintable = true

    val nodeStructure = Node(self,this.id)

    for( i <- 0 to m-1 ) {
        fingerTable += this.nodeStructure
    }

    def succ = {
        this.fingerTable(0)
    }

    def closestPrecedingFinger(id: Int) = {
        var temp = 0
        var cpf = this.nodeStructure

        breakable {
            for( i <- m-1 to 0 by -1) {
                temp = fingerTable(i).id
                if(Utility.isInRange(temp,this.id,id)) {
                    cpf = fingerTable(i)
                    break
                }
            }
        }
        cpf
    }

    def findPredecessor(id: Int) = {
        var tempNode = this.nodeStructure
        var ts = this.nodeStructure

        breakable {
            while(true) {
                var f = tempNode.ref ? GetSuccessor
                var result = Await.result(f, t.duration).asInstanceOf[Node]
                var succNode = result

                if(Utility.isInRightIncRange(id, tempNode.id, succNode.id)) {
                    break
                }

                var f1 = tempNode.ref ? GetCPF(id)
                ts = Await.result(f1, t.duration).asInstanceOf[Node]
                tempNode = ts
                if(tempNode == this.nodeStructure) {
                    break
                }
            }
        }
        tempNode
    }

    def findSuccessor(id: Int)  = {
        var tempNode = this.findPredecessor(id).ref
        var f = tempNode ? GetSuccessor
        var result = Await.result(f, t.duration).asInstanceOf[Node]
        result
    }

    def receive = {
        case PrepareNode => {
            Future {
                while(true) {
                    if(this.addRequestQueue.isEmpty) {
                        this.isRingPrintable = true
                    } else {
                        var node = this.addRequestQueue.dequeue
                        val nodeID = Utility.getNodeID(node)
                        val nodePred = this.findPredecessor(nodeID)

                        var f1 = nodePred.ref ? GetSuccessor
                        var r1 = Await.result(f1, t.duration).asInstanceOf[Node]
                        var nodeSucc = r1

                        nodeSucc.ref ! UpdatePredecessor(Node(node,nodeID))

                        node ! UpdateSuccessor(nodeSucc)
                        node ! UpdatePredecessor(nodePred)

                        nodePred.ref ! UpdatePredFT(Node(node,nodeID))
                    }
                    Thread sleep 3000
                }
            }
        }

        case PrintNode => {
            sender ! this.succ.ref
        }

        case GetPrintStatus => {
            sender ! this.isRingPrintable
        }

        case GetSuccessor => {
            sender ! this.succ
        }

        case UpdateSuccessor(node: Node) => {
            this.fingerTable(0) = node
        }

        case GetPredecessor => {
            sender ! this.pred
        }

        case UpdatePredecessor(node: Node) => {
            this.pred = node
        }

        case UpdatePredFT(r: Node) => {
            Future {
                var succNode = this.succ
                breakable {
                    for( i <- 0 to m-1) {
                        if(Utility.isInRightIncRange(this.id.toLong + Math.pow(2,i).toLong,this.id, r.id)) {
                            this.fingerTable(i) = r
                        } else {
                            break
                        }
                    }
                }

                var f2 = r.ref ? GetFingerTable
                var r2 = Await.result(f2, t.duration).asInstanceOf[ArrayBuffer[Node]]
                val ft = r2

                var k = 0
                breakable {
                    for( i <- 1 to m-1) {
                        if(Utility.isInRightIncRange(r.id.toLong + Math.pow(2,i).toLong,r.id, succNode.id)) {
                            ft(i) = succNode
                            k = i
                        } else {
                            break
                        }
                    }
                }

                for( i <- k to m-2) {
                    if(Utility.isInRightIncRange((r.id.toLong + Math.pow(2,i+1).toLong), (r.id.toLong + Math.pow(2,i).toLong), ft(i).id)) {
                        ft(i+1) = ft(i)
                    } else {
                        ft(i+1) = this.findSuccessor(Utility.ringPosition(r.id.toLong + Math.pow(2,i+1).toLong))
                    }
                }

                r.ref ! UpdateOthers
            }
        }

        case UpdateOthers => {
            Future {
                var p = this.pred
                val q = Queue.empty[Node]
                breakable {
                    for( i <- 0 to m-1) {
                        p = this.findPredecessor(Utility.ringPosition(this.id.toLong - Math.pow(2,i).toLong))
                        if(p.ref != self) {
                            if(!q.contains(p)) {
                                q += p
                            }
                        }
                    }
                    while(!q.isEmpty) {
                        p = q.dequeue
                        p.ref ! UpdateFingerTable(this.nodeStructure)
                    }
                }
                Thread sleep 2000
            }
        }

        case GetFingerTable => {
            sender ! this.fingerTable
        }

        case UpdateFingerTable(f: Node) => {
            Future {
                for( i <- 0 to m-1) {
                    if(Utility.isInLeftIncRange(f.id, (this.id.toLong + Math.pow(2,i).toLong), this.fingerTable(i).id)) {
                        this.fingerTable(i) = f
                    } else {
                    }
                }
            }
        }

        case UpdateFingerTableEntry(f: Node, i: Int) => {
            this.fingerTable(i) = f
        }

        case GetNodeStructure => {
            sender ! this.nodeStructure
        }

        case GetCPF(id: Int) => {
            val fSender = sender
            Future {
                fSender ! this.closestPrecedingFinger(id)
            }
        }

        case AddNodeToRing(node: ActorRef) => {
            this.addRequestQueue += node
            this.isRingPrintable = false
        }

        case T1 => {
            //println("FT:" + self.path.name + " - " + this.fingerTable)
        }

        case T2 => {
            ////println(this.ringNodeStructure)
        }

        case T3 => {
            //println(this.test.toString)
        }

        case _ =>
    }
}

object Chord extends App {
    if(args.length >= 1) {
        val numNodes = args(0).toInt
        val numRequests = args(1).toInt

        if(numNodes < 1) {
            println("The number of nodes should be greater than 0")
        } else {
            val system = ActorSystem("ChordRing")
            val n1 = system.actorOf(Props[ChordNode], name="node1")

            n1 ! PrepareNode

            for(i <- 2 to numNodes) {
                var temp = system.actorOf(Props[ChordNode], name="node"+i.toString)
                n1 ! AddNodeToRing(temp)
            }

            Thread sleep 20000

            printChordRing(n1)
            system.shutdown
        }
    } else {
       println("Not enough arguments")
    }

    def printChordRing(n: ActorRef) = {
        implicit val t = Timeout(3 seconds)
        var status = false
        breakable {
            do {
                val f = n ? GetPrintStatus
                val result = Await.result(f, t.duration).asInstanceOf[Boolean] // should handle the case where the we have a timeout
                status = result
                Thread sleep 3000
            } while (!status)
        }

        var temp = Node(n,Utility.getNodeID(n))
        var result = temp
        val lb = ListBuffer.empty[Node]
        breakable {
            while(true) {
                lb += temp
                println(temp)
                var f = temp.ref ? GetSuccessor
                result = Await.result(f, t.duration).asInstanceOf[Node] // should handle the case where the we have a timeout
                temp = result
                if(lb.contains(temp)) {
                    break
                }
            }
        }
        println("done")
    }
}
