package org.brewchain.bcapi.utils

import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.otransio.api.PackHeader

object PacketIMHelper {

  implicit class FPImplicit(p: FramePacket) {
    def getFrom[A](): String = {
      p.getExtStrProp(PackHeader.PACK_FROM);
    }
    def getTo[A](): String = {
      p.getExtStrProp(PackHeader.PACK_TO);
    }
    def getURI[A](): String = {
      p.getExtStrProp(PackHeader.PACK_URI);
    }
  }
}

