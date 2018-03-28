package org.fc.brewchain.p22p.node.router

import scala.collection.mutable.Set

object CMSCalc {

  case class CalcInfo(realn: Int = 10) {
    val delta = Math.sqrt(realn.asInstanceOf[Double]).asInstanceOf[Int];
    val maxDeep = realn / delta + 1
    // for bestN where n cannot sqrt with Int
//    val bestN = if (delta * delta == realn) realn else (delta + 1) * (delta + 1)
    val bestN = realn; //if (delta * delta == realn) realn else (delta + 1) * (delta + 1)
  }

  def deepLoop(startAt: Int = 0, currentDeep: Int = 0) //
  (implicit mapSets: scala.collection.mutable.Map[Int, Set[Int]],
    cc: CalcInfo): Unit = {
    //    println("deepLoop:" + startAt + ",curdeep=" + currentDeep + ",mmset" + mapSets)
    val curset =
      mapSets.get(startAt) match {
        case Some(set: Set[Int]) =>
          //          println("get Set:" + startAt + ":" + set)
          set
        case None =>
          //          println("startn:" + startAt)
          val s = Set.empty[Int]
          mapSets.put(startAt, s)
          s
      }
    if (currentDeep == 0) {
      curset.add((startAt + cc.bestN - 1) % cc.bestN)
    }
    curset.add((startAt + 1) % cc.bestN)
    var loop: Int = cc.delta - 2;

    while (loop > 0) {
      val deli = (startAt + cc.delta * loop) % cc.bestN;
      mapSets.get(deli) match {
        case Some(set: Set[Int]) =>
          set.add(startAt)
        case None =>
          //          mapSets.put(startAt, Set.empty[Int])/
          val s = Set(startAt)
          mapSets.put(deli, s)
          s
        //          println("get Nonthing:" + deli)
      }
      curset.add(deli)
      loop = loop - 1
    }
    //    println("deepLoop::" + startAt + ",curdeep=" + currentDeep + ",mmset" + curset)
  }
  def markCircleSets(n: Int = 10): scala.collection.mutable.Map[Int, Set[Int]] = {
    implicit val mapSets = scala.collection.mutable.Map.empty[Int, Set[Int]]; //leader==>follow
    var i = 0;
    implicit val cc = CalcInfo(n)
    while (i < cc.bestN) {
      deepLoop(i)
      i = i + 1
    }
    mapSets
  }

  def calcPath(startAt: Int = 0, currentDeep: Int = 0)(implicit mapSets: scala.collection.Map[Int, Set[Int]],
    cc: CalcInfo, result: Set[Int] = Set.empty[Int]): DeepTreeSet = {
    //    val curresult: (Int, Set[Any]) = Set.empty[(Int, Set[Any])]
    val stackSets: Set[(Int, Set[IntNode])] = Set.empty[(Int, Set[IntNode])]
    val curresult: Set[IntNode] = Set.empty[IntNode];
    stackSets.add((startAt, curresult))
    while (result.size < cc.bestN && stackSets.size > 0) {
      val t = stackSets.clone();
      stackSets.clear()
      t.map { _n =>
        val (popI, s) = _n
        result.add(popI);
        val nset = mapSets.get(popI).get;
        nset.map { f =>
          if (!result.contains(f)) {
            val newone = (f, Set.empty[IntNode])
            stackSets.add(newone)
            s.add(DeepTreeSet(f,NodeSet(newone._2)))
            //            s.add(newone)
            //            curresult.add(DeepTreeSet(popI));
            result.add(f);
          }
        }
      }
      //      println("loop:" + stackSets);
    }

    DeepTreeSet(startAt, NodeSet(curresult))
    //    if (currentDeep < cc.maxDeep) {
    //
    //      curresult.clone().map { f =>
    //        val bresult = calcPath(f.asInstanceOf[Int], currentDeep + 1)(mapSets, cc, result)
    //        curresult.remove(f)
    //        curresult.add(f -> bresult)
    //      }
    //    }

  }

  def calcRouteSets(startAt: Int = 0) //
  (implicit mapSets: scala.collection.Map[Int, Set[Int]]): //
  (DeepTreeSet, Set[Int]) = {
    val n = mapSets.size
    var i = 0;
    implicit val cc = CalcInfo(n)
    val result = Set.empty[Int]
    val treeresult = calcPath(startAt)(mapSets, cc, result);
    (treeresult, result)

  }
  def checkdeep(s: DeepTreeSet, deep: Int): Int = {
    var maxdeep = deep;
    s.treeHops.nodes.map { f =>
      f match {
        case vf: DeepTreeSet =>
          val d = checkdeep(vf, deep + 1)
          if (d > maxdeep) {
            maxdeep = d;
          }
        case _ => deep
      }
    }
    maxdeep
  }
  def main(args: Array[String]): Unit = {
    val n = 12;
    for (i <- 10 to 10) {
      val mm = markCircleSets(i);
      mm.map { f =>
//        println((f._1) + "==>" + f._2);
      }
      var maxlengthSet: Set[Int] = Set.empty[Int];
      var maxdeep = 0;
      mm.map { f =>
        val startAt = f._1;
        val (treere, result) = calcRouteSets(startAt)(mm)
        var mmaxdeep = 0
        //        treere.map(f => {
        val fs = checkdeep(treere, 0)
        if (fs > mmaxdeep) mmaxdeep = fs
        //        })
        if (maxdeep < mmaxdeep) {
          maxdeep = mmaxdeep
        }
        println("final result==>" + mmaxdeep + ":" + treere)
        if (result.size < i) {
          println("final ERROR result==>" + i + "==>" + result.size + ":" + result + "@" + startAt)
        }
        if (f._2.size > maxlengthSet.size) {
          maxlengthSet = f._2;
        }
      }
      val cc = CalcInfo(i);
      println("MaxLength=" + maxlengthSet.size + ",d=" + cc.delta + ",N=" + cc.bestN + ",maxdeep=" + maxdeep + ",n=" + i + ",==>" + maxlengthSet)
    }
  }

}



