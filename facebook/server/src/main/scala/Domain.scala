package fb

trait RestMessage

case class Timeout(message: String) extends RestMessage
case class TestMessage(randMsg: String) extends RestMessage