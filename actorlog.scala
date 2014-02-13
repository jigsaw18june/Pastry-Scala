import akka.actor._
import scala.reflect.io.Path

final class ActorLogCast(val ref: Any) {
  def !!(msg: Any)(implicit sender: ActorRef = Actor.noSender): Unit = {
	sender match {
      case Actor.noSender =>
        //println(" send " + msg); 
      case x =>
        //println(" send " + msg); 
        sender!Notification(msg, ref.toString, System.currentTimeMillis());  //ref.asInstanceOf[ActorSelection].!(msg)}
      }
      if (ref.isInstanceOf[ActorRef]) ref.asInstanceOf[ActorRef].!(msg) else ref.asInstanceOf[ActorSelection].!(msg)
  }
  
  def !(msg: Any)(implicit sender: ActorRef = Actor.noSender): Unit = {
    sender match {
      case Actor.noSender =>
        //println(" send " + msg); 
      case x =>
        //println(" send " + msg); 
        sender!Notification(msg, ref.toString, System.currentTimeMillis());  //ref.asInstanceOf[ActorSelection].!(msg)}
      }
      if (ref.isInstanceOf[ActorRef]) ref.asInstanceOf[ActorRef].!(msg) else ref.asInstanceOf[ActorSelection].!(msg)
  }
}

// Implicit casts for ActorRef and ActorSelection
trait Implicits {
  
  implicit def actorRef2ActorLog(ref: ActorRef): ActorLogCast = new ActorLogCast(ref)
  implicit def actorSelection2ActorLog(ref: ActorSelection): ActorLogCast = new ActorLogCast(ref)
}

object Implicits extends Implicits

case class Notification(val value: Any, val dest: String, val time: Long) {}

trait ActorLog extends Actor with Implicits { 
  val file = Path(self.path.name + ".log").toFile
  file.printlnAll(System.currentTimeMillis() + "|" + "INFO" + "|" + "Actor Name:" + self.path.name + " Time:" + System.currentTimeMillis())
  
  // needs to wrap notifiable around receive method defined in the class extending ActorLog
  def notifiable(receive: Receive): Receive = {
        case Notification(value, dest, time) =>
          //  println(time + ". send to: " + dest  + " Notification " + value)
            file.appendAll(time + "|" + self.path.name + "|" + "SEND" + "|" + dest + "|" + value + "\n")
        case msg =>
            //println(System.currentTimeMillis() + " Receive " + msg)
            file.appendAll(System.currentTimeMillis() + "|" + self.path.name + "|" + "RECV" + "|" + sender.toString + "|" + msg + "\n")
            receive(msg)
  }
  
  // user need to call this method if he wants to log the exception
  def exception(e: Throwable , values: Any*) =
  {
  		file.appendAll(System.currentTimeMillis() + "|" + self.path.name + "|" + "EXCP" + "|" + e.getLocalizedMessage() + "|" + "\n")
		file.appendAll(System.currentTimeMillis() + "|" + self.path.name + "|" + "EXCP" + "|" + e.getStackTraceString + "|" + "\n")
  }
  
  // user calls to log any message
  def Log(info: String) =
  {
    file.appendAll(System.currentTimeMillis() + "|" + self.path.name + "|" + "INFO" + "|" + info + "\n")
  }
}
