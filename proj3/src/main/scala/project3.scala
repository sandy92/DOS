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
case class RingNode(self: Node , pred: Node, succ: Node)

// Actor Messages
case object PrintNode
case object GetPrintStatus

case object GetSuccessor
case class UpdateSuccessor(node: RingNode)

case object GetPredecessor
case class UpdatePredecessor(node: Node)

case object UpdateOthers
case object GetFingerTable
case class UpdateFingerTable(ft: ArrayBuffer[RingNode])
case class UpdateFingerTableEntry(f: RingNode, i: Int)

case class Notify(node: Node)
case object PrepareNode
case object StartStabilization
case object GetNodeStructure
case object GetRingNodeStructure
case class GetCPF(id: Int)

case object T1
case object T2
case object T3

//case class NodeStructure(node: ActorRef, id: Int, pred: ActorRef, succ: ActorRef)

case class AddNodeToRing(node: ActorRef)

// Utility objects which contains the common functions used across the program
object Utility {
    val fingerTableSize = 31
    val maxRingCount = Math.pow(2,31).toInt

    def getID(key: String) = {
        var hash = MessageDigest.getInstance("SHA-1").digest(key.getBytes).map(e => "%02x".format(e)).mkString
        hash = hash.substring(0,Math.ceil(fingerTableSize/8.0).toInt)  // Considering the first 31 bits for the chord ring
        var id = Integer.parseInt(hash,16)
        if(id < 0) {
            id = id >>> 1
        }
        //println(key + " gave " + hash + " which resulted in " + id.toString)
        id
    }

    def getNodeID(node: ActorRef) = {
        //println(node.toString + " called me at " + System.currentTimeMillis().toString)
        this.getID(node.path.toString)
    }

    def ringPosition(id: Int) = {
        if(id < 0) {
            (id + maxRingCount) % maxRingCount
        } else {
            id % maxRingCount
        }
    }

    def isInRange(id: Int, left: Int, right: Int) = {
        var tempID = this.ringPosition(id)
        var tempLeft = this.ringPosition(left+1)
        var tempRight = this.ringPosition(right-1)
        if (tempLeft <= tempRight) {
            ((tempID >= tempLeft) && (tempID <= tempRight))
        } else {
            ((tempID >= tempLeft) || (tempID <= tempRight))
        }
    }

    def isInLeftIncRange(id: Int, left: Int, right: Int) = {
        (this.ringPosition(id) == this.ringPosition(left)) || this.isInRange(id,left,right)
    }

    def isInRightIncRange(id: Int, left: Int, right: Int) = {
        (this.ringPosition(id) == this.ringPosition(right)) || this.isInRange(id,left,right)
    }
}

class ChordNode extends Actor {
    implicit val t = Timeout(3 seconds)
    val id = Utility.getNodeID(self)
    val messages = ArrayBuffer.empty[String]
    val fingerTable = ArrayBuffer.empty[RingNode]
    val addRequestQueue = Queue.empty[ActorRef]
    val m = Utility.fingerTableSize
    var pred = Node(self,this.id)
    var isRingPrintable = true
    val test = ArrayBuffer.empty[Int] // remove
    test += 0 // remove

    val nodeStructure = Node(self,this.id)

    for( i <- 0 to m-1 ) {
        fingerTable += RingNode(this.nodeStructure, this.nodeStructure, this.nodeStructure)
    }

    def succ = {
        this.fingerTable(0)
    }

    def ringNodeStructure = {
        RingNode(this.nodeStructure, this.pred, this.succ.self)
    }

    def closestPrecedingFinger(id: Int) = {
        var temp = 0
        var cpf = this.ringNodeStructure
        // println("----")
        // println("inside cpf")

        breakable {
            for( i <- m-1 to 0 by -1) {
                //temp = Utility.getID(fingerTable(i).path.toString)
                // println("hereerere")
                temp = fingerTable(i).self.id
                // println(Utility.isInRange(temp,this.id,id).toString + " - " + temp.toString + " - " + this.id.toString + " - " + id.toString)
                if(Utility.isInRange(temp,this.id,id)) {
                    cpf = fingerTable(i)
                    //println("here") // remove
                    break
                }
            }
        }
        // println("outside cpf")
        // println("----")
        cpf
    }

    def findPredecessor(id: Int) = {
        //println("inside findPredecessor of "+ self.toString + " for an id of "+ id.toString)
        var tempNode = this.ringNodeStructure
        var ts = this.ringNodeStructure

/*        println(id)
        println(tempNode.self.id)
        println(tempNode.succ.id)
        println(Utility.isInRightIncRange(id, tempNode.self.id, tempNode.succ.id))*/

        //println(id, tempNode.self.id, tempNode.succ.id)
        //println(Utility.isInRightIncRange(id, tempNode.self.id, tempNode.succ.id))
        breakable {
            while(!Utility.isInRightIncRange(id, tempNode.self.id, tempNode.succ.id)) {
                //println("inside pred loop")
                var f = tempNode.self.ref ? GetCPF(id)
                ts = Await.result(f, t.duration).asInstanceOf[RingNode]
                tempNode = ts
                //println(tempNode.toString)
                if(tempNode.self == this.nodeStructure) {
                    break
                }
            }
        }
        tempNode
    }

    /*def findSuccessor(pred: ActorRef) = {
        var tempNode = pred
        var f = tempNode ? GetSuccessor
        var result = Await.result(f, t.duration).asInstanceOf[Node]
        result
    }*/

    def findSuccessor(id: Int)  = {
        //println("called findSuccessor on "+ self.toString + " for an id of "+ id.toString)
        var tempNode = this.findPredecessor(id).self.ref
        //println("the predecessor for " + id.toString + " is " + tempNode.toString)
        var f = tempNode ? GetSuccessor
        var result = Await.result(f, t.duration).asInstanceOf[RingNode]
        result
    }

    def receive = {
        case PrepareNode => {
            Future {
                while(true) {
                    if(this.addRequestQueue.isEmpty) {
                        //println("Nothing here, come back after some time")
                        this.isRingPrintable = true
                    } else {
                        //println(this.addRequestQueue.toString)
                        var node = this.addRequestQueue.dequeue
                        val nodeID = Utility.getNodeID(node)
                        val nodePred = this.findPredecessor(nodeID)
                        var nodeSucc = nodePred.succ

                        var f = nodeSucc.ref ? GetRingNodeStructure
                        var result = Await.result(f, t.duration).asInstanceOf[RingNode]
                        val n = result

                        node ! UpdateSuccessor(n)
                        node ! UpdatePredecessor(nodePred.self)
                        nodeSucc.ref ! UpdatePredecessor(Node(node,nodeID))
                        Thread sleep 1000 //remove

                        var f1 = node ? GetFingerTable
                        var r1 = Await.result(f1, t.duration).asInstanceOf[ArrayBuffer[RingNode]]
                        val ft = r1

                        for( i <- 0 to m-2) {
                            if(Utility.isInLeftIncRange((nodeID + Math.pow(2,i+1).toInt), this.id, (nodeID + Math.pow(2,i).toInt))) {
                                ft(i+1) = ft(i)
                                //println(ft(i+1).toString)
                            } else {
                                ft(i+1) = this.findSuccessor(nodeID + Math.pow(2,i+1).toInt)
                                //println(ft(i+1).toString)
                            }
                        }

                        //node ! UpdateFingerTable(ft)
                        node ! UpdateOthers
                        //println(node.toString)
                    }
                    Thread sleep 2000
                }
            }
        }
        case PrintNode => {
            println(self.path.toString + " -- " + this.id.toString)
            sender ! this.succ.self.ref
        }
        case GetPrintStatus => {
            sender ! this.isRingPrintable
        }
        case GetSuccessor => {
            sender ! this.succ
        }
        case UpdateSuccessor(node: RingNode) => {
            this.fingerTable(0) = node
            /*val fSender = sender
            Future {
                //println("-----")
                // println(this.succ.self.ref.toString)
                // println(node.self.ref.toString)
                //println(this.fingerTable(0).toString)

                //println(this.fingerTable(0).toString)
                // println(this.succ.self.ref.toString)
                //println("-----")
                //this.succ.ref ! Notify(this.nodeStructure)
            }*/
        }
        case GetPredecessor => {
            sender ! this.pred
            /*val fSender = sender
            Future {
                fS
            }*/
        }
        case UpdatePredecessor(node: Node) => {
            this.pred = node
            /*val fSender = sender
            Future {
                //println("-----")
                // println(this.succ.ref.toString)
                // println(node.ref.toString)
                //println(this.pred.toString)

                //println(this.pred.toString)
                // println(this.succ.ref.toString)
                //println("-----")
                //this.succ.ref ! Notify(this.nodeStructure)
            }*/
        }
        case UpdateOthers => {
            Future {
                var pred = this.ringNodeStructure
                breakable {
                    for( i <- 0 to m-1) {
                    //println("----")
                    //println(this.fingerTable.toString)
                    //println("----")
                    //for( i <- 0 to 0) {
                        //println(this.id.toString + " - " + (this.id - Math.pow(2,i).toInt).toString + " - " +pred.toString)
                        pred = this.findPredecessor(this.id - Math.pow(2,i).toInt)
                        pred.self.ref ! UpdateFingerTableEntry(this.ringNodeStructure,i)
                        //println(this.id.toString + " - " + (this.id - Math.pow(2,i).toInt).toString + " - " +pred.toString)
                        /*if(flag) {
                            pred = this.findPredecessor(this.id - Math.pow(2,i).toInt)
                            if(pred.self.ref  == self) {
                                flag = false
                            }
                        }*/
                    }
                }
            }
        }
        case GetFingerTable => {
            sender ! this.fingerTable
            /*val fSender = sender
            Future {
                println("------" + " for " + self.toString)
                for( i <- 0 to m-1) {
                    println(this.fingerTable(i).self.ref.path.toString + " - " + this.fingerTable(i).pred.ref.path.toString + " - " + this.fingerTable(i).succ.ref.path.toString)
                }
                println("------")
                fSender ! this.fingerTable
            }*/
        }
        case UpdateFingerTable(ft: ArrayBuffer[RingNode]) => {
            Future {
                for( i <- 0 to m-1) {
                    this.fingerTable(i) = ft(i)
                }
            }
        }
        case UpdateFingerTableEntry(f: RingNode, i: Int) => {
            Future {
                if(Utility.isInLeftIncRange(f.self.id, this.id, this.fingerTable(i).self.id)) {
                    this.fingerTable(i) = f
                    this.pred.ref ! UpdateFingerTableEntry(f, i)
                }
            }
        }
        case Notify(node: Node) => {
            val fSender = sender
            Future {
                // println("----")
                // println(node.ref.toString)
                // println(this.pred.ref.toString)
                if(this.pred.ref == self || Utility.isInRange(node.id, this.pred.id, this.id)) {
                    this.pred = node
                }
                // println(this.pred.ref.toString)
                // println(this.succ.ref.toString)
            }
        }
        case StartStabilization => {
            val fSender = sender
            Future {
                var f = this.succ.self.ref ? GetPredecessor
                var result = Await.result(f, t.duration).asInstanceOf[Node]
                var predNode = result
                if(Utility.isInRange(predNode.id, this.id, this.succ.self.id)) {
                    //this.succ = predNode
                }
                predNode.ref ! Notify(this.nodeStructure)
            }
        }
        case GetNodeStructure => {
            sender ! this.nodeStructure
            /*val fSender = sender
            Future {
                fSender ! this.nodeStructure
            }*/
        }
        case GetRingNodeStructure => {
            sender ! this.ringNodeStructure
            /*val fSender = sender
            Future {
                fSender ! this.ringNodeStructure
            }*/
        }
        case GetCPF(id: Int) => {
            val fSender = sender
            Future {
                fSender ! this.closestPrecedingFinger(id)
            }
        }
        /*case AddNodeToRing(node: ActorRef) => {
            println("here2 - " + System.currentTimeMillis.toString)
            Future {
                println("here3 - " + System.currentTimeMillis.toString)
                val f = node ? GetNodeStructure
                val result = Await.result(f, t.duration).asInstanceOf[Node]
                val n = result
                // println("cpf starts at " + System.currentTimeMillis())
                // var s = this.closestPrecedingFinger(n.id)
                // println("cpf ends at " + System.currentTimeMillis())
                //println(n.ref.toString)
                //println("here2")
                println("here4 - " + System.currentTimeMillis.toString)
                var succNode = this.findSuccessor(n.id)
                //var predNode = this.nodeStructure
                println("here - " + System.currentTimeMillis.toString)
                println(succNode.ref.toString)
                println("here5 - " + System.currentTimeMillis.toString)
                n.ref ! UpdateSuccessor(succNode)
                println("here6 - " + System.currentTimeMillis.toString)
                //println(succNode.ref.toString)
                //println("here3")

                //println("Success ? - " + n.ref.toString)

                // println("The new node is " + n.ref.toString)
                // println("The new node's pred is " + tempNode.self.ref.toString)
                // println("The new node's succ is " + tempNode.succ.ref.toString)


                /*val nodeID = Utility.getID(node.path.toString)
                val predNodeStructure = this.findPredecessor(nodeID)
                // val nodePred = this.findPredecessor(nodeID).node
                // val nodeSucc = this.findSuccessor(nodePred)
                val nodePred = predNodeStructure.node
                val nodeSucc = predNodeStructure.succ
                println("checking add")
                println(node)
                println(nodeID)
                println(nodePred)
                println(nodeSucc)
                // todo - check if the node id already exists in the ring*/
            }
        }*/
        case AddNodeToRing(node: ActorRef) => {
            this.addRequestQueue += node
            this.isRingPrintable = false
            //Future {
                // this.isRingPrintable = false
                //this.addRequestQueue += node
                // println(this.addRequestQueue.toString)
                //println("the node is " + node.toString)

                /*val nodeID = Utility.getNodeID(node)
                val nodePred = this.findPredecessor(nodeID)
                var nodeSucc = nodePred.succ

                var f = nodeSucc.ref ? GetRingNodeStructure
                var result = Await.result(f, t.duration).asInstanceOf[RingNode]
                val n = result

                //println(n.toString)

                node ! UpdateSuccessor(n)
                Thread sleep 1000 //remove
                node ! UpdatePredecessor(nodePred.self)
                Thread sleep 1000 //remove
                nodeSucc.ref ! UpdatePredecessor(Node(node,nodeID))

                Thread sleep 3000*/

                /*println("hereerere")
                println(this.ringNodeStructure.toString)
                var f1 = node ? GetRingNodeStructure
                var result1 = Await.result(f1, t.duration).asInstanceOf[RingNode]
                val n1 = result1
                println(n1.toString)*/

                /*var f1 = node ? GetFingerTable
                var r1 = Await.result(f1, t.duration).asInstanceOf[ArrayBuffer[RingNode]]
                val ft = r1*/

                //println(ft.toString)

                /*println("------" + " for old " + node.path.toString)
                for( i <- 0 to m-1) {
                    println(ft(i).self.ref.path.toString + " - " + ft(i).pred.ref.path.toString + " - " + ft(i).succ.ref.path.toString)
                }
                println("------")*/

                /*for( i <- 0 to m-2) {
                    if(Utility.isInLeftIncRange((nodeID + Math.pow(2,i+1).toInt), this.id, (nodeID + Math.pow(2,i).toInt))) {
                        ft(i+1) = ft(i)
                        //println(ft(i+1).toString)
                    } else {
                        ft(i+1) = this.findSuccessor(nodeID + Math.pow(2,i+1).toInt)
                        //println(ft(i+1).toString)
                    }
                }*/

                //node ! UpdateFingerTable(ft)
                //node ! UpdateOthers

                /*var f2 = node ? GetFingerTable
                var r2 = Await.result(f2, t.duration).asInstanceOf[ArrayBuffer[RingNode]]
                val ft2 = r2*/

                /*println("------" + " for new " + node.path.toString)
                for( i <- 0 to m-1) {
                    println(ft2(i).self.ref.path.toString + " - " + ft2(i).pred.ref.path.toString + " - " + ft2(i).succ.ref.path.toString)
                }
                println("------")*/

                /*println("------" + " for " + self.path.toString)
                for( i <- 0 to m-1) {
                    println(this.fingerTable(i).self.ref.path.toString + " - " + this.fingerTable(i).pred.ref.path.toString + " - " + this.fingerTable(i).succ.ref.path.toString)
                }
                println("------")*/
            //}
        }
        case T1 => {
            println(this.addRequestQueue.toString)
        }
        case T2 => {
            val fSender = sender
            Future {
                fSender ! this.test
            }
        }
        case T3 => {
            println(this.test.toString)
        }
        case _ =>
    }
}

object Chord extends App {
    //if(args.length >= 1) {
        //val numNodes = args(0).toInt
        //val numRequests = args(1).toInt

        val system = ActorSystem("ChordRing")
        val n1 = system.actorOf(Props[ChordNode], name="node1")
        val n2 = system.actorOf(Props[ChordNode], name="node2")
        val n3 = system.actorOf(Props[ChordNode], name="node3")
        val n4 = system.actorOf(Props[ChordNode], name="node4")
        val n5 = system.actorOf(Props[ChordNode], name="node5")
        val n6 = system.actorOf(Props[ChordNode], name="node6")

        /*println(n1.toString)
        println(n1.path.toString)
        println(Utility.getNodeID(n1))
        println(Utility.getID(n1.path.toString))


        println(n2.toString)
        println(n2.path.toString)
        println(Utility.getNodeID(n2))
        println(Utility.getID(n2.path.toString))*/

        //n1 ! T1
        n1 ! PrepareNode
        n1 ! AddNodeToRing(n2)
        n1 ! AddNodeToRing(n3)
        n1 ! AddNodeToRing(n5)
        n1 ! AddNodeToRing(n4)
        n1 ! AddNodeToRing(n6)
        //n1 ! T1
        /*Thread sleep 5000
        n1 ! StartStabilization
        Thread sleep 3000

        n1 ! AddNodeToRing(n2)
        Thread sleep 5000
        n1 ! StartStabilization
        Thread sleep 3000

        println("---")
        implicit val t = Timeout(3 seconds)
        var f1 = n1 ? GetRingNodeStructure
        var result1 = Await.result(f1, t.duration).asInstanceOf[RingNode]
        println(result1.self.ref.toString + " - " + result1.self.id.toString)
        println(result1.pred.ref.toString + " - " + result1.pred.id.toString)
        println(result1.succ.ref.toString + " - " + result1.succ.id.toString)
        println("---")
        var f2 = n3 ? GetRingNodeStructure
        var result2 = Await.result(f2, t.duration).asInstanceOf[RingNode]
        println(result2.self.ref.toString + " - " + result2.self.id.toString)
        println(result2.pred.ref.toString + " - " + result2.pred.id.toString)
        println(result2.succ.ref.toString + " - " + result2.succ.id.toString)
        println("---")
        var f3 = n2 ? GetRingNodeStructure
        var result3 = Await.result(f3, t.duration).asInstanceOf[RingNode]
        println(result3.self.ref.toString + " - " + result3.self.id.toString)
        println(result3.pred.ref.toString + " - " + result3.pred.id.toString)
        println(result3.succ.ref.toString + " - " + result3.succ.id.toString)*/

        /*n2 ! StartStabilization
        Thread sleep 3000
        n2 ! StartStabilization*/
        //n1 ! T1(n2)

     /*   implicit val timeout = Timeout(5 seconds)
        val future = n1 ? PrintNode
        val result = Await.result(future, timeout.duration).toString
        println(result)*/

        Thread sleep 5000
        printChordRing(n1)
        system.shutdown

        //printChordRing(n1)
        // remove

    //} else {
    //    println("Not enough arguments")
    //}

    def printChordRing(n: ActorRef) = {
        implicit val t = Timeout(3 seconds)
        var status = false
        breakable {
            // val f = n ? GetPrintStatus
            // val result = Await.result(f, t.duration).asInstanceOf[Boolean] // should handle the case where the we have a timeout
            // status = result
            // println(status)
            //Thread sleep 1000
            do {
                val f = n ? GetPrintStatus
                val result = Await.result(f, t.duration).asInstanceOf[Boolean] // should handle the case where the we have a timeout
                status = result
                // println("can't print yet")
                Thread sleep 1000
            } while (!status)
            // println("start printing")
        }
        var count = 0 // remove
        var temp = n
        var result = n
        val lb = ListBuffer.empty[ActorRef]
        breakable {
            while(true) {
                lb += temp
                var f = temp ? PrintNode
                result = Await.result(f, t.duration).asInstanceOf[ActorRef] // should handle the case where the we have a timeout
                count = count + 1
                temp = result
                if(lb.contains(temp)) {
                    break
                }
                //lb += temp
            }
            /*do {
                var f = temp ? PrintNode
                result = Await.result(f, t.duration).asInstanceOf[ActorRef] // should handle the case where the we have a timeout
                count = count + 1
                if(count > 10) {
                    break
                }
                temp = result
            } while(temp != n)*/
        }
        println("done")
        //var future = n ? PrintNode
        //val result = Await.result(future, timeout.duration).toString
    }
}
