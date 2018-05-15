package org.fc.brewchain.p22p.utils

import org.apache.commons.lang3.StringUtils
import org.slf4j.MDC
import onight.oapi.scala.traits.OLog
import org.fc.brewchain.p22p.node.router._
import org.fc.brewchain.p22p.pbgens.P22P.PSRouteMessage.PBNodeType
import org.fc.brewchain.p22p.pbgens.P22P.PSRouteMessage.PBNode
import org.fc.brewchain.p22p.pbgens.P22P.PSRouteMessage.PBDeepTreeSet
import org.fc.brewchain.p22p.pbgens.P22P.PSRouteMessage.PBNodeSet
import scala.collection.JavaConversions._
import com.google.protobuf.ByteString
import org.fc.brewchain.p22p.pbgens.P22P.PSRouteMessage.PBNodeSetOrBuilder

trait NodeSetHelper extends OLog {

  def scala2pb(node: IntNode): PBNode = {
    val pbo = PBNode.newBuilder()

    node match {
      case f: FullNodeSet =>
        pbo.setPbnt(PBNodeType.PB_FLAT_SET);
      case none: EmptySet =>
        pbo.setPbnt(PBNodeType.PB_EMPTY);
      case ns: NodeSet =>
        pbo.setPbnt(PBNodeType.PB_NODE_SET);
        val p = PBNodeSet.newBuilder();
        ns.nodes.map { x =>
          p.addNodes(scala2pb(x))
        }
        pbo.setData(p.build().toByteString())
      case subset: DeepTreeSet =>
        pbo.setPbnt(PBNodeType.PB_DEEP_TREE_SET);
        val tree = PBDeepTreeSet.newBuilder().setFromIdx(subset.fromIdx);
        val p = PBNodeSet.newBuilder();
        subset.treeHops.nodes.map { x =>
          p.addNodes(scala2pb(x))
        }
        tree.setTreeHops(p);
        pbo.setData(tree.build().toByteString())

      case subset: IntNode =>
        log.warn("unknow subset:" + subset)
    }

    pbo.build()
  }
  def pb2NodeSet(pbset: PBNodeSetOrBuilder): NodeSet = {
    val nodes = scala.collection.mutable.Set.empty[IntNode];
    pbset.getNodesList.map { x =>
      nodes.add(pb2scala(x));
    }
    NodeSet(nodes);
  }
  def pb2scala(pbo: PBNode): IntNode = {
    pbo.getPbnt match {
      case PBNodeType.PB_FLAT_SET =>
        FullNodeSet()
      case PBNodeType.PB_NODE_SET =>
        val nodes = scala.collection.mutable.Set.empty[IntNode];
        val pbset = PBNodeSet.newBuilder().mergeFrom(pbo.getData);
        pb2NodeSet(pbset);
      case PBNodeType.PB_DEEP_TREE_SET =>
        //subset: DeepTreeSet =>
        val treeset = PBDeepTreeSet.newBuilder().mergeFrom(pbo.getData);
        DeepTreeSet(treeset.getFromIdx, pb2NodeSet(treeset.getTreeHops))
      case PBNodeType.PB_EMPTY =>
        EmptySet()
      case _ =>
        EmptySet()
    }
  }

} 