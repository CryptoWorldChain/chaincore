package org.fc.brewchain.p22p.core

import scala.language.implicitConversions
import scala.collection.SeqView

import scala.collection.generic.IsSeqLike
import onight.oapi.scala.traits.OLog
import scala.collection.SeqLike
import scala.collection.mutable.Map
import scala.collection.IterableLike

object Votes {

  sealed abstract class VoteResult {
    def decision: Any
  }

  case class Converge(major: Any) extends VoteResult {
    override def decision = major
  }

  case class Undecisible() extends VoteResult {
    override def decision = None
  }

  case class NotConverge() extends VoteResult {
    override def decision = None
  }

  class VoteImpl[A, Repr](val coll: SeqLike[A, Repr]) {

    final def PBFTVote(choice: (A) => Option[Any] = { x => Some(x) }, total: Int = coll.size): VoteResult = {
      vote(2.0 / 3.0, total)(choice)
    }
    final def RCPTVote(choice: (A) => Option[Any] = { x => Some(x) }, total: Int = coll.size): VoteResult = {
      vote(8.0 / 10.0, total)(choice)
    }

    final def precentVote(precent: Float, choice: (A) => Option[Any] = { x => Some(x) }, total: Int = coll.size): VoteResult = {
      vote(precent, total)(choice)
    }
    private def ConvergeValue(v: Any): VoteResult = {
      v match {
        case Some(aa: Any) => Converge(aa)
        case _ => Converge(v)
      }
    }
    def vote(convprecent: Double, total: Int)(choice: (A) => Any): VoteResult = {
      val votemap = Map[Any, Int]();
      coll.foreach { cur =>
        val cur_choice = choice(cur)
        votemap.get(cur_choice) match {
          case Some(i: Int) => votemap.+=(cur_choice -> (i + 1))
          case _ => votemap.+=(cur_choice -> (1))
        }
      }
      if (votemap.size == 1 && total == 1) {
        return ConvergeValue(votemap.head._1)
      } else {
        votemap.map(kv => {
          if (kv._2 >= total * convprecent) {
            return ConvergeValue(kv._1)
          }
        })
        val f=(total * (1-convprecent) );
        if (total == coll.size ||((total - votemap.size) > f && votemap.size > f&&votemap.size>total/2)) {
          NotConverge()
        } else {
          Undecisible()
        }
      }
    }
  }
  implicit def vote[Repr, A](coll: Repr)(implicit fr: IsSeqLike[Repr]): VoteImpl[fr.A, Repr] = new VoteImpl(fr.conversion(coll))

}

