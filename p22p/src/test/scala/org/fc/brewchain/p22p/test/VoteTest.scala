package org.fc.brewchain.p22p.test

import org.fc.brewchain.p22p.core.Votes._
import org.fc.brewchain.bcapi.URLHelper
import java.net.URL

//import scala.collection.Searching._

object VoteTest {
  def main(args: Array[String]): Unit = {
    val l = List("a", "a", "a")
    println("pbft.vote=" + l.PBFTVote(f => Some(f), 5));
    println("rcpt.vote=" + l.RCPTVote().decision);
    println("rcpt.vote=" + l.precentVote(0.6F).decision);

    URLHelper.init()

    val u = new URL("tcp://127.0.0.1:5100");
    println("u="+u)
  }

}