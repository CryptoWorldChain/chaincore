package org.fc.brewchain.bcapi.crypto

import java.math.BigInteger
import org.apache.commons.lang3.StringUtils
import java.security.SecureRandom

trait BitMap {

  private val StrMapping = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM0123456789".toCharArray();
  private val radix = StrMapping.length;
  private val modx = BigInt(radix);

  def hexToInt(ch: Char): Int = {
    if (ch >= '0' && ch <= '9') ch - '0';
    else if (ch >= 'A' && ch <= 'F') ch - 'A' + 10;
    else if (ch >= 'a' && ch <= 'f') ch - 'a' + 10;
    else 0;
  }

  def int2Str(vi: Int): String = {
    var v = vi;
    val sb = new StringBuffer();
    while (v > 0) {
      sb.append(StrMapping.charAt(v % radix));
      v /= radix;
    }
    sb.toString();
  }

  def hexToMapping(lbi: BigInt) = {
    var v = lbi;
    val sb = new StringBuffer();
    //    println("modx="+modx)

    while (v.bitCount > 0) {
      //      println("v="+v.mod(modx))
      sb.append(StrMapping.charAt(v.mod(modx).intValue()));
      v = v / modx;
    }
    sb.reverse().toString();
  }

  def mapToHex(str: String): BigInt = {
    var v = BigInt(0);
    str.map { ch =>
      v = v % modx;
    }
    v;
  }

  def mapToBigInt(strl: String): BigInt = {
    var bi: BigInt = BigInt(0);
    strl.map { x =>
      //      bi = bi.multiply(modx).add( BigInt( StrMapping.indexOf(x), 10))
      bi = bi * modx + BigInt(StrMapping.indexOf(x))
      //      println("x=" + x + "==>" + new BigInteger("" + StrMapping.indexOf(x), 10)+"...bi="+bi.toString(16))
    }
    bi
  }

  
}
object test1 extends BitMap{
  def main(args: Array[String]): Unit = {

    val hexstr = "6647dccf7908a611dd50fa74548afd94164be77dcb9a7e455e8543c500ed7258";
    //    val hexstr = "100A";
    var bi = new BigInteger("0");
    bi = bi.setBit(8);
    println("bi=" + bi.toString(16) + ",bitcount=" + bi.bitCount + ",bitlen=" + bi.bitLength);
    println("biequal::" + StringUtils.equalsIgnoreCase(bi.toString(16), hexstr) + ":len=" + hexstr.length() + "==>" + bi.bitCount())
    val bix = hexToMapping(bi);
    println("bix::" + bix);
    val bistr = mapToBigInt("rcsiHZXq2BzAS86xBKwhdJsiK5pzRSggZgKLoqj");
    println("bistr=" + bistr.toString(16));
    println("biequal::" + StringUtils.equals(bistr.toString(16), (hexstr)) + ":len=" + hexstr.length())

    val bihexstr = mapToBigInt(hexstr);
    println("bihexstr=" + bihexstr.toString(16) + ",bitcount=" + bihexstr.bitCount);
    

  }
}