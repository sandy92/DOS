import akka.actor._
import spray.routing._
import spray.http._
import spray.http.StatusCodes._
import spray.httpx._
import spray.util._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
//import spray.httpx.SprayJsonSupport._
import MediaTypes._
import MyJsonProtocol._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends HttpService with Actor with FormDataUnmarshallers with PerRequestCreator {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test

  //import spray.httpx.SprayJsonSupport._

  implicit def actorRefFactory = context

  implicit val jsonRejectionHandler = RejectionHandler {
    case ValidationRejection(msg, cause) :: Nil =>
    complete(BadRequest, """{ "error":"""" + msg + """"}""")
  }

  implicit def myExceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: IllegalArgumentException =>
        complete(BadRequest, """{ "error":"""" + e.getMessage + """"}""")
    }

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)

  val myRoute =
    path("") {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            """{ "message":"Hello World!" }"""
          }
        }
      }
    } ~
    pathPrefix("user") {
      pathEnd {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              """{ "message":"Hello User!" }"""
            }
          }
        }
      } ~
      pathPrefix("create") {
        pathEnd {
          put {
            formFields('name, 'email ,'age.as[Int], 'publicKey) { (name, email, age, publicKey) =>
              user {
                CreateUser(name, email,age, publicKey)
              }
            }
          }
        }
      } ~
      pathPrefix("[0-9]+".r) { id =>
        pathEnd {
          get {
            user {
              GetUserDetails(id)
            }
          } ~ 
          delete {
            user {
              DeleteUser(id)
            }
          }
        } ~
        pathPrefix("likes") {
          pathEnd {
            get {
              user {
                GetLikesOf(id)
              }
            }
          }
        } ~
        pathPrefix("friends") {
          pathEnd {
            get {
              user {
                GetFriendsList(id)
              }
            } ~ 
            put {
              formFields('requestedBy) { (requestedBy) =>
                user {
                  AddFriend(id, requestedBy)
                }
              }
            }
          }
        } ~
        pathPrefix("posts") {
          pathEnd {
            get {
              user {
                GetPosts(id)
              }
            } ~
            put {
              formFields('message, 'postedBy) { (message, postedBy) => 
                fbPost {
                  CreatePost(message, postedBy, id)
                } 
              }
            }
          }
        } ~
        pathPrefix("albums") {
          pathEnd {
            get {
              user {
                GetAlbumsList(id)
              }
            }
          }
        }
      }
    } ~
    pathPrefix("page") {
      pathEnd {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              """{ "message":"Hello Page!" }"""
            }
          }
        }
      } ~
      pathPrefix("create") {
        pathEnd {
          put {
            formFields('name, 'webAddress ,'about) { (name, webAddress, about) =>
              page {
                CreatePage(name, webAddress,about)
              }
            }
          }
        }
      } ~
      pathPrefix("[0-9]+".r) { id =>
        pathEnd {
          get {
            page {
              GetPageDetails(id)
            }
          } ~ 
          delete {
            page {
              DeletePage(id)
            }
          }
        } ~ 
        pathPrefix("likes") {
          pathEnd {
            get {
              page {
                GetLikesOf(id)
              }
            }
          }
        } ~
        pathPrefix("posts") {
          pathEnd {
            get {
              page {
                GetPosts(id)
              }
            } ~
            put {
              formFields('message, 'postedBy) { (message, postedBy) => 
                fbPost {
                  CreatePost(message, postedBy, id)
                } 
              }
            }
          }
        } ~
        pathPrefix("albums") {
          pathEnd {
            get {
              page {
                GetAlbumsList(id)
              }
            }
          }
        }
      }
    } ~
    pathPrefix("album") {
      pathEnd {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              """{ "message":"Hello Album!" }"""
            }
          }
        }
      } ~
      pathPrefix("create") {
        pathEnd {
          put {
            formFields('name, 'profileID) { (name, profileID) =>
              album {
                CreateAlbum(name, profileID)
              }
            }
          }
        }
      } ~
      pathPrefix("[0-9]+".r) { id =>
        pathEnd {
          get {
            album {
              GetAlbumDetails(id)
            }
          } ~ 
          delete {
            album {
              DeleteAlbum(id)
            }
          }
        } ~
        pathPrefix("likes") {
          pathEnd {
            get {
              album {
                GetLikedBy(id)
              }
            }
          }
        } ~
        pathPrefix("photos") {
          pathEnd {
            get {
              album {
                GetPhotosFromAlbum(id)
              }
            }
          }
        }
      }
    } ~
    pathPrefix("photo") {
      pathEnd {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              """{ "message":"Hello Photo!" }"""
            }
          }
        }
      } ~
      pathPrefix("upload") {
        pathEnd {
          put {
            formFields('name, 'profileID, 'image.as[Array[Byte]], 'albumID) { (name, profileID, image, albumID) =>
              photo {
                UploadPhoto(name, profileID, image, albumID)
              }
              /*respondWithMediaType(`application/json`) { // XML is marshalled to `text/xml` by default, so we simply override here
                complete {
                  val x = new sun.misc.BASE64Encoder().encode(image)
                  """{ "message": """" + x + """" }"""
                }
              }*/
            }
          }
        }
      } ~
      pathPrefix("[0-9]+".r) { id =>
        pathEnd {
          get {
            photo {
              GetPhotoDetails(id)
            }
          } ~ 
          delete {
            photo {
              DeletePhoto(id)
            }
          }
        } ~
        pathPrefix("likes") {
          pathEnd {
            get {
              photo {
                GetLikedBy(id)
              }
            }
          }
        }
      }
    } ~
    pathPrefix("post") {
      pathEnd {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              """{ "message":"Hello Post!" }"""
            }
          }
        }
      } ~
      pathPrefix("[0-9]+".r) { id =>
        pathEnd {
          get {
            fbPost {
              GetPostDetails(id)
            }
          } ~ 
          delete {
            formFields('profileID) { profileID =>
              fbPost {
                DeletePost(id,profileID)
              }
            }
          }
        }
      }
    }
    path("test") {
      get {
        parameters('id) { id =>
          testMessage {
            TestMessage(id)
          }
        }
      }
    } ~
    path("na") {
      get {
        parameters('id) { id =>
          testMessage {
            TestMessage((id.toInt+1).toString)
          }
        }
      }
    }

    def testMessage(message : RestMessage): Route =
      ctx => perRequest(ctx, Props[Test], message)

    def user(message : RestMessage): Route =
      ctx => perRequest(ctx, Props[User], message)

    def page(message : RestMessage): Route =
      ctx => perRequest(ctx, Props[Page], message)

    def album(message : RestMessage): Route =
      ctx => perRequest(ctx, Props[Album], message)

    def photo(message : RestMessage): Route =
      ctx => perRequest(ctx, Props[Photo], message)

    def fbPost(message : RestMessage): Route =
      ctx => perRequest(ctx, Props[Post], message)
}