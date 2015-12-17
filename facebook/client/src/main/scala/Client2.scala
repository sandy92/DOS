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
  for( i <- 0 to 0) {
    n = system.actorOf(Props[ClientActor],name=i.toString)
    Data.userRefs += n
    n ! StartActor
  }

  // Data.userRefs(0) ! SimulateSecurePost
  // Data.userRefs(0) ! SimulateGetSecurePost
  // Data.userRefs(0) ! SimulateSecurePhoto
  Data.userRefs(0) ! SimulateGetSecurePhoto
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

      pipeline(Put("http://localhost:8080/user/"+userID+"/posts", FormData(Map("message" -> em, "postedBy" -> userID, "accessList" -> accessList.toJson.toString)))) onComplete {
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
    println("calling simulateGetSecurePost on " + self.path.toString)
    val postID = "5145034240515909"

    pipeline(Get("http://localhost:8080/post/"+postID+"?requestedBy="+userID)) onComplete {
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
            println(Crypto.aes.decrypt(encryptedMessage,key,temp(0)))
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
        println("Message: Unable to create a post")
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
        println("Message: Unable to create a post")
    }
  }



  /*def simulatePostOnFriendsWall = {
    // Post on friends wall
    println("calling simulatePostOnFriendsWall on " + self.path.toString)
    if(!friends.isEmpty) {
      val i = scala.util.Random.nextInt(friends.length)

      pipeline(Put("http://localhost:8080/user/"+(friends(i).id)+"/posts", FormData(Map("message" -> randomString(50),"postedBy" -> userID)))) onComplete {
        case Success(r) =>
          val x = r.entity.asString.parseJson.convertTo[Map[String,String]]
          val message = x.get("message").getOrElse("")
          if(!message.isEmpty) {
            println("Success: " + message.toString)
          } else {
            println("Message:" + x.get("error").getOrElse(""))
          }
        case Failure(e) =>
          println("Message: Unable to post on friend's wall")
      }
    }
  }*/

  /*// var userID: String = "1144892814719449"
  var userID: String = ""
  var pageID: String = ""

  val albums = scala.collection.mutable.HashMap.empty[String,String]

  val friends = scala.collection.mutable.ArrayBuffer.empty[Client]

  // albums += ("" -> "3144885581836637")

  

  

  def formHeaders(params: (String, String)*) =
    Seq(HttpHeaders.`Content-Disposition`("form-data", Map(params: _*)))

  def getClientData = Client(userID,self)

  def simulateCreateClient = {
    // Creating the new user
    println("calling simulateCreateClient on " + self.path.toString)
    if(userID.isEmpty) {
      pipeline(Put("http://localhost:8080/user/create",FormData(Map("name" -> randomString(10),"email" -> randomString(10),"age" -> (18 + Random.nextInt(20)).toString)))) onComplete {
        case Success(r) =>
          val x = r.entity.asString.parseJson.convertTo[Map[String,String]]
          val id = x.get("id").getOrElse("")
          if(!id.isEmpty) {
            userID = id
            Data.users += (id -> self)
          } else {
            println("Message:" + x.get("error").getOrElse(""))
          }
        case Failure(e) =>
          println("Message: Unable to create user")
      }
    }
  }

  def simulateCreateClientAlbum = {
    // Create Album
    println("calling simulateCreateClientAlbum on " + self.path.toString)
    val albumName = randomString(10)
    pipeline(Put("http://localhost:8080/album/create",FormData(Map("name" -> albumName,"profileID" -> userID)))) onComplete {
      case Success(r) =>
        val x = r.entity.asString.parseJson.convertTo[Map[String,String]]
        val id = x.get("id").getOrElse("")
        if(!id.isEmpty) {
          albums += (albumName -> id)
        } else {
          println("Message:" + x.get("error").getOrElse(""))
        }
      case Failure(e) =>
        println("Message: Unable to create album")
    }
  }

  def simulateGetClientAlbums = {
    // Get Albums
    println("calling simulateGetClientAlbums on " + self.path.toString)
    pipeline(Get("http://localhost:8080/user/"+userID+"/albums")) onComplete {
      case Success(r) =>
        val x = r.entity.asString.parseJson.asJsObject
        val e = x.fields.get("error")
        if(e.isDefined) {
          println("Message: " + e.get.toString)
        } else {
          val total = x.fields.get("total")
          if(total.isDefined) {
            val m = x.fields.get("albums").get.convertTo[Set[JsValue]].filter(_ != JsNull).map(_.convertTo[Map[String,String]])
            m.foreach(e => albums += (e.get("name").getOrElse("") -> e.get("albumID").getOrElse("")))
          } else {
            println("Message: Invalid Json format")
          }
        }
      case Failure(e) =>
        println("Message: Unable to get the album list")
    }
  }

  def simulateGetClientPhotos = {
    // Get Photos
    println("calling simulateGetClientPhotos on " + self.path.toString)
    if(!albums.isEmpty) {
      val v = albums.values.toArray
      val i = scala.util.Random.nextInt(v.length)
      pipeline(Get("http://localhost:8080/album/"+v(i)+"/photos")) onComplete {
        case Success(r) =>
          val x = r.entity.asString.parseJson.asJsObject
          val e = x.fields.get("error")
          if(e.isDefined) {
            println("Message: " + e.get.toString)
          } else {
            val total = x.fields.get("total")
            if(total.isDefined) {
              println("Total photos found: " + total.getOrElse("0").toString)
            } else {
              println("Message: Invalid Json format")
            }
          }
        case Failure(e) =>
          println("Message: Unable to get the posts")
       }
    }
  }

  def simulateUploadClientPhotos = {
    // upload photos
    println("calling simulateUploadClientPhotos on " + self.path.toString)
    if(!albums.isEmpty) {
      val v = albums.values.toArray
      val i = scala.util.Random.nextInt(v.length)

      val file = new File("client/src/main/resources/facebook-logo.jpg").getCanonicalPath()
      val bis = new BufferedInputStream(new FileInputStream(file))
      val bArray = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray

      val httpData = HttpData(bArray)
      val httpEntity = HttpEntity(MediaTypes.`image/jpeg`, httpData).asInstanceOf[HttpEntity.NonEmpty]
      val formFile = FormFile("image", httpEntity)

      val formData = MultipartFormData(Seq(
        BodyPart(randomString(10), formHeaders("name" -> "name")),
        BodyPart(userID, formHeaders("name" -> "profileID")),
        BodyPart(v(i), formHeaders("name" -> "albumID")),
        BodyPart(formFile, "image")
      ))

      pipeline(Put("http://localhost:8080/photo/upload", formData)) onComplete {
        case Success(r) =>
          val x = r.entity.asString.parseJson.convertTo[Map[String,String]]
          val id = x.get("id").getOrElse("")
          if(!id.isEmpty) {
            println("Photo uploaded successfully - id - "+ id.toString)
          } else {
            println("Message:" + x.get("error").getOrElse(""))
          }
        case Failure(e) =>
          println("Message: Unable to upload the photo")
      }
    }
  }

  def simulatePostOnFriendsWall = {
    // Post on friends wall
    println("calling simulatePostOnFriendsWall on " + self.path.toString)
    if(!friends.isEmpty) {
      val i = scala.util.Random.nextInt(friends.length)

      pipeline(Put("http://localhost:8080/user/"+(friends(i).id)+"/posts", FormData(Map("message" -> randomString(50),"postedBy" -> userID)))) onComplete {
        case Success(r) =>
          val x = r.entity.asString.parseJson.convertTo[Map[String,String]]
          val message = x.get("message").getOrElse("")
          if(!message.isEmpty) {
            println("Success: " + message.toString)
          } else {
            println("Message:" + x.get("error").getOrElse(""))
          }
        case Failure(e) =>
          println("Message: Unable to post on friend's wall")
      }
    }
  }

  def simulateGetPosts = {
    // Get Albums
    println("calling simulateGetPosts on " + self.path.toString)
    pipeline(Get("http://localhost:8080/user/"+userID+"/posts")) onComplete {
      case Success(r) =>
        val x = r.entity.asString.parseJson.asJsObject
        val e = x.fields.get("error")
        if(e.isDefined) {
          println("Message: " + e.get.toString)
        } else {
          val total = x.fields.get("total")
          if(total.isDefined) {
            println("Total posts found: " + total.getOrElse("0").toString)
          } else {
            println("Message: Invalid Json format")
          }
        }
      case Failure(e) =>
        println("Message: Unable to get the post list")
    }
  }*/

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