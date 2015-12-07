import spray.http._
import spray.client.pipelining._
import scala.util._
import akka.actor._
import spray.httpx._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import scala.concurrent._
import scala.concurrent.duration._
import spray.json._
import spray.routing._
import spray.httpx.SprayJsonSupport._
import MediaTypes._
import java.io._

case object StartActor
case object StopSystem

case class Client(id: String, ref: ActorRef)
case class AddtoFriendList(c: Client)

object Client extends App {
  implicit val system = ActorSystem("api-client")
  import system.dispatcher // execution context for futures
  //import system.log

  //val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  //val response: Future[HttpResponse] = pipeline(Get("http://example.com"))

  Future {
    for( i <- 0 to 100) {
      system.actorOf(Props[ClientActor],name="c"+i.toString) ! StartActor
      Thread sleep 1000
    }
  }
}

class ClientActor extends Actor {
  import MyJsonProtocol._

  implicit val system = context.system
  import system.dispatcher
  import system.log

  // var userID: String = "1144892814719449"
  var userID: String = ""
  var pageID: String = ""

  val albums = scala.collection.mutable.HashMap.empty[String,String]

  val friends = scala.collection.mutable.ArrayBuffer.empty[Client]

  // albums += ("" -> "3144885581836637")

  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  def randomString(length: Int): String = Random.alphanumeric.take(length).mkString

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

  def simulateAddFriends = {
    // Add friends
    println("calling simulateAddFriends on " + self.path.toString)
    val f1 = Data.getRandomUser

    if(f1.isDefined) {
      val f = f1.get
      if(f.id != userID) {
        pipeline(Put("http://localhost:8080/user/"+(f.id)+"/friends", FormData(Map("requestedBy" -> userID)))) onComplete {
          case Success(r) =>
            val x = r.entity.asString.parseJson.convertTo[Map[String,String]]
            val message = x.get("message").getOrElse("")
            if(!message.isEmpty) {
              addtoFriendList(f)
              f.ref ! AddtoFriendList(getClientData)
              println("Success: " + message.toString)
            } else {
              println("Message:" + x.get("error").getOrElse(""))
            }
          case Failure(e) =>
            println("Message: Unable to add friends")
        }
      }
    }
  }

  def addtoFriendList(c: Client) = {
    if(!(friends contains c)) {
      friends += c
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
  }

  def receive = {
    case StartActor =>
      //pipeline(Put("http://localhost:8080/user/create",FormData(Seq("name" -> Random.nextString(10),"email" -> Random.nextString(10),"age" -> (18 + Random.nextInt(20)))))) onComplete {

      simulateCreateClient
      Thread sleep 1000

      Future {
        var count = 0
        system.scheduler.schedule(0 seconds,2000 + Random.nextInt(2000) milliseconds) {
          count = count + 1
          if(count >= 1000) {
            self ! StopSystem
          }
          if(!userID.isEmpty) {
            Random.nextInt(20) match {
              case 0 => simulateCreateClientAlbum
              case 1 => simulateGetClientAlbums
              case 2 => simulateGetClientAlbums
              case 3 => simulateGetClientPhotos
              case 4 => simulatePostOnFriendsWall
              case 5 => simulatePostOnFriendsWall
              case 6 => simulateAddFriends
              case 7 => simulateAddFriends
              case 8 => simulateUploadClientPhotos
              case 9 => simulateUploadClientPhotos
              case 10 => simulateGetClientPhotos
              case 11 => simulateGetClientPhotos
              case 12 => simulateGetPosts
              case 13 => simulateGetPosts
              case 14 => simulateGetPosts
              case 15 => simulateGetClientPhotos
              case 16 => simulateGetPosts
              case 17 => simulateGetClientPhotos
              case 18 => simulateGetPosts
              case 19 => simulateGetClientPhotos
              case _ =>
            }
          }
        }
      }


    case AddtoFriendList(c: Client) => addtoFriendList(c)
    case StopSystem => system.shutdown
    case _ =>
  }
}

object Data {
  val users = scala.collection.mutable.HashMap.empty[String, ActorRef]

  def getRandomUser = {
    val v = users.keys.toArray
    if(v.length > 0) {
      val i = scala.util.Random.nextInt(v.length)
      Some(Client(v(i), users(v(i))))
    } else {
      None
    }
  }
}
