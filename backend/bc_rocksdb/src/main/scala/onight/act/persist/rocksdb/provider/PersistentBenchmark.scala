package net.atos.mts.akka.persistence.rocksdb

import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await

class MyPersistentActor (maxtodelete: Int) extends PersistentActor {
	
	override def persistenceId = "my-persistent-actor"
	var counter = 0;  
	def receiveRecover: Receive = {
    	case cmd => { println("RECOVER:" + cmd)}
	}

	def receiveCommand: Receive = {
    	case cmd =>
    		persist(cmd) {  
    		  counter+=1; e => println(e); sender() ! "OK"
    		  if(counter%1000==0)
    			  deleteMessages(getCurrentPersistentMessage.sequenceNr,true)
    		}
	}
}

object PersistentBenchmark extends App { 
	val system = ActorSystem("benchmark")
	
    val config = system.settings.config.getConfig("akka.persistence.benchmark")
    
	implicit val timeout = Timeout(config.getInt("timeout") seconds)
	
	val processor = system.actorOf(Props[MyPersistentActor])
	
	for(i <- 1 to config.getInt("numMessages")) {
		val formatted = String.format("%0"+config.getInt("sizeMessage")+"d", int2Integer(i))
		val future = processor ? formatted
		val result = Await.result(future, timeout.duration).asInstanceOf[String]
	}
	
	system.shutdown
}	