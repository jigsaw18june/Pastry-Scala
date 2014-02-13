import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationLong
import scala.util.Random
//import Implicits.actorRef2ContextualActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.PoisonPill

object gossip {
  
  trait testGene
  case class converged(id: Int) extends testGene
  case class rumor(id: Int, message: Int) extends testGene
  case class pushSum(s: Double, w: Double) extends testGene
  case class create extends testGene
  case class terminate extends testGene
  case class removeNeighbor(removeId: Int) extends testGene
  case class result(value: Double) extends testGene
  
  
  object Topology extends Enumeration {
    type Topology = Value
    val Line, Full, Grid2D, imp2D = Value
  }
  
  object Algorithm extends Enumeration {
    type Algorithm = Value
    val Gossip, PushSum = Value
  }
  
  import Topology._
  import Algorithm._
  import Implicits._
  
  
  class Worker(id: Int, topology: Topology, algorithm: Algorithm, numActors: Int)  extends ActorLog {
	  
      var msgCounter: Int = 0
      var termCounter = 0
      var message: Int = 0
      var isConverged: Boolean = false
      var hasMsg: Boolean = false
      val neighbors = new ArrayBuffer[Int]()
      var s: Double = id + 1
      var w: Double = 1
      
      set_topology()
      
      def set_topology() = {
        topology match {
          case Grid2D =>
            var dim: Int = Math.sqrt(numActors).toInt
            // check for top neighbor
            if (id - dim >= 0)  neighbors += id-dim
            // check for bottom neighbor
            if (id + dim < dim*dim) neighbors += id + dim
            // check for the left neighbor
            if (id%dim != 0)  neighbors += id-1
            // check for the right neighbor
            if (id%dim != dim-1)  neighbors += id+1
          case Line =>
            // check for left neighbor
            if( id-1 >= 0) neighbors+= id-1
            // check for right neighbor
            if(id+1 <= numActors-1) neighbors+= id+1
          case imp2D =>
             var dim: Int = Math.sqrt(numActors).toInt
            // check for top neighbor
            if (id - dim >= 0)  neighbors += id-dim
            // check for bottom neighbor
            if (id + dim < dim*dim) neighbors += id + dim
            // check for the left neighbor
            if (id%dim != 0)  neighbors += id-1
            // check for the right neighbor
            if (id%dim != dim-1)  neighbors += id+1
            // for one extra neighbor
            var t = Random.nextInt(numActors)
            while(t == id)
              t = Random.nextInt(numActors)
            neighbors+= t
        }
      }
      
      def getdestination(): Int = {
        var t:Int = 0
        topology match {
            case Line =>
              t = if (Math.random() > 0.5 && neighbors.length > 1) neighbors(1) else neighbors(0)
            case Full =>
              t = Random.nextInt(numActors)
              while(t == id)
                t = Random.nextInt(numActors)
            case Grid2D =>
              t = neighbors(Random.nextInt(neighbors.length))
            case imp2D =>
              t = neighbors(Random.nextInt(neighbors.length))
        }
        return t
      }

      def receive = notifiable {
      case rumor(source, message) =>
        if (isConverged == false) {
          msgCounter += 1
          this.message = message
          if (msgCounter <= GOSSIP_TERMINATION_COUNT - 1) {
            for (i <- 0 until numMessages)
              context.actorSelection("../" + getdestination().toString()) ! rumor(id, message)
          } else {
            context.parent ! converged(id)
            self ! PoisonPill
            isConverged = true
          }
        }
        
        case removeNeighbor(removeId) =>
        neighbors -= removeId
      
      }
    }
  
  class Master(topology: Topology, algorithm: Algorithm, numActors: Int) extends ActorLog {

    val convergedActors = new ListBuffer[Int]()
    val termFailed = new ListBuffer[Int]()
    val message: Int = 10
    var terminalCount: Int = numActors
    var startTime:Long = 0
    var convergedCount: Int = 0
    
    def receive = notifiable {
      case `create` =>
        // create the actors
        for(i <- 0 until numActors)
        context.actorOf(Props(new Worker(i, topology, algorithm, numActors)), i.toString);
        // start the algorithm
        startTime = System.currentTimeMillis
        algorithm match {
          case Gossip =>
            numMessages = 4
            context.actorSelection(Random.nextInt(numActors).toString())!rumor(0, message)
          case PushSum =>
            context.actorSelection(Random.nextInt(numActors).toString())!pushSum(0, 0)
        }
        
      case converged(id) =>
        //increase the converged actor count
        convergedCount+=1
        //Add the converged actor to the list
        convergedActors += id
        if (convergedCount == terminalCount)
        {
          var time_diff = System.currentTimeMillis() - startTime
          //println("System Terminating" + " count " + convergedCount)
          println("Convergence time is: " + time_diff.millis)
          self ! PoisonPill
          //context.system.shutdown
        }
        
       case result(value) =>
         var time_diff = System.currentTimeMillis() - startTime
         //println("The average is " + value)
         println("Convergence time is: " + time_diff.millis)
         exit
    }
    
    override def postStop() {
    	context.system.shutdown
    }
    
  }
  
  def getNextPerfectSquare (num:Int):Int = {
      var nextNum: Double = math.sqrt(num)
      var temp = nextNum.toInt
      if (nextNum % 1 != 0) temp+=1
      (temp*temp)
  }

    var numMessages = 1
    val GOSSIP_TERMINATION_COUNT = 5
    val PUSH_SUM_TERM_COUNT = 3
    val failureN = 5
    

   
  def main(args: Array[String]) {

      var numActors = 4
      val algorithm = Algorithm.Gossip
      val topology = Topology.Full
      println("Algorithm: " + algorithm + "; Topology: " + topology + "; No. of Nodes: " + numActors)
      if(topology == Grid2D || topology == imp2D)
      {
        numActors = getNextPerfectSquare(numActors)
      }
      val system = ActorSystem("Gossip-Pushsum")
      val master = system.actorOf(Props(new Master(topology, algorithm, numActors)), "master")
      master!create
    }
}
