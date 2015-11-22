import akka.actor._
import scala.concurrent.duration._
import spray.http._
import spray.httpx._
import spray.http.StatusCodes._
import spray.routing._
import MyJsonProtocol._
import fb._

trait PerRequest extends Actor with SprayJsonSupport {
  
  import context._

  def r: RequestContext
  def target: ActorRef
  def message: RestMessage 

  setReceiveTimeout(5.seconds)
  target ! message

  def receive = {
    case t: RestMessage => {
      r.complete(OK,t)
      stop(self)
    }
    case ReceiveTimeout => {
      r.complete(GatewayTimeout,Timeout("timed out"))
      stop(self)
    }
    case _ =>
  }
}

trait PerRequestCreator {
  this: Actor =>

  case class WithProps(r: RequestContext, props: Props, message: RestMessage) extends PerRequest {
    lazy val target = context.actorOf(props)
  }

  def perRequest(r: RequestContext, props: Props, message: RestMessage) =
    context.actorOf(Props(new WithProps(r, props, message)))
}