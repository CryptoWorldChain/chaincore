package org.brewchain.ecrypto.test

import org.brewchain.ecrypto.impl.EncInstance
import org.brewchain.core.crypto.HashUtil

object TestEnc {
  def main(args: Array[String]): Unit = {
    val enc = new EncInstance();
    val key = enc.genKeys();
    println("bcuid:" + key.getBcuid)
    println("pri:  " + key.getPrikey);
    println("pub:  " + key.getPubkey);
    println("addr: " + key.getAddress);
    val content = "测试";
    val hash = HashUtil.sha3(content.getBytes);
    println("hash: " + hash)
    val sign = enc.ecSign(key.getPrikey, hash);
    println("sign: " + enc.hexEnc(sign))
    val vsign = enc.ecVerify(key.getPubkey, hash, sign)
    println("verify:" + vsign)
  }
}