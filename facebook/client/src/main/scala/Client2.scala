import akka.actor._
import java.io._
import scala.util._
import scala.concurrent._
import scala.concurrent.duration._

import akka.pattern.ask
import akka.util.Timeout

import spray.http._
import spray.client.pipelining._
import spray.httpx._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import spray.json._
import spray.routing._
import spray.httpx.SprayJsonSupport._
import MediaTypes._

import java.security._
import java.security.spec._
import javax.crypto._
import javax.crypto.spec._

case object StartActor
case object StopSystem
case object SimulateSecurePost
case object SimulateGetSecurePost
case object SimulateSecurePhoto
case object SimulateGetSecurePhoto

// case class Client(id: String, ref: ActorRef, publicKey: String)
case class Client(id: String, publicKey: PublicKey)
case class AddtoFriendList(c: Client)

object Client2 extends App {
  implicit val system = ActorSystem("api-client")
  import system.dispatcher // execution context for futures
  
  var n:ActorRef = _
  for( i <- 0 to 9) {
    n = system.actorOf(Props[ClientActor],name=i.toString)
    Data.userRefs += n
    n ! StartActor
  }

  // Data.userRefs(0) ! SimulateSecurePost
  Data.userRefs(0) ! SimulateGetSecurePost
  // Data.userRefs(0) ! SimulateSecurePhoto
  // Data.userRefs(0) ! SimulateGetSecurePhoto


  Data.userRefs(1) ! SimulateGetSecurePost
  Data.userRefs(3) ! SimulateGetSecurePost
  Data.userRefs(4) ! SimulateGetSecurePost
  Data.userRefs(6) ! SimulateGetSecurePost
  Data.userRefs(7) ! SimulateGetSecurePost
}

class ClientActor extends Actor {
  import MyJsonProtocol._

  implicit val system = context.system
  import system.dispatcher
  import system.log

  private val privateKey = Crypto.rsa.decodePrivateKey(scala.io.Source.fromInputStream(getClass.getResourceAsStream("/keys/"+self.path.name + ".priv")).getLines.mkString)
  val publicKey = Crypto.rsa.decodePublicKey(scala.io.Source.fromInputStream(getClass.getResourceAsStream("/keys/"+self.path.name + ".pub")).getLines.mkString)

  Future {
    Thread sleep 4000
    system.shutdown
  }

  val userID: String = Data.userList(self.path.name.toInt)
  val albums = scala.collection.mutable.HashMap.empty[String,String]
  albums += ("Photos" -> Data.albumList(self.path.name.toInt))

  val friends = scala.collection.mutable.ArrayBuffer.empty[Client]

  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  def randomString(length: Int): String = Random.alphanumeric.take(length).mkString

  def simulateGetFriendsList = {
    // Get Albums
    // println("calling simulateGetFriendsList on " + self.path.toString)
    // println(userID)
    pipeline(Get("http://localhost:8080/user/"+userID+"/friends")) onComplete {
      case Success(r) =>
        val x = r.entity.asString.parseJson.asJsObject
        val e = x.fields.get("error")
        if(e.isDefined) {
          println("Message: " + e.get.toString)
        } else {
          val total = x.fields.get("total")
          if(total.isDefined) {
            val m = x.fields.get("friends").get.convertTo[Set[JsValue]].filter(_ != JsNull).map(_.convertTo[Map[String,Either[String,Int]]])
            m.foreach(e => this.friends += extractFriendInfo(e,context))
            // println(friends)
          } else {
            println("Message: Invalid Json format")
          }
        }
      case Failure(e) =>
        println("Message: Unable to get the album list")
    }
  }

  def extractFriendInfo(m: Map[String,Either[String,Int]], c: ActorContext) = {
    val userID = m.get("userID").get.left.toOption.get
    val publicKey = Crypto.rsa.decodePublicKey(m.get("publicKey").get.left.toOption.get)

    Client(userID,publicKey)
  }

  def simulateSecurePost = {
    // Post on friends wall
    println("calling simulateSecurePost on " + self.path.toString)
    if(!friends.isEmpty) {
      val a = scala.collection.mutable.ArrayBuffer.empty[Int]
      a += scala.util.Random.nextInt(friends.length)
      a += scala.util.Random.nextInt(friends.length)
      a += scala.util.Random.nextInt(friends.length)
      val b = a.distinct
      var m = "Testing secure post"
      var key = Crypto.aes.generateSecretKey
      var initVector = randomString(16)
      val em = Crypto.aes.encrypt(m,key,initVector)
      val accessList = Map(userID -> ((initVector)+"~~~~~~"+Crypto.rsa.encrypt(Crypto.aes.encodeKey(key),this.publicKey))) ++ b.map(e => friends(e).id -> ((initVector)+"~~~~~~"+Crypto.rsa.encrypt(Crypto.aes.encodeKey(key),friends(e).publicKey))).toMap

      // Erasing data from memory
      m = null
      key = null
      //iv = null

      val s = "Posted '"+em+"' on '"+userID+"' by '"+userID+"' for '"+accessList.toJson.toString+"'"
      val md = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"))
      val hash = md.map("%02x".format(_)).mkString
      val signature = Crypto.rsa.encrypt(hash,this.privateKey)

      pipeline(Put("http://localhost:8080/user/"+userID+"/posts", FormData(Map("message" -> em, "postedBy" -> userID, "accessList" -> accessList.toJson.toString, "signature" -> signature)))) onComplete {
        case Success(r) =>
          val x = r.entity.asString.parseJson.convertTo[Map[String,String]]
          val id = x.get("id").getOrElse("")
          if(!id.isEmpty) {
            println("Post Created Successfully - id: " + id.toString)
          } else {
            println("Error:" + x.get("error").getOrElse(""))
          }
        case Failure(e) =>
          println("Message: Unable to create a post")
      }
    }
  }


  def simulateGetSecurePost = {
    // Post on friends wall
    // println("calling simulateGetSecurePost on " + self.path.toString)
    val postID = "5145034240515909"

    val s = "'"+userID+"' requested post with id '"+postID+"'"
    val md = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"))
    val hash = md.map("%02x".format(_)).mkString
    val signature = Crypto.rsa.encrypt(hash,this.privateKey)

    pipeline(Get("http://localhost:8080/post/"+postID,FormData(Map("requestedBy"->userID, "signature"->signature)))) onComplete {
      case Success(r) =>
        val x = r.entity.asString.parseJson.asJsObject
        val e = x.fields.get("error")
        if(e.isDefined) {
          println("Message: " + e.get.toString)
        } else {
          val encryptedMessage = x.fields.get("message").getOrElse("").toString.replaceAll("^\"|\"$", "")
          val encryptedKey = x.fields.get("key").getOrElse("").toString.replaceAll("^\"|\"$", "")
          if(!encryptedMessage.isEmpty && !encryptedKey.isEmpty) {
            var temp = encryptedKey.split("~~~~~~")
            var t1 = temp(1).replace("\\n","\n")
            var k = Crypto.rsa.decrypt(t1,this.privateKey)
            var key = Crypto.aes.decodeKey(k)
            println("--------")
            println("calling simulateGetSecurePost on " + self.path.toString + " -- message: " + Crypto.aes.decrypt(encryptedMessage,key,temp(0)))
            println("--------")
            temp = null
            t1 = null
            k = null
            key = null
          } else {
            println("Error: Bad data format")
          }
        }
      case Failure(e) =>
        println("Message: Unable to get the post")
    }
  }

  def simulateSecurePhoto = {
    // upload photos
    println("calling simulateSecurePhoto on " + self.path.toString)
    if(!friends.isEmpty) {
      val a = scala.collection.mutable.ArrayBuffer.empty[Int]
      a += scala.util.Random.nextInt(friends.length)
      a += scala.util.Random.nextInt(friends.length)
      a += scala.util.Random.nextInt(friends.length)
      val b = a.distinct

      val file = new File("client/src/main/resources/facebook-logo.jpg").getCanonicalPath()
      val bis = new BufferedInputStream(new FileInputStream(file))
      val bArray = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray

      var m = new sun.misc.BASE64Encoder().encode(bArray)
      var key = Crypto.aes.generateSecretKey
      var initVector = randomString(16)
      val em = Crypto.aes.encrypt(m,key,initVector)
      val accessList = Map(userID -> ((initVector)+"~~~~~~"+Crypto.rsa.encrypt(Crypto.aes.encodeKey(key),this.publicKey))) ++ b.map(e => friends(e).id -> ((initVector)+"~~~~~~"+Crypto.rsa.encrypt(Crypto.aes.encodeKey(key),friends(e).publicKey))).toMap

      // Erasing data from memory
      m = null
      key = null

      val formData = FormData(Map("name" -> randomString(10), "profileID" -> userID, "albumID" -> albums.get("Photos").get, "image" -> em, "accessList" -> accessList.toJson.toString))

      pipeline(Put("http://localhost:8080/photo/upload", formData)) onComplete {
        case Success(r) =>
          val x = r.entity.asString.parseJson.convertTo[Map[String,String]]
          val id = x.get("id").getOrElse("")
          if(!id.isEmpty) {
            println("Photo Uploaded Successfully - id: " + id.toString)
          } else {
            println("Error:" + x.get("error").getOrElse(""))
          }
        case Failure(e) =>
          println("Message: Unable to upload the photo")
      }
    }
  }

  def simulateGetSecurePhoto = {
    // upload photos
    println("calling simulateGetSecurePhoto on " + self.path.toString)
    val photoID = "4145034630696255"

    pipeline(Get("http://localhost:8080/photo/"+photoID+"?requestedBy="+userID)) onComplete {
      case Success(r) =>
        val x = r.entity.asString.parseJson.asJsObject
        val e = x.fields.get("error")
        if(e.isDefined) {
          println("Message: " + e.get.toString)
        } else {
          val encryptedData = x.fields.get("data").getOrElse("").toString.replaceAll("^\"|\"$", "")
          val encryptedKey = x.fields.get("key").getOrElse("").toString.replaceAll("^\"|\"$", "")
          if(!encryptedData.isEmpty && !encryptedKey.isEmpty) {
            var temp = encryptedKey.split("~~~~~~")
            var t1 = temp(1).replace("\\n","\n")
            var k = Crypto.rsa.decrypt(t1,this.privateKey)
            var key = Crypto.aes.decodeKey(k)
            println("--------")
            // println(key)
            val b = Crypto.aes.decrypt(encryptedData,key,temp(0))
            var m = new sun.misc.BASE64Encoder().encode(b.getBytes)
            println(b)
            println("--------")
            temp = null
            t1 = null
            k = null
            key = null
          } else {
            println("Error: Bad data format")
          }
        }
      case Failure(e) =>
        println("Message: Unable to get the photo")
    }
  }

  def receive = {
    case StartActor => {
      simulateGetFriendsList
      Thread sleep 2000
    }
    case SimulateSecurePost => {
      simulateSecurePost
    }
    case SimulateGetSecurePost => simulateGetSecurePost
    case SimulateSecurePhoto => simulateSecurePhoto
    case SimulateGetSecurePhoto => simulateGetSecurePhoto
    // case AddtoFriendList(c: Client) => addtoFriendList(c)
    case StopSystem => system.shutdown
    case _ => println(self)
  }
}

object Data {
  val userList = List("1144893542420086","1144893540411542","1144893541816256","1144893540919886","1144893543422787","1144893544556242","1144893540356447","1144893544627809","1144893541841666","1144893540905395")
  val albumList = List("3145022849447309","3145022850757946","3145022851983720","3145022853298873","3145022854293602","3145022855825357","3145022856734044","3145022857958772","3145022858740567","3145022859850649")
  val users = scala.collection.mutable.HashMap.empty[String, ActorRef]
  val userRefs = scala.collection.mutable.ArrayBuffer.empty[ActorRef]

  /*def getRandomUser = {
    val v = users.keys.toArray
    if(v.length > 0) {
      val i = scala.util.Random.nextInt(v.length)
      Some(Client(v(i), users(v(i))))
    } else {
      None
    }
  }*/
}

/*
sadd friends:user:1144893542420086 user:1144893540411542 user:1144893540919886 user:1144893543422787 user:1144893540356447 user:1144893544627809
sadd friends:user:1144893540411542 user:1144893542420086 user:1144893541816256 user:1144893544556242 user:1144893544627809 user:1144893541841666
sadd friends:user:1144893541816256 user:1144893540411542 user:1144893540919886 user:1144893543422787 user:1144893541841666 user:1144893540905395
sadd friends:user:1144893540919886 user:1144893542420086 user:1144893541816256 user:1144893540356447 user:1144893541841666 user:1144893540905395
sadd friends:user:1144893543422787 user:1144893542420086 user:1144893541816256 user:1144893544556242 user:1144893544627809 user:1144893541841666
sadd friends:user:1144893544556242 user:1144893540411542 user:1144893543422787 user:1144893540356447 user:1144893544627809 user:1144893540905395
sadd friends:user:1144893540356447 user:1144893542420086 user:1144893540919886 user:1144893544556242 user:1144893541841666 user:1144893540905395
sadd friends:user:1144893544627809 user:1144893542420086 user:1144893540411542 user:1144893543422787 user:1144893544556242 user:1144893540905395
sadd friends:user:1144893541841666 user:1144893540411542 user:1144893541816256 user:1144893540919886 user:1144893543422787 user:1144893540356447
sadd friends:user:1144893540905395 user:1144893541816256 user:1144893540919886 user:1144893544556242 user:1144893540356447 user:1144893540905395
*/

/*
1144893542420086 - 3145022849447309
1144893540411542 - 3145022850757946
1144893541816256 - 3145022851983720
1144893540919886 - 3145022853298873
1144893543422787 - 3145022854293602
1144893544556242 - 3145022855825357
1144893540356447 - 3145022856734044
1144893544627809 - 3145022857958772
1144893541841666 - 3145022858740567
1144893540905395 - 3145022859850649
*/