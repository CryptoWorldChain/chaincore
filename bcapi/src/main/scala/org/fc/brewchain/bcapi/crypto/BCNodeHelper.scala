package org.fc.brewchain.bcapi.crypto

import java.security.SecureRandom
import onight.tfw.outils.serialize.SessionIDGenerator
import scala.BigInt
import scala.math.BigInt.int2bigInt

object BCNodeHelper {

  private val StrMapping = "qwertyuiFGHJKLZXCopzxcvbnmQWERTY567238DasdfghjklVBNM014UIOPAS9".toCharArray();
  private val mapIdex = StrMapping.map { ch => ch -> StrMapping.indexOf(ch) }.toMap[Char, Int]
  private val radix = StrMapping.length;
  private val modx = BigInt(radix);

  def bytesEnc(bb: Array[Byte]): String = {
    bb.foldLeft("")((a, b) => a + StrMapping.charAt(((b >> 4) & 0x0F) % radix) + StrMapping.charAt((b & 0x0F) % radix))
  }

  def bytesDec(str: String): Array[Byte] = {
    str.map { ch => StrMapping.indexOf(ch).asInstanceOf[Byte] }.toArray
  }

  def mixStr(str: String, key: String): String = {
    val offset: Int = Math.abs((key.hashCode() + 100) % radix);
    str.map { ch =>
      mapIdex.get(ch) match {
        case Some(i) =>
          StrMapping.apply((i + offset) % radix)
        case _ => ch
      }
    }.toString()
  }

  def decMixStr(mixstr: String, key: String): String = {
    val offset: Int = Math.abs((key.hashCode() + 100) % radix);
    mixstr.map { ch =>
      mapIdex.get(ch) match {
        case Some(i) =>
          StrMapping.apply((i - offset + radix) % radix)
        case _ => ch
      }
    }.toString()
  }

//  def nextUID(key: String = "BCC2018"): String = {
//    //    val id = UUIG.generate()
//    val ran = new SecureRandom(key.getBytes);
//    //ran.generateSeed(System.currentTimeMillis().asInstanceOf[Int])
//    val eckey = new ECKey(ran);
//    val encby = HashUtil.ripemd160(eckey.getPubKey);
//    //    println("hex=" + Hex.toHexString(encby))
//    val i = encby.foldLeft(BigInt(0))((a, b) => a * 0xFF + BigInt(Math.abs(b)));
//    //    println("i=" + i)
//    val id = BitMap.hexToMapping(i)
//    val mix = mixStr(id, key);
//    mix + SessionIDGenerator.genSum(mix)
//  }
//  def checkUID(str: String, key: String = "BCC2018"): Boolean = {
//    SessionIDGenerator.checkSum(str)
//  }
//  def main(args: Array[String]): Unit = {
//
//    val ostr = "hello world";
//    //    val hexstr = "100A";
//    val bstr = bytesEnc(ostr.getBytes)
//    println("bstr=" + bstr);
////    println("Hex::" + Hex.toHexString(ostr.getBytes));
//    val mixstr = mixStr(ostr, "abc");
//    println("Mix::" + mixstr);
//    println("DMix:" + decMixStr(mixstr, "abc"));
//    val strstr = new String(bytesDec(bstr))
//    //    println("oo:" + strstr + "::" + StringUtils.equals(strstr, ostr))
//    for (i <- 1 to 100) {
//      val uid = nextUID() + StrMapping(Math.abs(Math.random() * 10000 % radix).asInstanceOf[Int]);
//      println("nextUID==" + uid + ",check=" + checkUID(uid));
//
//    }
//  }
}