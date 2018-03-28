package org.fc.brewchain.p22p.node.router

import scala.collection.immutable.Set
import onight.oapi.scala.traits.OLog

sealed trait IntNode

case class EmptySet() extends IntNode
case class FullNodeSet() extends IntNode
case class TreeNodeSet(nodes: Set[TreeNodeSet]) extends IntNode

//only one layer ,for Random Node Router
case class FlatSet(fromIdx: Int, nextHops: BigInt = BigInt(0)) extends IntNode
// Tree Router , form Circle Node Router
case class NodeSet(nodes: scala.collection.mutable.Set[IntNode]) extends IntNode

case class DeepTreeSet(fromIdx: Int, treeHops: NodeSet) extends IntNode 
 

